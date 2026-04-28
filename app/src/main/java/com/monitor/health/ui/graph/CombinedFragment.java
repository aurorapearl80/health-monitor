package com.monitor.health.ui.graph;

import android.graphics.Color;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;


import com.monitor.health.R;
import com.monitor.health.graph.ReadingAdapter;
import com.monitor.health.graph.ReadingData;

import com.monitor.health.viewmodel.ReadingsViewModel;
import com.monitor.health.viewmodel.SharedDataViewModel;

import java.util.List;

public class CombinedFragment extends Fragment {
    private static final String ARG_TYPE = "type";
    private String type;
    private SharedDataViewModel sharedViewModel;
    private ReadingsViewModel vm;

    private static final String BACKGROUND_COLOR = "#2E7D32";
    private static final String CHART_BACKGROUND_COLOR = "#4CAF50";
    private static final String GAUGE_ARC_CHART_BACKGROUND_COLOR = "#EF5350";

    private LinearLayout rootLayout;
    //private LineChart chart;
    private RecyclerView recyclerView;
    private ReadingAdapter adapter;

    // Day-specific UI elements
    private TextView tvDayHighest, tvDayAverage, tvDayTotalReadings;

    // Week-specific UI elements
    private TextView tvWeekHighest, tvWeekAverage, tvWeekTotalReadings, tvWeekDateRange;

    // Month-specific UI elements
    private TextView tvMonthHighest, tvMonthAverage, tvMonthTotalReadings, tvMonthDateRange;

    // Common stats elements
    private TextView tvReadings, tvDateRange;

    public static CombinedFragment newInstance(String type) {
        CombinedFragment fragment = new CombinedFragment();
        Bundle args = new Bundle();
        args.putString(ARG_TYPE, type);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            type = getArguments().getString(ARG_TYPE);
        }
        sharedViewModel = new ViewModelProvider(requireActivity()).get(SharedDataViewModel.class);
        vm = new ViewModelProvider(requireActivity()).get(ReadingsViewModel.class);

        Log.d("CombinedFragment", "Day data loaded: ");
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        // Inflate different layouts based on type
        int layoutId = getLayoutForType();
        return inflater.inflate(layoutId, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        initializeCommonViews(view);
        initializeTypeSpecificViews(view);
        setupRecyclerView();
        observeColorChanges();
        observeDataChanges();
    }

    private int getLayoutForType() {
        switch (type) {
            case "DAY":
                return R.layout.fragment_day_graph;
            case "WEEK":
                return R.layout.fragment_week_graph;
            case "MONTH":
                return R.layout.fragment_month_graph;
            default:
                return R.layout.fragment_combined;
        }
    }

    private void initializeCommonViews(View view) {
        rootLayout = view.findViewById(R.id.rootLayout);
        rootLayout.setBackgroundColor(Color.parseColor(BACKGROUND_COLOR));

        //chart = view.findViewById(R.id.chart);
        recyclerView = view.findViewById(R.id.recyclerView);
    }

    private void initializeTypeSpecificViews(View view) {
        if ("DAY".equals(type)) {
            initializeDayViews(view);
        } else if ("WEEK".equals(type)) {
            initializeWeekViews(view);
        } else if ("MONTH".equals(type)) {
            initializeMonthViews(view);
        }
    }

    private void initializeDayViews(View view) {
        tvReadings = view.findViewById(R.id.tvReadings);
    }

    private void initializeWeekViews(View view) {
//
        tvReadings = view.findViewById(R.id.tvReadings);
    }

    private void initializeMonthViews(View view) {

        tvReadings = view.findViewById(R.id.tvReadings);
    }

    private void setupRecyclerView() {
        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        adapter = new ReadingAdapter();
        recyclerView.setAdapter(adapter);
    }

    private void observeDataChanges() {
        if ("DAY".equals(type)) {
            sharedViewModel.getDayData().observe(getViewLifecycleOwner(), this::updateDayUI);
        } else if ("WEEK".equals(type)) {
            sharedViewModel.getWeekData().observe(getViewLifecycleOwner(), this::updateWeekUI);
        } else if ("MONTH".equals(type)) {
            sharedViewModel.getMonthData().observe(getViewLifecycleOwner(), this::updateMonthUI);
        }
    }

    private void updateDayUI(List<ReadingData> readings) {
        if (readings == null || readings.isEmpty()) {
            return;
        }
        adapter.setData(readings);
        //setupChart(readings);
        //calculateDayStats(readings);
    }

    private void updateWeekUI(List<ReadingData> readings) {
        if (readings == null || readings.isEmpty()) {
            return;
        }
        adapter.setData(readings);
        //setupChart(readings);
        //calculateWeekStats(readings);
    }

    private void updateMonthUI(List<ReadingData> readings) {
        if (readings == null || readings.isEmpty()) {
            return;
        }
        adapter.setData(readings);
        //setupChart(readings);
        //calculateMonthStats(readings);
    }

    private void observeColorChanges() {
        sharedViewModel.getBackgroundColor().observe(getViewLifecycleOwner(), color -> {
            rootLayout.setBackgroundColor(Color.parseColor(color));

            if (adapter != null) {
                adapter.setBackgroundColor(color);
            }
        });

        sharedViewModel.getChartBackgroundColor().observe(getViewLifecycleOwner(), color -> {
        });
    }
}