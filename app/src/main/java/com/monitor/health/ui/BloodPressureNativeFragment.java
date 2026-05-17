package com.monitor.health.ui;

import static com.monitor.health.utility.AppUtils.getTodayDate;

import android.content.Intent;
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

import com.monitor.health.ApiClient;
import com.monitor.health.Constant;
import com.monitor.health.R;
import com.monitor.health.adapter.HealthManager;
import com.monitor.health.chart.HeartRateChartView;
import com.monitor.health.database.DatabaseClient;
import com.monitor.health.databinding.FragmentBloodPressureNativeBinding;
import com.monitor.health.entity.HeartRateJarEntity;
import com.monitor.health.model.BPJumper;
import com.monitor.health.request.BloodPressureRequest;
import com.monitor.health.response.bloodpressure.BloodPressureResponse;
import com.monitor.health.utility.BPReading;
import com.monitor.health.utility.BloodPressureEstimator;
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

public class BloodPressureNativeFragment extends BaseFragment {

    private static final String TAG = "BloodPressureFragment";

    private FragmentBloodPressureNativeBinding binding;
    private HeartRateChartView heartRateChart;
    private TextView currentBpmText;
    private SharedDataViewModel model;
    private DatabaseClient databaseClient;

    private Handler handler;
    private Runnable heartRateUpdater;

    private final Handler uiHandler = new Handler(Looper.getMainLooper());
    private static final long MIN_LOADER_MS = 1500L;
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

        View content = getContentContainer().getChildAt(0);
        binding = FragmentBloodPressureNativeBinding.bind(content);

        heartRateChart = binding.heartRateChart;
        currentBpmText = binding.currentBpm;

        initializeHeartRateData();
        startHeartRateUpdates();
        getLatestHeartRate();

        model = new ViewModelProvider(requireActivity()).get(SharedDataViewModel.class);
        model.getHeartRateMonitor().observe(getViewLifecycleOwner(), heartRate -> {
            if (binding == null) return;
            if (heartRateChart != null) heartRateChart.addHeartRateReading(heartRate);
            if (currentBpmText != null) currentBpmText.setText(String.valueOf(heartRate));
        });

        binding.btnMeasure.setOnClickListener(v -> startMeasuring());
    }

    // ===== Measuring logic =====

    private void startMeasuring() {
        if (measuring || binding == null) return;
        measuring = true;
        binding.btnMeasure.setEnabled(false);
        measureStartMs = android.os.SystemClock.uptimeMillis();

        binding.tvStatus.setText("Measuring…");
        binding.progress.setVisibility(View.VISIBLE);

        HealthManager.getInstance(requireContext())
                .startHeartRateMeasurement(new HealthManager.ValueCallback<Integer>() {
                    @Override
                    public void onValue(Integer bpm) {
                        long elapsed = android.os.SystemClock.uptimeMillis() - measureStartMs;
                        long delay   = Math.max(0L, MIN_LOADER_MS - elapsed);

                        uiHandler.postDelayed(() -> {
                            if (binding == null) return;

                            if (bpm != null && bpm > 0) {
                                BloodPressureEstimator estimator = new BloodPressureEstimator();
                                BPReading bp = estimator.estimateBloodPressure(bpm);

                                binding.tvValue.setText(
                                        "Systolic: " + bp.systolic + "\nDiastolic: " + bp.diastolic);

                                // Save locally, broadcast to UI, then send to server
                                sendBloodPressure(bp.systolic, bp.diastolic, bpm);
                            } else {
                                binding.tvValue.setText("--");
                            }

                            binding.progress.setVisibility(View.GONE);
                            binding.tvStatus.setText("Done");
                            measuring = false;
                            binding.btnMeasure.setEnabled(true);
                        }, delay);
                    }

                    @Override
                    public void onError(String error) {
                        long elapsed = android.os.SystemClock.uptimeMillis() - measureStartMs;
                        long delay   = Math.max(0L, MIN_LOADER_MS - elapsed);

                        uiHandler.postDelayed(() -> {
                            if (binding == null) return;
                            binding.progress.setVisibility(View.GONE);
                            binding.tvStatus.setText(error);
                            measuring = false;
                            binding.btnMeasure.setEnabled(true);
                        }, delay);
                    }
                });
    }

    // ===== Send blood pressure (same pattern as BleScanService.sendBPJumper) =====

    private void sendBloodPressure(double systolic, double diastolic, int bpm) {
        String serial    = DeviceUtils.getIMEI(requireContext());
        String androidId = DeviceUtils.getIMEI(requireContext());

        // 1. Save locally so UI and offline sync always have the data
        saveDataBPJumper((int) systolic, (int) diastolic, bpm, 1, serial);

        // 2. Broadcast immediately so MainActivity / fragments update without waiting for the server
        ArrayList<Double> bloodpressureList =
                new ArrayList<>(Arrays.asList(systolic, diastolic, (double) bpm));
        Intent fallIntent = new Intent(Constant.ACTION_BLOOD_PRESSURE);
        fallIntent.setPackage(requireContext().getPackageName());
        fallIntent.putExtra(Constant.VALUE_BLOOD_PRESSURE, bloodpressureList);
        requireContext().sendBroadcast(fallIntent);

        // 3. Send to server
        BloodPressureRequest request = new BloodPressureRequest(
                getTodayDate(),
                serial,
                systolic,
                diastolic,
                "66437be266c8833a1c42d7aa",
                bpm,
                "Asia/Manila"
        );

        Call<BloodPressureResponse> call = ApiClient
                .getUserService(Constant.BASE_URL_BGM, Constant.TOKEN_DR_WATCH_API, androidId)
                .sendBloodPressure(request);

        call.enqueue(new Callback<BloodPressureResponse>() {
            @Override
            public void onResponse(Call<BloodPressureResponse> call,
                                   Response<BloodPressureResponse> response) {
                Log.d(TAG, "sendBloodPressure response success="
                        + response.isSuccessful() + " code=" + response.code());
                if (response.isSuccessful()) {
                    playNotificationSound();
                }
            }

            @Override
            public void onFailure(Call<BloodPressureResponse> call, Throwable t) {
                Log.e(TAG, "sendBloodPressure server error: " + t.getMessage());
            }
        });
    }

    private void saveDataBPJumper(int systolic, int diastolic, int pulseRate,
                                   int status, String serial) {
        try {
            BPJumper bpJumper = new BPJumper(systolic, diastolic, pulseRate, status, serial);
            databaseClient.getAppDatabase().bpJumperDao().insertBPJumper(bpJumper);
        } catch (Exception e) {
            Log.e(TAG, "saveDataBPJumper error: " + e.getMessage());
        }
    }

    private void playNotificationSound() {
        try {
            Uri notification = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION);
            Ringtone ringtone = RingtoneManager.getRingtone(requireContext(), notification);
            ringtone.play();
        } catch (Exception ignored) {}
    }

    // ===== Chart / DB helpers =====

    private void initializeHeartRateData() { loadHeartRateDataAsync(); }
    private void startHeartRateUpdates()   { /* placeholder */ }

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

    public static BloodPressureNativeFragment newInstance() {
        return new BloodPressureNativeFragment();
    }

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
        heartRateChart.updateHeartRateData(new float[]{55, 58});
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
