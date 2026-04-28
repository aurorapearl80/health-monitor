package com.monitor.health.ui;

import static com.monitor.health.utility.AppUtils.getTodayDate;

import android.media.Ringtone;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.View;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.lifecycle.ViewModelProvider;

import com.airbnb.lottie.LottieDrawable;
import com.monitor.health.ApiClient;
import com.monitor.health.Constant;
import com.monitor.health.R;
import com.monitor.health.ReadingsRequest;
import com.monitor.health.adapter.HealthManager;
import com.monitor.health.chart.HeartRateChartView;
import com.monitor.health.database.DatabaseClient;
import com.monitor.health.databinding.FragmentBloodPressureNativeBinding;
import com.monitor.health.entity.HeartRateJarEntity;
import com.monitor.health.model.Reading;
import com.monitor.health.utility.BPReading;
import com.monitor.health.utility.BloodPressureEstimator;
import com.monitor.health.utility.DeviceUtils;
import com.monitor.health.viewmodel.SharedDataViewModel;

import java.util.Arrays;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class BloodPressureNativeFragment extends BaseFragment {

    private static final String TAG = "BloodPressureFragment";

    // ----- UI / VM / DB -----
    private FragmentBloodPressureNativeBinding binding;
    private HeartRateChartView heartRateChart;
    private TextView currentBpmText;
    private SharedDataViewModel model;
    private DatabaseClient databaseClient;

    // ----- Chart update placeholders (kept from your code) -----
    private Handler handler;
    private Runnable heartRateUpdater;

    // ----- UX timing for loader/Lottie -----
    private final Handler uiHandler = new Handler(Looper.getMainLooper());
    private static final long MIN_LOADER_MS = 1500L;  // show loader at least 1.5s
    private long measureStartMs = 0L;
    private boolean measuring = false;

    // ===== BaseFragment hooks =====
    @Override
    protected int getContentLayoutResId() {
        return R.layout.fragment_blood_pressure_native;
    }

    @Override
    protected void onBaseViewCreated(@NonNull View root) {
        databaseClient = DatabaseClient.getInstance(getActivity());

        // Bind to child content inflated by BaseFragment
        View content = getContentContainer().getChildAt(0);
        binding = FragmentBloodPressureNativeBinding.bind(content);

        // Chart refs (if used by your layout)
        heartRateChart = binding.heartRateChart;
        currentBpmText = binding.currentBpm;

        // Lottie initial state (no autoplay)
       // binding.heartRateLine.cancelAnimation();
        //binding.heartRateLine.setProgress(0f);
        //binding.heartRateLine.setRepeatCount(LottieDrawable.INFINITE);
        //binding.heartRateLine.setSpeed(1.0f);

        // Data wire-ups
        initializeHeartRateData();
        startHeartRateUpdates();
        getLatestHeartRate();

        model = new ViewModelProvider(requireActivity()).get(SharedDataViewModel.class);
        model.getHeartRateMonitor().observe(getViewLifecycleOwner(), heartRate -> {
            if (binding == null) return;
            if (heartRateChart != null) heartRateChart.addHeartRateReading(heartRate);
            if (currentBpmText != null) currentBpmText.setText(String.valueOf(heartRate));
        });

        // Measure button
        binding.btnMeasure.setOnClickListener(v -> startMeasuring());
    }

    // ===== Measuring logic (HR -> estimate BP -> send) =====
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
                .startHeartRateMeasurement(new HealthManager.ValueCallback<Integer>() {
                    @Override
                    public void onValue(Integer bpm) {
                        long elapsed = android.os.SystemClock.uptimeMillis() - measureStartMs;
                        long delay = Math.max(0L, MIN_LOADER_MS - elapsed);

                        uiHandler.postDelayed(() -> {
                            if (binding == null) return;

                            if (bpm != 0) {
                                // Estimate BP from HR (your existing logic)
                                BloodPressureEstimator estimator = new BloodPressureEstimator();
                                BPReading bp = estimator.estimateBloodPressure(bpm);

                                binding.tvValue.setText("Systolic: " + bp.systolic + "\nDiastolic: " + bp.diastolic);
                                sendBloodPressureRateSync(bp.systolic, bp.diastolic);
                            } else {
                                binding.tvValue.setText("--");
                            }

                            binding.progress.setVisibility(View.GONE);
                            binding.tvStatus.setText("Done");

                           // binding.heartRateLine.cancelAnimation();
                            //binding.heartRateLine.setProgress(1f);

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

                           // binding.heartRateLine.cancelAnimation();
                            //binding.heartRateLine.setProgress(0f);

                            measuring = false;
                            binding.btnMeasure.setEnabled(true);
                        }, delay);
                    }
                });
    }

    // ===== Network sync =====
    private void sendBloodPressureRateSync(Integer systolic, Integer diastolic) {
        Log.d(TAG, "Sending BP synchronously: " + systolic + "/" + diastolic);
        try {
            String token = "bNWZsV#BeZvaNb*gF@3Z^7tCNhCT29Vw8Vi%4T%";
            Reading reading = new Reading(
                    false,
                    "Asia/Manila",
                    "jtm00025b94050c",
                    Arrays.asList((double) systolic, (double) diastolic, 0.0),
                    "66437be266c8833a1c42d7aa",
                    "5bb306382598931ffbd1b624",
                    getTodayDate(),
                    DeviceUtils.getIMEI(requireContext())
            );
            ReadingsRequest payload = new ReadingsRequest(Arrays.asList(reading));

            ApiClient.getUserService(Constant.BASE_URL_BGM, token, DeviceUtils.getIMEI(requireContext()))
                    .sendReadings(payload)
                    .enqueue(new Callback<Object>() {
                        @Override
                        public void onResponse(Call<Object> call, Response<Object> response) {
                            if (response.isSuccessful()) {
                                Log.d(TAG, "âœ… BP data sent successfully");
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
        } catch (Exception ignored) {}
    }

    // ===== Chart/sample data helpers (kept from your code) =====
    private void initializeHeartRateData() { loadHeartRateDataAsync(); }
    private void startHeartRateUpdates() { /* no-op placeholder for your chart timing */ }

    private void stopHeartRateUpdates() {
        if (handler != null && heartRateUpdater != null) {
            handler.removeCallbacks(heartRateUpdater);
            handler = null;
            heartRateUpdater = null;
        }
    }

    @Override
    public void onPause() {
        super.onPause();
        stopHeartRateUpdates();
        //if (binding != null) binding.heartRateLine.cancelAnimation();
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        stopHeartRateUpdates();
        uiHandler.removeCallbacksAndMessages(null);
        heartRateChart = null;
        currentBpmText = null;
        binding = null;
    }

    public void onHeartRateReading(float bpm) {
        if (binding != null && isAdded()) {
            if (heartRateChart != null) heartRateChart.addHeartRateReading(bpm);
            if (currentBpmText != null) currentBpmText.setText(String.valueOf((int) bpm));
        }
    }

    public static BloodPressureNativeFragment newInstance() { return new BloodPressureNativeFragment(); }

    private float[] convertToFloatArray(List<HeartRateJarEntity> list) {
        if (list == null || list.isEmpty()) return new float[0];
        float[] result = new float[list.size()];
        for (int i = 0; i < list.size(); i++) result[i] = (float) list.get(i).getValue();
        return result;
    }

    private void loadHeartRateDataAsync() {
        ExecutorService executor = Executors.newSingleThreadExecutor();
        Handler mainHandler = new Handler(Looper.getMainLooper());
        executor.execute(() -> {
            try {
                List<HeartRateJarEntity> list = DatabaseClient
                        .getInstance(getActivity())
                        .getAppDatabase()
                        .heartRateJarDao()
                        .getAllHeartRate();
                float[] sampleData = convertToFloatArray(list);
                mainHandler.post(() -> {
                    if (binding == null) return;
                    if (sampleData.length > 0) {
                        heartRateChart.updateHeartRateData(sampleData);
                        Log.d(TAG, "Updated chart with " + sampleData.length + " DB values");
                    } else {
                        loadSampleData();
                    }
                });
            } catch (Exception e) {
                Log.e(TAG, "Error loading HR data: " + e.getMessage());
                mainHandler.post(this::loadSampleData);
            }
        });
    }

    private void loadSampleData() {
        if (binding == null) return;
        float[] sampleData = {55, 58};
        heartRateChart.updateHeartRateData(sampleData);
        Log.d(TAG, "Using sample data");
    }

    private void getLatestHeartRate() {
        HeartRateJarEntity data = DatabaseClient
                .getInstance(getActivity())
                .getAppDatabase()
                .heartRateJarDao()
                .getLatestHeartRate();
        if (binding != null && data != null) {
            binding.restingBpm.setText(String.valueOf(Math.round(data.getValue())));
        }
    }
}
