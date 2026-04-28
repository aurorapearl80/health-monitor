package com.monitor.health.services;
import android.annotation.SuppressLint;
import android.app.Service;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.media.Ringtone;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.BatteryManager;
import android.os.Build;
import android.os.IBinder;
import android.provider.Settings;
import android.util.Log;
import android.widget.Toast;

import com.monitor.health.ApiClient;
import com.monitor.health.Constant;
import com.monitor.health.request.SendAlarmRequest;
import com.monitor.health.utility.DeviceUtils;

import java.util.Locale;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class FallDetectionService extends Service implements SensorEventListener {

    private SensorManager sensorManager;
    private Sensor accelerometer;

    private static final float FALL_THRESHOLD = 19.0f; // m/sÂ² â€” adjust as needed
    private static final String TAG = "FallDetectionService";
    String _model;                     // e.g., SM-G925I
    String _maker;              // e.g., Samsung
    String osVersion;       // e.g., 4.4, 12, 13
    String _country;
    String androidId;
    private static final int MAX_RETRY_COUNT = 3; // Set max retry attempts
    private int retryCount = 0;
    int batteryPercent = 0;
    SharedPreferences prefs;

    @Override
    public void onCreate() {
        super.onCreate();

        //androidId = Settings.Secure.getString(getContentResolver(), Settings.Secure.ANDROID_ID);
        androidId = DeviceUtils.getIMEI(getApplicationContext());

        _model = Build.MODEL;                     // e.g., SM-G925I
        _maker = Build.MANUFACTURER;              // e.g., Samsung
        osVersion = Build.VERSION.RELEASE;       // e.g., 4.4, 12, 13
        _country = Locale.getDefault().getCountry(); // e.g., PR empty for now

        BatteryManager batteryManager = (BatteryManager) getSystemService(BATTERY_SERVICE);
        IntentFilter ifilter = new IntentFilter(Intent.ACTION_BATTERY_CHANGED);
        Intent batteryStatus = registerReceiver(null, ifilter);
        int level = batteryStatus.getIntExtra(BatteryManager.EXTRA_LEVEL, -1);
        int scale = batteryStatus.getIntExtra(BatteryManager.EXTRA_SCALE, -1);
        batteryPercent = (level * 100) / scale;

       prefs = getSharedPreferences("LocationPrefs", MODE_PRIVATE);

        sensorManager = (SensorManager) getSystemService(SENSOR_SERVICE);
        if (sensorManager != null) {
            accelerometer = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
            if (accelerometer != null) {
                sensorManager.registerListener(this, accelerometer, SensorManager.SENSOR_DELAY_NORMAL);
            }
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (sensorManager != null) {
            sensorManager.unregisterListener(this);
        }
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        // Service keeps running until explicitly stopped
        return START_STICKY;
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null; // Not using binding
    }

    @Override
    public void onSensorChanged(SensorEvent event) {
        //Log.d(TAG, "Fall detected! is running");
        if (event.sensor.getType() == Sensor.TYPE_ACCELEROMETER) {
            float x = event.values[0];
            float y = event.values[1];
            float z = event.values[2];

            // Compute magnitude of acceleration
            float acceleration = (float) Math.sqrt(x * x + y * y + z * z);

            if (acceleration > FALL_THRESHOLD) {
                //Toast.makeText(this, "Sending SOS", Toast.LENGTH_SHORT).show();
                //Log.d(TAG, "Fall detected!");

                // Send broadcast to MainActivity
                Intent fallIntent = new Intent("com.monitor.health.FALL_DETECTED");
                fallIntent.putExtra("acceleration", acceleration);
                sendBroadcast(fallIntent);
                //sendAlarm();
            }
        }
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {
        // We can ignore this for now
    }

    public void sendAlarm() {

        double latitude = Double.longBitsToDouble(prefs.getLong("latitude", Double.doubleToRawLongBits(0)));
        double longitude = Double.longBitsToDouble(prefs.getLong("longitude", Double.doubleToRawLongBits(0)));
//        SendAlarmRequest sendAlarmRequest = new SendAlarmRequest(latitude, longitude, androidId, 1, 1, batteryPercent, true,
//                _model, _maker,
//                "0", _country);
        // sample long:148.752 Lat: 87588.701
        SendAlarmRequest sendAlarmRequest = new SendAlarmRequest(latitude, longitude, androidId, 1, 1, batteryPercent, true,
                _model, _maker,
                "0", _country);
        Call<Object> call = ApiClient.getUserService(Constant.BASE_URL_BGM, Constant.TOKEN_DR_WATCH_API, androidId)
                .sendAlarm(sendAlarmRequest);

        call.enqueue(new Callback<Object>() {
            @Override
            public void onResponse(Call<Object> call, Response<Object> response) {
                Log.d(TAG, "SDK_003_CREATE_UPDATE_USER DRS_020_SEND_ALARM code " + response.code());
                Log.d(TAG, "SDK_003_CREATE_UPDATE_USER DRS_020_SEND_ALARM body " + response.body());
                Log.d(TAG, "SDK_003_CREATE_UPDATE_USER DRS_020_SEND_ALARM toString " + response.toString());
                Log.d(TAG, "SDK_003_CREATE_UPDATE_USER DRS_020_SEND_ALARM message " + response.message());
                // Hide progress
                if (response.code() == 200) {
                    Log.d(TAG, "Alarm sent successfully!");
                    retryCount = 0; // Reset retry count on success
                    playNotificationSound();
                } else {
                    handleRetry();
                }
            }

            @Override
            public void onFailure(Call<Object> call, Throwable t) {
                handleRetry();
            }
        });
    }
    @SuppressLint("LongLogTag")
    private void handleRetry() {
        if (retryCount < MAX_RETRY_COUNT) {
            retryCount++;
            Log.d(TAG, "Retrying sendAlarm... Attempt " + retryCount);
            sendAlarm();
        } else {
            Log.d(TAG, "Max retry attempts reached. Failed to send alarm.");
        }
    }

    private void playNotificationSound() {
        try {
            Uri notification = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION);
            Ringtone ringtone = RingtoneManager.getRingtone(getApplicationContext(), notification);
            ringtone.play();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}