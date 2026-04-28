package com.monitor.health.services;
import android.annotation.SuppressLint;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Build;
import android.os.Bundle;
import android.os.IBinder;
import android.util.Log;
import android.widget.Toast;

import androidx.core.app.NotificationCompat;

import com.monitor.health.ApiClient;
import com.monitor.health.Constant;
import com.monitor.health.ReadingsRequest;
import com.monitor.health.database.DatabaseClient;
import com.monitor.health.model.HeartRateEntity;
import com.monitor.health.model.Reading;
import com.monitor.health.utility.DeviceUtils;

import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.TimeZone;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class HeartRateService extends Service {

    private static final String TAG = "HeartRateService";
    private static final String CHANNEL_ID = "hr_channel";

    private SensorManager sensorManager;
    private Sensor heartRateSensor;

    private float lastSentHeartRate = -1;
    private long lastSentTime = 0;

    private Sensor stepDetectorSensor;

    //Step
    private static final float STEP_THRESHOLD = 11f;  // Tune as needed
    private long lastStepTime = 0;
    private int stepCount = 0;

    private String androidId;

    DatabaseClient databaseClient;

    // External service actions
    public static final String HEART_ACTION = "com.hsciot.healthy.action.start";
    public static final String BLOOD_HEART_ACTION = "com.monitor.health.blood.action.start";
    public static final int ACTION_HEART = 1 << 11;
    public static final int ACTION_BLOODO = 1 << 28;

    // Additional broadcast from the system
    public static final String ACTION_IOTSERVICES = "com.monitor.health.iotservices";

    // Forwarding actions
    public static final String HEART_RATE_UPDATE = "com.monitor.health.HEART_RATE_UPDATE";
    public static final String BLOOD_OXYGEN_UPDATE = "com.monitor.health.BLOOD_OXYGEN_UPDATE";



    private SensorEventListener heartRateListener = new SensorEventListener() {
        @Override
        public void onSensorChanged(SensorEvent event) {
//            if (event.values.length > 0) {
//                float heartRate = event.values[0];
//                //Log.d(TAG, "HR: " + heartRate);
//                Intent intent = new Intent("com.monitor.health.HEART_RATE_UPDATE");
//                intent.putExtra("heartRate", heartRate);
//                sendBroadcast(intent);
//
//                if (heartRate > 0 && heartRate != lastSentHeartRate) {
//                    long currentTime = System.currentTimeMillis();
//                    if (currentTime - lastSentTime > 10000) {
//                        lastSentHeartRate = heartRate;
//                        lastSentTime = currentTime;
//
//                        // TODO: Send to API here if needed
//                        Toast.makeText(getApplicationContext(), "Heart Rate: " + heartRate, Toast.LENGTH_SHORT).show();
//                    }
//                }
//            }

            if (event.sensor.getType() == Sensor.TYPE_HEART_RATE) {
                float heartRate = event.values[0];
                Log.d(TAG, "HR: " + heartRate);

                // Broadcast HR
                Intent hrIntent = new Intent("com.example.myapplication.HEART_RATE_UPDATE");
                hrIntent.putExtra("heartRate", heartRate);
                sendBroadcast(hrIntent);
                //float heartRate = event.values[0];
                //Log.d(TAG, "HR: " + heartRate);
                Intent intent = new Intent("com.monitor.health.HEART_RATE_UPDATE");
                intent.putExtra("heartRate", heartRate);
                sendBroadcast(intent);

                if (heartRate > 0 && heartRate != lastSentHeartRate) {
                    long currentTime = System.currentTimeMillis();
                    if (currentTime - lastSentTime > 10000) {
                        lastSentHeartRate = heartRate;
                        lastSentTime = currentTime;
                        //Toast.makeText(getApplicationContext(), "Heart Rate: " + heartRate, Toast.LENGTH_SHORT).show();
                       // sendHeartRate(heartRate);
                        //saveHeartRate(heartRate);
                        // TODO: Send to API here if needed

                    }
                }

//            } else if (event.sensor.getType() == Sensor.TYPE_STEP_DETECTOR) {
            }
//            if (event.sensor.getType() == Sensor.TYPE_ACCELEROMETER) {
//                float x = event.values[0];
//                float y = event.values[1];
//                float z = event.values[2];
//
//                float magnitude = (float) Math.sqrt(x * x + y * y + z * z);
//
//                if (magnitude > STEP_THRESHOLD) {
//                    long now = System.currentTimeMillis();
//                    if (now - lastStepTime > 500) { // debounce
//                        lastStepTime = now;
//                        stepCount++;
//
//                        Intent intent = new Intent("com.monitor.health.STEP_UPDATE");
//                        intent.putExtra("steps", stepCount);
//                        sendBroadcast(intent);
//
//                        Log.d(TAG, "Step detected! Total: " + stepCount);
//                    }
//                }
//            }

            if (event.sensor.getType() == Sensor.TYPE_STEP_DETECTOR) {
                Toast.makeText(getApplicationContext(), "TYPE_STEP_DETECTOR: " + Sensor.TYPE_STEP_DETECTOR, Toast.LENGTH_SHORT).show();
            }
        }

        @Override
        public void onAccuracyChanged(Sensor sensor, int accuracy) {}
    };

    @SuppressLint({"HardwareIds", "UnspecifiedRegisterReceiverFlag"})
    @Override
    public void onCreate() {
        super.onCreate();
        createNotificationChannel();
        startForeground(2, createNotification());

        databaseClient = DatabaseClient.getInstance(getApplicationContext());

        //saveHeartRate(3);
        sensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);
        heartRateSensor = sensorManager.getDefaultSensor(Sensor.TYPE_HEART_RATE);
        // Heart rate
//        if (heartRateSensor != null) {
//            sensorManager.registerListener(heartRateListener, heartRateSensor, SensorManager.SENSOR_DELAY_NORMAL);
//            Log.d(TAG, "Heart rate sensor listener registered");
//        } else {
//            Log.e(TAG, "Heart rate sensor not available");
//            stopSelf();
//        }
//
//        // Step Detector
//        stepDetectorSensor = sensorManager.getDefaultSensor(Sensor.TYPE_STEP_DETECTOR);
//        if (stepDetectorSensor != null) {
//            sensorManager.registerListener(heartRateListener, stepDetectorSensor, SensorManager.SENSOR_DELAY_NORMAL);
//            Log.d(TAG, "Step detector sensor listener registered");
//        } else {
//            Log.e(TAG, "Step detector sensor not available");
//        }

        // Register receivers for heart rate, blood oxygen, and IoT services
        IntentFilter filter = new IntentFilter();
        filter.addAction("com.monitor.health.action.result");
        filter.addAction("com.monitor.health.blood.action.result");
        registerReceiver(externalReceiver, filter);

        // âœ… Register IoT service receiver
        IntentFilter iotFilter = new IntentFilter();
        iotFilter.addAction(ACTION_IOTSERVICES);
        registerReceiver(dataReceiver, iotFilter);

        // Start measurement services
        triggerHeartRate();
        triggerBloodOxygen();

        //androidId = Settings.Secure.getString(getContentResolver(), Settings.Secure.ANDROID_ID);
        androidId = DeviceUtils.getIMEI(getApplicationContext());
//        List<Sensor> sensorList = sensorManager.getSensorList(Sensor.TYPE_ALL);
//        for (Sensor s : sensorList) {
//            Log.d(TAG, "Available sensor: " + s.getName() + " type: " + s.getType());
//        }

    }

    // Receiver for heart rate and blood oxygen
    private final BroadcastReceiver externalReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if ("com.monitor.health.action.result".equals(action)) {
                float heartRate = intent.getFloatExtra("heartRate", -1f);
                Log.d(TAG, "Received heart rate: " + heartRate);

                Intent toMain = new Intent(HEART_RATE_UPDATE);
                toMain.putExtra("heartRate", heartRate);
                sendBroadcast(toMain);
                sendHeartRate(heartRate);
            } else if ("com.monitor.health.blood.action.result".equals(action)) {
                float spo2 = intent.getFloatExtra("spo2", -1f);
                Log.d(TAG, "Received blood oxygen: " + spo2);

                Intent toMain = new Intent(BLOOD_OXYGEN_UPDATE);
                toMain.putExtra("spo2", spo2);
                sendBroadcast(toMain);
                sendBloodOxygen(spo2);
            }
        }
    };
    private BroadcastReceiver externalHeartRateReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if ("com.monitor.health.action.result".equals(intent.getAction())) {
                float heartRate = intent.getFloatExtra("heartRate", -1f);
                Log.d(TAG, "Received external heart rate: " + heartRate);

                // âœ… Send to MainActivity
                Intent toMain = new Intent("com.monitor.health.HEART_RATE_UPDATE");
                toMain.putExtra("heartRate", heartRate);
                sendBroadcast(toMain);

                // âœ… Save or send to API if needed
                if (heartRate > 0) {
                    sendHeartRate(heartRate);
                }
            }
        }
    };

    private void triggerHeartRate() {
        Intent it = new Intent(HEART_ACTION);
        it.setPackage("com.monitor.health");
        it.putExtra("action", ACTION_HEART);
        it.putExtra("count", 1);
        startService(it);
        Log.d(TAG, "Heart rate measurement triggered.");
    }

    private void triggerBloodOxygen() {
        Intent it = new Intent(BLOOD_HEART_ACTION);
        it.setPackage("com.monitor.health");
        it.putExtra("action", ACTION_BLOODO);
        it.putExtra("count", 1);
        startService(it);
        Log.d(TAG, "Blood oxygen measurement triggered.");
    }

    // Receiver for IoT system broadcasts
    private final BroadcastReceiver dataReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            Log.d(TAG, "IoT Broadcast received: " + action);

            if ("com.monitor.health.action.result".equals(action)) {
                // âœ… Heart rate
                float heartRate = intent.getFloatExtra("heartRate", -1f);
                Log.d(TAG, "Received external heart rate: " + heartRate);

                Intent toMain = new Intent("com.monitor.health.HEART_RATE_UPDATE");
                toMain.putExtra("heartRate", heartRate);
                sendBroadcast(toMain);

            }
            if ("com.monitor.health.blood.action.result".equals(action)) {
                // âœ… Blood Oxygen (SpO2)
                float spo2 = intent.getFloatExtra("spo2", -1f);
                Log.d(TAG, "Received external blood oxygen (SpO2): " + spo2);

                Intent toMain = new Intent("com.monitor.health.BLOOD_OXYGEN_UPDATE");
                toMain.putExtra("spo2", spo2);
                sendBroadcast(toMain);

            }
//            if ("android.hsc.iotservices".equals(action)) {
//                // âœ… Generic IoT Service Broadcast
//                Log.d(TAG, "Received broadcast from IoT services");
//
//                // Forward to main if needed
//                Intent toMain = new Intent("com.monitor.health.IOT_UPDATE");
//                Bundle extras = intent.getExtras();
//                if (extras != null) {
//                    toMain.putExtras(extras); // pass everything to main
//                }
//                sendBroadcast(toMain);
//            }

            // Log all intent extras
            Bundle extras = intent.getExtras();
            if (extras != null) {
                for (String key : extras.keySet()) {
                    Object value = extras.get(key);
                    Log.d(TAG, "Key: " + key + " = " + value);
                }
            }
        }
    };



    private void sendHeartRate(float hr) {
        // Upload to server or save locally
    }

    private void sendBloodOxygen(float spo2) {
        // Upload to server or save locally
    }

//    private void triggerExternalHeartRateApp() {
//        Intent it = new Intent(HEART_ACTION);
//        it.setPackage("com.hsc.heartrate");
//        it.putExtra("action", ACTION_HEART);
//        it.putExtra("count", 1);
//        try {
//            startService(it);
//            Log.d(TAG, "Triggered external heart rate app");
//        } catch (Exception e) {
//            Log.e(TAG, "Failed to start external heart rate service", e);
//        }
//    }

    @Override
    public void onDestroy() {
        super.onDestroy();
       // sensorManager.unregisterListener(heartRateListener);
        Log.d(TAG, "Heart rate sensor listener unregistered");
    }

    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(
                    CHANNEL_ID,
                    "Heart Rate Service",
                    NotificationManager.IMPORTANCE_LOW
            );
            getSystemService(NotificationManager.class).createNotificationChannel(channel);
        }
    }

    private Notification createNotification() {
        return new NotificationCompat.Builder(this, CHANNEL_ID)
                .setContentTitle("Heart rate monitoring")
                .setContentText("Measuring heart rate in background...")
                .setSmallIcon(android.R.drawable.ic_menu_compass)
                .build();
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    public void sendHeartRate(double heartRate) {
        Log.d(TAG, "sending temperature "+ heartRate);
        // Usage example in your activity or service
        String token = "bNWZsV#BeZvaNb*gF@3Z^7tCNhCT29Vw8Vi%4T%";
        Reading reading = new Reading(
                false,
                "Asia/Manila",
                "jtm00025b94050c",
                Arrays.asList(0.0,heartRate),
                "66437be266c8833a1c42d7aa",
                "5bb306382598931ffbd1b626",
                getDate(),
                "66437be266c8833a1c42d7aa"
        );
        List<Reading> readingsList = Arrays.asList(reading);
        ReadingsRequest readingsRequest = new ReadingsRequest(readingsList);

        Call<Object> call = ApiClient.getUserService(Constant.BASE_URL_BGM,token, androidId).sendReadings(readingsRequest);
        call.enqueue(new Callback<Object>() {
            @Override
            public void onResponse(Call<Object> call, Response<Object> response) {
                Log.d(TAG, "sendHeartRate DRS_020_SEND_ALARM code " + response.code());
                Log.d(TAG, "sendHeartRate DRS_020_SEND_ALARM body " + response.body());
                Log.d(TAG, "sendHeartRate DRS_020_SEND_ALARM toString " + response.toString());
                Log.d(TAG, "sendHeartRate DRS_020_SEND_ALARM message " + response.message());
                if (response.isSuccessful()) {
                    // Request successful
                    // Handle response if needed
                    //saveTemperatureData(temperature);
                    // restartBle();
                } else {
                    // Request failed
                    // Handle error
                    // restartBle();
                }
            }

            @Override
            public void onFailure(Call<Object> call, Throwable t) {
                // Request failed
                // Handle failure
                //restartBle();
            }
        });
    }

    public String getDate() {
        // Get the current date and time
        Calendar calendar = Calendar.getInstance();
        Date currentDate = calendar.getTime();

        // Set the desired time zone offset (e.g., +08:00)
        TimeZone timeZone = TimeZone.getTimeZone("GMT+08:00");
        calendar.setTimeZone(timeZone);

        // Format the date and time
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss");
        sdf.setTimeZone(timeZone);
        String formattedDate = sdf.format(currentDate);

        // Get the offset in hours and minutes
        int offsetMillis = timeZone.getOffset(currentDate.getTime());
        int offsetHours = offsetMillis / (60 * 60 * 1000);
        int offsetMinutes = Math.abs((offsetMillis / (60 * 1000)) % 60);

        // Format the offset
        String offset = String.format("%s%02d:%02d", offsetHours >= 0 ? "+" : "-", Math.abs(offsetHours), offsetMinutes);


        // Combine formatted date and offset
        String finalFormattedDate = formattedDate +""+offset;
        Log.d(TAG, "Date - "+offset);
        Log.d(TAG, "Date  -"+finalFormattedDate);

        return finalFormattedDate;

    }

    public void saveHeartRate(double heartRate) {
        databaseClient.getAppDatabase().heartRateDao().insertHeartRate(new HeartRateEntity(heartRate));
    }
}
