package com.monitor.health.services;

import static com.monitor.health.Constant.ACTION_HEALTH_UPDATE;

import android.annotation.SuppressLint;
import android.app.HSystemAssistManager;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Handler;
import android.os.IBinder;
import android.util.Log;

public class HeartRateOxygenStepService extends Service {

    private static final String TAG = "TestService";
    private HSystemAssistManager systemAssistManager;
    private SensorManager mSensorManager;
    private static final int TYPE_HEART_RATE = 21;

    private int heartRateValue = 0;
    private int bloodRateValue = 0;
    private int steps;
    private Sensor mSensor;
    private float semaphore;
    private float light;

    private Handler handler = new Handler();
    private Runnable runnable;

    public final static String ACTION = "action";
    public final static String ACTION_IOTSERVICES = "android.hsc.iotservices";
    public final static int ACTION_HEART = 1 << 11;
    public final static int ACTION_BLOODO = 102343;

    private SensorEventListener mHeartRateListener = new SensorEventListener() {
        @Override
        public void onSensorChanged(SensorEvent event) {
            steps = (int) (event.values[0] + 0.5f);
            heartRateValue = steps;

            if (event.values.length > 1) {
                bloodRateValue = (int) (event.values[1] + 0.5f);
            }
            if (event.values.length > 2) {
                int altBlood = (int) (event.values[2] + 0.5f);
                if (altBlood > 0 && altBlood != heartRateValue) {
                    bloodRateValue = altBlood;
                }
            }
            if (event.values.length > 5) {
                semaphore = event.values[5];
            }
            if (event.values.length > 6) {
                light = event.values[6];
            }

            handler.post(runnable);
        }

        @Override
        public void onAccuracyChanged(Sensor sensor, int accuracy) {
            Log.d(TAG, "Sensor accuracy changed: " + accuracy);
        }
    };

    private final BroadcastReceiver dataReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (intent == null || !intent.hasExtra(ACTION)) return;
            int action = intent.getIntExtra(ACTION, -1);
            if (action == ACTION_HEART) {
                heartRateValue = intent.getIntExtra("heart", 0);
            } else if (action == ACTION_BLOODO) {
                bloodRateValue = intent.getIntExtra("blood", 0);
            }
            broadcastResults();
        }
    };

    @Override
    public void onCreate() {
        super.onCreate();
        initializeSystemServices();
        setupSensorListener();
        registerBroadcastReceiver();

        runnable = this::broadcastResults;
    }

    @SuppressLint("WrongConstant")
    private void initializeSystemServices() {
        systemAssistManager = (HSystemAssistManager) getSystemService("hsystemassist");
        mSensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);
        systemAssistManager.isEnableAccelerate(this);
    }

    private void setupSensorListener() {
        mSensor = mSensorManager.getDefaultSensor(TYPE_HEART_RATE);
        if (mSensor != null) {
            mSensorManager.registerListener(mHeartRateListener, mSensor, SensorManager.SENSOR_DELAY_NORMAL);
        }
    }

    @SuppressLint("UnspecifiedRegisterReceiverFlag")
    private void registerBroadcastReceiver() {
        IntentFilter filter = new IntentFilter();
        filter.addAction(ACTION_IOTSERVICES);
        registerReceiver(dataReceiver, filter);
    }

    private void broadcastResults() {
        Intent intent = new Intent(ACTION_HEALTH_UPDATE);
        intent.putExtra("heartRate", heartRateValue);
        intent.putExtra("bloodRate", bloodRateValue);
        try {
            intent.putExtra("steps", systemAssistManager.getSetpCount());
        } catch (Exception e) {
            intent.putExtra("steps", -1);
        }
        sendBroadcast(intent);
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        return START_STICKY;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (mSensorManager != null) {
            mSensorManager.unregisterListener(mHeartRateListener);
        }
        unregisterReceiver(dataReceiver);
        handler.removeCallbacks(runnable);
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null; // Not binding
    }
}

