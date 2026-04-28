package com.monitor.health.services;

import android.app.*;
import android.content.*;
import android.os.*;
import android.util.Log;
import androidx.core.app.NotificationCompat;
import com.monitor.health.R;
import com.monitor.health.receiver.CallActionReceiver;
import com.monitor.health.ui.IncomingCallActivity;

public class CallFgService extends Service {
    public static final String EXTRA_CALLER = "caller_name";
    public static final String EXTRA_TOKEN  = "video_token";
    public static final String EXTRA_ROOM   = "room_name";

    public static final String ACTION_ANSWER  = "com.monitor.health.ACTION_ANSWER";
    public static final String ACTION_DECLINE = "com.monitor.health.ACTION_DECLINE";

    private static final String CHANNEL_ID = "incoming_call_channel";
    private static final int NOTIF_ID = 1001;

    @Override public int onStartCommand(Intent intent, int flags, int startId) {
        String caller = intent.getStringExtra(EXTRA_CALLER);
        String token  = intent.getStringExtra(EXTRA_TOKEN);
        String room   = intent.getStringExtra(EXTRA_ROOM);

        createChannelIfNeeded();

        // Full-screen intent (will be ignored on this watch when screen is off, but harmless)
        Intent fsIntent = new Intent(this, IncomingCallActivity.class)
                .putExtra("caller_name", caller)
                .putExtra("video_token", token)
                .putExtra("room_name", room)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK
                        | Intent.FLAG_ACTIVITY_CLEAR_TOP
                        | Intent.FLAG_ACTIVITY_SINGLE_TOP);

        PendingIntent fsPi = PendingIntent.getActivity(
                this, 0, fsIntent, PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);

        // Actions (BroadcastReceiver below will start the activity on user tap)
        Intent ans = new Intent(this, CallActionReceiver.class).setAction(ACTION_ANSWER)
                .putExtra(EXTRA_CALLER, caller).putExtra(EXTRA_TOKEN, token).putExtra(EXTRA_ROOM, room);
        PendingIntent ansPi = PendingIntent.getBroadcast(this, 1, ans,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);

        Intent dec = new Intent(this, CallActionReceiver.class).setAction(ACTION_DECLINE);
        PendingIntent decPi = PendingIntent.getBroadcast(this, 2, dec,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);

        Notification n = new NotificationCompat.Builder(this, CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_call)
                .setContentTitle("Incoming call")
                .setContentText(caller != null ? caller : "Unknown")
                .setCategory(NotificationCompat.CATEGORY_CALL)
                .setPriority(NotificationCompat.PRIORITY_HIGH) // Heads-up on 8.1
                .setDefaults(NotificationCompat.DEFAULT_ALL)   // sound/vibrate/LED
                .setOngoing(true)
                .setAutoCancel(false)
                .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
                .setFullScreenIntent(fsPi, true)               // ignored when blocked, fine otherwise
                .addAction(new NotificationCompat.Action(0, "Answer", ansPi))
                .addAction(new NotificationCompat.Action(0, "Decline", decPi))
                .build();

        startForeground(NOTIF_ID, n);

        // If the screen is already on, try to bring UI now (allowed on many vendor builds)
        try {
            PowerManager pm = (PowerManager) getSystemService(Context.POWER_SERVICE);
            boolean interactive = (pm != null && pm.isInteractive());
            if (interactive) startActivity(fsIntent);
        } catch (Throwable t) {
            Log.w("CallFgService", "startActivity failed (expected on this firmware): " + t.getMessage());
        }

        return START_NOT_STICKY;
    }

    @Override public IBinder onBind(Intent intent) { return null; }

    private void createChannelIfNeeded() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel c = new NotificationChannel(
                    CHANNEL_ID, "Incoming Calls", NotificationManager.IMPORTANCE_HIGH);
            c.setLockscreenVisibility(Notification.VISIBILITY_PUBLIC);
            c.enableVibration(true);
            c.setDescription("Call alerts");
            getSystemService(NotificationManager.class).createNotificationChannel(c);
        }
    }
}
