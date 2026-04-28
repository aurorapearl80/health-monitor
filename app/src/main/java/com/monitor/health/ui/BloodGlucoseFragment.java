package com.monitor.health.ui;

import static android.content.Context.BATTERY_SERVICE;
import static android.content.Context.MODE_PRIVATE;

import static com.monitor.health.utility.AppUtils.getTodayDate;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.media.Ringtone;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.BatteryManager;
import android.os.Build;
import android.os.Bundle;

import androidx.annotation.DrawableRes;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.wear.widget.WearableLinearLayoutManager;
import androidx.wear.widget.WearableRecyclerView;

import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import com.monitor.health.ApiClient;
import com.monitor.health.Constant;
import com.monitor.health.MainActivity;
import com.monitor.health.NetworkUtils;
import com.monitor.health.R;
import com.monitor.health.ReadingsRequest;
import com.monitor.health.adapter.PageType;
import com.monitor.health.adapter.StatsAdapter;
import com.monitor.health.dao.BPJumperDao;
import com.monitor.health.dao.HeartRateDao;
import com.monitor.health.dao.OximeterDao;
import com.monitor.health.dao.TemperatureDao;
import com.monitor.health.database.DatabaseClient;
import com.monitor.health.model.BPJumper;
import com.monitor.health.model.HeartRateEntity;
import com.monitor.health.model.Oximeter;
import com.monitor.health.model.Reading;
import com.monitor.health.model.ReadingValue;
import com.monitor.health.model.Temperature;
import com.monitor.health.model.WeighingScale;
import com.monitor.health.model.healthscore.DataObjectDto;
import com.monitor.health.model.healthscore.UserDrWatch;
import com.monitor.health.request.SendAlarmRequest;
import com.monitor.health.response.alldata.DataItem;
import com.monitor.health.response.alldata.ReadingMetricValue;
import com.monitor.health.response.alldata.ReadingType;
import com.monitor.health.response.alldata.Root;
import com.monitor.health.response.alldata.TypesAvailability;
import com.monitor.health.response.glocuse.GlucoseServerResponse;
import com.monitor.health.ui.graph.WeightGraphActivity;
import com.monitor.health.utility.AppUtils;
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
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;

public class BloodGlucoseFragment extends Fragment  implements QuickActionsHandler {


    private TextView txtBloodGlucose, tvBPM, txtTimeAgo, tvUnit, tvOutcome;
    private ImageView chartIcon;

    private static final String TAG = "BloodGlucoseFragment";

    private WearableRecyclerView statsRecyclerView;
    private View rootView;

    List<ProfileFragment.StatItem> stats;
    StatsAdapter adapter;


    SharedPreferences prefs;
    SharedPreferences.Editor editor;

    DatabaseClient databaseClient;

    View rowGlucose;
    View rowBloodPressure;
    View rowWeight;
    View rowBloodOxygen;
    View rowEcg;
    View temperature;

    ReadingValue list;

    private QuickActionsHelper quickActionsHelper;
    String _model;                     // e.g., SM-G925I
    String _maker;              // e.g., Samsung
    String osVersion;       // e.g., 4.4, 12, 13
    String _country;
    String androidId;
    private static final int MAX_RETRY_COUNT = 3; // Set max retry attempts
    private int retryCount = 0;
    int batteryPercent = 0;



    // TODO: Replace with your actual screen layout that includes @layout/layout_metrics_panel
    private static final int LAYOUT_ID = R.layout.fragment_blood_glucose;

    private ReadingsViewModel vm;

    public BloodGlucoseFragment() {
        // Required empty public constructor
    }



    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        databaseClient = DatabaseClient.getInstance(getActivity());
        prefs = requireActivity().getSharedPreferences(Constant.PREFERENCE_BLOOD_GLUCOSE, MODE_PRIVATE);
        editor = prefs.edit();


    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        // IDs must match the XML
        txtBloodGlucose = view.findViewById(R.id.tvValueBG);
        txtTimeAgo      = view.findViewById(R.id.tvTimeAgo);
        tvUnit      = view.findViewById(R.id.tvUnit);
        tvOutcome      = view.findViewById(R.id.tvOutcome);
        tvBPM      = view.findViewById(R.id.tvBPM);
        chartIcon     = view.findViewById(R.id.chartIcon);

        SharedDataViewModel model =
                new ViewModelProvider(requireActivity()).get(SharedDataViewModel.class);

        vm = new ViewModelProvider(this).get(ReadingsViewModel.class);


        View panel = view.findViewById(R.id.metricsPanel);

// Each include now has a real root you can grab by id
        rowGlucose       = panel.findViewById(R.id.rowGlucose);
        rowBloodPressure = panel.findViewById(R.id.rowBloodPressure);
        rowWeight        = panel.findViewById(R.id.rowWeight);
        rowBloodOxygen   = panel.findViewById(R.id.rowBloodOxygen);
        rowEcg           = panel.findViewById(R.id.rowEcg);
        temperature           = panel.findViewById(R.id.temperature);


        chartIcon.setOnClickListener(chart -> {
            Intent intent = new Intent(getActivity(), WeightGraphActivity.class);
            intent.putExtra("backgroundColor", "BloodGlucose");
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
                if (txtBloodGlucose != null) txtBloodGlucose.setText("--");
                if (tvUnit != null) tvUnit.setText("");
                if (tvOutcome != null) tvOutcome.setText("");
                if (txtTimeAgo != null) txtTimeAgo.setText("--");
                if (tvBPM != null) tvBPM.setText("--");
                bindMetric(
                        rowGlucose,
                        R.drawable.ic_blood_drop_foreground,
                        Constant.BLOOD_GLUCOSE,
                        "--",
                        "--",
                        v -> {
                            if (getActivity() instanceof MainActivity) {
                                // Glucose
                                ((MainActivity) getActivity()).navigateTo(PageType.BLOOD_GLUCOSE);
                            }
                        }
                );

                model.getGlucoseData().observe(getViewLifecycleOwner(), glucose -> {
                    list = databaseClient.getAppDatabase().readingValueDao().getLatestReadingValue();

                    if (list != null) {
                        String formattedTime = list.getCreatedAtFormatted();
                        if (formattedTime != null && !formattedTime.isEmpty()) {
                            txtTimeAgo.setText(formattedTime);
                        }
                        txtBloodGlucose.setText(String.valueOf(glucose));
                    }
                });

                return;
            }

            Log.d(TAG, "Loading here " + g + " this is the data");

            // If primary value TextView is absent, log for debugging and bail (keeps your original behavior)
            if (txtBloodGlucose == null) {
                Log.e("BloodGlucoseFragment",
                        "tv_value_bg was not found in this layout. " +
                                "Is it inside an <include> or a RecyclerView row?");
                return;
            }

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

            // Outcome (optional)
            String glucoseOutcome = "";
            if (g.getRatingInfo() != null && g.getRatingInfo().getReadingOutcome() != null) {
                glucoseOutcome = g.getRatingInfo().getReadingOutcome();
            }

            // Update immediate UI fields if present
            txtBloodGlucose.setText("--".equals(valueStr) ? "--" : valueStr);
            if (tvUnit != null) tvUnit.setText(unit);
            if (tvOutcome != null) tvOutcome.setText(glucoseOutcome);
            //txtTimeAgo.setText("Just now");

            // Time + event description observers
            vm.getGlucoseUpdatedAt().observe(getViewLifecycleOwner(), timeAgo -> {
                String displayTime = (timeAgo != null) ? timeAgo : "--";
                if (txtTimeAgo != null) txtTimeAgo.setText(displayTime);

                Log.d(TAG, "Loading here glucose herer " + displayTime + " this is the data");

                vm.getGlucoseEventDescription().observe(getViewLifecycleOwner(), bpm -> {
                    String bpmText = (bpm != null) ? bpm : "--";
                    if (tvBPM != null) tvBPM.setText(bpmText);
                    Log.d(TAG, "Loading here " + bpmText + " this is the data");

                    boolean convert = g.isShould_convert();
                    String unitValue = convert ? "mmol/L" : "mg/dL";

                    // Bind the metric row with safe defaults
                    bindMetric(
                            rowGlucose,
                            R.drawable.ic_blood_drop_foreground,
                            Constant.BLOOD_GLUCOSE,
                            displayTime,                                    // e.g., "a day ago" or "--"
                            "--".equals(valueStr) ? "--" : unitValue, // e.g., "108 mg/dL" or "--"
                            v -> {
                                if (getActivity() instanceof MainActivity) {
                                    // Glucose
                                    ((MainActivity) getActivity()).navigateTo(PageType.BLOOD_GLUCOSE);
                                }

                            }
                    );

                    model.getGlucoseData().observe(getViewLifecycleOwner(), glucose -> {
                        list = databaseClient.getAppDatabase().readingValueDao().getLatestReadingValue();
                        txtBloodGlucose.setText(String.valueOf(glucose));
                        //add the value need to convert the value of realtime
                        if (list != null) {
                            String formattedTime = list.getCreatedAtFormatted();
                            if (formattedTime != null && !formattedTime.isEmpty()) {
                                txtTimeAgo.setText(formattedTime);
                            }
                        }
                    });

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
                        // If both values are present, show "SYS/DIA unit"
                        displayValue = Math.round(g.get(0).getValue())
                                + "/" + Math.round(g.get(1).getValue())
                                + " " + unit;

                    } catch (Exception e) {
                        // Any failure falls back to default already set
                    }

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
                        Constant.WEIGHT,
                        "--",
                        "-- kgs",
                        v -> {
                            if (getActivity() instanceof MainActivity) {
                                // Weight
                                ((MainActivity) getActivity()).navigateTo(PageType.WEIGHT);
                            }
                        });
                return;
            }

            vm.getWeightUpdatedAt().observe(getViewLifecycleOwner(), timeAgo -> {
                if (timeAgo == null) {
                    // Missing timestamp â†’ show defaults
                    bindMetric(rowWeight,
                            R.drawable.ic_weight_foreground,
                            Constant.WEIGHT,
                            "--",
                            "-- kgs",
                            v -> {
                                if (getActivity() instanceof MainActivity) {
                                    // Weight
                                    ((MainActivity) getActivity()).navigateTo(PageType.WEIGHT);
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
                            Constant.WEIGHT,
                            displayTime,
                            displayValue,
                            v -> {
                                if (getActivity() instanceof MainActivity) {
                                    // Weight
                                    ((MainActivity) getActivity()).navigateTo(PageType.WEIGHT);
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

        // Setup quick actions
        setupQuickActions();


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
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        rootView = inflater.inflate(R.layout.fragment_blood_glucose, container, false);
        return rootView;
    }

    @Override
    public void onResume() {
        super.onResume();
        updateConnectionIcon();
        if (NetworkUtils.isInternetConnected(requireContext())) {
            Log.d(TAG, "this is hello world");
            vm.fetchLatest(
                    Constant.BASE_URL_BGM,
                    "bNWZsV#BeZvaNb*gF@3Z^7tCNhCT29Vw8Vi%4T%",
                    DeviceUtils.getIMEI(requireContext())
            );

            vm.getTypesAvailabilityMutableLiveData()
                    .observe(this, typesAvailability -> {
                        if (typesAvailability == null) return;
                        updateMetricsVisibility(typesAvailability);
                        updateNavigationPages(typesAvailability);  // ðŸ‘ˆ add this
                    });

            syncGlucose();
            syncBloodPressureData();
            syncSpo2();
            syncHeartRate();
            syncTemperature();
            ((MainActivity) requireActivity()).startFetchingSteps();
        }
        else {
            // Retrieve data from SharedPreferences
            list = databaseClient.getAppDatabase().readingValueDao().getLatestReadingValue();
            if (list != null) {
                String formattedTime = list.getCreatedAtFormatted();
                if (formattedTime != null && !formattedTime.isEmpty()) {
                    txtTimeAgo.setText(formattedTime);
                }
                txtBloodGlucose.setText(MessageFormat.format("{0}", (int) list.getGlucose()));

                // Bind the metric row with safe defaults
                bindMetric(
                        rowGlucose,
                        R.drawable.ic_blood_drop_foreground,
                        Constant.BLOOD_GLUCOSE,
                        MessageFormat.format("{0}", (int) list.getGlucose()),                                    // e.g., "a day ago" or "--"
                        formattedTime,
                        v -> {
                            if (getActivity() instanceof MainActivity) {
                                // Glucose
                                ((MainActivity) getActivity()).navigateTo(PageType.BLOOD_GLUCOSE);
                            }

                        }
                );
            }
            displayOffLineData();
        }

        Log.d(TAG, "On Resume");

    }

    @SuppressLint("WrongConstant")
    private void syncTemperature() {

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

    private void updateConnectionIcon() {
        android.widget.ImageView iv = requireView().findViewById(R.id.ivConnectionStatus);
        if (iv == null) return;
        NetworkUtils.ConnectionQuality quality = NetworkUtils.getConnectionQuality(requireContext());
        if (quality == NetworkUtils.ConnectionQuality.STRONG) {
            iv.setImageResource(R.drawable.ic_signal_strong);
            iv.setVisibility(android.view.View.VISIBLE);
        } else if (quality == NetworkUtils.ConnectionQuality.WEAK) {
            iv.setImageResource(R.drawable.ic_signal_weak);
            iv.setVisibility(android.view.View.VISIBLE);
        } else {
            iv.setVisibility(android.view.View.INVISIBLE);
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
    private void updateNavigationPages(TypesAvailability types) {
        if (!(getActivity() instanceof MainActivity)) return;

        List<PageType> pages = new ArrayList<>();

        if (types.isBloodGlucose()) pages.add(PageType.BLOOD_GLUCOSE);
        if (types.isBloodPressure()) pages.add(PageType.BLOOD_PRESSURE);
        if (types.isWeight()) pages.add(PageType.WEIGHT);
        if (types.isBloodOxygen()) pages.add(PageType.BLOOD_OXYGEN);
        if (types.isElectrocardiogram()) pages.add(PageType.ECG);
        if (types.isTemperature()) pages.add(PageType.TEMPERATURE);

        ((MainActivity) getActivity()).setVisiblePages(pages);
    }

    public void syncGlucose() {

        List<ReadingValue> readingValueList =  databaseClient.getAppDatabase().readingValueDao().getAllReadingValues();
        if(readingValueList != null && !readingValueList.isEmpty()) {
            for (ReadingValue readingValue: readingValueList
                 ) {
                sendGlucose(readingValue.getGlucose(), readingValue.getMailValue(), readingValue.getId());
            }
        }
    }


    public void sendGlucose(long glucose, int mailValue, long id) {
        // Usage example in your activity or service

        Log.d(TAG, "The data of glucose Reading list count : "+glucose);

        String serial = PreferenceHelper.getInstance(getContext()).getString("EMPECS", "");
        Reading reading = new Reading(
                false,
                "Asia/Manila",
                "5be9a0e03d320b73e5f7aa71",
                Arrays.asList((double)glucose, (double)mailValue),
                "5e4c0db6bc20236a64ca3467",
                "5bb306382598931ffbd1b623",
                DateUtils.getDate(),
                serial
        );
        List<Reading> readingsList = Arrays.asList(reading);
        ReadingsRequest readingsRequest = new ReadingsRequest(readingsList);

        Call<Object> call = ApiClient.getUserService(Constant.BASE_URL_BGM,"bNWZsV#BeZvaNb*gF@3Z^7tCNhCT29Vw8Vi%4T%", DeviceUtils.getIMEI(getActivity())).sendReadings(readingsRequest);
        call.enqueue(new Callback<Object>() {
            @Override
            public void onResponse(Call<Object> call, Response<Object> response) {

                if (response.isSuccessful()) {
                    // Request successful
                    // Handle response if needed
                    //saveData(glucose);
                    databaseClient.getAppDatabase().readingValueDao().deleteById(id);


                }
            }

            @Override
            public void onFailure(Call<Object> call, Throwable t) {
                // Request failed
                // Handle failure

            }
        });
    }

    public void displayOffLineData() {
        BPJumper list = databaseClient.getAppDatabase().bpJumperDao().getLatestBPJumper();
        if (list != null) {
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

        quickActionsHelper = new QuickActionsHelper(rootView != null ? rootView : getView(), this);
//
//        // Initialize badge count (you can get this from your data source)
        updateMessageBadge(2); // Replace with actual count
    }

    @Override
    public void onSettingsClicked() {
        showSaveChangesDialog();
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

    private void showSaveChangesDialog() {
        SmartWatchAlertDialog.showSaveDialog(getActivity(),
                "We detected you may have fell or requested Emergency Call?",
                new SmartWatchAlertDialog.DialogListener() {
                    @Override
                    public void onOkClicked() {
                        //saveChanges();
                        sendAlarm();                    }

                    @Override
                    public void onCancelClicked() {
                        //discardChanges();
                    }
                });
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

    private void playNotificationSound() {
        try {
            Uri notification = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION);
            Ringtone ringtone = RingtoneManager.getRingtone(getActivity(), notification);
            ringtone.play();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}