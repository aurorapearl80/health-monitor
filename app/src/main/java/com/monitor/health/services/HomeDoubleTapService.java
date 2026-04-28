package com.monitor.health.services;

import android.annotation.SuppressLint;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Build;
import android.os.IBinder;
import android.os.SystemClock;
import android.util.Log;
import android.widget.Toast;

import androidx.core.app.NotificationCompat;

public class HomeDoubleTapService extends Service {
    private static final String CHANNEL_ID = "home_tap";
    private static final int NOTIF_ID = 1001;
    private static final long DOUBLE_TAP_MS = 350L;

    private boolean foregroundStarted = false;
    private long lastTapUptime = 0L;

    private final BroadcastReceiver sysDialogs = new BroadcastReceiver() {
        @Override public void onReceive(Context c, Intent i) {
            if (!Intent.ACTION_CLOSE_SYSTEM_DIALOGS.equals(i.getAction())) return;
            String reason = i.getStringExtra("reason"); // may be null
            if (reason == null || "homekey".equals(reason) || "recentapps".equals(reason)) {
                long now = SystemClock.uptimeMillis();
                if (now - lastTapUptime <= DOUBLE_TAP_MS) {
                    onHomeDoubleTap();
                    lastTapUptime = 0L;
                } else {
                    lastTapUptime = now;
                }
            }
        }
    };

    @SuppressLint("UnspecifiedRegisterReceiverFlag")
    @Override public void onCreate() {
        super.onCreate();
        createChannelIfNeeded();                       // 1) channel first
        goForegroundIfNeeded();                        // 2) immediately foreground
        registerReceiver(sysDialogs,                   // 4) register after foreground
                new IntentFilter(Intent.ACTION_CLOSE_SYSTEM_DIALOGS));
    }

    @Override public int onStartCommand(Intent intent, int flags, int startId) {
        goForegroundIfNeeded();                        // defensive: if onCreate got skipped/failed
        return START_STICKY;
    }

    private void goForegroundIfNeeded() {
        if (foregroundStarted) return;
        Notification n = new NotificationCompat.Builder(this, CHANNEL_ID)
                .setSmallIcon(android.R.drawable.stat_notify_more)
                .setContentTitle("Listening for Home double-tap")
                .setOngoing(true)
                .setPriority(NotificationCompat.PRIORITY_MIN)
                .build();
        try {
            startForeground(NOTIF_ID, n);
            foregroundStarted = true;
        } catch (Throwable t) {
            // If this throws, youâ€™ll still get the Oreo ANR. Log it to find the real cause.
            Log.e("HomeDT", "startForeground failed", t);
        }
    }

    private void createChannelIfNeeded() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel ch = new NotificationChannel(
                    CHANNEL_ID, "Home Double Tap", NotificationManager.IMPORTANCE_MIN);
            ch.setShowBadge(false);
            NotificationManager nm = getSystemService(NotificationManager.class);
            if (nm != null) nm.createNotificationChannel(ch);
        }
    }

    private void onHomeDoubleTap() {
        Toast.makeText(this, "Double tap detected!", Toast.LENGTH_SHORT).show();
        // TODO: your action here
    }

    @Override public void onDestroy() {
        try { unregisterReceiver(sysDialogs); } catch (Exception ignore) {}
        stopForeground(true);
        super.onDestroy();
    }

    @Override public IBinder onBind(Intent intent) { return null; }
}
