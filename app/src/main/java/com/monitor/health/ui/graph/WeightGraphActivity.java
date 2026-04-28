package com.monitor.health.ui.graph;

import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.Observer;
import androidx.lifecycle.ViewModelProvider;
import androidx.viewpager2.widget.ViewPager2;

import com.monitor.health.Constant;
import com.monitor.health.R;
import com.monitor.health.graph.ReadingData;
import com.monitor.health.graph.ViewPagerAdapter;
import com.monitor.health.response.alldata.DataItem;
import com.monitor.health.response.alldata.ReadingMetricValue;
import com.monitor.health.response.readinghistory.ReadingHistoryItem;
import com.monitor.health.utility.DateUtils;
import com.monitor.health.utility.DeviceUtils;
import com.monitor.health.utility.UnitConverter;
import com.monitor.health.viewmodel.ReadingsViewModel;
import com.monitor.health.viewmodel.SharedDataViewModel;
import com.google.android.material.tabs.TabLayout;
import com.google.android.material.tabs.TabLayoutMediator;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

public class WeightGraphActivity extends AppCompatActivity {

    private ViewPager2 viewPager;
    private TabLayout tabLayout;
    private ViewPagerAdapter adapter;
    private SharedDataViewModel sharedViewModel;
    private ReadingsViewModel vm;

    private String currentTab = "DAY";
    private FrameLayout loadingOverlay;
    private TextView tvLoadingMessage;

    // Observers
    private Observer<List<DataItem>> dayObserver;
    private Observer<List<DataItem>> weekObserver;
    private Observer<List<DataItem>> monthObserver;

    // ECG reading-history observers
    private Observer<List<ReadingHistoryItem>> ecgDayObserver;
    private Observer<List<ReadingHistoryItem>> ecgWeekObserver;
    private Observer<List<ReadingHistoryItem>> ecgMonthObserver;

    String readingType = "BloodGlucose";

    // API Constants
    private static final String BASE_URL = "bNWZsV#BeZvaNb*gF@3Z^7tCNhCT29Vw8Vi%4T%";
    private static final String TAG = "WeightGraphActivity";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_weight_graph);

        // Initialize ViewModels
        sharedViewModel = new ViewModelProvider(this).get(SharedDataViewModel.class);
        vm = new ViewModelProvider(this).get(ReadingsViewModel.class);

        // Initialize UI components
        initializeViews();

        // Get reading type from intent
        readingType = getReadingTypeFromIntent();
        applyThemeForReadingType(readingType);

        // Setup adapter and pager
        setupViewPager();

        // Load initial data
        loadInitialData();
    }

    private void initializeViews() {
        viewPager = findViewById(R.id.viewPager);
        tabLayout = findViewById(R.id.tabLayout);
        loadingOverlay = findViewById(R.id.loadingOverlay);
        tvLoadingMessage = findViewById(R.id.tvLoadingMessage);

        // Observe background color changes
        sharedViewModel.getBackgroundColor().observe(this, color -> {
            findViewById(R.id.mainLayout).setBackgroundColor(Color.parseColor(color));
            tabLayout.setBackgroundColor(Color.parseColor(color));
        });
    }

    private String getReadingTypeFromIntent() {
        Intent intent = getIntent();
        if (intent.hasExtra("backgroundColor")) {
            return intent.getStringExtra("backgroundColor");
        }
        return "WeightFragment";
    }

    private void applyThemeForReadingType(String readingType) {
        if (readingType != null) {
            if (readingType.contains("BloodGlucose")) {
                changeTheme("#2196F3", "#2196F3", "#2196F3");
            } else if (readingType.contains("WeightFragment")) {
                changeTheme("#2E7D32", "#4CAF50", "#EF5350");
            } else if (readingType.contains("BloodPressureFragment")) {
                changeTheme("#FF7043", "#FF7043", "#FF7043");
            } else if (readingType.contains("BloodOxygenFragment")) {
                changeTheme("#f186c0", "#f186c0", "#f186c0");
            } else if (readingType.contains("TemperatureFragment")) {
                changeTheme("#AD1457", "#AD1457", "#AD1457");
            } else if (readingType.contains("ElectrocardiogramFragment")) {
                changeTheme("#AA00FF", "#AA00FF", "#AA00FF");
            }



        }
    }

    private void setupViewPager() {
        adapter = new ViewPagerAdapter(this);
        viewPager.setAdapter(adapter);

        new TabLayoutMediator(tabLayout, viewPager, (tab, position) -> {
            switch (position) {
                case 0:
                    tab.setText("DAY");
                    break;
                case 1:
                    tab.setText("WEEK");
                    break;
                case 2:
                    tab.setText("MONTH");
                    break;
            }
        }).attach();

        // Setup tab listener
        setupTabListener();
    }

    private void setupTabListener() {
        tabLayout.addOnTabSelectedListener(new TabLayout.OnTabSelectedListener() {
            @Override
            public void onTabSelected(TabLayout.Tab tab) {
                int position = tab.getPosition();
                currentTab = getTabName(position);

                switch (position) {
                    case 0:
                        onDayTabSelected();
                        break;
                    case 1:
                        onWeekTabSelected();
                        break;
                    case 2:
                        onMonthTabSelected();
                        break;
                }
            }

            @Override
            public void onTabUnselected(TabLayout.Tab tab) {
                // No action needed
            }

            @Override
            public void onTabReselected(TabLayout.Tab tab) {
                // Refresh data on reselect
                onTabSelected(tab);
            }
        });
    }

    private String getTabName(int position) {
        switch (position) {
            case 0:
                return "DAY";
            case 1:
                return "WEEK";
            case 2:
                return "MONTH";
            default:
                return "DAY";
        }
    }

    private void loadInitialData() {
        showLoading(true, "Fetching data from server...");

        String startDate = DateUtils.getTodayIsoStart();
        String endDate = DateUtils.getTodayIsoEnd();

        fetchDayData(startDate, endDate);
    }

    private void onDayTabSelected() {
        Log.d("WeightGraphActivity", "Day tab selected");
        showLoading(true, "Fetching data from server...");

        if (dayObserver != null) {
            vm.getTodayReadings().removeObserver(dayObserver);
        }

        String startDate = DateUtils.getTodayIsoStart();
        String endDate = DateUtils.getTodayIsoEnd();

        fetchDayData(startDate, endDate);
    }

    private void fetchDayData(String startDate, String endDate) {
        String readingTypeId = isEcgReadingType() ? "Heart Rate" : getReadingTypeConstant();

        if (isEcgReadingType()) {
            if (ecgDayObserver != null) vm.getEcgHistoryDay().removeObserver(ecgDayObserver);
            Log.d(TAG, "[REQUEST] fetchEcgHistoryDay | endpoint: " + Constant.BASE_URL_BGM
                    + " | imei: " + DeviceUtils.getIMEI(getApplicationContext())
                    + " | readingTypeId: " + readingTypeId
                    + " | startDate: " + startDate + " | endDate: " + endDate
                    + " | fullUrl: " + Constant.BASE_URL_BGM + "api/doctor-watches/reading-history"
                    + "?metric=" + readingTypeId
                    + "&start_date=" + startDate
                    + "&end_date=" + endDate
                    + "&per_page=100&page=1");
            vm.fetchEcgHistoryDay(Constant.BASE_URL_BGM, BASE_URL,
                    DeviceUtils.getIMEI(getApplicationContext()), readingTypeId, startDate, endDate);
            ecgDayObserver = items -> {
                if (items == null) return;
                Log.d(TAG, "[RESULT] fetchEcgHistoryDay | count: " + items.size() + " | data: " + items);
                sharedViewModel.setDayData(items.isEmpty() ? new ArrayList<>() : convertEcgHistoryToReadingData(items));
                showLoading(false, "");
            };
            vm.getEcgHistoryDay().observe(this, ecgDayObserver);
            return;
        }

        Log.d(TAG, "[REQUEST] fetchToday | endpoint: " + Constant.BASE_URL_BGM
                + " | imei: " + DeviceUtils.getIMEI(getApplicationContext())
                + " | readingTypeId: " + readingTypeId
                + " | startDate: " + startDate + " | endDate: " + endDate
                + " | fullUrl: " + Constant.BASE_URL_BGM + "api/doctor-watches/readings"
                + "?reading_type_id=" + readingTypeId
                + "&start_date=" + startDate
                + "&end_date=" + endDate
                + "&per_page=all&page=1&sort=-createdAt");
        vm.fetchToday(Constant.BASE_URL_BGM, BASE_URL,
                DeviceUtils.getIMEI(getApplicationContext()), readingTypeId, startDate, endDate);

        dayObserver = todayData -> {
            if (todayData == null) return;
            Log.d(TAG, "[RESULT] fetchToday | count: " + todayData.size() + " | data: " + todayData);
            if (!todayData.isEmpty()) {
                List<ReadingData> readingList = convertToReadingData(todayData);
                sharedViewModel.setDayData(readingList);
                Log.d(TAG, "Day data loaded: " + readingList.size() + " items");
            } else {
                sharedViewModel.setDayData(new ArrayList<>());
            }
            showLoading(false, "");
        };

        vm.getTodayReadings().observe(this, dayObserver);
    }

    private void onWeekTabSelected() {
        Log.d("WeightGraphActivity", "Week tab selected");
        showLoading(true, "Fetching data from server...");

        if (weekObserver != null) {
            vm.getWeekReadings().removeObserver(weekObserver);
        }

        String startDate = DateUtils.getDateDaysAgoIsoStart(7);
        String endDate = DateUtils.getTodayIsoEnd();

        fetchWeekData(startDate, endDate);
    }

    private void fetchWeekData(String startDate, String endDate) {
        String readingTypeId = getReadingTypeConstant();

        if (isEcgReadingType()) {
            if (ecgWeekObserver != null) vm.getEcgHistoryWeek().removeObserver(ecgWeekObserver);
            Log.d(TAG, "[REQUEST] fetchEcgHistoryWeek | endpoint: " + Constant.BASE_URL_BGM
                    + " | imei: " + DeviceUtils.getIMEI(getApplicationContext())
                    + " | readingTypeId: " + readingTypeId
                    + " | startDate: " + startDate + " | endDate: " + endDate
                    + " | fullUrl: " + Constant.BASE_URL_BGM + "api/doctor-watches/reading-history"
                    + "?metric=" + readingTypeId
                    + "&start_date=" + startDate
                    + "&end_date=" + endDate
                    + "&per_page=100&page=1");
            vm.fetchEcgHistoryWeek(Constant.BASE_URL_BGM, BASE_URL,
                    DeviceUtils.getIMEI(getApplicationContext()), readingTypeId, startDate, endDate);
            ecgWeekObserver = items -> {
                if (items == null) return;
                Log.d(TAG, "[RESULT] fetchEcgHistoryWeek | count: " + items.size() + " | data: " + items);
                sharedViewModel.setWeekData(items.isEmpty() ? new ArrayList<>() : convertEcgHistoryToReadingData(items));
                showLoading(false, "");
            };
            vm.getEcgHistoryWeek().observe(this, ecgWeekObserver);
            return;
        }

        Log.d(TAG, "[REQUEST] fetchWeek | endpoint: " + Constant.BASE_URL_BGM
                + " | imei: " + DeviceUtils.getIMEI(getApplicationContext())
                + " | readingTypeId: " + readingTypeId
                + " | startDate: " + startDate + " | endDate: " + endDate
                + " | fullUrl: " + Constant.BASE_URL_BGM + "api/doctor-watches/readings"
                + "?reading_type_id=" + readingTypeId
                + "&start_date=" + startDate
                + "&end_date=" + endDate
                + "&per_page=all&page=1&sort=-createdAt");
        vm.fetchWeek(Constant.BASE_URL_BGM, BASE_URL,
                DeviceUtils.getIMEI(getApplicationContext()), readingTypeId, startDate, endDate);

        weekObserver = weekData -> {
            if (weekData == null) return;
            Log.d(TAG, "[RESULT] fetchWeek | count: " + weekData.size() + " | data: " + weekData);
            if (!weekData.isEmpty()) {
                List<ReadingData> readingList = convertToReadingData(weekData);
                sharedViewModel.setWeekData(readingList);
                Log.d(TAG, "Week data loaded: " + readingList.size() + " items");
            } else {
                sharedViewModel.setWeekData(new ArrayList<>());
            }
            showLoading(false, "");
        };

        vm.getWeekReadings().observe(this, weekObserver);
    }

    private void onMonthTabSelected() {
        Log.d("WeightGraphActivity", "Month tab selected");
        showLoading(true, "Fetching data from server...");

        if (monthObserver != null) {
            vm.getMonthsReadings().removeObserver(monthObserver);
        }

        String startDate = DateUtils.getDateDaysAgoIsoStart(30);
        String endDate = DateUtils.getTodayIsoEnd();

        fetchMonthData(startDate, endDate);
    }

    private void fetchMonthData(String startDate, String endDate) {
        String readingTypeId = isEcgReadingType() ? "Heart Rate" : getReadingTypeConstant();

        if (isEcgReadingType()) {
            if (ecgMonthObserver != null) vm.getEcgHistoryMonth().removeObserver(ecgMonthObserver);
            Log.d(TAG, "[REQUEST] fetchEcgHistoryMonth | endpoint: " + Constant.BASE_URL_BGM
                    + " | imei: " + DeviceUtils.getIMEI(getApplicationContext())
                    + " | readingTypeId: " + readingTypeId
                    + " | startDate: " + startDate + " | endDate: " + endDate
                    + " | fullUrl: " + Constant.BASE_URL_BGM + "api/doctor-watches/reading-history"
                    + "?metric=" + readingTypeId
                    + "&start_date=" + startDate
                    + "&end_date=" + endDate
                    + "&per_page=100&page=1");
            vm.fetchEcgHistoryMonth(Constant.BASE_URL_BGM, BASE_URL,
                    DeviceUtils.getIMEI(getApplicationContext()), readingTypeId, startDate, endDate);
            ecgMonthObserver = items -> {
                if (items == null) return;
                Log.d(TAG, "[RESULT] fetchEcgHistoryMonth | count: " + items.size() + " | data: " + items);
                sharedViewModel.setMonthData(items.isEmpty() ? new ArrayList<>() : convertEcgHistoryToReadingData(items));
                showLoading(false, "");
            };
            vm.getEcgHistoryMonth().observe(this, ecgMonthObserver);
            return;
        }

        Log.d(TAG, "[REQUEST] fetchMonth | endpoint: " + Constant.BASE_URL_BGM
                + " | imei: " + DeviceUtils.getIMEI(getApplicationContext())
                + " | readingTypeId: " + readingTypeId
                + " | startDate: " + startDate + " | endDate: " + endDate
                + " | fullUrl: " + Constant.BASE_URL_BGM + "api/doctor-watches/readings"
                + "?reading_type_id=" + readingTypeId
                + "&start_date=" + startDate
                + "&end_date=" + endDate
                + "&per_page=all&page=1&sort=-createdAt");
        vm.fetchMonth(Constant.BASE_URL_BGM, BASE_URL,
                DeviceUtils.getIMEI(getApplicationContext()), readingTypeId, startDate, endDate);

        monthObserver = monthData -> {
            if (monthData == null) return;
            Log.d(TAG, "[RESULT] fetchMonth | count: " + monthData.size() + " | data: " + monthData);
            if (!monthData.isEmpty()) {
                List<ReadingData> readingList = convertToReadingData(monthData);
                sharedViewModel.setMonthData(readingList);
                Log.d(TAG, "Month data loaded: " + readingList.size() + " items");
            } else {
                sharedViewModel.setMonthData(new ArrayList<>());
            }
            showLoading(false, "");
        };

        vm.getMonthsReadings().observe(this, monthObserver);
    }

    private List<ReadingData> convertToReadingData(List<DataItem> dataItems) {
        //Notes: Status 1 = Glucose, 2 = Blood Pressure, 3 = Weight, 4 = Blood Oxygen, 5 = Electrocardiogram, 6 = Temperature
        int viewLayout = 1;
        if (readingType != null) {
            if (readingType.contains("BloodGlucose")) {
                viewLayout = 1;
            } else if (readingType.contains("WeightFragment")) {
                viewLayout = 2;
            } else if (readingType.contains("BloodPressureFragment")) {
                viewLayout = 3;
            } else if (readingType.contains("BloodPressureFragment")) {
                viewLayout = 4;
            } else if (readingType.contains("TemperatureFragment")) {
                viewLayout = 5;
            } else if (readingType.contains("ElectrocardiogramFragment")) {
                viewLayout = 6;
            }

        }

        List<ReadingData> readingList = new ArrayList<>();
        for (DataItem item : dataItems) {
//            Log.wtf("DATA getReadingValue", item.getReadingMetricValues().get(0).isShould_convert()+" result");
//            for (ReadingMetricValue metricValue: item.getReadingMetricValues()) {
//                Log.wtf("DATA getReadingValue metricValue",  metricValue.getConvertedValues().get(0).getValue()+"");
//            }
            try {
                //String ratingInfo = item.getReadingMetricValues().get(0).getRatingInfo();
                float value0 = (float) ((double) item.getValue().get(0));
                float value1 = 0;
                if (readingType.contains("WeightFragment")) {
                    String unit = item.getReadingMetricValues().get(0).isShould_convert() ? "kgs" : "pounds";
                    double actualValue = item.getReadingMetricValues().get(0).isShould_convert() ? value0 : UnitConverter.kgToLbsWholeNumber(value0);
                    readingList.add(new ReadingData(
                            DateUtils.formatDate(item.getDeviceReadingDate()),
                            DateUtils.formatTime(item.getDeviceReadingDate()),
                            (float) actualValue,
                            value1,
                            unit,
                            viewLayout
                    ));
                } else if (readingType.contains("BloodOxygenFragment")) {
                    value1 = (float) ((double) item.getValue().get(1));
                    String unit = "%";
                    // Choose the correct value
                    double selectedValue = (value0 != 0) ? value0 : value1;

                    readingList.add(new ReadingData(
                            DateUtils.formatDate(item.getDeviceReadingDate()),
                            DateUtils.formatTime(item.getDeviceReadingDate()),
                            (int) selectedValue,
                            value1,
                            unit,
                            viewLayout
                    ));
                } else if (readingType.contains("TemperatureFragment")) {
                    String unit = item.getReadingMetricValues().get(0).isShould_convert() ? Constant.CELSIUS_VALUE : Constant.FAHRENHEIT_VALUE;
                    double actualValue = item.getReadingMetricValues().get(0).isShould_convert() ? value0 : UnitConverter.celsiusToFahrenheit(value0);
                    readingList.add(new ReadingData(
                            DateUtils.formatDate(item.getDeviceReadingDate()),
                            DateUtils.formatTime(item.getDeviceReadingDate()),
                            (float) actualValue,
                            value1,
                            unit,
                            viewLayout
                    ));
                }
                else {
                    value1 = (float) ((double) item.getValue().get(1));
                    readingList.add(new ReadingData(
                            DateUtils.formatDate(item.getDeviceReadingDate()),
                            DateUtils.formatTime(item.getDeviceReadingDate()),
                            value0,
                            value1,
                            getStatus(value1),
                            viewLayout
                    ));
                }


            } catch (Exception e) {
                Log.e("WeightGraphActivity", "Error converting data item", e);
            }
        }

        return readingList;
    }

    //Get the data specific ID of device
    private String getReadingTypeConstant() {
        Intent intent = getIntent();
        if (intent.hasExtra("backgroundColor")) {
            String type = intent.getStringExtra("backgroundColor");
            if (type != null && type.contains("BloodGlucose")) {
                return Constant.ID_BLOOD_GLUCOSE;
            } else if (type != null && type.contains("BloodPressureFragment")) {
                return Constant.ID_BLOOD_PRESSURE;
            } else if (type != null && type.contains("WeightFragment")) {
                return Constant.ID_WEIGHT;
            } else if (type != null && type.contains("BloodOxygenFragment")) {
                return Constant.ID_BLOOD_OXYGEN;
            } else if (type != null && type.contains("TemperatureFragment")) {
                return Constant.ID_TEMPERATURE;
            }  else if (type != null && type.contains("ElectrocardiogramFragment")) {
                return "Heart Rate";
            }

        }
        return Constant.ID_BLOOD_PRESSURE; // Default
    }

    private boolean isEcgReadingType() {
        return readingType != null && readingType.contains("ElectrocardiogramFragment");
    }

    private List<ReadingData> convertEcgHistoryToReadingData(List<ReadingHistoryItem> items) {
        List<ReadingData> result = new ArrayList<>();
        for (ReadingHistoryItem item : items) {
            if (item.getValue() == 0) continue;
            try {
                result.add(new ReadingData(
                        DateUtils.formatDate(item.getDate()),
                        DateUtils.formatTime(item.getDate()),
                        (float) item.getValue(),
                        0f,
                        item.getUnit() != null ? item.getUnit() : "bpm",
                        6
                ));
            } catch (Exception e) {
                Log.e("WeightGraphActivity", "Error converting ECG history item", e);
            }
        }
        return result;
    }

    private String getStatus(float value) {
        if (value < 18.5) return "Underweight";
        if (value < 25) return "Normal";
        if (value < 30) return "Overweight";
        return "Obese";
    }

    private void showLoading(boolean show, String message) {
        if (loadingOverlay != null) {
            loadingOverlay.setVisibility(show ? View.VISIBLE : View.GONE);

            if (show && tvLoadingMessage != null) {
                tvLoadingMessage.setText(message);
            }
        }
    }

    public void changeTheme(String bgColor, String chartBgColor, String gaugeColor) {
        sharedViewModel.setBackgroundColor(bgColor);
        sharedViewModel.setChartBackgroundColor(chartBgColor);
        sharedViewModel.setGaugeArcColor(gaugeColor);
    }

    @Override
    protected void onResume() {
        super.onResume();

        // Reload data for current tab
        switch (currentTab) {
            case "DAY":
                onDayTabSelected();
                break;
            case "WEEK":
                onWeekTabSelected();
                break;
            case "MONTH":
                onMonthTabSelected();
                break;
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        // Remove observers to prevent memory leaks
        if (dayObserver != null) vm.getTodayReadings().removeObserver(dayObserver);
        if (weekObserver != null) vm.getWeekReadings().removeObserver(weekObserver);
        if (monthObserver != null) vm.getMonthsReadings().removeObserver(monthObserver);
        if (ecgDayObserver != null) vm.getEcgHistoryDay().removeObserver(ecgDayObserver);
        if (ecgWeekObserver != null) vm.getEcgHistoryWeek().removeObserver(ecgWeekObserver);
        if (ecgMonthObserver != null) vm.getEcgHistoryMonth().removeObserver(ecgMonthObserver);
    }
}