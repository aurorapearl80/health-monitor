package com.monitor.health;

import android.annotation.SuppressLint;
import android.app.HSystemAssistManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.widget.TextView;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;

import com.monitor.health.sleep.SleepMonitor;
import com.monitor.health.utility.BPReading;
import com.monitor.health.utility.BloodPressureEstimator;

public class TestActivity extends AppCompatActivity {

    private static final String TAG = "TestActivity";
    private HSystemAssistManager systemAssistManager;
    private SensorManager mSensorManager;
    private static final int TYPE_HEART_RATE = 21;

    // Measurement variables
    private int heartRateValue = 0;
    private int bloodRateValue = 0;
    private int steps;
    private Sensor mSensor;
    private float semaphore;
    private float light;

    private Handler handler = new Handler();
    private Runnable runnable;

    // UI Elements
    private TextView tv_heart_rate, tv_blood_rate, txt_steps, tv_blood_pressure, tv_sleep;

    // Constants
    public final static String ACTION = "action";
    public final static String HEART_ACTION = "com.monitor.health.healthy.action.start";
    public final static String BLOOD_ACTION = "com.monitor.health.blood.action.start";
    public final static String HEART_PKG = "com.hsc.heartrate";
    public final static long DELAY_BLOOD_RESULTS = 1000 * 58; // 58 seconds timeout
    public final static long DELAY_HEART_RESULTS = 1000 * 40; // 40 seconds timeout
    public final static String ACTION_IOTSERVICES = "android.hsc.iotservices";

    public final static int ACTION_HEART = 1 << 11;
    public final static int ACTION_BLOODO = 102343;

    private SensorEventListener mHeartRateListener = new SensorEventListener() {
        @Override
        public void onSensorChanged(SensorEvent event) {
            steps = (int) (event.values[0] + 0.5f);

            Log.d(TAG, "Sensor value received: " + steps);

            // Try to extract both heart rate and blood rate from sensor data
            heartRateValue = steps; // Primary sensor value as heart rate

            // Check if there are additional values in the sensor event
            if (event.values.length > 1) {
                bloodRateValue = (int) (event.values[1] + 0.5f);
                Log.d(TAG, "Blood rate from sensor[1]: " + bloodRateValue);
            }

            // Alternative: Try to get blood rate from different sensor indices
            if (event.values.length > 2) {
                int alternativeBlood = (int) (event.values[2] + 0.5f);
                if (alternativeBlood > 0 && alternativeBlood != heartRateValue) {
                    bloodRateValue = alternativeBlood;
                    Log.d(TAG, "Blood rate from sensor[2]: " + bloodRateValue);
                }
            }

            // Get additional sensor values
            if (event.values.length > 5) {
                semaphore = event.values[5];
            }
            if (event.values.length > 6) {
                light = event.values[6];
            }

            Log.d(TAG, "Heart Rate: " + heartRateValue + ", Blood Rate: " + bloodRateValue);
            handler.post(runnable);
        }

        @Override
        public void onAccuracyChanged(Sensor sensor, int accuracy) {
            Log.d(TAG, "Sensor accuracy changed: " + accuracy);
        }
    };

    @SuppressLint({"WrongConstant", "UnspecifiedRegisterReceiverFlag"})
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_test);

        initializeViews();
        initializeSystemServices();
        setupSensorListener();
        registerBroadcastReceiver();

        // Enable both heart rate and blood rate monitoring simultaneously
        enableBothMeasurements();
        updateUI();
//
//
//        // Using default population averages
//        BloodPressureEstimator estimator1 = new BloodPressureEstimator();
//        BPReading reading1 = estimator1.estimateBloodPressure(85);
//        Log.d(TAG, "The data of the reading is : "+reading1.toString());
//
//        // Initialize
//        SleepMonitor sleepMonitor = new SleepMonitor();
//        sleepMonitor.setBaselineHeartRate(70.0);
//        sleepMonitor.startSleepMonitoring();
//
//// Feed your existing heart rate data
//        sleepMonitor.updateHeartRate(65); // From your sensor
//        sleepMonitor.updateHeartRate(63); // Next reading
//        sleepMonitor.updateHeartRate(61); // Next reading
//
//        // Check sleep state
//        SleepMonitor.SleepState state = sleepMonitor.getCurrentSleepState();
//        Log.d(TAG, "The data of the sleep is : "+state.toString());



    }

    private void initializeViews() {
        // Initialize TextViews for displaying both measurements
        tv_heart_rate = findViewById(R.id.tvValueBG);
        tv_blood_rate = findViewById(R.id.tvUnit);
        txt_steps = findViewById(R.id.tvTimeAgo);
        tv_blood_pressure = findViewById(R.id.tvBloodPressure);
        tv_sleep = findViewById(R.id.tvSleep);
    }

    @SuppressLint("WrongConstant")
    private void initializeSystemServices() {
        systemAssistManager = (HSystemAssistManager) getSystemService("hsystemassist");
        mSensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);
        systemAssistManager.isEnableAccelerate(this);

        //Retrieve blood pressure data via below API
//
    }

    private void setupSensorListener() {
        mSensor = mSensorManager.getDefaultSensor(TYPE_HEART_RATE);
        if (mSensor != null) {
            mSensorManager.registerListener(mHeartRateListener, mSensor, SensorManager.SENSOR_DELAY_NORMAL);
            Log.d(TAG, "Heart rate sensor registered successfully");
        } else {
            Log.e(TAG, "Heart rate sensor not available");
        }
    }

    @SuppressLint("UnspecifiedRegisterReceiverFlag")
    private void registerBroadcastReceiver() {
        IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(ACTION_IOTSERVICES);
        registerReceiver(dataReceiver, intentFilter);
    }

    private void enableBothMeasurements() {
        try {
            // Try to enable both modes - this might work on some hardware
            systemAssistManager.setHeartrateMode(1); // Heart rate
            // Small delay to ensure first mode is set
            new Handler().postDelayed(() -> {
                try {
                    systemAssistManager.setHeartrateMode(2); // Blood rate
                    Log.d(TAG, "Both measurement modes enabled");
                } catch (Exception e) {
                    Log.e(TAG, "Error enabling blood rate mode: " + e.getMessage());
                }
            }, 100);

        } catch (Exception e) {
            Log.e(TAG, "Error enabling heart rate mode: " + e.getMessage());
        }

        runnable = new Runnable() {
            @Override
            public void run() {
                updateUI();
            }
        };
    }

    private void updateUI() {
        runOnUiThread(() -> {
            // Update heart rate display
            if (tv_heart_rate != null) {
                tv_heart_rate.setText("Heart Rate: " + heartRateValue + " BPM");
            }

            // Update blood rate display
            if (tv_blood_rate != null) {
                tv_blood_rate.setText("Blood Rate: " + bloodRateValue);
            }

            // Update steps display
            if (txt_steps != null) {
                try {
                    int stepCount = systemAssistManager.getSetpCount();
                    txt_steps.setText("Steps: " + stepCount);
                } catch (Exception e) {
                    Log.e(TAG, "Error getting step count: " + e.getMessage());
                    txt_steps.setText("Steps: --");
                }
            }

            String bloodpressure=systemAssistManager.getBloodPressure();
            tv_blood_pressure.setText(String.format("Value: %s", bloodpressure));
            //Retrieve sleep data via below API
            String sleep=systemAssistManager.getSleep();
            tv_sleep.setText("Sleep : "+sleep);
        });
    }

    private final BroadcastReceiver dataReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (intent == null || !intent.hasExtra(ACTION)) {
                Log.d(TAG, "Intent is null or missing action!");
                return;
            }

            int action = intent.getIntExtra(ACTION, -1);
            Log.d(TAG, "Received broadcast action: " + action);

            if (action != -1) {
                switch (action) {
                    case ACTION_HEART:
                        int heart = intent.getIntExtra("heart", 0);
                        Log.d(TAG, "Received heart rate via broadcast: " + heart);
                        heartRateValue = heart;
                        updateUI();
                        break;

                    case ACTION_BLOODO:
                        int bloodo = intent.getIntExtra("blood", 0);
                        Log.d(TAG, "Received blood rate via broadcast: " + bloodo);
                        bloodRateValue = bloodo;
                        updateUI();
                        break;

                    default:
                        Log.d(TAG, "Unknown action received: " + action);
                        break;
                }
            } else {
                Log.d(TAG, "Device disconnected or measurement stopped");
            }
        }
    };

    @Override
    protected void onResume() {
        super.onResume();
        // Re-register sensor listener when activity resumes
        if (mSensor != null) {
            mSensorManager.registerListener(mHeartRateListener, mSensor, SensorManager.SENSOR_DELAY_NORMAL);
        }
        updateUI();
    }

    @Override
    protected void onPause() {
        super.onPause();
        // Unregister sensor listener to save battery
        mSensorManager.unregisterListener(mHeartRateListener);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        // Clean up resources
        if (mSensorManager != null) {
            mSensorManager.unregisterListener(mHeartRateListener);
        }
        if (dataReceiver != null) {
            try {
                unregisterReceiver(dataReceiver);
            } catch (Exception e) {
                Log.e(TAG, "Error unregistering receiver: " + e.getMessage());
            }
        }
        if (handler != null) {
            handler.removeCallbacks(runnable);
        }
    }

    // Public methods for getting current values
    public int getCurrentHeartRate() {
        return heartRateValue;
    }

    public int getCurrentBloodRate() {
        return bloodRateValue;
    }
}