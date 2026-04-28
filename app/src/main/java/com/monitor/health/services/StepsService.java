package com.monitor.health.services;
import android.annotation.SuppressLint;
import android.app.HSystemAssistManager;
import android.app.Service;
import android.content.Intent;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Binder;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.util.Log;

import androidx.localbroadcastmanager.content.LocalBroadcastManager;


public class StepsService extends Service {

    private static final String TAG = "SensorDataService";
    private static final int TYPE_HEART_RATE = 21;

    // Broadcast action constants
    public static final String ACTION_SENSOR_DATA = "com.yourpackage.SENSOR_DATA";
    public static final String EXTRA_STEPS = "steps";
    public static final String EXTRA_SEMAPHORE = "semaphore";
    public static final String EXTRA_LIGHT = "light";
    public static final String EXTRA_HEART_RATE = "heart_rate";
    public static final String EXTRA_STEP_COUNT = "step_count";

    private HSystemAssistManager mHSystemAssistManager;
    private SensorManager mSensorManager;
    private Sensor mSensor;

    private int steps;
    private float semaphore;
    private float light;
    private int stepCount;

    private Handler handler;
    private final LocalBinder binder = new LocalBinder();

    // Interface for activity communication
    public interface SensorDataListener {
        void onSensorDataChanged(int steps, float semaphore, float light, int stepCount);
    }

    private SensorDataListener sensorDataListener;

    public class LocalBinder extends Binder {
        public StepsService getService() {
            return StepsService.this;
        }
    }

    @SuppressLint("WrongConstant")
    @Override
    public void onCreate() {
        super.onCreate();
        Log.d(TAG, "Service created");

        handler = new Handler(Looper.getMainLooper());

        // Initialize sensor manager and custom system manager
        mSensorManager = (SensorManager) getSystemService(SENSOR_SERVICE);
        mHSystemAssistManager = (HSystemAssistManager) getSystemService("hsystemassist");

        // Get heart rate sensor
        if (mSensorManager != null) {
            mSensor = mSensorManager.getDefaultSensor(TYPE_HEART_RATE);
        }

        startSensorMonitoring();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Log.d(TAG, "Service started");
        return START_STICKY; // Service will restart if killed
    }

    @Override
    public IBinder onBind(Intent intent) {
        return binder;
    }

    private void startSensorMonitoring() {
        // Register heart rate sensor listener
        if (mSensor != null && mSensorManager != null) {
            mSensorManager.registerListener(mHeartRateListener, mSensor, SensorManager.SENSOR_DELAY_NORMAL);
            Log.d(TAG, "Heart rate sensor registered");
        }

        // Enable accelerometer through custom system manager
        if (mHSystemAssistManager != null) {
            mHSystemAssistManager.isEnableAccelerate(this);
            Log.d(TAG, "Accelerometer enabled");
        }

        // Start periodic data collection
        startDataCollection();
    }

    private void startDataCollection() {
        handler.post(dataCollectionRunnable);
    }

    private final Runnable dataCollectionRunnable = new Runnable() {
        @Override
        public void run() {
            // Get step count from system manager
            if (mHSystemAssistManager != null) {
                stepCount = mHSystemAssistManager.getSetpCount();
            }

//            Log.d(TAG, String.format("Data collected - Steps: %d, Semaphore: %.2f, Light: %.2f, StepCount: %d",
//                    steps, semaphore, light, stepCount));

            // Notify activity through callback
            if (sensorDataListener != null) {
                sensorDataListener.onSensorDataChanged(steps, semaphore, light, stepCount);
            }

            // Send broadcast to activity
            sendSensorDataBroadcast();

            // Schedule next collection (every 10 seconds)
            handler.postDelayed(this, 10000);
        }
    };

    private final SensorEventListener mHeartRateListener = new SensorEventListener() {
        @Override
        public void onSensorChanged(SensorEvent event) {
            //if (event.values.length > 6) {
                steps = (int) (event.values[0] + 0.5f);
                semaphore = event.values[5];
                light = event.values[6];

                //Log.d(TAG, "Sensor changed - Steps: " + steps + ", Semaphore: " + semaphore + ", Light: " + light);
            //}
        }

        @Override
        public void onAccuracyChanged(Sensor sensor, int accuracy) {
            Log.d(TAG, "Sensor accuracy changed: " + accuracy);
        }
    };

    private void sendSensorDataBroadcast() {
        Intent intent = new Intent(ACTION_SENSOR_DATA);
        intent.putExtra(EXTRA_STEPS, steps);
        intent.putExtra(EXTRA_SEMAPHORE, semaphore);
        intent.putExtra(EXTRA_LIGHT, light);
        intent.putExtra(EXTRA_STEP_COUNT, stepCount);

        LocalBroadcastManager.getInstance(this).sendBroadcast(intent);
    }

    // Method to set listener for direct communication
    public void setSensorDataListener(SensorDataListener listener) {
        this.sensorDataListener = listener;
    }

    // Getter methods for current values
    public int getSteps() { return steps; }
    public float getSemaphore() { return semaphore; }
    public float getLight() { return light; }
    public int getStepCount() { return stepCount; }

    @Override
    public void onDestroy() {
        super.onDestroy();
        Log.d(TAG, "Service destroyed");

        // Unregister sensor listeners
        if (mSensorManager != null && mHeartRateListener != null) {
            mSensorManager.unregisterListener(mHeartRateListener);
        }

        // Remove handler callbacks
        if (handler != null) {
            handler.removeCallbacks(dataCollectionRunnable);
        }

        // Clear listener
        sensorDataListener = null;
    }

    // Method to force data update
    public void requestDataUpdate() {
        handler.post(dataCollectionRunnable);
    }
}
