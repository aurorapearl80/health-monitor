package com.monitor.health.services;


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

    private static final int NOTIFICATION_ID = 1001;

    // Heart rate modes
    public static final int MODE_HEART_RATE = 1;
    public static final int MODE_BLOOD_OXYGEN = 2;
    public static final int MODE_HAND_OFF_DETECTION = 3;

    private SensorManager mSensorManager;
    private Sensor mSensor;        // HR sensor (fallback)
    private Sensor mSpO2Sensor;    // dedicated SpO2 sensor when available
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

    private void initializeSensor() {
        mSensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);
        mSpO2Sensor = discoverSpO2Sensor();
        // Use dedicated SpO2 sensor if found; otherwise fall back to HR sensor (values[1] channel)
        mSensor = (mSpO2Sensor != null) ? mSpO2Sensor
                : mSensorManager.getDefaultSensor(Sensor.TYPE_HEART_RATE);
        Log.d("BloodOxygenSensorService", "SpO2 sensor: "
                + (mSpO2Sensor != null ? mSpO2Sensor.getName() : "none, using HR fallback"));
    }

    /**
     * Scan all available sensors for a dedicated SpO2 / oximeter sensor.
     * Works on Samsung Galaxy Watch, Pixel Watch, and other Wear OS devices that
     * expose blood oxygen as a named sensor (without requiring vendor SDKs).
     */
    private Sensor discoverSpO2Sensor() {
        for (Sensor s : mSensorManager.getSensorList(Sensor.TYPE_ALL)) {
            String name   = s.getName().toLowerCase();
            String vendor = s.getVendor().toLowerCase();
            if (name.contains("spo2")   || name.contains("oxim")   || name.contains("oxygen") ||
                vendor.contains("spo2") || vendor.contains("oxim") || vendor.contains("oxygen")) {
                Log.d("BloodOxygenSensorService", "Found SpO2 sensor: " + s.getName()
                        + " type=" + s.getType());
                return s;
            }
        }
        return null;
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

    // Sensor event listener — handles both dedicated SpO2 sensor and HR-channel fallback
    private final SensorEventListener mHeartRateListener = new SensorEventListener() {
        @Override
        public void onSensorChanged(SensorEvent event) {
            if (event.values == null || event.values.length == 0) return;

            float spo2 = 0;

            if (mSpO2Sensor != null) {
                // Dedicated SpO2 sensor: values[0] is the saturation percentage
                spo2 = normalizeSpO2(Math.round(event.values[0]));
            } else {
                // HR sensor fallback: SpO2 is sometimes multiplexed on channels 1–4
                // (Samsung Watch, Pixel Watch, and other Wear OS devices)
                spo2 = extractSpO2FromHRChannels(event);
            }

            if (spo2 > 0) {
                sendOxygenDataBroadcast(spo2);
            }
        }

        @Override
        public void onAccuracyChanged(Sensor sensor, int accuracy) {
            Log.d("BloodOxygenSensorService", "Sensor accuracy changed: " + accuracy);
        }
    };

    /**
     * Scan sensor channels 1–4 for a plausible SpO2 reading.
     * Many Wear OS watches multiplex blood oxygen on the same sensor as heart rate.
     */
    private float extractSpO2FromHRChannels(SensorEvent event) {
        int len = Math.min(event.values.length, 5);
        for (int i = 1; i < len; i++) {
            float norm = normalizeSpO2(Math.round(event.values[i]));
            if (norm > 0) return norm;
        }
        return 0;
    }

    /**
     * Normalize raw sensor values to a 0–100 SpO2 percentage.
     * Different OEM firmware scales: 95, 950, or 9500 can all mean 95%.
     */
    private float normalizeSpO2(int raw) {
        if (raw <= 0)                           return 0;
        if (raw <= 100)                         return raw;
        if (raw >= 900  && raw <= 1000)         return raw / 10f;
        if (raw >= 9000 && raw <= 10000)        return raw / 100f;
        return 0;
    }

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
        stopMonitoring();
    }
}

