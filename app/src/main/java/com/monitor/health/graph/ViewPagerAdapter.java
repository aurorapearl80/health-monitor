package com.monitor.health.graph;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;
import androidx.viewpager2.adapter.FragmentStateAdapter;

import com.monitor.health.ui.graph.CombinedFragment;

public class ViewPagerAdapter extends FragmentStateAdapter {

    public ViewPagerAdapter(@NonNull FragmentActivity fragmentActivity) {
        super(fragmentActivity);
    }

    @NonNull
    @Override
    public Fragment createFragment(int position) {
        switch (position) {
            case 0:
                return CombinedFragment.newInstance("DAY");
            case 1:
                return CombinedFragment.newInstance("WEEK");
            case 2:
                return CombinedFragment.newInstance("MONTH");
            default:
                return CombinedFragment.newInstance("DAY");
        }
    }

    @Override
    public int getItemCount() {
        return 3;
    }
}