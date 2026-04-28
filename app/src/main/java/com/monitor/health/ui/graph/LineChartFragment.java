package com.monitor.health.ui.graph;
import android.graphics.Color;
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
import com.github.mikephil.charting.charts.LineChart;
import com.github.mikephil.charting.components.XAxis;
import com.github.mikephil.charting.components.YAxis;
import com.github.mikephil.charting.data.Entry;
import com.github.mikephil.charting.data.LineData;
import com.github.mikephil.charting.data.LineDataSet;
import java.util.ArrayList;
import java.util.List;

public class LineChartFragment extends Fragment {
    private static final String ARG_TYPE = "type";
    private String type;
    private LineChart chart;
    private TextView tvAverage, tvInRange, tvOutOfRange, tvReadings, tvDateRange;
    private RecyclerView recyclerView;
    private ReadingAdapter adapter;

    public static LineChartFragment newInstance(String type) {
        LineChartFragment fragment = new LineChartFragment();
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
        return inflater.inflate(R.layout.fragment_line_chart, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        chart = view.findViewById(R.id.chart);
        tvAverage = view.findViewById(R.id.tvAverage);
        tvInRange = view.findViewById(R.id.tvInRange);
        tvOutOfRange = view.findViewById(R.id.tvOutOfRange);
        tvReadings = view.findViewById(R.id.tvReadings);
        tvDateRange = view.findViewById(R.id.tvDateRange);
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

        setupChart(readings);
        calculateStats(readings);
        tvDateRange.setText("Sep 24 - Sep 30");
    }

    private List<ReadingData> generateSampleData() {
        List<ReadingData> data = new ArrayList<>();

//        if ("WEEK".equals(type)) {
//            data.add(new ReadingData("Sep 29", "11:00 PM", 48.3f, 8.6f, "Underweight"));
//            data.add(new ReadingData("Sep 29", "11:00 PM", 47.2f, 8.4f, "Underweight"));
//            data.add(new ReadingData("Sep 29", "10:09 PM", 60.2f, 10.7f, "Underweight"));
//            data.add(new ReadingData("Sep 29", "10:07 PM", 58.2f, 10.3f, "Underweight"));
//            data.add(new ReadingData("Sep 27", "11:30 PM", 146.8f, 26.1f, "Normal"));
//            data.add(new ReadingData("Sep 27", "11:29 PM", 152.3f, 27.1f, "Normal"));
//            data.add(new ReadingData("Sep 26", "11:00 PM", 150.6f, 26.7f, "Normal"));
//            data.add(new ReadingData("Sep 25", "10:30 PM", 148.9f, 26.4f, "Normal"));
//        } else {
//            data.add(new ReadingData("Today", "11:00 PM", 48.3f, 8.6f, "Underweight"));
//            data.add(new ReadingData("Today", "8:30 PM", 47.8f, 8.5f, "Underweight"));
//            data.add(new ReadingData("Today", "6:15 PM", 49.1f, 8.7f, "Underweight"));
//        }

        return data;
    }

    private void setupChart(List<ReadingData> readings) {
        List<Entry> entries = new ArrayList<>();

        if ("WEEK".equals(type)) {
            entries.add(new Entry(0, 75));
            entries.add(new Entry(1, 50));
            entries.add(new Entry(2, 150));
            entries.add(new Entry(3, 175));
            entries.add(new Entry(4, 150));
            entries.add(new Entry(5, 150));
            entries.add(new Entry(6, 150));
            entries.add(new Entry(7, 75));
            entries.add(new Entry(8, 150));
            entries.add(new Entry(9, 75));
            entries.add(new Entry(10, 50));
            entries.add(new Entry(11, 150));
            entries.add(new Entry(12, 50));
        } else {
            for (int i = 0; i < readings.size(); i++) {
                entries.add(new Entry(i, readings.get(i).weight));
            }
        }

        LineDataSet dataSet = new LineDataSet(entries, "Weight");
        dataSet.setColor(Color.WHITE);
        dataSet.setLineWidth(3f);
        dataSet.setDrawCircles(false);
        dataSet.setDrawValues(false);
        dataSet.setDrawFilled(false);
        dataSet.setMode(LineDataSet.Mode.LINEAR);

        LineData lineData = new LineData(dataSet);
        chart.setData(lineData);

        chart.getDescription().setEnabled(false);
        chart.getLegend().setEnabled(false);
        chart.setTouchEnabled(false);
        chart.setDragEnabled(false);
        chart.setScaleEnabled(false);
        chart.setDrawGridBackground(true);
        chart.setGridBackgroundColor(Color.parseColor("#4CAF50"));

        XAxis xAxis = chart.getXAxis();
        xAxis.setPosition(XAxis.XAxisPosition.BOTTOM);
        xAxis.setDrawGridLines(true);
        xAxis.setGridColor(Color.parseColor("#66FFFFFF"));
        xAxis.setTextColor(Color.WHITE);
        xAxis.setDrawLabels(false);

        YAxis leftAxis = chart.getAxisLeft();
        leftAxis.setDrawGridLines(true);
        leftAxis.setGridColor(Color.parseColor("#66FFFFFF"));
        leftAxis.setTextColor(Color.WHITE);
        leftAxis.setAxisMinimum(0f);
        leftAxis.setAxisMaximum(250f);

        YAxis rightAxis = chart.getAxisRight();
        rightAxis.setEnabled(false);

        chart.invalidate();
    }

    private void calculateStats(List<ReadingData> readings) {
        float total = 0;
        int inRange = 0;
        int outOfRange = 0;

        for (ReadingData reading : readings) {
            total += reading.weight;
            if ("Normal".equals(reading.status)) {
                inRange++;
            } else {
                outOfRange++;
            }
        }

        float average = readings.size() > 0 ? total / readings.size() : 0;
        float inRangePercent = readings.size() > 0 ? (inRange * 100.0f / readings.size()) : 0;
        float outOfRangePercent = readings.size() > 0 ? (outOfRange * 100.0f / readings.size()) : 0;

        tvAverage.setText(String.format("%.1f lb\nAVERAGE", average));
        tvInRange.setText(String.format("%.1f %%\nIN RANGE", inRangePercent));
        tvOutOfRange.setText(String.format("%.1f %%\nOUT OF RANGE", outOfRangePercent));
        tvReadings.setText(readings.size() + " readings");
    }
}