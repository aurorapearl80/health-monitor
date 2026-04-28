package com.monitor.health;

import android.os.Bundle;
import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.ViewModelProvider;
import androidx.viewpager2.widget.ViewPager2;

import android.widget.Toast;

import com.monitor.health.adapter.CarouselAdapter;
import com.monitor.health.adapter.ScreenSlidePagerAdapter;
import com.monitor.health.viewmodel.SharedDataViewModel;
import com.google.android.material.bottomnavigation.BottomNavigationView;

import java.util.Arrays;
import java.util.List;

public class DashActivity extends AppCompatActivity {


    private ViewPager2 viewPager;
    private CarouselAdapter adapter;

    private ScreenSlidePagerAdapter pagerAdapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_dashboard);

        viewPager = findViewById(R.id.viewPager);

        pagerAdapter = new ScreenSlidePagerAdapter(this);
        viewPager.setAdapter(pagerAdapter);

        SharedDataViewModel model = new ViewModelProvider(this).get(SharedDataViewModel.class);
        model.setHeartRate("76");
    }

}
