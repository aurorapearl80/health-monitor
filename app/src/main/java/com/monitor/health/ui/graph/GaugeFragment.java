package com.monitor.health.ui.graph;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.monitor.health.R;
import com.monitor.health.graph.ReadingAdapter;
import com.monitor.health.graph.ReadingData;

import java.util.ArrayList;
import java.util.List;

public class GaugeFragment extends Fragment {
    private static final String ARG_TYPE = "type";
    private String type;
    private GaugeView gaugeView;
    private TextView tvLowest, tvHighest, tvAverageMain;
    private TextView tvLowCount, tvHighCount, tvDateRange, tvTotalReadings;
    private RecyclerView recyclerView;
    private ReadingAdapter adapter;

    public static GaugeFragment newInstance(String type) {
        GaugeFragment fragment = new GaugeFragment();
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
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_gauge, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        gaugeView = view.findViewById(R.id.gaugeView);
        tvLowest = view.findViewById(R.id.tvLowest);
        tvHighest = view.findViewById(R.id.tvHighest);
        tvAverageMain = view.findViewById(R.id.tvAverageMain);
        tvLowCount = view.findViewById(R.id.tvLowCount);
        tvHighCount = view.findViewById(R.id.tvHighCount);
        tvDateRange = view.findViewById(R.id.tvDateRange);
        tvTotalReadings = view.findViewById(R.id.tvTotalReadings);
        recyclerView = view.findViewById(R.id.recyclerView);

        setupRecyclerView();
        loadData();
    }

    private void setupRecyclerView() {
        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        adapter = new ReadingAdapter();
        recyclerView.setAdapter(adapter);
    }

    private void loadData() {
        List<ReadingData> readings = generateSampleData();
        adapter.setData(readings);
        setupGauge(readings);
    }

    private List<ReadingData> generateSampleData() {
        List<ReadingData> data = new ArrayList<>();
//        data.add(new ReadingData("Sep 29", "11:00 PM", 21.9f, 8.6f, "Underweight"));
//        data.add(new ReadingData("Sep 29", "11:00 PM", 21.4f, 8.4f, "Underweight"));
//        data.add(new ReadingData("Sep 28", "10:30 PM", 150.2f, 26.6f, "Normal"));
//        data.add(new ReadingData("Sep 27", "11:30 PM", 146.8f, 26.1f, "Normal"));
//        data.add(new ReadingData("Sep 15", "9:00 PM", 148.5f, 26.3f, "Normal"));
        return data;
    }

    private void setupGauge(List<ReadingData> readings) {
        float lowest = Float.MAX_VALUE;
        float highest = Float.MIN_VALUE;
        float total = 0;

        for (ReadingData reading : readings) {
            if (reading.weight < lowest) lowest = reading.weight;
            if (reading.weight > highest) highest = reading.weight;
            total += reading.weight;
        }

        float average = readings.size() > 0 ? total / readings.size() : 0;

        gaugeView.setValue(average, 100f);

        tvLowest.setText(String.format("%.1f kg\nLOWEST", lowest));
        tvHighest.setText(String.format("%.1f kg\nHIGHEST", highest));
        tvAverageMain.setText(String.format("%.1f kg\nAVERAGE", average));
        tvLowCount.setText("0 reading");
        tvHighCount.setText("0 reading");
        tvDateRange.setText("Aug 30 - Sep 30");
        tvTotalReadings.setText(readings.size() + " readings");
    }
}