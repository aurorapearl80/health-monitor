package com.monitor.health.ui;

import static com.monitor.health.utility.AppUtils.getTodayDate;

import android.content.SharedPreferences;
import android.media.Ringtone;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.View;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.lifecycle.ViewModelProvider;

import com.airbnb.lottie.LottieDrawable;
import com.monitor.health.ApiClient;
import com.monitor.health.Constant;
import com.monitor.health.R;
import com.monitor.health.ReadingsRequest;
import com.monitor.health.adapter.HealthManager;
import com.monitor.health.databinding.FragmentStepsBinding;
import com.monitor.health.model.Reading;
import com.monitor.health.utility.DeviceUtils;
import com.monitor.health.viewmodel.SharedDataViewModel;

import java.util.Arrays;
import java.util.List;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class StepsFragment extends BaseFragment {

    private static final String TAG = "StepsFragment";

    private FragmentStepsBinding binding;
    private SharedDataViewModel model;

    // Loader/Lottie timing
    private final Handler uiHandler = new Handler(Looper.getMainLooper());
    private static final long MIN_LOADER_MS = 1500L; // keep loader visible at least 1.5s
    private long measureStartMs = 0L;
    private boolean measuring = false;

    // ===== BaseFragment hooks =====
    @Override
    protected int getContentLayoutResId() {
        return R.layout.fragment_steps;
    }

    @Override
    protected void onBaseViewCreated(@NonNull View root) {
        // Bind to child content inflated by BaseFragment
        View content = getContentContainer().getChildAt(0);
        binding = FragmentStepsBinding.bind(content);

        // Lottie initial state (do NOT autoplay)
        if (binding.heartRateLine != null) {
            //binding.heartRateLine.cancelAnimation();
           // binding.heartRateLine.setProgress(0f);
            //binding.heartRateLine.setRepeatCount(LottieDrawable.INFINITE);
            //binding.heartRateLine.setSpeed(1.0f);
        }

        model = new ViewModelProvider(requireActivity()).get(SharedDataViewModel.class);
        model.getStepCount().observe(getViewLifecycleOwner(), step -> {
            if (binding == null) return;
            binding.heart.setText(String.valueOf(step));
        });

        binding.btnMeasure.setOnClickListener(v -> startFetchingSteps());
    }

    private void startFetchingSteps() {
        if (measuring || binding == null) return;
        measuring = true;
        binding.btnMeasure.setEnabled(false);

        measureStartMs = android.os.SystemClock.uptimeMillis();

        binding.tvStatus.setText("Fetchingâ€¦");
        binding.progress.setVisibility(View.VISIBLE);

        if (binding.heartRateLine != null) {
            //binding.heartRateLine.setProgress(0f);
            //binding.heartRateLine.playAnimation();
        }

        HealthManager.getInstance(requireContext()).getSteps(new HealthManager.ValueCallback<Integer>() {
            @Override
            public void onValue(Integer steps) {
                long elapsed = android.os.SystemClock.uptimeMillis() - measureStartMs;
                long delay = Math.max(0L, MIN_LOADER_MS - elapsed);

                uiHandler.postDelayed(() -> {
                    if (binding == null) return;

                    binding.progress.setVisibility(View.GONE);
                    binding.tvValue.setText(String.valueOf(steps));
                    binding.tvStatus.setText("Done");

                    if (binding.heartRateLine != null) {
                        //binding.heartRateLine.cancelAnimation();
                        //binding.heartRateLine.setProgress(1f);
                    }
                    Toast.makeText(getContext(), "Sending steps: " + steps, Toast.LENGTH_SHORT).show();
                    if (steps != null && steps > 0) {
                        SharedPreferences prefs = requireContext().getSharedPreferences("steps_prefs", android.content.Context.MODE_PRIVATE);
                        int lastStepCount = prefs.getInt("last_step_count", 0);

                        int delta;
                        if (steps < lastStepCount) {
                            // Device restarted â€” step counter was reset to zero
                            delta = steps;
                        } else {
                            delta = steps - lastStepCount;
                        }

                        if (delta > 0) {
                            prefs.edit().putInt("last_step_count", steps).apply();
                            sendStepsSync(delta);
                        }
                    }

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

                    if (binding.heartRateLine != null) {
                        //binding.heartRateLine.cancelAnimation();
                        //binding.heartRateLine.setProgress(0f);
                    }

                    measuring = false;
                    binding.btnMeasure.setEnabled(true);
                }, delay);
            }
        });
    }

    private void sendStepsSync(Integer steps) {
        Toast.makeText(getContext(), "Sending steps: " + steps, Toast.LENGTH_SHORT).show();
        Log.d(TAG, "Sending steps synchronously: " + steps);
        try {
            String token = "bNWZsV#BeZvaNb*gF@3Z^7tCNhCT29Vw8Vi%4T%";

            Reading reading = new Reading(
                    false,
                    "Asia/Manila",
                    "jtm00025b94050c",
                    // API expects a list; here we send [steps]
                    Arrays.asList((double) steps),
                    "66437be266c8833a1c42d7aa",
                    "5bb306382598931ffbd1b629",
                    getTodayDate(),
                    DeviceUtils.getIMEI(requireContext())
            );

            ReadingsRequest payload = new ReadingsRequest(List.of(reading));

            ApiClient.getUserService(Constant.BASE_URL_BGM, token, DeviceUtils.getIMEI(requireContext()))
                    .sendReadings(payload)
                    .enqueue(new Callback<Object>() {
                        @Override
                        public void onResponse(Call<Object> call, Response<Object> response) {
                            if (response.isSuccessful()) {
                                Log.d(TAG, "âœ… Steps data sent successfully");
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
        } catch (Exception ignored) {}
    }

    // ===== Lifecycle tidy-ups =====
    @Override
    public void onPause() {
        super.onPause();
        if (binding != null && binding.heartRateLine != null) {
            //binding.heartRateLine.cancelAnimation();
        }
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        uiHandler.removeCallbacksAndMessages(null);
        binding = null;
    }
}
