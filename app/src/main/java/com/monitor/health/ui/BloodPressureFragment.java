package com.monitor.health.ui;

import static android.content.Context.BATTERY_SERVICE;
import static android.content.Context.MODE_PRIVATE;

import static com.monitor.health.utility.AppUtils.getTodayDate;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.media.Ringtone;
import android.media.RingtoneManager;
import android.net.ConnectivityManager;
import android.net.Uri;
import android.os.BatteryManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.DrawableRes;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;

import com.monitor.health.ApiClient;
import com.monitor.health.Constant;
import com.monitor.health.MainActivity;
import com.monitor.health.NetworkUtils;
import com.monitor.health.R;
import com.monitor.health.ReadingsRequest;
import com.monitor.health.adapter.HealthManager;
import com.monitor.health.adapter.PageType;
import com.monitor.health.dao.BPJumperDao;
import com.monitor.health.dao.HeartRateDao;
import com.monitor.health.dao.OximeterDao;
import com.monitor.health.dao.TemperatureDao;
import com.monitor.health.database.DatabaseClient;
import com.monitor.health.databinding.FragmentBloodPressureBinding;
import com.monitor.health.model.BPJumper;
import com.monitor.health.model.HeartRateEntity;
import com.monitor.health.model.Oximeter;
import com.monitor.health.model.Reading;
import com.monitor.health.model.ReadingValue;
import com.monitor.health.model.Temperature;
import com.monitor.health.model.WeighingScale;
import com.monitor.health.request.SendAlarmRequest;
import com.monitor.health.response.alldata.RatingInfo;
import com.monitor.health.response.alldata.TypesAvailability;
import com.monitor.health.ui.graph.WeightGraphActivity;
import com.monitor.health.utility.BPReading;
import com.monitor.health.utility.BloodPressureEstimator;
import com.monitor.health.utility.DateUtils;
import com.monitor.health.utility.DeviceUtils;
import com.monitor.health.utility.PreferenceHelper;
import com.monitor.health.utility.QuickActionsHandler;
import com.monitor.health.utility.QuickActionsHelper;
import com.monitor.health.utility.SmartWatchAlertDialog;
import com.monitor.health.utility.TimeAgo;
import com.monitor.health.viewmodel.ReadingsViewModel;
import com.monitor.health.viewmodel.SharedDataViewModel;

import java.text.MessageFormat;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class BloodPressureFragment extends Fragment implements QuickActionsHandler {

    private final String TAG = "BloodPressureFragment";
    private FragmentBloodPressureBinding binding;


    //private TextView imeView,tvBloodPressure,tvBPM;
    DatabaseClient databaseClient;
    SharedDataViewModel model;


    View rowGlucose;
    View rowBloodPressure;
    View rowWeight;
    View rowBloodOxygen;
    View rowEcg;
    View temperature;
    // ----- UX timing for loader/Lottie -----
    private final Handler uiHandler = new Handler(Looper.getMainLooper());
    private static final long MIN_LOADER_MS = 1500L;  // show loader at least 1.5s
    private long measureStartMs = 0L;
    private boolean measuring = false;

    private ReadingsViewModel vm;
    private ConnectivityManager.NetworkCallback networkCallback;

    private QuickActionsHelper quickActionsHelper;
    String _model;                     // e.g., SM-G925I
    String _maker;              // e.g., Samsung
    String osVersion;       // e.g., 4.4, 12, 13
    String _country;
    String androidId;
    private static final int MAX_RETRY_COUNT = 3; // Set max retry attempts
    private int retryCount = 0;
    int batteryPercent = 0;
    SharedPreferences prefs;
    SharedPreferences.Editor editor;

    @Override
    public View onCreateView(
            @NonNull LayoutInflater inflater, ViewGroup container,
            Bundle savedInstanceState
    ) {

        binding = FragmentBloodPressureBinding.inflate(inflater, container, false);
        return binding.getRoot();

    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        databaseClient = DatabaseClient.getInstance(getActivity());
        prefs = requireActivity().getSharedPreferences(Constant.PREFERENCE_BLOOD_GLUCOSE, MODE_PRIVATE);
        editor = prefs.edit();

    }

    public void onViewCreated(@NonNull View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        vm = new ViewModelProvider(this).get(ReadingsViewModel.class);
        // Set the text
        model = new ViewModelProvider(requireActivity()).get(SharedDataViewModel.class);



        View panel = view.findViewById(R.id.metricsPanel);

// Each include now has a real root you can grab by id
        rowGlucose       = panel.findViewById(R.id.rowGlucose);
        rowBloodPressure = panel.findViewById(R.id.rowBloodPressure);
        rowWeight        = panel.findViewById(R.id.rowWeight);
        rowBloodOxygen   = panel.findViewById(R.id.rowBloodOxygen);
        rowEcg           = panel.findViewById(R.id.rowEcg);
        temperature           = panel.findViewById(R.id.temperature);

        binding.chartIcon.setOnClickListener(chart -> {
            Intent intent = new Intent(getActivity(), WeightGraphActivity.class);
            intent.putExtra("backgroundColor", "BloodPressureFragment");
            startActivity(intent);
        });

        vm.getLoading().observe(getViewLifecycleOwner(), isLoading -> {
            // show/hide progress
            Log.d(TAG, "Loading here");
        });

        vm.getError().observe(getViewLifecycleOwner(), err -> {
            if (err != null) Log.d("ERR", err);
        });

        // Glucose
        vm.getGlucose().observe(getViewLifecycleOwner(), g -> {
            // If the value object itself is missing â†’ show defaults and exit
            if (g == null) {

                bindMetric(
                        rowGlucose,
                        R.drawable.ic_blood_drop_foreground,
                        "Blood Glucose",
                        "--",
                        "--",
                        v -> {
                            if (getActivity() instanceof MainActivity) {
                                // Glucose
                                ((MainActivity) getActivity()).navigateTo(PageType.BLOOD_GLUCOSE);
                            }
                        }
                );
                return;
            }

            // If primary value TextView is absent, log for debugging and bail (keeps your original behavior)

            // Build unit/value safely
            String unit = (g.getUnit() != null) ? g.getUnit() : "";
            double rawValue = g.getValue();
            String valueStr;

            // Ensure integer-like numbers render nicely (e.g., 108 not 108.0)
            if (Double.isNaN(rawValue)) {
                valueStr = "--";
            } else {
                valueStr = (rawValue == Math.floor(rawValue))
                        ? String.valueOf((int) rawValue)
                        : String.valueOf(rawValue);
            }

            String glucoseValueText = ("--".equals(valueStr))
                    ? "--"
                    : (valueStr + (unit.isEmpty() ? "" : " " + unit));


            // Time + event description observers
            vm.getGlucoseUpdatedAt().observe(getViewLifecycleOwner(), timeAgo -> {
                String displayTime = (timeAgo != null) ? timeAgo : "--";
                vm.getGlucoseEventDescription().observe(getViewLifecycleOwner(), bpm -> {
                    String bpmText = (bpm != null) ? bpm : "--";
                    Log.d(TAG, "Loading here " + bpmText + " this is the data");

                    // Bind the metric row with safe defaults
                    bindMetric(
                            rowGlucose,
                            R.drawable.ic_blood_drop_foreground,
                            "Blood Glucose",
                            displayTime,                                    // e.g., "a day ago" or "--"
                            "--".equals(valueStr) ? "--" : glucoseValueText, // e.g., "108 mg/dL" or "--"
                            v -> {
                                if (getActivity() instanceof MainActivity) {
                                    // Glucose
                                    ((MainActivity) getActivity()).navigateTo(PageType.BLOOD_GLUCOSE);
                                }
                            }
                    );
                });
            });
        });
        // Blood pressure
        vm.getBloodPressure().observe(getViewLifecycleOwner(), g -> {
            if (g == null || g.size() <= 2 || g.get(0) == null || g.get(2) == null) {
                // Not enough BP data â†’ show defaults
                bindMetric(rowBloodPressure,
                        R.drawable.ic_heart_rate,
                        Constant.BLOOD_PRESSURE,
                        "--",
                        "--/-- mmHg",
                        v -> {
                            if (getActivity() instanceof MainActivity) {
                                // BP
                                ((MainActivity) getActivity()).navigateTo(PageType.BLOOD_PRESSURE);
                            }
                        });

                return;
            }

            vm.getBpUpdatedAt().observe(getViewLifecycleOwner(), timeAgo -> {
                if (timeAgo == null) {
                    // Missing timestamp â†’ show defaults
                    bindMetric(rowBloodPressure,
                            R.drawable.ic_heart_rate,
                            Constant.BLOOD_PRESSURE,
                            "--",
                            "--/-- mmHg",
                            v -> {
                                if (getActivity() instanceof MainActivity) {
                                    // BP
                                    ((MainActivity) getActivity()).navigateTo(PageType.BLOOD_PRESSURE);
                                }
                            });

                    return;
                }

                vm.getBpEventDescription().observe(getViewLifecycleOwner(), bpm -> {
                    if (bpm == null) return;

                    String displayTime = timeAgo;

                    // Prefer unit from systolic if available; fallback to "mmHg"

                    String unit = (g.get(0).getUnit() != null && !g.get(0).getUnit().isEmpty())
                            ? g.get(0).getUnit()
                            : "mmHg";

                    String displayValue = "--/-- " + unit;

                    try {
                        int systolic3 = (int) Math.round(g.get(0).getValue());
                        int diastolic3 = (int) Math.round(g.get(1).getValue());
                        int pbm3 = (int) Math.round(g.get(2).getValue());

                        if (diastolic3 == 0) {
                            // Swap display if diastolic is zero
                            displayValue = pbm3 + "/" + systolic3 + " " + unit;

                            binding.tvSystolic.setText(String.valueOf(pbm3));
                            binding.tvDiastolic.setText(String.valueOf(systolic3));
                            binding.tvBPM.setText("BPM: " + diastolic3); // shows 0 as BPM
                        } else {
                            // Normal display
                            displayValue = systolic3 + "/" + diastolic3 + " " + unit;

                            binding.tvSystolic.setText(String.valueOf(systolic3));
                            binding.tvDiastolic.setText(String.valueOf(diastolic3));
                            binding.tvBPM.setText("BPM: " + pbm3);
                        }

                    } catch (Exception e) {
                        Log.e("BP_DISPLAY", "Error setting display values", e);
                        // Fallback to default display
                    }

// Finally, show your combined display text
                    //binding.tvCombinedValue.setText(displayValue);
                    binding.tvTimeAgo.setText(displayTime);
                    //binding.tvOutcome.setText(g.get(0).getRatingInfo().getReadingOutcome());
                    String outcome = "-";

                    if (g != null && !g.isEmpty() && g.get(0) != null) {
                        RatingInfo rating = g.get(0).getRatingInfo();
                        String tmp = (rating != null) ? rating.getReadingOutcome() : null;
                        if (tmp != null && !tmp.trim().isEmpty()) outcome = tmp;
                    }

                    binding.tvOutcome.setText(outcome);
                    bindMetric(rowBloodPressure,
                            R.drawable.ic_heart_rate,
                            Constant.BLOOD_PRESSURE,
                            displayTime,
                            displayValue,
                            v -> {
                                if (getActivity() instanceof MainActivity) {
                                    // BP
                                    ((MainActivity) getActivity()).navigateTo(PageType.BLOOD_PRESSURE);
                                }
                            });



                });
            });
        });
        // Weight
        vm.getWeight().observe(getViewLifecycleOwner(), g -> {
            if (g == null || g.isEmpty()) {
                // No Weight data â†’ show defaults
                bindMetric(rowWeight,
                        R.drawable.ic_weight_foreground,
                        "Weight",
                        "--",
                        "-- kgs",
                        v -> {
                            if (getActivity() instanceof MainActivity) {
                                // Weight
                                ((MainActivity) getActivity()).navigateTo(PageType.WEIGHT);
                                Log.d(TAG, "weight this sis the tag value 1:");

                            }
                        });
                return;
            }

            vm.getWeightUpdatedAt().observe(getViewLifecycleOwner(), timeAgo -> {
                if (timeAgo == null) {
                    // Missing timestamp â†’ show defaults
                    bindMetric(rowWeight,
                            R.drawable.ic_weight_foreground,
                            "Weight",
                            "--",
                            "-- kgs",
                            v -> {
                                if (getActivity() instanceof MainActivity) {
                                    // Weight
                                    ((MainActivity) getActivity()).navigateTo(PageType.WEIGHT);
                                    Log.d(TAG, "weight this sis the tag value 2:");
                                }
                            });
                    return;
                }

                vm.getWeightEventDescription().observe(getViewLifecycleOwner(), bpm -> {
                    if (bpm == null) return;
                    String displayTime = timeAgo;
                    String displayValue = "-- kgs";
                    // Safely read from first item (index 0), second converted value (index 1)
                    boolean convert = g.get(0).isShould_convert();
                    double weightVal = 0;
                    String unitValue = convert ? "kgs" : "lbs";
                    if (g.get(0) != null
                            && g.get(0).getConvertedValues() != null
                            && g.get(0).getConvertedValues().size() > 1
                            && g.get(0).getConvertedValues().get(1) != null) {
                        //check if the value is true display kilo else pounds
                        weightVal = g.get(0).getConvertedValues().get(1).getValue();
                        Log.d(TAG, "this sis the tag value:"+ convert);
                        if(convert) {
                            displayValue = weightVal + " "+unitValue; // keep consistent rounding with other metrics

                        } else {
                            displayValue =  g.get(0).getConvertedValues().get(0).getValue() + " "+unitValue; // keep consistent rounding with other metrics
                        }
                        PreferenceHelper.getInstance(getContext()).putString(Constant.weightUnit, unitValue);
                    }

                    PreferenceHelper.getInstance(getContext()).putBoolean(Constant.weightUnitBoolean, convert);
                    bindMetric(rowWeight,
                            R.drawable.ic_weight_foreground,
                            "Weight",
                            displayTime,
                            displayValue,
                            v -> {
                                if (getActivity() instanceof MainActivity) {
                                    // Weight
                                    ((MainActivity) getActivity()).navigateTo(PageType.WEIGHT);
                                    Log.d(TAG, "weight this sis the tag value 3:");
                                }
                            });
                });
            });
        });
        // Blood oxygen
        vm.getOxygen().observe(getViewLifecycleOwner(), g -> {
            if (g == null || g.size() <= 1) {
                // No oxygen data or not enough entries â†’ show defaults
                bindMetric(rowBloodOxygen,
                        R.drawable.ic_oxygen_foreground,
                        "Blood Oxygen",
                        "--",
                        "-- %",
                        v -> {
                            if (getActivity() instanceof MainActivity) {
                                // Oxygen
                                ((MainActivity) getActivity()).navigateTo(PageType.BLOOD_OXYGEN);
                            }
                        });
                return;
            }

            vm.getOxygenUpdatedAt().observe(getViewLifecycleOwner(), timeAgo -> {
                if (timeAgo == null) {
                    // Missing timestamp â†’ show defaults
                    bindMetric(rowBloodOxygen,
                            R.drawable.ic_oxygen_foreground,
                            "Blood Oxygen",
                            "--",
                            "-- %",
                            v -> {
                                if (getActivity() instanceof MainActivity) {
                                    // Oxygen
                                    ((MainActivity) getActivity()).navigateTo(PageType.BLOOD_OXYGEN);
                                }
                            });
                    return;
                }

                vm.getOxygenEventDescription().observe(getViewLifecycleOwner(), bpm -> {
                    if (bpm == null) return;

                    String displayTime = timeAgo;
                    String displayValue = "-- %";

                    // Safely read from the second item (index 1)
                    if (g.get(1) != null
                            && g.get(1).getConvertedValues() != null
                            && !g.get(1).getConvertedValues().isEmpty()) {
                        displayValue = Math.round(
                                g.get(1).getConvertedValues().get(0).getValue()
                        ) + " %";
                    }

                    bindMetric(rowBloodOxygen,
                            R.drawable.ic_oxygen_foreground,
                            "Blood Oxygen",
                            displayTime,
                            displayValue,
                            v -> {
                                if (getActivity() instanceof MainActivity) {
                                    // Oxygen
                                    ((MainActivity) getActivity()).navigateTo(PageType.BLOOD_OXYGEN);
                                }
                            });
                });
            });
        });
        // ECG
        vm.getEcgServer().observe(getViewLifecycleOwner(), g ->{

            if (g != null) {
                bindMetric(rowEcg,
                        R.drawable.ic_graph_increase_foreground,
                        "Heart Rate",
                        TimeAgo.relativeFromIsoUtc(g.getDateFormat()),
                        String.valueOf((int) g.getHeartRate()),
                        v -> {
                            if (getActivity() instanceof MainActivity) {
                                // ECG
                                ((MainActivity) getActivity()).navigateTo(PageType.ECG);
                            }
                        });
            }

        });
        // Temperature
        vm.getTemperature().observe(getViewLifecycleOwner(), g -> {
            if (g == null || g.isEmpty()) {
                // No Temperature data â†’ show defaults
                bindMetric(temperature,
                        R.drawable.ic_temperature2_foreground,
                        Constant.TEMPERATURE,
                        "--",
                        "-- ",
                        v -> {
                            if (getActivity() instanceof MainActivity) {
                                // Temperature
                                ((MainActivity) getActivity()).navigateTo(PageType.TEMPERATURE);
                            }
                        });
                return;
            }

            vm.getTemperatureUpdatedAt().observe(getViewLifecycleOwner(), timeAgo -> {
                if (timeAgo == null) {
                    // No timestamp â†’ still show default
                    bindMetric(temperature,
                            R.drawable.ic_temperature2_foreground,
                            Constant.TEMPERATURE,
                            "--",
                            "-- ",
                            v -> {
                                if (getActivity() instanceof MainActivity) {
                                    // Temperature
                                    ((MainActivity) getActivity()).navigateTo(PageType.TEMPERATURE);
                                }
                            });
                    return;
                }

                vm.getTemperatureEventDescription().observe(getViewLifecycleOwner(), bpm -> {
                    if (bpm == null) return;

                    // Default values
                    String displayTime = timeAgo;
                    String displayValue = "-- ";

                    if (g.get(0).getConvertedValues() != null
                            && !g.get(0).getConvertedValues().isEmpty()) {
                        displayValue =
                                g.get(0).getConvertedValues().get(1).getValue() + " ";
                    }

                    bindMetric(temperature,
                            R.drawable.ic_temperature2_foreground,
                            Constant.TEMPERATURE,
                            displayTime,
                            displayValue,
                            v -> {
                                if (getActivity() instanceof MainActivity) {
                                    // Temperature
                                    ((MainActivity) getActivity()).navigateTo(PageType.TEMPERATURE);
                                }
                            });
                });
            });
        });

        model.getBloodPressureData().observe(getViewLifecycleOwner(), list -> {
            if (list == null || list.size() < 3) return;
            int systolic = (int) Math.floor(list.get(0));
            int diastolic = (int) Math.floor(list.get(1));
            int pbm = (int) Math.floor(list.get(2));
            Log.d(TAG, "BP data received: systolic=" + systolic + " diastolic=" + diastolic + " bpm=" + pbm);
            binding.tvSystolic.setText(String.valueOf(systolic));
            binding.tvDiastolic.setText(String.valueOf(diastolic));
            binding.tvBPM.setText("BPM: " + pbm);
            BPJumper listDb = databaseClient.getAppDatabase().bpJumperDao().getLatestBPJumper();
            if (listDb != null) {
                binding.tvTimeAgo.setText(listDb.getCreatedAtFormatted());
            }
            bindMetric(rowBloodPressure,
                    R.drawable.ic_heart_rate,
                    Constant.BLOOD_PRESSURE,
                    binding.tvTimeAgo.getText().toString(),
                    systolic + "/" + diastolic + " mmHg",
                    v -> {
                        if (getActivity() instanceof MainActivity) {
                            ((MainActivity) getActivity()).navigateTo(PageType.BLOOD_PRESSURE);
                        }
                    });
        });

        // Measure button
        binding.btnMeasure.setOnClickListener(v -> startMeasuring());

        setupQuickActions();


    }

    // ===== Measuring logic (HR -> estimate BP -> send) =====
    private void startMeasuring() {
        if (measuring || binding == null) return;
        measuring = true;
        binding.btnMeasure.setEnabled(false);

        measureStartMs = android.os.SystemClock.uptimeMillis();
        binding.tvStatus.setVisibility(View.VISIBLE);
        binding.tvStatus.setText("Measuringâ€¦");
        binding.progress.setVisibility(View.VISIBLE);
        //binding.heartRateLine.setProgress(0f);
        //binding.heartRateLine.playAnimation();

        HealthManager.getInstance(requireContext())
                .startHeartRateMeasurement(new HealthManager.ValueCallback<Integer>() {
                    @SuppressLint("SetTextI18n")
                    @Override
                    public void onValue(Integer bpm) {
                        long elapsed = android.os.SystemClock.uptimeMillis() - measureStartMs;
                        long delay = Math.max(0L, MIN_LOADER_MS - elapsed);
                        Log.wtf("BloodPressure", "Blood pressure value: "+bpm);
                        uiHandler.postDelayed(() -> {
                            if (binding == null) return;

                            if (bpm != 0) {
                                // Estimate BP from HR (your existing logic)
                                BloodPressureEstimator estimator = new BloodPressureEstimator();
                                BPReading bp = estimator.estimateBloodPressure(bpm);

                                //binding.tvValue.setText("Systolic: " + bp.systolic + "\nDiastolic: " + bp.diastolic);
                                // Log raw (before offset)
                                Log.d(TAG, "BPJumper (fragment before offset) - systolic=" + bp.systolic + ", diastolic=" + bp.diastolic + ", bpm=" + bpm);

                                // Apply offsets per requirement
                                int adjustedSystolic = bp.systolic + 10;
                                int adjustedDiastolic = bp.diastolic + 5;

                                // Log adjusted (after offset)
                                Log.d(TAG, "BPJumper (fragment after offset) - systolic=" + adjustedSystolic + ", diastolic=" + adjustedDiastolic + ", bpm=" + bpm);

                                // Show adjusted values in the UI
                                binding.tvSystolic.setText(String.valueOf(adjustedSystolic));
                                binding.tvDiastolic.setText(String.valueOf(adjustedDiastolic));
                                binding.tvStatus.setVisibility(View.GONE);

                                binding.tvTimeAgo.setText("just now");
                                if (NetworkUtils.isInternetConnected(requireContext())) {
                                    // Send adjusted values
                                    Log.d(TAG, "BPJumper (fragment send) - sending adjusted systolic=" + adjustedSystolic + ", diastolic=" + adjustedDiastolic);
                                    sendBloodPressureRateSync(adjustedSystolic, adjustedDiastolic);
                                } else {
                                    // Persist adjusted values offline
                                    BPJumper bpJumper = new BPJumper(adjustedSystolic, adjustedDiastolic, 0, 1, DeviceUtils.getIMEI(requireContext()));
                                    Log.d(TAG, "BPJumper (fragment offline save) - systolic=" + adjustedSystolic + ", diastolic=" + adjustedDiastolic + ", bpm=0");
                                    databaseClient.getAppDatabase().bpJumperDao().insertBPJumper(bpJumper);
                                }
                            } else {
                                //binding.tvSystolic.setText("--");
                                //binding.tvDiastolic.setText("--");
                                binding.tvStatus.setVisibility(View.VISIBLE);
                                binding.tvStatus.setText("Please wear the device");
                            }

                            binding.progress.setVisibility(View.GONE);
                            //binding.tvStatus.setText("Done");
                            //binding.tvStatus.setVisibility(View.GONE);

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
                            //binding.tvStatus.setText(error);
                            binding.tvStatus.setText("Please wear the device");

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




    private void bindMetric(@Nullable View row,
                            @DrawableRes int iconRes,
                            @NonNull String title,
                            @NonNull String timeAgo,
                            @NonNull String value,
                            @Nullable View.OnClickListener onClick) {
        if (row == null) return;

        ImageView ivIcon = row.findViewById(R.id.ivIcon);
        TextView tvTitle = row.findViewById(R.id.tvTitle);
        TextView tvTime  = row.findViewById(R.id.tvTime);
        TextView tvValue = row.findViewById(R.id.tvValue);

        if (ivIcon != null) ivIcon.setImageResource(iconRes);
        if (tvTitle != null) tvTitle.setText(title);
        if (tvTime  != null) tvTime.setText(timeAgo);
        if (tvValue != null) tvValue.setText(value);

        // Enable click + ripple + haptics
        row.setClickable(true);
        row.setFocusable(true);
        row.setOnClickListener(v -> {
            v.performHapticFeedback(android.view.HapticFeedbackConstants.VIRTUAL_KEY);
            if (onClick != null) onClick.onClick(v);
        });
    }



    @Override
    public void onDestroyView() {
        super.onDestroyView();
        if (networkCallback != null) {
            ConnectivityManager cm = (ConnectivityManager) requireContext().getSystemService(android.content.Context.CONNECTIVITY_SERVICE);
            if (cm != null) cm.unregisterNetworkCallback(networkCallback);
            networkCallback = null;
        }
        binding = null;
    }

    @Override
    public void onResume() {
        super.onResume();
        updateConnectionIcon();
        NetworkUtils.ConnectionQuality quality = NetworkUtils.getConnectionQuality(requireContext());
        if (quality != NetworkUtils.ConnectionQuality.NONE) {
            networkCallback = NetworkUtils.scheduleOnStrongConnection(requireContext(), () -> {
                if (!isAdded() || binding == null) return;
                vm.fetchLatest(
                        Constant.BASE_URL_BGM,
                        "bNWZsV#BeZvaNb*gF@3Z^7tCNhCT29Vw8Vi%4T%",
                        DeviceUtils.getIMEI(requireContext())
                );
                vm.getTypesAvailabilityMutableLiveData()
                        .observe(this, typesAvailability -> {
                            if (typesAvailability == null) return;
                            updateMetricsVisibility(typesAvailability);
                        });
                //syncBloodPressureData();
                //syncSpo2();
                //syncHeartRate();
                //syncStep();
                //((MainActivity) requireActivity()).startFetchingSteps();
            });
        } else {
            BPJumper list = databaseClient.getAppDatabase().bpJumperDao().getLatestBPJumper();
            if (list != null) {
                binding.tvSystolic.setText(String.format(Locale.getDefault(), "%.0f", (double) list.getSystolic()));
                binding.tvDiastolic.setText(String.format(Locale.getDefault(), "%.0f", (double) list.getDiastolic()));
                binding.tvBPM.setText(String.format(Locale.getDefault(), "BPM: %.0f", (double) list.getPulseRate()));
                binding.tvTimeAgo.setText(list.getCreatedAtFormatted());

                bindMetric(rowBloodPressure,
                        R.drawable.ic_heart_rate,
                        Constant.BLOOD_PRESSURE,
                        Math.round(list.getSystolic())+"/"+Math.round(list.getDiastolic()),
                        list.getCreatedAtFormatted(),
                        v -> {
                            if (getActivity() instanceof MainActivity) {
                                // BP
                                ((MainActivity) getActivity()).navigateTo(PageType.BLOOD_PRESSURE);
                            }
                        });


            }

            displayOffLineData();
        }

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

    @SuppressLint("WrongConstant")
    private void syncStep() {

        //databaseClient.getAppDatabase().temperatureDao().insertTemperature(new Temperature(82, 1, "test"));
        // databaseClient.getAppDatabase().temperatureDao().insertTemperature(new Temperature(83, 1, "test"));
        //databaseClient.getAppDatabase().temperatureDao().insertTemperature(new Temperature(84, 1, "test"));
        //databaseClient.getAppDatabase().temperatureDao().insertTemperature(new Temperature(85, 1, "test"));
        //databaseClient.getAppDatabase().temperatureDao().insertTemperature(new Temperature(86, 1, "test"));


        TemperatureDao dao = databaseClient.getAppDatabase().temperatureDao();
        List<Temperature> temperatureEntities = dao.getAllTemperature();
        List<Reading> readingsList = new ArrayList<>();

        for (Temperature bp : temperatureEntities) {
            Log.d("temperature----", bp.getTemperature() + "");

            Reading reading = new Reading();            // NEW object each iteration
            reading.setManual(false);
            reading.setTimezone("Asia/Manila");
            reading.setSource("jtm00025b94050c");
            reading.setValue(Arrays.asList(
                    bp.getTemperature()
            ));
            reading.setDevice(Constant.DEVICE_TEMPERATURE);
            reading.setReadingType("5bb306382598931ffbd1b628");
            reading.setDate(DateUtils.toIso8601Manila(bp.getCreatedAt()));
            reading.setSerial("5bc3cb14cba82b066cae7bc1");
            readingsList.add(reading);                  // append, don't replace
        }

// set once, after the loop
        ReadingsRequest readingsRequest = new ReadingsRequest();
        readingsRequest.setReadings(readingsList);
        if(!readingsRequest.getReadings().isEmpty()) {
            sendStepServer(readingsRequest);
        }


    }
    @SuppressLint("WrongConstant")
    private void syncHeartRate() {

        //databaseClient.getAppDatabase().heartRateDao().insertHeartRate(new HeartRateEntity(23));
        //databaseClient.getAppDatabase().heartRateDao().insertHeartRate(new HeartRateEntity(24));
        //databaseClient.getAppDatabase().heartRateDao().insertHeartRate(new HeartRateEntity(25));
        //.getAppDatabase().heartRateDao().insertHeartRate(new HeartRateEntity(26));

        HeartRateDao dao = databaseClient.getAppDatabase().heartRateDao();
        List<HeartRateEntity> heartRateEntities = dao.getAllHeartRate();
        List<Reading> readingsList = new ArrayList<>();

        for (HeartRateEntity bp : heartRateEntities) {

            Reading reading = new Reading();            // NEW object each iteration
            reading.setManual(false);
            reading.setTimezone("Asia/Manila");
            reading.setSource("jtm00025b94050c");
            reading.setValue(Arrays.asList(
                    0.0,
                    bp.getValue()
            ));
            reading.setDevice("66437be266c8833a1c42d7aa");
            reading.setReadingType("5bb306382598931ffbd1b626");
            reading.setDate(DateUtils.toIso8601Manila(bp.getCreatedAt()));
            reading.setSerial("jtm00025b94050c");
            readingsList.add(reading);                  // append, don't replace
        }
        ReadingsRequest readingsRequest = new ReadingsRequest();
        readingsRequest.setReadings(readingsList);
        if(!readingsRequest.getReadings().isEmpty()) {
            sendHeartRateServer(readingsRequest);
        }


    }
    @SuppressLint("WrongConstant")
    private void syncSpo2() {

        OximeterDao dao = databaseClient.getAppDatabase().oximeterDao();
        List<Oximeter> oximeterEntities = dao.getAllOximeterByStatus(1);
        Log.d("Oximeter----", String.valueOf(oximeterEntities.size()));
        List<Reading> readingsList = new ArrayList<>();

        for (Oximeter bp : oximeterEntities) {
            Log.d("Oximeter----", bp.getPulseRate() + "");

            Reading reading = new Reading();            // NEW object each iteration
            reading.setManual(false);
            reading.setTimezone("Asia/Manila");
            reading.setSource(bp.getSerial());
            reading.setValue(Arrays.asList(
                    (double) bp.getPulseRate(),
                    (double) bp.getOxygen()
            ));
            reading.setDevice(Constant.DEVICE_OXIMETER);
            reading.setReadingType("5bb306382598931ffbd1b626");
            reading.setDate(DateUtils.toIso8601Manila(bp.getCreatedAt()));
            reading.setSerial(bp.getSerial());
            readingsList.add(reading);                  // append, don't replace
        }

// set once, after the loop
        ReadingsRequest readingsRequest = new ReadingsRequest();
        readingsRequest.setReadings(readingsList);
        if(!readingsRequest.getReadings().isEmpty()) {
            sendOximeterServer(readingsRequest);
        }


    }

    private void syncBloodPressureData() {

        BPJumperDao dao = databaseClient.getAppDatabase().bpJumperDao();
        List<BPJumper> heartRateEntities = dao.getAllBPJumper();
        List<Reading> readingsList = new ArrayList<>();
        for (BPJumper bp : heartRateEntities) {
            Reading reading = new Reading();            // NEW object each iteration
            reading.setManual(false);
            reading.setTimezone("Asia/Manila");
            reading.setSource("jtm00025b94050c");
            reading.setValue(Arrays.asList(
                    (double) bp.getSystolic(),
                    (double) bp.getDiastolic(),
                    (double) bp.getPulseRate()
            ));
            reading.setDevice("66437be266c8833a1c42d7aa");
            reading.setReadingType("5bb306382598931ffbd1b624");
            //reading.setDate(DateUtils.getDate());       // or bp.getCreatedAt() if you want per-row time
            reading.setDate(DateUtils.toIso8601Manila(bp.getCreatedAt()));
            reading.setSerial("jtm00025b94050c");
            readingsList.add(reading);                  // append, don't replace
        }

// set once, after the loop
        ReadingsRequest readingsRequest = new ReadingsRequest();
        readingsRequest.setReadings(readingsList);
        if(!readingsRequest.getReadings().isEmpty()) {
            sendBPJumper(readingsRequest);
        }


    }

    public void sendBPJumper(ReadingsRequest readingsRequest) {
        Log.d(TAG, "Success : sending the BP");

        Call<Object> call = ApiClient.getUserService(Constant.BASE_URL_BGM,Constant.TOKEN_DR_WATCH_API, DeviceUtils.getIMEI(requireContext())).sendReadings(readingsRequest);
        call.enqueue(new Callback<Object>() {
            @Override
            public void onResponse(Call<Object> call, Response<Object> response) {
                Log.d(TAG, "Bulk Success : "+response);
                Log.d(TAG, "Bulk Success : "+response.code());
                Log.d(TAG, "Bulk Success : "+response.message());
                Log.d(TAG, "Bulk Success : "+response.toString());
                BPJumperDao dao = databaseClient.getAppDatabase().bpJumperDao();
                dao.deleteAll();

            }

            @Override
            public void onFailure(Call<Object> call, Throwable t) {
                Log.d(TAG, "Bulk error : "+t.toString());
            }
        });
    }

    public void sendOximeterServer(ReadingsRequest readingsRequest) {
        Log.d(TAG, "Success : sending the BP");

        Call<Object> call = ApiClient.getUserService(Constant.BASE_URL_BGM,Constant.TOKEN_DR_WATCH_API, DeviceUtils.getIMEI(requireContext())).sendReadings(readingsRequest);
        call.enqueue(new Callback<Object>() {
            @Override
            public void onResponse(Call<Object> call, Response<Object> response) {
                Log.d(TAG, "Bulk Success : "+response);
                Log.d(TAG, "Bulk Success : "+response.code());
                Log.d(TAG, "Bulk Success : "+response.message());
                OximeterDao dao = databaseClient.getAppDatabase().oximeterDao();
                dao.deleteAll();
            }

            @Override
            public void onFailure(Call<Object> call, Throwable t) {
                Log.d(TAG, "Bulk error : "+t.toString());
            }
        });
    }

    public void sendHeartRateServer(ReadingsRequest readingsRequest) {
        Log.d(TAG, "Success : sending the BP");

        Call<Object> call = ApiClient.getUserService(Constant.BASE_URL_BGM,Constant.TOKEN_DR_WATCH_API, DeviceUtils.getIMEI(requireContext())).sendReadings(readingsRequest);
        call.enqueue(new Callback<Object>() {
            @Override
            public void onResponse(Call<Object> call, Response<Object> response) {
                Log.d(TAG, "Bulk Success : "+response);
                Log.d(TAG, "Bulk Success : "+response.code());
                Log.d(TAG, "Bulk Success : "+response.message());
                HeartRateDao dao  = databaseClient.getAppDatabase().heartRateDao();
                dao.deleteAll();
            }

            @Override
            public void onFailure(Call<Object> call, Throwable t) {
                Log.d(TAG, "Bulk error : "+t.toString());
            }
        });
    }
    public void sendStepServer(ReadingsRequest readingsRequest) {
        Log.d(TAG, "Success : sending the BP");

        Call<Object> call = ApiClient.getUserService(Constant.BASE_URL_BGM,Constant.TOKEN_DR_WATCH_API, DeviceUtils.getIMEI(requireContext())).sendReadings(readingsRequest);
        call.enqueue(new Callback<Object>() {
            @Override
            public void onResponse(Call<Object> call, Response<Object> response) {
                Log.d(TAG, "Bulk Success : "+response);
                Log.d(TAG, "Bulk Success : "+response.code());
                Log.d(TAG, "Bulk Success : "+response.message());
                TemperatureDao dao  = databaseClient.getAppDatabase().temperatureDao();
                dao.deleteAll();
            }

            @Override
            public void onFailure(Call<Object> call, Throwable t) {
                Log.d(TAG, "Bulk error : "+t.toString());
            }
        });
    }

    //Hide feature
    private void updateMetricsVisibility(TypesAvailability types) {

        rowGlucose.setVisibility(
                types.isBloodGlucose() ? View.VISIBLE : View.GONE
        );

        rowBloodPressure.setVisibility(
                types.isBloodPressure() ? View.VISIBLE : View.GONE
        );

        rowWeight.setVisibility(
                types.isWeight() ? View.VISIBLE : View.GONE
        );

        rowBloodOxygen.setVisibility(
                types.isBloodOxygen() ? View.VISIBLE : View.GONE
        );

        rowEcg.setVisibility(
                types.isElectrocardiogram() ? View.VISIBLE : View.GONE
        );

        temperature.setVisibility(
                types.isTemperature() ? View.VISIBLE : View.GONE
        );
    }

    public void displayOffLineData() {

        ReadingValue readingValue = databaseClient.getAppDatabase().readingValueDao().getLatestReadingValue();
        if (readingValue != null) {
            String formattedTime = readingValue.getCreatedAtFormatted();

            // Bind the metric row with safe defaults
            bindMetric(
                    rowGlucose,
                    R.drawable.ic_blood_drop_foreground,
                    Constant.BLOOD_GLUCOSE,
                    MessageFormat.format("{0}", (int) readingValue.getGlucose()),                                    // e.g., "a day ago" or "--"
                    formattedTime,
                    v -> {
                        if (getActivity() instanceof MainActivity) {
                            // Glucose
                            ((MainActivity) getActivity()).navigateTo(PageType.BLOOD_GLUCOSE);
                        }

                    }
            );
        }

        WeighingScale weighingScale = databaseClient.getAppDatabase().weighingScaleDao().getLatestWeighingScale();
        if (weighingScale != null) {
            bindMetric(rowWeight,
                    R.drawable.ic_weight_foreground,
                    "Weight",
                    weighingScale.getCreatedAtFormatted(),
                    String.valueOf(weighingScale.getWeight()),
                    v -> {
                        if (getActivity() instanceof MainActivity) {
                            // Weight
                            ((MainActivity) getActivity()).navigateTo(PageType.WEIGHT);
                        }
                    });
        }

        Oximeter oximeter = databaseClient.getAppDatabase().oximeterDao().getLatestOximeter();
        if (oximeter != null) {
            String formattedTime = oximeter.getCreatedAtFormatted();
            assert formattedTime != null;
            bindMetric(rowBloodOxygen,
                    R.drawable.ic_oxygen_foreground,
                    Constant.BLOOD_OXYGEN,
                    formattedTime,
                    oximeter.getPulseRate()+"",
                    v -> {
                        if (getActivity() instanceof MainActivity) {
                            // Oxygen
                            ((MainActivity) getActivity()).navigateTo(PageType.BLOOD_OXYGEN);
                        }
                    });

        }
        HeartRateEntity data = databaseClient.getAppDatabase().heartRateDao().getLatestHeartRate();
        if (data != null) {
            bindMetric(rowEcg,
                    R.drawable.ic_graph_increase_foreground,
                    "Heart Rate",
                    DateUtils.getTimeAgo(data.getCreatedAt()),
                    String.valueOf(Math.round(data.getValue())),
                    v -> {
                        if (getActivity() instanceof MainActivity) {
                            // ECG
                            ((MainActivity) getActivity()).navigateTo(PageType.ECG);
                        }
                    });

        }
        Temperature temperature1 = databaseClient.getAppDatabase().temperatureDao().getLatestTemperature();
        if (temperature1 != null) {
            String valueBg = String.format(Locale.getDefault(), "%.1f", temperature1.getTemperature());

            bindMetric(temperature,
                    R.drawable.ic_temperature2_foreground,
                    Constant.TEMPERATURE,
                    temperature1.getCreatedAtFormatted(),
                    valueBg,
                    v -> {
                        if (getActivity() instanceof MainActivity) {
                            // Temperature
                            ((MainActivity) getActivity()).navigateTo(PageType.TEMPERATURE);
                        }
                    });
        }
    }


    @Override
    public void setupQuickActions() {

        androidId =  DeviceUtils.getIMEI(getActivity());
        _model = Build.MODEL;                     // e.g., SM-G925I
        _maker = Build.MANUFACTURER;              // e.g., Samsung
        osVersion = Build.VERSION.RELEASE;       // e.g., 4.4, 12, 13
        _country = Locale.getDefault().getCountry(); // e.g., PR empty for now

        BatteryManager batteryManager = (BatteryManager) getActivity().getSystemService(BATTERY_SERVICE);
        IntentFilter ifilter = new IntentFilter(Intent.ACTION_BATTERY_CHANGED);
        Intent batteryStatus = getActivity().registerReceiver(null, ifilter);
        int level = batteryStatus.getIntExtra(BatteryManager.EXTRA_LEVEL, -1);
        int scale = batteryStatus.getIntExtra(BatteryManager.EXTRA_SCALE, -1);
        batteryPercent = (level * 100) / scale;

        quickActionsHelper = new QuickActionsHelper(binding.getRoot() != null ? binding.getRoot() : getView(), this);
//
//        // Initialize badge count (you can get this from your data source)
        updateMessageBadge(2); // Replace with actual count
    }

    @Override
    public void onSettingsClicked() {
    }

    @Override
    public void onHealthClicked() {
        showSettingUpBleDevice();
    }

    @Override
    public void onNotificationsClicked() {
        Intent intent = new Intent(getActivity(), MessagesActivity.class);
        startActivity(intent);
    }

    @Override
    public void updateMessageBadge(int count) {
        if (quickActionsHelper != null) {
            quickActionsHelper.updateMessageBadge(count);
        }
    }

    private void showSettingUpBleDevice() {

        // Inside your Fragment's onClick or a specific logic block
        Intent intent = new Intent(getActivity(), BleDeviceSelectionActivity.class);
        startActivity(intent);
    }


    public void sendAlarm() {

        double latitude = Double.longBitsToDouble(prefs.getLong("latitude", Double.doubleToRawLongBits(0)));
        double longitude = Double.longBitsToDouble(prefs.getLong("longitude", Double.doubleToRawLongBits(0)));
//        SendAlarmRequest sendAlarmRequest = new SendAlarmRequest(latitude, longitude, androidId, 1, 1, batteryPercent, true,
//                _model, _maker,
//                "0", _country);
        // Location sample: lat: 148.752, Long: 87588.701
        SendAlarmRequest sendAlarmRequest = new SendAlarmRequest(latitude, longitude, androidId, 1, 1, batteryPercent, true,
                _model, _maker,
                "0", _country);
        Call<Object> call = ApiClient.getUserService(Constant.BASE_URL_BGM, Constant.TOKEN_DR_WATCH_API, androidId)
                .sendAlarm(sendAlarmRequest);

        call.enqueue(new Callback<Object>() {
            @Override
            public void onResponse(Call<Object> call, Response<Object> response) {
                // Hide progress
                if (response.code() == 200) {
                    Log.d(TAG, "Alarm sent successfully!");
                    retryCount = 0; // Reset retry count on success
                    playNotificationSound();
                } else {
                    handleRetry();
                }
            }

            @Override
            public void onFailure(Call<Object> call, Throwable t) {
                handleRetry();
            }
        });
    }
    @SuppressLint("LongLogTag")
    private void handleRetry() {
        if (retryCount < MAX_RETRY_COUNT) {
            retryCount++;
            Log.d(TAG, "Retrying sendAlarm... Attempt " + retryCount);
            sendAlarm();
        } else {
            Log.d(TAG, "Max retry attempts reached. Failed to send alarm.");
        }
    }

}