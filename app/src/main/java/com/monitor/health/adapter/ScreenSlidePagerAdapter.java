package com.monitor.health.adapter;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;
import androidx.viewpager2.adapter.FragmentStateAdapter;

import com.monitor.health.ui.BloodOxygenFragment;
import com.monitor.health.ui.BloodPressureFragment;
import com.monitor.health.ui.BloodPressureNativeFragment;
import com.monitor.health.ui.ElectrocardiogramFragment;
import com.monitor.health.ui.HeartRateFragment;
import com.monitor.health.ui.OxygenFragment;
import com.monitor.health.ui.ProfileFragment;
import com.monitor.health.ui.SleepFragment;
import com.monitor.health.ui.StepsFragment;
import com.monitor.health.ui.TemperatureFragment;
import com.monitor.health.ui.VideoFragment;
import com.monitor.health.ui.WeightFragment;
import com.monitor.health.ui.BloodGlucoseFragment;

public class ScreenSlidePagerAdapter extends FragmentStateAdapter {

    private final java.util.List<PageType> pages = new java.util.ArrayList<>();

    public ScreenSlidePagerAdapter(@NonNull FragmentActivity fa) {
        super(fa);
    }

    public void setPages(java.util.List<PageType> newPages) {
        pages.clear();
        if (newPages != null) pages.addAll(newPages);
        notifyDataSetChanged();
    }

    public int indexOf(PageType type) {
        return pages.indexOf(type);
    }

    @NonNull
    @Override
    public Fragment createFragment(int position) {
        PageType type = pages.get(position);

        switch (type) {
            case BLOOD_GLUCOSE: return new BloodGlucoseFragment();
            case BLOOD_PRESSURE: return new BloodPressureFragment();
            case WEIGHT: return new WeightFragment();
            case BLOOD_OXYGEN: return new BloodOxygenFragment();
            case ECG: return new ElectrocardiogramFragment();
            case TEMPERATURE: return new TemperatureFragment();
            case HEART_RATE: return new HeartRateFragment();
           // case OXYGEN: return new OxygenFragment();
            case STEPS: return new StepsFragment();
           // case BLOOD_PRESSURE_NATIVE: return new BloodPressureNativeFragment();
            case SLEEP: return new SleepFragment();
            case PROFILE: return new ProfileFragment();
            default: return new ProfileFragment();
        }
    }

    @Override
    public int getItemCount() {
        return pages.size();
    }

    // âœ… IMPORTANT for dynamic updates:
    @Override
    public long getItemId(int position) {
        return pages.get(position).name().hashCode();
    }

    @Override
    public boolean containsItem(long itemId) {
        for (PageType t : pages) {
            if (t.name().hashCode() == itemId) return true;
        }
        return false;
    }
}

