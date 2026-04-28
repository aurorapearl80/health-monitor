package com.monitor.health.graph;


import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.monitor.health.R;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class ReadingAdapter extends RecyclerView.Adapter<ReadingAdapter.ViewHolder> {
    private List<ReadingData> data = new ArrayList<>();
    private String backgroundColor = "#2E7D32"; // Default color

    public void setData(List<ReadingData> data) {
        this.data = data;
        notifyDataSetChanged();
    }

    public void setBackgroundColor(String color) {
        this.backgroundColor = color;
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_reading, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        ReadingData reading = data.get(position);
        holder.tvDate.setText(reading.date);
        holder.tvTime.setText(reading.time);
        //Glucose
        if(reading.getViewLayout() == 1) {
            holder.tvWeight.setText( Math.round(reading.weight)+"");
            holder.tvBmi.setText("mg/dL");
            holder.tvStatus.setText(getStatusGlucose(reading.getBmi()));
        }
        //Weight
        else if(reading.getViewLayout() == 2) {
            holder.tvWeight.setText(reading.weight+"");
            holder.tvBmi.setText(reading.getStatus());
            holder.tvStatus.setText(getStatusWeight(reading.getBmi()));
        }
        //Blood pressure
        else if(reading.getViewLayout() == 3) {
            holder.tvWeight.setText( Math.round(reading.weight)+"/"+Math.round(reading.bmi)+"");
            holder.tvBmi.setText("mmHg");
            holder.tvStatus.setText(getStatusBloodPressure(reading.getBmi()));
        }
        //Blood oxygen
        else if(reading.getViewLayout() == 4) {
            holder.tvWeight.setText(reading.weight+"");
            holder.tvBmi.setText(reading.getStatus());
            holder.tvStatus.setText(getStatusBloodOxygen(reading.getBmi()));
        }
        // Temperature
        else if(reading.getViewLayout() == 5) {
            holder.tvWeight.setText(String.format(Locale.getDefault(), "%.1f", reading.weight));
            holder.tvBmi.setText(reading.getStatus());
            holder.tvStatus.setText(getStatusTemperature(reading.getBmi()));
        }
        // ECG / Heart Rate
        else if(reading.getViewLayout() == 6) {
            holder.tvWeight.setText(Math.round(reading.weight) + "");
            holder.tvBmi.setText(reading.getStatus());
            holder.tvStatus.setText(getStatusTemperature(reading.getBmi()));
        }
//        else {
//            holder.tvWeight.setText( Math.round(reading.weight)+"/"+Math.round(reading.bmi)+"");
//            holder.tvBmi.setText("mmHg");
//            holder.tvStatus.setText(getStatusBloodPressure(reading.getBmi()));
//        }
//        holder.tvBmi.setText("mmHg");
//        holder.tvStatus.setText(reading.status);
        // Set background color for each item
        int bgColor = Color.parseColor(backgroundColor);
        holder.itemView.setBackgroundColor(bgColor);
        applyAccessibleTextColors(holder, bgColor);
    }

    private void applyAccessibleTextColors(@NonNull ViewHolder holder, int background) {
        boolean useDarkText = isLightColor(background);
        int primary = useDarkText ? Color.parseColor("#111111") : Color.WHITE;
        int secondary = useDarkText ? Color.parseColor("#2F2F2F") : Color.parseColor("#F2F2F2");

        holder.tvDate.setTextColor(primary);
        holder.tvWeight.setTextColor(primary);
        holder.tvTime.setTextColor(secondary);
        holder.tvBmi.setTextColor(secondary);
        holder.tvStatus.setTextColor(primary);
    }

    private boolean isLightColor(int color) {
        double luminance = (0.299 * Color.red(color) + 0.587 * Color.green(color) + 0.114 * Color.blue(color)) / 255.0;
        return luminance > 0.6;
    }

    private String getStatusGlucose(float value) {
//        if (value < 18.5) return "Underweight";
//        if (value < 25) return "Normal";
//        if (value < 30) return "Overweight";
        return "Low";
    }

    private String getStatusBloodPressure(float value) {
//        if (value < 18.5) return "Underweight";
//        if (value < 25) return "Normal";
//        if (value < 30) return "Overweight";
        return "Good";
    }

    private String getStatusWeight(float value) {
        if (value < 18.5) return "Underweight";
        if (value < 25) return "Normal";
        if (value < 30) return "Overweight";
        return "Obese";
    }

    private String getStatusBloodOxygen(float value) {
//        if (value < 18.5) return "Underweight";
//        if (value < 25) return "Normal";
//        if (value < 30) return "Overweight";
        return "Good";
    }

    private String getStatusTemperature(float value) {
//        if (value < 18.5) return "Underweight";
//        if (value < 25) return "Normal";
//        if (value < 30) return "Overweight";
        return "Good";
    }

    @Override
    public int getItemCount() {
        return data.size();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        TextView tvDate, tvTime, tvWeight, tvBmi, tvStatus;

        ViewHolder(@NonNull View itemView) {
            super(itemView);
            tvDate = itemView.findViewById(R.id.tvDate);
            tvTime = itemView.findViewById(R.id.tvTime);
            tvWeight = itemView.findViewById(R.id.tvWeight);
            tvBmi = itemView.findViewById(R.id.tvBmi);
            tvStatus = itemView.findViewById(R.id.tvStatus);
        }
    }
}
