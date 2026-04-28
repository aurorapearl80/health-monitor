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
import android.os.Binder;
import android.os.Build;
import android.os.IBinder;
import android.util.Log;

import androidx.core.app.NotificationCompat;

import com.monitor.health.Constant;

public class BloodOxygenSensorService extends Service {

    private static final int TYPE_HEART_RATE = 21;
    private static final int NOTIFICATION_ID = 1001;

    // Heart rate modes
    public static final int MODE_HEART_RATE = 1;
    public static final int MODE_BLOOD_OXYGEN = 2;
    public static final int MODE_HAND_OFF_DETECTION = 3;

    // Broadcast actions
    private SensorManager mSensorManager;
    private Sensor mSensor;
    private HSystemAssistManager mHSystemAssistManager;
    private IBinder mBinder = new HeartRateServiceBinder();
    private boolean isMonitoring = false;
    private int currentMode = MODE_HEART_RATE;

    @Override
    public void onCreate() {
        super.onCreate();
        initializeSensor();
        createNotificationChannel();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        startForeground(NOTIFICATION_ID, createNotification());
        return START_STICKY;
    }

    @Override
    public IBinder onBind(Intent intent) {
        return mBinder;
    }

    @SuppressLint("WrongConstant")
    private void initializeSensor() {
        Log.d("BloodOxygenSensorService", "Started monitoring mode: initialize");
        mSensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);
        mSensor = mSensorManager.getDefaultSensor(TYPE_HEART_RATE);
        // Initialize your HSystemAssistManager here
        mHSystemAssistManager = (HSystemAssistManager)getSystemService("hsystemassist");
    }

    // Binder class for local binding
    public class HeartRateServiceBinder extends Binder {
        public BloodOxygenSensorService getService() {
            return BloodOxygenSensorService.this;
        }
    }

    // Public methods to control the service
    public void startOxygenMonitoring() {
        startMonitoring(MODE_HEART_RATE);
    }

    public void startBloodOxygenMonitoring() {
        startMonitoring(MODE_BLOOD_OXYGEN);
    }

    public void startHandOffDetection() {
        startMonitoring(MODE_HAND_OFF_DETECTION);
    }

    private void startMonitoring(int mode) {
        currentMode = mode;

        if (mHSystemAssistManager != null) {
//            mHSystemAssistManager.setHeartrateMode(1);
//            mHSystemAssistManager.setHeartrateMode(1/2/3);
//            1=heart rate
//            2=blood oxygen
//            3=hand off detection

            mHSystemAssistManager.setHeartrateMode(2);
        }

        if (mSensor != null && !isMonitoring) {
            boolean registered = mSensorManager.registerListener(
                    mHeartRateListener,
                    mSensor,
                    SensorManager.SENSOR_DELAY_NORMAL
            );

            if (registered) {
                isMonitoring = true;
                Log.d("BloodOxygenSensorService", "Started monitoring mode: " + mode);
            } else {
                sendErrorBroadcast("Failed to register sensor listener");
            }
        } else if (mSensor == null) {
            sendErrorBroadcast("Heart rate sensor not available");
        }
    }

    public void stopMonitoring() {
        if (mSensorManager != null && mHeartRateListener != null && isMonitoring) {
            mSensorManager.unregisterListener(mHeartRateListener);
            isMonitoring = false;
            Log.d("BloodOxygenSensorService", "Stopped monitoring");
        }
    }

    public boolean isMonitoring() {
        return isMonitoring;
    }

    public int getCurrentMode() {
        return currentMode;
    }

    // Sensor event listener
    private final SensorEventListener mHeartRateListener = new SensorEventListener() {
        @Override
        public void onSensorChanged(SensorEvent event) {
           // Log.d("BloodOxygenSensorService", "Heart Rate: running...");
            if (event.sensor.getType() == TYPE_HEART_RATE) {
                //Log.d("BloodOxygenSensorService", "Heart Rate: running..."+TYPE_HEART_RATE);
                float heartRate = event.values[0];

                // Send broadcast with heart rate data
                sendOxygenDataBroadcast(heartRate);

                //Log.d("BloodOxygenSensorService", "Heart Rate: " + heartRate + " BPM, Mode: " + currentMode);
            }
        }

        @Override
        public void onAccuracyChanged(Sensor sensor, int accuracy) {
            Log.d("HeartRateService", "Sensor accuracy changed: " + accuracy);
        }
    };

    // Broadcast methods
    private void sendOxygenDataBroadcast(float heartRate) {
        Intent intent = new Intent(Constant.ACTION_BLOOD_OXYGEN_DATA);
        intent.putExtra(Constant.EXTRA_BLOOD_OXYGEN_VALUE, heartRate);
        intent.putExtra(Constant.EXTRA_SENSOR_MODE, currentMode);
        sendBroadcast(intent);
    }

    private void sendErrorBroadcast(String errorMessage) {
        Intent intent = new Intent(Constant.ACTION_SENSOR_ERROR);
        intent.putExtra(Constant.EXTRA_ERROR_MESSAGE, errorMessage);
        sendBroadcast(intent);
    }

    // Notification methods
    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(
                    "OXYGEN_CHANNEL",
                    "Oxygen Monitoring",
                    NotificationManager.IMPORTANCE_LOW
            );
            channel.setDescription("Oxygen sensor monitoring service");

            NotificationManager manager = getSystemService(NotificationManager.class);
            manager.createNotificationChannel(channel);
        }
    }

    private Notification createNotification() {
        return new NotificationCompat.Builder(this, "OXYGEN_CHANNEL")
                .setContentTitle("Oxygen Monitor")
                .setContentText("Monitoring oxygen...")
                .setSmallIcon(android.R.drawable.ic_menu_info_details)
                .setOngoing(true)
                .build();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
       // stopMonitoring();
//        mSensorManager = null;
//        mSensor = null;
//        mHSystemAssistManager = null;
    }
}

