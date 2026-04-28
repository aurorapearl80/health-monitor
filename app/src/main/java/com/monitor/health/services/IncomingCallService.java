package com.monitor.health.services;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.os.IBinder;
import android.os.PowerManager;
import android.util.Log;
import android.widget.RemoteViews;

import androidx.annotation.Nullable;
import androidx.core.app.NotificationCompat;

import com.monitor.health.R;
import com.monitor.health.receiver.CallActionReceiver;
import com.monitor.health.ui.IncomingCallActivity;

public class IncomingCallService extends Service {

    public static final String EXTRA_CALLER = "caller_name";
    public static final String EXTRA_TOKEN  = "video_token";
    public static final String EXTRA_ROOM   = "room_name";

    public static final String CHANNEL_ID_INCOMING = "incoming_call_channel";
    public static final int    NOTIF_ID_INCOMING   = 1001;

    public static final String ACTION_ANSWER  = "com.monitor.health.ACTION_ANSWER";
    public static final String ACTION_DECLINE = "com.monitor.health.ACTION_DECLINE";

    @Override
    public int onStartCommand(@Nullable Intent intent, int flags, int startId) {
        String caller = intent != null ? intent.getStringExtra(EXTRA_CALLER) : "Unknown";
        String token  = intent != null ? intent.getStringExtra(EXTRA_TOKEN)  : null;
        String room   = intent != null ? intent.getStringExtra(EXTRA_ROOM)   : null;
        Log.wtf("NOTIFICATION", "RUNNING HERE");

        createIncomingChannel();

        // Full-screen intent (OEM may ignore while screen off, but harmless)
        Intent fsIntent = new Intent(this, IncomingCallActivity.class)
                .putExtra(EXTRA_CALLER, caller)
                .putExtra(EXTRA_TOKEN, token)
                .putExtra(EXTRA_ROOM, room)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK
                        | Intent.FLAG_ACTIVITY_CLEAR_TOP
                        | Intent.FLAG_ACTIVITY_SINGLE_TOP);

        PendingIntent fsPi = PendingIntent.getActivity(
                this, 0, fsIntent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
        );

        // Actions -> BroadcastReceiver
        Intent answerI = new Intent(this, CallActionReceiver.class)
                .setAction(ACTION_ANSWER)
                .putExtra(EXTRA_CALLER, caller)
                .putExtra(EXTRA_TOKEN, token)
                .putExtra(EXTRA_ROOM, room);

        PendingIntent answerPi = PendingIntent.getBroadcast(
                this, 1, answerI,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
        );

        Intent declineI = new Intent(this, CallActionReceiver.class)
                .setAction(ACTION_DECLINE);

        PendingIntent declinePi = PendingIntent.getBroadcast(
                this, 2, declineI,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
        );

        // ---- Custom layout with icon buttons ----
        RemoteViews rv = new RemoteViews(getPackageName(), R.layout.notification_incoming_call);
        // e.g. "Incoming call from Care Provider"
        String title = getString(R.string.incoming_call_from, caller); // define in strings.xml
        rv.setTextViewText(R.id.tv_title, title);
        rv.setOnClickPendingIntent(R.id.btn_answer, answerPi);
        rv.setOnClickPendingIntent(R.id.btn_decline, declinePi);

        Notification n = new NotificationCompat.Builder(this, CHANNEL_ID_INCOMING)
                .setSmallIcon(R.drawable.ic_call) // status bar icon
                .setCategory(NotificationCompat.CATEGORY_CALL)
                .setPriority(NotificationCompat.PRIORITY_HIGH)   // heads-up on 8.1
                .setDefaults(NotificationCompat.DEFAULT_ALL)      // sound/vibrate/LED
                .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
                .setOngoing(true)
                .setAutoCancel(false)
                .setFullScreenIntent(fsPi, true)                  // ignored if OEM blocks FSI
                .setStyle(new NotificationCompat.DecoratedCustomViewStyle())
                .setCustomContentView(rv)
                .setCustomBigContentView(rv)
                .build();

        startForeground(NOTIF_ID_INCOMING, n);

        // If screen is already on, try to surface the UI immediately
        PowerManager pm = (PowerManager) getSystemService(Context.POWER_SERVICE);
        boolean interactive = pm != null && pm.isInteractive();
        if (interactive) {
            try { startActivity(fsIntent); } catch (Throwable ignored) { }
        }

        return START_NOT_STICKY;
    }

    @Nullable @Override
    public IBinder onBind(Intent intent) { return null; }

    private void createIncomingChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel chan = new NotificationChannel(
                    CHANNEL_ID_INCOMING, "Incoming Calls",
                    NotificationManager.IMPORTANCE_HIGH
            );
            chan.setDescription("Incoming call alerts");
            chan.setLockscreenVisibility(Notification.VISIBILITY_PUBLIC);
            chan.enableVibration(true);

            NotificationManager nm = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
            if (nm != null) nm.createNotificationChannel(chan);
        }
    }
}
