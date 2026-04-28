package com.monitor.health.adapter;

import android.annotation.SuppressLint;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import android.app.HSystemAssistManager;

public class HealthManager {
    private static final String TAG = "HealthManager";

    // ---- Existing constants
    public static final String ACTION = "action";
    public static final String ACTION_IOTSERVICES = "android.hsc.iotservices";
    public static final int ACTION_HEART = 1 << 11;
    public static final int ACTION_BLOODO = 102343;
    private static final int TYPE_HEART_RATE = 21;

    private static final long TIMEOUT_HEART_MS = 40_000L;
    private static final long TIMEOUT_SPO2_MS  = 58_000L;

    // ---- NEW: explicit vendor â€œstartâ€ trigger details (adjust if your ROM differs)
    private static final String HEART_PKG    = "com.hsc.heartrate";
    private static final String BLOOD_ACTION = "com.monitor.health.blood.action.start";

    private static HealthManager INSTANCE;
    private final Context app;
    private final SensorManager sensorManager;
    private final HSystemAssistManager assist;
    private final Handler main = new Handler(Looper.getMainLooper());

    private Sensor heartSensor;

    // one-shot callbacks
    private ValueCallback<Integer> heartCallback;
    private ValueCallback<Integer> spo2Callback;

    // ---- NEW: keep separate timeout runnables instead of nuking all
    private Runnable heartTimeout;
    private Runnable spo2Timeout;
    // Add field
    private Sensor spo2Sensor;

    private WearListener wearListener;

    public enum WearState {
        UNKNOWN,
        WORN,
        NOT_WORN
    }

    private WearState currentWearState = WearState.UNKNOWN;


    public static synchronized HealthManager getInstance(Context ctx) {
        if (INSTANCE == null) INSTANCE = new HealthManager(ctx.getApplicationContext());
        return INSTANCE;
    }

    @SuppressLint("WrongConstant")
    private HealthManager(Context app) {
        this.app = app;
        this.sensorManager = (SensorManager) app.getSystemService(Context.SENSOR_SERVICE);
        this.assist = (HSystemAssistManager) app.getSystemService("hsystemassist");
        try { assist.isEnableAccelerate(app); } catch (Throwable ignored) {}
        heartSensor = sensorManager.getDefaultSensor(TYPE_HEART_RATE);
        discoverVendorSensors();      // <--- add this
        registerReceiver();
    }

    // Call this in constructor after heartSensor = ...
    private void discoverVendorSensors() {
        try {
            for (Sensor s : sensorManager.getSensorList(Sensor.TYPE_ALL)) {
                String name = String.valueOf(s.getName()).toLowerCase();
                String vendor = String.valueOf(s.getVendor()).toLowerCase();
                // Heuristics: look for "ox", "oxi", "sp" "spo2", "oxygen"
                if (name.contains("spo2") || name.contains("oxim") || name.contains("oxygen") ||
                        vendor.contains("spo2") || vendor.contains("oxim") || vendor.contains("oxygen")) {
                    spo2Sensor = s; // remember first candidate
                    Log.d(TAG, "Found potential SpO2 sensor: " + s.getName() + " / " + s.getVendor() + " / type=" + s.getType());
                    break;
                }
            }
        } catch (Throwable t) {
            Log.w(TAG, "discoverVendorSensors failed: " + t.getMessage());
        }
    }

    // ---------------- Public API ----------------

    public void startHeartRateMeasurement(ValueCallback<Integer> cb) {
        // cancel previous
        heartCallback = null;
        clearHeartTimeout();

        this.heartCallback = cb;
        try { assist.setHeartrateMode(1); } catch (Throwable t) {
            Log.e(TAG, "setHeartrateMode(1) failed: " + t.getMessage());
        }
        if (heartSensor != null) {
            sensorManager.registerListener(hrListener, heartSensor, SensorManager.SENSOR_DELAY_NORMAL);
        }

        heartTimeout = () -> completeHeartWithError("Timed out");
        main.postDelayed(heartTimeout, TIMEOUT_HEART_MS);
    }

    public void startBloodOxygenMeasurement(ValueCallback<Integer> cb) {
        spo2Callback = null;
        clearSpO2Timeout();
        this.spo2Callback = cb;

        // Switch vendor mode (often required)
        try { assist.setHeartrateMode(2); }
        catch (Throwable t) { Log.e(TAG, "setHeartrateMode(2) failed: " + t.getMessage()); }

        // Prefer a vendor SpO2 sensor if we found one, else fallback to heart sensor multiplexing
        boolean registered = false;
        if (spo2Sensor != null) {
            try {
                registered = sensorManager.registerListener(hrListener, spo2Sensor, SensorManager.SENSOR_DELAY_NORMAL);
                Log.d(TAG, "Registered SpO2 sensor listener: " + registered);
            } catch (Throwable t) {
                Log.w(TAG, "register spo2Sensor failed: " + t.getMessage());
            }
        }
        if (!registered && heartSensor != null) {
            try {
                registered = sensorManager.registerListener(hrListener, heartSensor, SensorManager.SENSOR_DELAY_NORMAL);
                Log.d(TAG, "Registered heart sensor for multiplexed SpO2: " + registered);
            } catch (Throwable t) {
                Log.w(TAG, "register heartSensor for SpO2 failed: " + t.getMessage());
            }
        }

        // Some ROMs need a start broadcast; try both explicit and implicit
        try {
            Intent start = new Intent(BLOOD_ACTION);
            // First try implicit (no package) so vendor receivers can catch it
            app.sendBroadcast(start);
            // Then also try explicit to known pkg
            start.setPackage(HEART_PKG);
            app.sendBroadcast(start);
        } catch (Throwable t) {
            Log.e(TAG, "Sending BLOOD_ACTION failed: " + t.getMessage());
        }

        // Timeout
        spo2Timeout = () -> completeSpO2WithError("Timed out");
        main.postDelayed(spo2Timeout, TIMEOUT_SPO2_MS);
        // cancel previous
//        spo2Callback = null;
//        clearSpO2Timeout();
//
//        this.spo2Callback = cb;
//
//        // Switch vendor mode (keeps OEM happy)
//        try { assist.setHeartrateMode(2); }
//        catch (Throwable t) { Log.e(TAG, "setHeartrateMode(2) failed: " + t.getMessage()); }
//
//        // IMPORTANT: register the same sensor listener so we receive the multiplexed values
//        if (heartSensor != null) {
//            sensorManager.registerListener(hrListener, heartSensor, SensorManager.SENSOR_DELAY_NORMAL);
//        }
//
//        // Optional (some ROMs need a start broadcast)
//        try {
//            Intent start = new Intent(BLOOD_ACTION);  // e.g. "com.monitor.health.blood.action.start"
//            start.setPackage(HEART_PKG);              // e.g. "com.hsc.heartrate" (adjust if different)
//            app.sendBroadcast(start);
//        } catch (Throwable t) {
//            Log.e(TAG, "Sending BLOOD_ACTION failed: " + t.getMessage());
//        }
//
//        // Arm timeout
//        spo2Timeout = () -> completeSpO2WithError("Timed out");
//        main.postDelayed(spo2Timeout, TIMEOUT_SPO2_MS);
    }

    public void getSteps(ValueCallback<Integer> cb) {
        try {
            int steps = assist.getSetpCount();
            cb.onValue(steps);
        } catch (Throwable t) {
            cb.onError("Steps unavailable: " + t.getMessage());
        }
    }

    public interface ValueCallback<T> {
        void onValue(T value);
        void onError(String error);
    }

    // ---------------- Internal ----------------

    private final SensorEventListener hrListener = new SensorEventListener() {
        @Override public void onSensorChanged(SensorEvent event) {
            // Heart (only if someone asked for it)
            if (heartCallback != null) {
                int bpm = (int) (event.values[0] + 0.5f);
                if (bpm <= 0) return; // skip warmup/zero readings, wait for real value
                ValueCallback<Integer> cb = heartCallback; heartCallback = null;
                cleanupHeartListener();
                clearHeartTimeout();
                if (wearListener != null) wearListener.onWorn();
                cb.onValue(bpm);
            }

            // SpO2 (only if someone asked for it)
            if (spo2Callback != null) {
                Integer spo2 = parseSpO2FromEvent(event);
                if (spo2 != null) {
                    ValueCallback<Integer> scb = spo2Callback; spo2Callback = null;
                    cleanupHeartListener();
                    clearSpO2Timeout();
                    if (wearListener != null) wearListener.onWorn();  // we got a signal
                    scb.onValue(spo2);
                }
            }
            // Heart rate
//            if (heartCallback != null) {
//                int bpm = (int) (event.values[0] + 0.5f);
//                ValueCallback<Integer> cb = heartCallback; heartCallback = null;
//                cleanupHeartListener();
//                clearHeartTimeout();
//                cb.onValue(bpm);
//            }
//
//            // SpOâ‚‚ multiplexed on same event (common on these ROMs)
//            if (spo2Callback != null) {
//                int spo2 = -1;
//
//                if (event.values.length > 1) {
//                    int v1 = (int) (event.values[1] + 0.5f);
//                    if (v1 > 0 && v1 <= 100) spo2 = v1;
//                }
//                if (spo2 < 0 && event.values.length > 2) {
//                    int v2 = (int) (event.values[2] + 0.5f);
//                    if (v2 > 0 && v2 <= 100) spo2 = v2;
//                }
//
//                if (spo2 > 0) {
//                    ValueCallback<Integer> scb = spo2Callback; spo2Callback = null;
//                    // Stop listening once we have SpOâ‚‚
//                    cleanupHeartListener();
//                    clearSpO2Timeout();
//                    scb.onValue(spo2);
//                }
//            }
        }
        @Override public void onAccuracyChanged(Sensor sensor, int accuracy) {}
    };

    private void cleanupHeartListener() {
        try { sensorManager.unregisterListener(hrListener); } catch (Throwable ignored) {}
    }

    private void clearHeartTimeout() {
        if (heartTimeout != null) {
            main.removeCallbacks(heartTimeout);
            heartTimeout = null;
        }
    }

    private void clearSpO2Timeout() {
        if (spo2Timeout != null) {
            main.removeCallbacks(spo2Timeout);
            spo2Timeout = null;
        }
    }

    private void completeHeartWithError(String msg) {
        if (heartCallback != null) {
            ValueCallback<Integer> cb = heartCallback; heartCallback = null;
            cleanupHeartListener();
            clearHeartTimeout();
            // --- NEW: no reading â†’ likely not worn
            if (wearListener != null) wearListener.onNotWorn();
            cb.onError(msg);
        }
    }

    private void completeSpO2WithError(String msg) {
        if (spo2Callback != null) {
            ValueCallback<Integer> cb = spo2Callback; spo2Callback = null;
            clearSpO2Timeout();
            if (wearListener != null) wearListener.onNotWorn();
            cb.onError(msg);
        }
    }

    @SuppressLint("UnspecifiedRegisterReceiverFlag")
    private void registerReceiver() {
        IntentFilter f = new IntentFilter();
        f.addAction(ACTION_IOTSERVICES);

        // API 33+ requires an explicit flag
        if (Build.VERSION.SDK_INT >= 33) {
            app.registerReceiver(iotReceiver, f, Context.RECEIVER_NOT_EXPORTED);
        } else {
            app.registerReceiver(iotReceiver, f);
        }
        Log.d(TAG, "iotReceiver registered");
    }

//    private final BroadcastReceiver iotReceiver = new BroadcastReceiver() {
//        @Override public void onReceive(Context context, Intent intent) {
//            if (intent == null || !intent.hasExtra(ACTION)) return;
//
//            // DEBUG: dump extras once to learn exact keys
//            for (String k : intent.getExtras().keySet()) {
//                Object v = intent.getExtras().get(k);
//                Log.d(TAG, "extra: " + k + "=" + v);
//            }
//
//            int action = intent.getIntExtra(ACTION, -1);
//            Log.d(TAG, "iotReceiver action=" + action);
//
//            if (action == ACTION_HEART) {
//                int heart = intent.getIntExtra("heart", 0);
//                if (heartCallback != null) {
//                    ValueCallback<Integer> cb = heartCallback; heartCallback = null;
//                    cleanupHeartListener();
//                    clearHeartTimeout();
//                    cb.onValue(heart);
//                }
//            } else if (action == ACTION_BLOODO) {
//                // Vendor key is "blood" (donâ€™t use "spo2" unless your ROM says so)
//                int spo2 = intent.getIntExtra("blood", -1);
//                if (spo2Callback != null) {
//                    ValueCallback<Integer> cb = spo2Callback; spo2Callback = null;
//                    clearSpO2Timeout();
//                    cb.onValue(spo2);
//                }
//            }
//        }
//    };
    private final BroadcastReceiver iotReceiver = new BroadcastReceiver() {
        @Override public void onReceive(Context context, Intent intent) {
            if (intent == null) return;

            // Log extras to learn device behavior
            try {
                if (intent.getExtras() != null) {
                    for (String k : intent.getExtras().keySet()) {
                        Object v = intent.getExtras().get(k);
                        Log.d(TAG, "iot extra: " + k + "=" + v);
                    }
                }
            } catch (Throwable ignored) {}

            // Some ROMs deliver without 'action' extra; keep your original path, but also try generic parse
            int action = intent.getIntExtra(ACTION, -1);
            if (action == ACTION_HEART && heartCallback != null) {
                int heart = intent.getIntExtra("heart", 0);
                ValueCallback<Integer> cb = heartCallback; heartCallback = null;
                cleanupHeartListener();
                clearHeartTimeout();
                if (heart > 0 && wearListener != null) wearListener.onWorn();
                cb.onValue(heart);
                return;
            }

            if (spo2Callback != null) {
                Integer spo2 = parseSpO2FromIntent(intent);
                if (spo2 != null) {
                    ValueCallback<Integer> cb = spo2Callback; spo2Callback = null;
                    cleanupHeartListener();
                    clearSpO2Timeout();
                    cb.onValue(spo2);
                }
            }
        }
    };

    /** Try multiple keys and value normalizations for SpOâ‚‚ from broadcast extras. */
    private Integer parseSpO2FromIntent(Intent intent) {
        if (intent == null || intent.getExtras() == null) return null;

        // candidate keys we often see on vendor ROMs
        final String[] keys = {"blood", "spo2", "SPO2", "oxygen", "oximeter", "SpO2"};
        for (String k : keys) {
            if (!intent.hasExtra(k)) continue;
            try {
                int v = intent.getIntExtra(k, -1);
                Integer norm = normalizeSpO2(v);
                if (norm != null) return norm;
            } catch (Throwable ignored) {}
        }
        // try as float/double too
        for (String k : keys) {
            Object o = intent.getExtras().get(k);
            if (o instanceof Number) {
                int v = (int) Math.round(((Number) o).doubleValue());
                Integer norm = normalizeSpO2(v);
                if (norm != null) return norm;
            }
        }
        return null;
    }

    /** Try to normalize raw vendor values into a 0â€“100 SpOâ‚‚. */
    private Integer normalizeSpO2(int raw) {
        if (raw <= 0) return null;

        // Common patterns seen on devices:
        //  95..100     -> already %
        //  950..1000   -> value / 10
        //  9500..10000 -> value / 100
        if (raw <= 100) return raw;
        if (raw >= 900 && raw <= 1000) return raw / 10;
        if (raw >= 9000 && raw <= 10000) return raw / 100;

        // If device returns e.g. 120 meaning 120% (nonsense), clamp
        if (raw > 100 && raw < 200) return Math.min(raw, 100);

        return null;
    }

    /** Extract SpOâ‚‚ from a SensorEvent with multiple index/range strategies. */
    private Integer parseSpO2FromEvent(SensorEvent e) {
        if (e == null || e.values == null) return null;

        // Try up to first 5 channels
        int len = Math.min(e.values.length, 5);
        for (int i = 1; i < len; i++) { // indices 1..4 are common for aux streams
            int v = (int) (e.values[i] + 0.5f);
            Integer norm = normalizeSpO2(v);
            if (norm != null) return norm;
        }
        return null;
    }

    public interface WearListener {
        void onWorn();
        void onNotWorn();
        // Optional:
        default void onDisconnected() {}
        default void onBluetoothOff() {}
    }

    public void setWearListener(WearListener listener) {
        this.wearListener = listener;
    }

    public WearState getCurrentWearState() {
        return currentWearState;
    }

    // When your detection logic decides:
    private void updateWearState(WearState state) {
        currentWearState = state;
        if (wearListener != null) {
            if (state == WearState.WORN) wearListener.onWorn();
            else if (state == WearState.NOT_WORN) wearListener.onNotWorn();
        }
    }
}
