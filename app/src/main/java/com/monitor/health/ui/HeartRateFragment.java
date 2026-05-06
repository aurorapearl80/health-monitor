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
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.lifecycle.ViewModelProvider;

import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.View;
import android.widget.TextView;

import com.airbnb.lottie.LottieDrawable;
import com.monitor.health.ApiClient;
import com.monitor.health.Constant;
import com.monitor.health.NetworkUtils;
import com.monitor.health.R;
import com.monitor.health.ReadingsRequest;
import com.monitor.health.adapter.HealthManager;
import com.monitor.health.chart.HeartRateChartView;
import com.monitor.health.database.DatabaseClient;
import com.monitor.health.databinding.FragmentHeartRateBinding;
import com.monitor.health.entity.HeartRateJarEntity;
import com.monitor.health.model.Reading;
import com.monitor.health.utility.DeviceUtils;
import com.monitor.health.viewmodel.SharedDataViewModel;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class HeartRateFragment extends BaseFragment {

    private static final String TAG = "HeartRateFragment";

    // --- Wear detection (sensor-based) ---
    //private static final long WEAR_INACTIVITY_MS = 20_000L; // 20s without signal => NOT_WORN
    private static final long WEAR_INACTIVITY_MS = 5_000L; // 5s without signal => NOT_WORN
    private SensorManager sensorManager;
    private Sensor heartSensor;
    private final Handler main = new Handler(Looper.getMainLooper());
    private Runnable wearInactivity;
    private boolean sensorListening = false;

    private final Handler uiHandler = new Handler(Looper.getMainLooper());
    private static final long MIN_LOADER_MS = 1500L; // show loader at least 1.5s
    private long measureStartMs = 0L;
    private boolean measuring = false;

    private final SensorEventListener wearSensorListener = new SensorEventListener() {
        @Override public void onSensorChanged(SensorEvent e) {
            int bpm = (int) (e.values != null && e.values.length > 0 ? e.values[0] + 0.5f : 0);
            //Log.d(TAG, "HR sensor event bpm=" + bpm);

            if (bpm > 0) {
                // WORN: hide overlay + update VM
                setWatchState(WatchStateOverlayView.State.HIDDEN);
                if (model != null) model.setWorn();

                // re-arm inactivity timer
                armWearInactivityTimer();
            }
            // If bpm==0 we don't immediately flip to NOT_WORN; timer will handle it.
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

        // Permission check (BODY_SENSORS is required on many devices/SDKs)
        if (ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.BODY_SENSORS)
                != PackageManager.PERMISSION_GRANTED) {
            Log.w(TAG, "BODY_SENSORS not granted -> show NO_PERMISSION");
            //setWatchState(WatchStateOverlayView.State.NO_PERMISSION);
            if (model != null) model.setNoPermission();
            return;
        }

        sensorManager = (SensorManager) requireContext().getSystemService(android.content.Context.SENSOR_SERVICE);
        heartSensor = (sensorManager != null) ? sensorManager.getDefaultSensor(Sensor.TYPE_HEART_RATE) : null;

        if (sensorManager == null || heartSensor == null) {
            Log.w(TAG, "No heart-rate sensor -> show CONNECTING/NOT_WORN fallback");
            //setWatchState(WatchStateOverlayView.State.CONNECTING);
            if (model != null) model.setDisconnected();
            return;
        }

        boolean ok = sensorManager.registerListener(
                wearSensorListener, heartSensor, SensorManager.SENSOR_DELAY_NORMAL);
        sensorListening = ok;

        // Until we get a positive reading, assume NOT_WORN so overlay shows
        setWatchState(WatchStateOverlayView.State.NOT_WORN);
        if (model != null) model.setNotWorn();
        armWearInactivityTimer();

        Log.d(TAG, "HR sensor listening started: " + ok);
    }

    private void stopWearSensor() {
        if (!sensorListening) return;
        try {
            if (sensorManager != null) sensorManager.unregisterListener(wearSensorListener);
        } catch (Throwable ignored) {}
        sensorListening = false;
        if (wearInactivity != null) main.removeCallbacks(wearInactivity);
        wearInactivity = null;
        Log.d(TAG, "HR sensor listening stopped");
    }

    // --- Existing members ---
    private FragmentHeartRateBinding binding;
    private HeartRateChartView heartRateChart;
    private TextView currentBpmText;
    private Handler handler;
    private Runnable heartRateUpdater;
    private SharedDataViewModel model;
    DatabaseClient databaseClient;

    @Override
    protected int getContentLayoutResId() {
        return R.layout.fragment_heart_rate;
    }

    @Override
    protected void onBaseViewCreated(@NonNull View root) {
        databaseClient = DatabaseClient.getInstance(getActivity());

        View content = getContentContainer().getChildAt(0);
        binding = FragmentHeartRateBinding.bind(content);

        heartRateChart = binding.heartRateChart;
        currentBpmText = binding.currentBpm;

        initializeHeartRateData();
        startHeartRateUpdates();
        getLatestHeartRate();

        // make sure it is stopped initially
        //binding.heartRateLine.cancelAnimation();
       // binding.heartRateLine.setProgress(0f); // reset to first frame
        //binding.heartRateLine.setRepeatCount(LottieDrawable.INFINITE); // keep looping while measuring (optional)
        //binding.heartRateLine.setSpeed(1.0f);  // normal speed (optional)

        model = new ViewModelProvider(requireActivity()).get(SharedDataViewModel.class);
        model.getHeartRateMonitor().observe(getViewLifecycleOwner(), heartRate -> {
            heartRateChart.addHeartRateReading(heartRate);
            currentBpmText.setText(String.valueOf(heartRate));
        });

        binding.btnMeasure.setOnClickListener(btnView -> {
            if (measuring) return;                  // avoid double-tap
            measuring = true;
            binding.btnMeasure.setEnabled(false);

            measureStartMs = android.os.SystemClock.uptimeMillis();

            // Show loader + start Lottie
            binding.tvStatus.setText("Measuringâ€¦");
            binding.progress.setVisibility(View.VISIBLE);
            //binding.heartRateLine.setProgress(0f);
            //binding.heartRateLine.playAnimation();

            HealthManager.getInstance(requireContext())
                    .startHeartRateMeasurement(new HealthManager.ValueCallback<Integer>() {
                        @Override
                        public void onValue(Integer bpm) {
                            long elapsed = android.os.SystemClock.uptimeMillis() - measureStartMs;
                            long delay = Math.max(0L, MIN_LOADER_MS - elapsed);

                            uiHandler.postDelayed(() -> {
                                binding.progress.setVisibility(View.GONE);
                                binding.tvValue.setText(bpm + " BPM");
                                binding.tvStatus.setText("Done");

                                //binding.heartRateLine.cancelAnimation();
                                //binding.heartRateLine.setProgress(1f); // optional final frame

                                if (bpm != 0) sendHeartRateSync(bpm);

                                measuring = false;
                                binding.btnMeasure.setEnabled(true);
                            }, delay);
                        }

                        @Override
                        public void onError(String error) {
                            long elapsed = android.os.SystemClock.uptimeMillis() - measureStartMs;
                            long delay = Math.max(0L, MIN_LOADER_MS - elapsed);

                            uiHandler.postDelayed(() -> {
                                binding.progress.setVisibility(View.GONE);
                                binding.tvStatus.setText(error);

                                //binding.heartRateLine.cancelAnimation();
                               // binding.heartRateLine.setProgress(0f);

                                measuring = false;
                                binding.btnMeasure.setEnabled(true);
                            }, delay);
                        }
                    });
        });

        // If you ALSO had a HealthManager wear listener, remove it to avoid double control.
        // We now rely on the sensor listener above as the single source of truth in this fragment.
    }

    // Start sensor listening with the Fragment's visible lifecycle
    @Override public void onStart() {
        super.onStart();
        startWearSensor();
    }

    @Override public void onStop() {
        super.onStop();
        stopWearSensor();
    }

    @Override
    public void onPause() {
        super.onPause();
        stopHeartRateUpdates();
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        stopHeartRateUpdates();
        stopWearSensor();
        heartRateChart = null;
        currentBpmText = null;
        binding = null;
        uiHandler.removeCallbacksAndMessages(null); // avoid stray callbacks
    }

    // ---- your existing code below ----

    private void sendHeartRateSync(Integer heartRateValue) {
        Log.d(TAG, "Sending heart rate data synchronously " + heartRateValue);
        try {
            List<Double> heartRateList = new ArrayList<>();
            heartRateList.add(0.0);
            heartRateList.add((double) heartRateValue);

            String token = "bNWZsV#BeZvaNb*gF@3Z^7tCNhCT29Vw8Vi%4T%";
            Reading reading = new Reading(
                    false,
                    "Asia/Manila",
                    "jtm00025b94050c",
                    heartRateList,
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
                        Log.d(TAG, "âœ… Heart rate data sent successfully");
                        playNotificationSound();
                    } else {
                        Log.e(TAG, "âŒ Server returned error: " + response.code() + " - " + response.message());
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

    private void initializeHeartRateData() { loadHeartRateDataAsync(); }
    private void startHeartRateUpdates() { /* your impl */ }

    private void stopHeartRateUpdates() {
        if (handler != null && heartRateUpdater != null) {
            handler.removeCallbacks(heartRateUpdater);
            handler = null;
            heartRateUpdater = null;
        }
    }

    public void onHeartRateReading(float bpm) {
        if (binding != null && isAdded()) {
            heartRateChart.addHeartRateReading(bpm);
            currentBpmText.setText(String.valueOf((int) bpm));
        }
    }

    public static HeartRateFragment newInstance() { return new HeartRateFragment(); }

    private float[] convertToFloatArray(List<HeartRateJarEntity> list) {
        if (list == null || list.isEmpty()) return new float[0];
        float[] result = new float[list.size()];
        for (int i = 0; i < list.size(); i++) { result[i] = (float) list.get(i).getValue(); }
        return result;
    }

    private void loadHeartRateDataAsync() {
        ExecutorService executor = Executors.newSingleThreadExecutor();
        android.os.Handler mainHandler = new android.os.Handler(Looper.getMainLooper());
        executor.execute(() -> {
            try {
                List<HeartRateJarEntity> list = DatabaseClient
                        .getInstance(getActivity())
                        .getAppDatabase()
                        .heartRateJarDao()
                        .getAllHeartRate();
                float[] sampleData = convertToFloatArray(list);
                mainHandler.post(() -> {
                    if (sampleData.length > 0) {
                        heartRateChart.updateHeartRateData(sampleData);
                        Log.d("HeartRateChart", "Updated chart with " + sampleData.length + " database values");
                    } else {
                        loadSampleData();
                    }
                });
            } catch (Exception e) {
                Log.e("HeartRateChart", "Error loading data: " + e.getMessage());
                mainHandler.post(this::loadSampleData);
            }
        });
    }

    private void loadSampleData() {
        float[] sampleData = { 55, 58 };
        heartRateChart.updateHeartRateData(sampleData);
        Log.d("HeartRateChart", "Using sample data");
    }

    @Override
    public void onResume() {
        super.onResume();
        updateConnectionIcon();
    }

    private void updateConnectionIcon() {
        if (binding == null) return;
        NetworkUtils.ConnectionQuality quality = NetworkUtils.getConnectionQuality(requireContext());
        android.widget.ImageView iv = binding.ivConnectionStatus;
        if (quality == NetworkUtils.ConnectionQuality.STRONG) {
            iv.setImageResource(R.drawable.ic_signal_strong);
            iv.setVisibility(android.view.View.VISIBLE);
        } else if (quality == NetworkUtils.ConnectionQuality.WEAK) {
            iv.setImageResource(R.drawable.ic_signal_weak);
            iv.setVisibility(android.view.View.VISIBLE);
            NetworkUtils.showSlowConnectionToast(requireContext());
        } else {
            iv.setVisibility(android.view.View.INVISIBLE);
        }
    }

    private void getLatestHeartRate() {
        HeartRateJarEntity data = DatabaseClient
                .getInstance(getActivity())
                .getAppDatabase()
                .heartRateJarDao()
                .getLatestHeartRate();
        if (data != null) {
            binding.restingBpm.setText(String.valueOf(Math.round(data.getValue())));
        }
    }
}
