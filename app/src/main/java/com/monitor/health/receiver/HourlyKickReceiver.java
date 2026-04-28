package com.monitor.health.receiver;

import android.annotation.SuppressLint;
import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Build;
import android.os.SystemClock;
import android.util.Log;

import androidx.core.content.ContextCompat;

import com.monitor.health.Constant;
import com.monitor.health.services.TestService;

/**
 * Periodic kick receiver that starts TestService, then re-schedules itself
 * using a dynamic interval (persisted in SharedPreferences).
 * Supports units: minutes, hours, seconds.
 */
public class HourlyKickReceiver extends BroadcastReceiver {

    private static final int REQ_CODE = 20201;

    // ---- Persistence ----
    private static final String PREFS_NAME = "freq_prefs";
    private static final String KEY_INTERVAL_MS = "interval_ms";

    // Safe default = 1 hour (used until server provides a value)
    private static final long DEFAULT_INTERVAL_MS = 60L * 60L * 1000L;

    @SuppressLint({"ObsoleteSdkInt", "UnsafeProtectedBroadcastReceiver"})
    @Override
    public void onReceive(Context context, Intent intent) {
        if (intent == null) return;

        // 1) Re-arm FIRST so the next run is guaranteed even if service start throws.
        scheduleNext(context);

        // 2) Start your service (foreground on O+)
        Intent svc = new Intent(context, TestService.class);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            ContextCompat.startForegroundService(context, svc);
        } else {
            context.startService(svc);
        }
    }

    // ---------- Public API ----------

    /**
     * Returns true if the alarm PendingIntent already exists (alarm is still armed).
     * Returns false after a reboot (system clears all PendingIntents) or first run.
     */
    public static boolean isAlarmSet(Context context) {
        Intent i = new Intent(context, HourlyKickReceiver.class).setAction(Constant.ACTION_KICK);
        PendingIntent pi = PendingIntent.getBroadcast(context, REQ_CODE, i,
                PendingIntent.FLAG_NO_CREATE | PendingIntent.FLAG_IMMUTABLE);
        return pi != null;
    }

    /**
     * Re-schedule using the last saved interval (or default if none).
     * Call from onReceive() and from BOOT_COMPLETED receiver.
     */
    @SuppressLint("MissingPermission")
    public static void scheduleNext(Context context) {
        long interval = readSavedInterval(context, DEFAULT_INTERVAL_MS);
        scheduleExact(context, interval);
    }

    /**
     * Re-schedule using a dynamic interval (millis) and persist it.
     * Call this after fetching settings from your server.
     */
    @SuppressLint("MissingPermission")
    public static void scheduleNext(Context context, long intervalMillis) {
        if (intervalMillis <= 0) intervalMillis = DEFAULT_INTERVAL_MS;
        saveInterval(context, intervalMillis);
        scheduleExact(context, intervalMillis);
    }

    /**
     * Schedule a one-time retry without overwriting the saved normal interval.
     * After the retry fires, onReceive() will restore the normal schedule automatically.
     */
    @SuppressLint("MissingPermission")
    public static void scheduleRetry(Context context, long retryMillis) {
        if (retryMillis <= 0) retryMillis = DEFAULT_INTERVAL_MS;
        scheduleExact(context, retryMillis);
    }

    /**
     * Convenience: pass server values like (1, "hr") or (2, "minutes") or (1, "min").
     * Converts to millis, persists, and schedules.
     */
    public static void setIntervalFromServer(Context context, int measurementInterval, String intervalUnit) {
        long intervalMs = convertToMillis(measurementInterval, intervalUnit);
        scheduleNext(context, intervalMs);
    }

    // ---------- Internals ----------

    @SuppressLint({"MissingPermission", "ScheduleExactAlarm"})
    private static void scheduleExact(Context context, long intervalMillis) {
        try {
            AlarmManager am = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
            Intent i = new Intent(context, HourlyKickReceiver.class).setAction(Constant.ACTION_KICK);

            PendingIntent pi = PendingIntent.getBroadcast(
                    context, REQ_CODE, i,
                    PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
            );

            long triggerAt = SystemClock.elapsedRealtime() + intervalMillis;
            am.setExactAndAllowWhileIdle(AlarmManager.ELAPSED_REALTIME_WAKEUP, triggerAt, pi);
        } catch (SecurityException e) {
            // SCHEDULE_EXACT_ALARM permission missing or revoked on Android 12+.
            // Fall back to inexact alarm so the chain is never silently broken.
            Log.w("HourlyKickReceiver", "Exact alarm denied â€” falling back to inexact: " + e.getMessage());
            try {
                AlarmManager am = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
                Intent i = new Intent(context, HourlyKickReceiver.class).setAction(Constant.ACTION_KICK);
                PendingIntent pi = PendingIntent.getBroadcast(
                        context, REQ_CODE, i,
                        PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
                );
                long triggerAt = SystemClock.elapsedRealtime() + intervalMillis;
                am.setAndAllowWhileIdle(AlarmManager.ELAPSED_REALTIME_WAKEUP, triggerAt, pi);
            } catch (Exception ex) {
                Log.e("HourlyKickReceiver", "Failed to schedule even inexact alarm: " + ex.getMessage());
            }
        }
    }

    private static void saveInterval(Context context, long intervalMs) {
        SharedPreferences sp = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        sp.edit().putLong(KEY_INTERVAL_MS, intervalMs).apply();
    }

    private static long readSavedInterval(Context context, long fallback) {
        SharedPreferences sp = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        return sp.getLong(KEY_INTERVAL_MS, fallback);
    }

    /**
     * Converts (interval, unit) â†’ milliseconds.
     * Supports: "min", "mins", "minute", "minutes", "hr", "hour", "hours",
     * "sec", "second", "seconds", "day", "days".
     * Defaults to hours if unit is unknown.
     */
    private static long convertToMillis(int interval, String unitRaw) {
        if (interval <= 0) return DEFAULT_INTERVAL_MS;
        String unit = unitRaw == null ? "" : unitRaw.trim().toLowerCase();

        switch (unit) {
            case "min":
            case "mins":
            case "minute":
            case "minutes":
                return interval * 60L * 1000L;

            case "hr":
            case "hour":
            case "hours":
                return interval * 60L * 60L * 1000L;

            case "day":
            case "days":
                return interval * 24L * 60L * 60L * 1000L;

            case "sec":
            case "second":
            case "seconds":
                return interval * 1000L;

            default:
                // Unknown unit â†’ treat as hours for safety
                return interval * 60L * 60L * 1000L;
        }
    }
}