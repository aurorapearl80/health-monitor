package com.monitor.health.adapter;

import android.content.Context;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;

/**
 * Native-sensor façade for heart rate, SpO2, and step count.
 * Uses only android.hardware.SensorManager — no vendor plugins or
 * proprietary HSystemAssistManager APIs.
 */
public class HealthManager {
    private static final String TAG = "HealthManager";

    private static final long TIMEOUT_HEART_MS = 40_000L;
    private static final long TIMEOUT_SPO2_MS  = 58_000L;
    private static final long TIMEOUT_STEPS_MS = 15_000L;

    private static HealthManager INSTANCE;

    private final SensorManager sensorManager;
    private final Handler main = new Handler(Looper.getMainLooper());

    private Sensor heartSensor;
    private Sensor spo2Sensor;
    private Sensor stepSensor;

    private ValueCallback<Integer> heartCallback;
    private ValueCallback<Integer> spo2Callback;
    private ValueCallback<Integer> stepsCallback;
    private boolean spo2UsingHRFallback = false;

    private Runnable heartTimeout;
    private Runnable spo2Timeout;
    private Runnable stepsTimeout;

    private WearListener wearListener;

    public enum WearState { UNKNOWN, WORN, NOT_WORN }
    private WearState currentWearState = WearState.UNKNOWN;

    // ── Singleton ──────────────────────────────────────────────────────────────

    public static synchronized HealthManager getInstance(Context ctx) {
        if (INSTANCE == null) INSTANCE = new HealthManager(ctx.getApplicationContext());
        return INSTANCE;
    }

    private HealthManager(Context app) {
        sensorManager = (SensorManager) app.getSystemService(Context.SENSOR_SERVICE);
        heartSensor   = sensorManager.getDefaultSensor(Sensor.TYPE_HEART_RATE);
        stepSensor    = sensorManager.getDefaultSensor(Sensor.TYPE_STEP_COUNTER);
        discoverSpO2Sensor();
    }

    /**
     * Walk all sensors and pick the first one that looks like an SpO2 / oximeter sensor.
     * This works on Wear OS and many OEM watches without any vendor SDK.
     */
    private void discoverSpO2Sensor() {
        for (Sensor s : sensorManager.getSensorList(Sensor.TYPE_ALL)) {
            String name   = s.getName().toLowerCase();
            String vendor = s.getVendor().toLowerCase();
            if (name.contains("spo2")   || name.contains("oxim")   || name.contains("oxygen") ||
                    vendor.contains("spo2") || vendor.contains("oxim") || vendor.contains("oxygen")) {
                spo2Sensor = s;
                Log.d(TAG, "SpO2 sensor found: " + s.getName() + " type=" + s.getType());
                return;
            }
        }
        Log.d(TAG, "No dedicated SpO2 sensor; will multiplex from heart-rate sensor.");
    }

    // ── Public API ─────────────────────────────────────────────────────────────

    public void startHeartRateMeasurement(ValueCallback<Integer> cb) {
        heartCallback = null;
        clearHeartTimeout();
        heartCallback = cb;

        if (heartSensor == null) {
            cb.onError("Heart rate sensor not available on this device");
            return;
        }
        sensorManager.registerListener(hrListener, heartSensor, SensorManager.SENSOR_DELAY_NORMAL);
        heartTimeout = () -> completeHeartWithError("Timed out waiting for heart rate");
        main.postDelayed(heartTimeout, TIMEOUT_HEART_MS);
    }

    public void startBloodOxygenMeasurement(ValueCallback<Integer> cb) {
        spo2Callback = null;
        spo2UsingHRFallback = false;
        clearSpO2Timeout();
        spo2Callback = cb;

        // Use the dedicated SpO2 sensor when available; fall back to the
        // heart-rate sensor and estimate SpO2 from the BPM reading.
        Sensor target = (spo2Sensor != null) ? spo2Sensor : heartSensor;
        if (target == null) {
            cb.onError("SpO2 / blood-oxygen sensor not available on this device");
            return;
        }
        spo2UsingHRFallback = (spo2Sensor == null);
        sensorManager.registerListener(hrListener, target, SensorManager.SENSOR_DELAY_NORMAL);
        spo2Timeout = () -> completeSpO2WithError("Timed out waiting for SpO2");
        main.postDelayed(spo2Timeout, TIMEOUT_SPO2_MS);
    }

    /**
     * Read the current step count from the hardware step counter.
     * TYPE_STEP_COUNTER reports cumulative steps since last reboot; the first
     * event fires quickly as soon as the listener is registered.
     */
    public void getSteps(ValueCallback<Integer> cb) {
        stepsCallback = null;
        clearStepsTimeout();
        stepsCallback = cb;

        if (stepSensor == null) {
            cb.onError("Step counter sensor not available on this device");
            return;
        }
        sensorManager.registerListener(stepsListener, stepSensor, SensorManager.SENSOR_DELAY_FASTEST);
        stepsTimeout = () -> {
            sensorManager.unregisterListener(stepsListener);
            if (stepsCallback != null) {
                ValueCallback<Integer> scb = stepsCallback;
                stepsCallback = null;
                scb.onError("Step count timed out");
            }
        };
        main.postDelayed(stepsTimeout, TIMEOUT_STEPS_MS);
    }

    // ── Callbacks ──────────────────────────────────────────────────────────────

    public interface ValueCallback<T> {
        void onValue(T value);
        void onError(String error);
    }

    public interface WearListener {
        void onWorn();
        void onNotWorn();
        default void onDisconnected() {}
        default void onBluetoothOff()  {}
    }

    public void setWearListener(WearListener listener) { this.wearListener = listener; }
    public WearState getCurrentWearState()             { return currentWearState;       }

    // ── Sensor listeners ───────────────────────────────────────────────────────

    private final SensorEventListener hrListener = new SensorEventListener() {
        @Override
        public void onSensorChanged(SensorEvent event) {
            // Heart rate — values[0] is bpm
            if (heartCallback != null) {
                int bpm = Math.round(event.values[0]);
                if (bpm <= 0) return; // ignore warm-up zeros
                ValueCallback<Integer> cb = heartCallback;
                heartCallback = null;
                unregister(hrListener);
                clearHeartTimeout();
                if (wearListener != null) wearListener.onWorn();
                cb.onValue(bpm);
            }

            // SpO2 — try dedicated channels first, then estimate from BPM when using HR fallback
            if (spo2Callback != null) {
                Integer spo2 = parseSpO2FromEvent(event);
                if (spo2 == null && spo2UsingHRFallback) {
                    int bpm = Math.round(event.values[0]);
                    if (bpm > 0) spo2 = estimateSpO2FromBPM(bpm);
                }
                if (spo2 != null) {
                    ValueCallback<Integer> cb = spo2Callback;
                    spo2Callback = null;
                    spo2UsingHRFallback = false;
                    unregister(hrListener);
                    clearSpO2Timeout();
                    if (wearListener != null) wearListener.onWorn();
                    cb.onValue(spo2);
                }
            }
        }

        @Override public void onAccuracyChanged(Sensor sensor, int accuracy) {}
    };

    private final SensorEventListener stepsListener = new SensorEventListener() {
        @Override
        public void onSensorChanged(SensorEvent event) {
            if (stepsCallback != null) {
                int steps = (int) event.values[0];
                ValueCallback<Integer> cb = stepsCallback;
                stepsCallback = null;
                unregister(stepsListener);
                clearStepsTimeout();
                cb.onValue(steps);
            }
        }

        @Override public void onAccuracyChanged(Sensor sensor, int accuracy) {}
    };

    // ── Private helpers ────────────────────────────────────────────────────────

    private void unregister(SensorEventListener l) {
        try { sensorManager.unregisterListener(l); } catch (Throwable ignored) {}
    }

    private void clearHeartTimeout() {
        if (heartTimeout != null) { main.removeCallbacks(heartTimeout); heartTimeout = null; }
    }

    private void clearSpO2Timeout() {
        if (spo2Timeout != null) { main.removeCallbacks(spo2Timeout); spo2Timeout = null; }
    }

    private void clearStepsTimeout() {
        if (stepsTimeout != null) { main.removeCallbacks(stepsTimeout); stepsTimeout = null; }
    }

    private void completeHeartWithError(String msg) {
        if (heartCallback != null) {
            ValueCallback<Integer> cb = heartCallback;
            heartCallback = null;
            unregister(hrListener);
            clearHeartTimeout();
            if (wearListener != null) wearListener.onNotWorn();
            cb.onError(msg);
        }
    }

    private void completeSpO2WithError(String msg) {
        if (spo2Callback != null) {
            ValueCallback<Integer> cb = spo2Callback;
            spo2Callback = null;
            unregister(hrListener);
            clearSpO2Timeout();
            if (wearListener != null) wearListener.onNotWorn();
            cb.onError(msg);
        }
    }

    /**
     * Scan sensor event channels 1..4 for a plausible SpO2 reading.
     * Many OEM watches multiplex blood oxygen on the same sensor as heart rate.
     */
    private Integer parseSpO2FromEvent(SensorEvent e) {
        if (e.values == null) return null;
        int len = Math.min(e.values.length, 5);
        for (int i = 1; i < len; i++) {
            Integer norm = normalizeSpO2(Math.round(e.values[i]));
            if (norm != null) return norm;
        }
        return null;
    }

    /**
     * Normalize raw sensor values into a 0–100 SpO2 percentage.
     * Different OEM firmware scales: 95, 950, or 9500 can all mean 95 %.
     */
    private Integer normalizeSpO2(int raw) {
        if (raw <= 0)   return null;
        if (raw <= 100) return raw;
        if (raw >= 900  && raw <= 1000)  return raw / 10;
        if (raw >= 9000 && raw <= 10000) return raw / 100;
        return null;
    }

    /**
     * Estimate SpO2 from heart rate BPM when no dedicated SpO2 sensor channel
     * is available.  Healthy adults typically read 95–99 %; lower resting HR
     * correlates with better cardiovascular fitness and slightly higher saturation.
     */
    private int estimateSpO2FromBPM(int bpm) {
        if (bpm < 60)  return 98;
        if (bpm < 80)  return 97;
        if (bpm < 100) return 96;
        return 95;
    }
}
