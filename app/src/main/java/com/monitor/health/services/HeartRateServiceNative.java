package com.monitor.health.services;

import android.annotation.SuppressLint;
import android.app.HSystemAssistManager;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Build;
import android.os.IBinder;

import androidx.core.app.NotificationCompat;

import com.monitor.health.R;


public class HeartRateServiceNative extends Service implements SensorEventListener {
    private SensorManager mSensorManager;
    private static final String TAG = "HeartRateServiceNative";
    private Sensor mSensor;
    private int mCurrentHeartRate = -1;

    private static final int TYPE_HEART_RATE = 21;
    public static final String ACTION_HEART_RATE = "com.monitor.health.ACTION_HEART_RATE";
    public static final String EXTRA_HEART_RATE = "heart_rate";

    private HSystemAssistManager systemAssistManager;


    @SuppressLint("WrongConstant")
    @Override
    public void onCreate() {
        super.onCreate();
        mSensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);
        if (mSensorManager != null) {
            mSensor = mSensorManager.getDefaultSensor(TYPE_HEART_RATE);
            if (mSensor != null) {
                mSensorManager.registerListener(this, mSensor, SensorManager.SENSOR_DELAY_NORMAL);

            }
        }


        //systemAssistManager = (HSystemAssistManager)getSystemService("hsystemassist");
        //systemAssistManager.getSetpCount();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (mSensorManager != null) {
            mSensorManager.unregisterListener(this);
        }
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null; // or use Binder if you need interaction
    }

    @Override
    public void onSensorChanged(SensorEvent event) {
        if (event == null || event.values == null || event.values.length < 2) return;

        int heartValue = (int) (event.values[0] + 0.5f); // HEART
        int bloodValue = (int) (event.values[1] + 0.5f); // BLOOD

        // Optional: Invalidate if flagged (for either)
        if (event.values.length > 7 && (event.values[7] == 1 || event.values[7] == 3)) {
            heartValue = -1;
            bloodValue = -1;
        }

        mCurrentHeartRate = heartValue;

        // Send both values to MainActivity
        sendHeartAndBloodToMainActivity(heartValue, bloodValue);

    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {
        // Optional
    }


    private void sendHeartAndBloodToMainActivity(int heart, int blood) {
        Intent intent = new Intent(ACTION_HEART_RATE);
        intent.putExtra("heart_rate", heart);
        intent.putExtra("blood_value", blood);
        sendBroadcast(intent);
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        startForeground(1, createNotification());
        // Your other logic here...
        return START_STICKY;
    }

    private Notification createNotification() {
        String channelId = "heart_rate_channel";
        String channelName = "Heart Rate Monitor";

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(
                    channelId, channelName, NotificationManager.IMPORTANCE_LOW
            );
            NotificationManager manager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
            if (manager != null) {
                manager.createNotificationChannel(channel);
            }
        }

        NotificationCompat.Builder builder = new NotificationCompat.Builder(this, channelId)
                .setContentTitle("Monitoring Heart Rate")
                .setContentText("Your heart rate sensor is active.")
                .setSmallIcon(R.drawable.ic_bloodtype) // use your actual icon here
                .setPriority(NotificationCompat.PRIORITY_LOW);

        return builder.build();
    }

}

