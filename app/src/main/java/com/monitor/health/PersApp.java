package com.monitor.health;

import android.app.Application;
import android.content.SharedPreferences;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.work.Configuration;

import com.monitor.health.receiver.HourlyKickReceiver;

public class PersApp  extends Application implements Configuration.Provider {

    @NonNull
    @Override
    public Configuration getWorkManagerConfiguration() {
        return new Configuration.Builder()
                .setMinimumLoggingLevel(android.util.Log.DEBUG) // Optional
                .build();
    }

    @Override
    public void onCreate() {
        super.onCreate();

        // Load the saved value
        SharedPreferences sp = getSharedPreferences("kick_prefs", MODE_PRIVATE);
        long savedIntervalMs = sp.getLong("saved_interval_ms", -1L);
        long targetIntervalMs = 1L * 60L * 60L * 1000L; // 1 hour in ms

        // Only update when the target interval changes; always re-arm so it
        // survives device reboots (AlarmManager is cleared by the OS on reboot).
        if (savedIntervalMs != targetIntervalMs) {
            Log.d("PersApp", "Scheduling HourlyKickReceiver for every 1 hour");
            HourlyKickReceiver.setIntervalFromServer(this, 1, "hour");
            sp.edit().putLong("saved_interval_ms", targetIntervalMs).apply();
        } else {
            // Re-arm only if the alarm was cleared (reboot or first boot after install).
            // Skipping re-arm when the alarm is already set prevents each process restart
            // from resetting the 1-hour countdown to zero (which would cause it to never fire).
            if (!HourlyKickReceiver.isAlarmSet(this)) {
                Log.d("PersApp", "Re-arming HourlyKickReceiver with 1-hour interval (alarm was cleared)");
                HourlyKickReceiver.scheduleNext(this, targetIntervalMs);
            } else {
                Log.d("PersApp", "HourlyKickReceiver alarm already set, skipping re-arm");
            }
        }
    }
}