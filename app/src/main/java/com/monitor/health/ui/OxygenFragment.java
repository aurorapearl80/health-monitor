package com.monitor.health.ui;

import static com.monitor.health.utility.AppUtils.getTodayDate;

import android.Manifest;
import android.content.pm.PackageManager;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.media.Ringtone;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.lifecycle.ViewModelProvider;

import com.airbnb.lottie.LottieDrawable;
import com.monitor.health.ApiClient;
import com.monitor.health.Constant;
import com.monitor.health.R;
import com.monitor.health.ReadingsRequest;
import com.monitor.health.adapter.HealthManager;
import com.monitor.health.databinding.FragmentOxygenBinding;
import com.monitor.health.model.Reading;
import com.monitor.health.utility.DeviceUtils;
import com.monitor.health.viewmodel.SharedDataViewModel;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class OxygenFragment extends BaseFragment {

    private static final String TAG = "OxygenFragment";

    // --- Wear detection (sensor-based) ---
    private static final long WEAR_INACTIVITY_MS = 3_000L; // 5s without signal => NOT_WORN
    private SensorManager sensorManager;
    private Sensor heartSensor;
    private final Handler main = new Handler(Looper.getMainLooper());
    private Runnable wearInactivity;
    private boolean sensorListening = false;

    // --- Measure UX timing ---
    private final Handler uiHandler = new Handler(Looper.getMainLooper());
    private static final long MIN_LOADER_MS = 1500L; // show loader at least 1.5s
    private long measureStartMs = 0L;
    private boolean measuring = false;

    // --- VM / binding ---
    private SharedDataViewModel model;
    private FragmentOxygenBinding binding;

    // ===== BaseFragment hooks =====
    @Override
    protected int getContentLayoutResId() {
        return R.layout.fragment_oxygen;
    }

    @Override
    protected void onBaseViewCreated(@NonNull View root) {
        // Bind to the already-inflated child content from BaseFragment
        View content = getContentContainer().getChildAt(0);
        binding = FragmentOxygenBinding.bind(content);

        // Lottie initial state: do NOT autoplay
        //binding.heartRateLine.cancelAnimation();
        //binding.heartRateLine.setProgress(0f);
        //binding.heartRateLine.setRepeatCount(LottieDrawable.INFINITE);
        //binding.heartRateLine.setSpeed(1.0f);

        model = new ViewModelProvider(requireActivity()).get(SharedDataViewModel.class);
        model.getOxygen().observe(getViewLifecycleOwner(), spo2 -> {
            if (binding == null) return;
            binding.heart.setText(String.valueOf(spo2));
        });

        binding.btnMeasure.setOnClickListener(v -> startMeasuring());
    }

    // ===== Measuring logic =====
    private void startMeasuring() {
        if (measuring || binding == null) return;
        measuring = true;
        binding.btnMeasure.setEnabled(false);

        measureStartMs = android.os.SystemClock.uptimeMillis();

        binding.tvStatus.setText("Measuringâ€¦");
        binding.progress.setVisibility(View.VISIBLE);
        //binding.heartRateLine.setProgress(0f);
        //binding.heartRateLine.playAnimation();

        HealthManager.getInstance(requireContext())
                .startBloodOxygenMeasurement(new HealthManager.ValueCallback<Integer>() {
                    @Override
                    public void onValue(Integer spo2) {
                        long elapsed = android.os.SystemClock.uptimeMillis() - measureStartMs;
                        long delay = Math.max(0L, MIN_LOADER_MS - elapsed);

                        uiHandler.postDelayed(() -> {
                            if (binding == null) return;

                            binding.progress.setVisibility(View.GONE);
                            binding.tvValue.setText(spo2 + " %");
                            binding.tvStatus.setText("Done");

                            //binding.heartRateLine.cancelAnimation();
                            //binding.heartRateLine.setProgress(1f);

                            if (spo2 != 0) sendOxygenSync(spo2);

                            measuring = false;
                            binding.btnMeasure.setEnabled(true);
                        }, delay);
                    }

                    @Override
                    public void onError(String error) {
                        long elapsed = android.os.SystemClock.uptimeMillis() - measureStartMs;
                        long delay = Math.max(0L, MIN_LOADER_MS - elapsed);

                        uiHandler.postDelayed(() -> {
                            if (binding == null) return;

                            binding.progress.setVisibility(View.GONE);
                            binding.tvStatus.setText(error);

                            //binding.heartRateLine.cancelAnimation();
                            //binding.heartRateLine.setProgress(0f);

                            measuring = false;
                            binding.btnMeasure.setEnabled(true);
                        }, delay);
                    }
                });
    }

    // ===== Wear detection via HR sensor (for NOT_WORN overlay consistency) =====
    private final SensorEventListener wearSensorListener = new SensorEventListener() {
        @Override public void onSensorChanged(android.hardware.SensorEvent e) {
            int bpm = (int) (e.values != null && e.values.length > 0 ? e.values[0] + 0.5f : 0);
            if (bpm > 0) {
                setWatchState(WatchStateOverlayView.State.HIDDEN);
                if (model != null) model.setWorn();
                armWearInactivityTimer();
            }
        }
        @Override public void onAccuracyChanged(Sensor sensor, int accuracy) {}
    };

    private void armWearInactivityTimer() {
        if (wearInactivity != null) main.removeCallbacks(wearInactivity);
        wearInactivity = () -> {
            Log.d(TAG, "Wear inactivity timeout -> NOT_WORN");
            setWatchState(WatchStateOverlayView.State.NOT_WORN);
            if (model != null) model.setNotWorn();
        };
        main.postDelayed(wearInactivity, WEAR_INACTIVITY_MS);
    }

    private void startWearSensor() {
        if (sensorListening) return;

        if (ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.BODY_SENSORS)
                != PackageManager.PERMISSION_GRANTED) {
            Log.w(TAG, "BODY_SENSORS not granted");
            //setWatchState(WatchStateOverlayView.State.NO_PERMISSION);
            if (model != null) model.setNoPermission();
            return;
        }

        sensorManager = (SensorManager) requireContext().getSystemService(android.content.Context.SENSOR_SERVICE);
        heartSensor = (sensorManager != null) ? sensorManager.getDefaultSensor(Sensor.TYPE_HEART_RATE) : null;

        if (sensorManager == null || heartSensor == null) {
            Log.w(TAG, "No heart-rate sensor available");
            //setWatchState(WatchStateOverlayView.State.CONNECTING);
            if (model != null) model.setDisconnected();
            return;
        }

        boolean ok = sensorManager.registerListener(
                wearSensorListener, heartSensor, SensorManager.SENSOR_DELAY_NORMAL);
        sensorListening = ok;

        // Show NOT_WORN until we get a positive reading
        setWatchState(WatchStateOverlayView.State.NOT_WORN);
        if (model != null) model.setNotWorn();
        armWearInactivityTimer();

        Log.d(TAG, "HR sensor listening started: " + ok);
    }

    private void stopWearSensor() {
        if (!sensorListening) return;
        try { if (sensorManager != null) sensorManager.unregisterListener(wearSensorListener); }
        catch (Throwable ignored) {}
        sensorListening = false;
        if (wearInactivity != null) main.removeCallbacks(wearInactivity);
        wearInactivity = null;
        Log.d(TAG, "HR sensor listening stopped");
    }

    // ===== Lifecycle =====
    @Override public void onStart() {
        super.onStart();
        startWearSensor();
    }

    @Override public void onStop() {
        super.onStop();
        stopWearSensor();
    }

    @Override public void onPause() {
        super.onPause();
        //if (binding != null) binding.heartRateLine.cancelAnimation();
    }

    @Override public void onDestroyView() {
        super.onDestroyView();
        stopWearSensor();
        uiHandler.removeCallbacksAndMessages(null);
        binding = null;
    }

    // ===== Network sync =====
    private void sendOxygenSync(Integer oxygenValue) {
        Log.d(TAG, "Sending oxygen data synchronously " + oxygenValue);
        try {
            List<Double> values = new ArrayList<>();
            values.add((double) oxygenValue);
            values.add(0.0);

            String token = "bNWZsV#BeZvaNb*gF@3Z^7tCNhCT29Vw8Vi%4T%";
            Reading reading = new Reading(
                    false,
                    "Asia/Manila",
                    "jtm00025b94050c",
                    values,
                    "66437be266c8833a1c42d7aa",
                    "5bb306382598931ffbd1b626",
                    getTodayDate(),
                    DeviceUtils.getIMEI(requireContext())
            );
            List<Reading> readingsList = Arrays.asList(reading);
            ReadingsRequest readingsRequest = new ReadingsRequest(readingsList);

            Call<Object> call = ApiClient.getUserService(Constant.BASE_URL_BGM, token,
                    DeviceUtils.getIMEI(requireContext())).sendReadings(readingsRequest);
            call.enqueue(new Callback<Object>() {
                @Override
                public void onResponse(Call<Object> call, Response<Object> response) {
                    if (response.isSuccessful()) {
                        Log.d(TAG, "âœ… Oxygen data sent successfully");
                        playNotificationSound();
                    } else {
                        Log.e(TAG, "âŒ Server error: " + response.code() + " - " + response.message());
                    }
                }
                @Override
                public void onFailure(Call<Object> call, Throwable t) {
                    Log.e(TAG, "âŒ Sync failed", t);
                }
            });

        } catch (Exception e) {
            Log.e(TAG, "âŒ Exception during server sync", e);
        }
    }

    private void playNotificationSound() {
        try {
            Uri notification = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION);
            Ringtone ringtone = RingtoneManager.getRingtone(requireContext(), notification);
            ringtone.play();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
