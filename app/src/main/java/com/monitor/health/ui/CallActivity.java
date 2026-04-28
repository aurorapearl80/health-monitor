package com.monitor.health.ui;

import android.os.Bundle;

import com.google.android.material.snackbar.Snackbar;

import androidx.appcompat.app.AppCompatActivity;

import android.util.Log;
import android.view.View;

import androidx.navigation.NavController;
import androidx.navigation.Navigation;
import androidx.navigation.ui.AppBarConfiguration;
import androidx.navigation.ui.NavigationUI;

import com.monitor.health.databinding.ActivityCallBinding;

import com.monitor.health.R;

import android.os.Bundle;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;

public class CallActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_call);

        String callerName = getIntent().getStringExtra("caller_name");

       // String callerName = getIntent().getStringExtra("caller_name");
        String token      = getIntent().getStringExtra("video_token");
        String roomName   = getIntent().getStringExtra("room_name");

        Log.wtf("TOKEN", token);


        TextView callerNameTV = findViewById(R.id.call_caller_name);
        callerNameTV.setText("In call with: " + (callerName != null ? callerName : "Unknown"));

        // Implement your call functionality here
    }
}