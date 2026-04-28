package com.monitor.health.adapter;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.List;

public class CarouselAdapter extends RecyclerView.Adapter<CarouselAdapter.ViewHolder> {

    private final Context context;
    private final List<Integer> pageLayouts;

    public CarouselAdapter(Context context, List<Integer> pageLayouts) {
        this.context = context;
        this.pageLayouts = pageLayouts;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(pageLayouts.get(viewType), parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        // You can bind data here if needed
    }

    @Override
    public int getItemCount() {
        return pageLayouts.size();
    }

    @Override
    public int getItemViewType(int position) {
        return position;
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        public ViewHolder(View itemView) {
            super(itemView);
        }
    }
}

