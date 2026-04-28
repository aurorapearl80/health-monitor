package com.monitor.health.adapter;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.monitor.health.R;
import com.monitor.health.ui.ProfileFragment;

import java.util.List;

public class StatsAdapter extends RecyclerView.Adapter<StatsAdapter.StatViewHolder> {
    private List<ProfileFragment.StatItem> stats;

    public StatsAdapter(List<ProfileFragment.StatItem> stats) {
        this.stats = stats;
    }

    @NonNull
    @Override
    public StatViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_stat, parent, false);
        return new StatViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull StatViewHolder holder, int position) {
        ProfileFragment.StatItem stat = stats.get(position);
        holder.bind(stat);
    }

    @Override
    public int getItemCount() {
        return stats.size();
    }

    static class StatViewHolder extends RecyclerView.ViewHolder {
        private ImageView iconView;
        private TextView titleView;
        private TextView valueView;

        public StatViewHolder(@NonNull View itemView) {
            super(itemView);
            iconView = itemView.findViewById(R.id.stat_icon);
            titleView = itemView.findViewById(R.id.stat_title);
            valueView = itemView.findViewById(R.id.stat_value);
        }

        public void bind(ProfileFragment.StatItem stat) {
            iconView.setImageResource(stat.getIconRes());
            titleView.setText(stat.getTitle());
            valueView.setText(stat.getValue());
        }
    }
}