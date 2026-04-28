package com.monitor.health.services;

import android.annotation.SuppressLint;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.media.AudioAttributes;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Build;
import android.os.PowerManager;
import android.util.Log;

import androidx.annotation.Nullable;
import androidx.core.app.NotificationCompat;

import com.monitor.health.R;
import com.monitor.health.receiver.CallActionReceiver;
import com.monitor.health.ui.IncomingCallActivity;
import com.google.firebase.messaging.FirebaseMessaging;
import com.google.firebase.messaging.FirebaseMessagingService;
import com.google.firebase.messaging.RemoteMessage;
import java.util.Map;

public class MyFirebaseMessagingService extends FirebaseMessagingService {

    private static final String TAG = "MyFirebaseMsgService";
    private static final String CALL_CHANNEL_ID = "incoming_call_channel";
    private static final int CALL_NOTIFICATION_ID = 1001;

    @Override
    public void onMessageReceived(RemoteMessage remoteMessage) {
        Log.d(TAG, "onMessageReceived called!");
        Log.d(TAG, "From: " + remoteMessage.getFrom());

        // Check if message contains data payload
        if (remoteMessage.getData().size() > 0) {
            Log.d(TAG, "Message data payload: " + remoteMessage.getData());
            handleCallNotification(remoteMessage.getData());
        }

        // Check if message contains notification payload
        if (remoteMessage.getNotification() != null) {
            Log.d(TAG, "Message Notification Body: " + remoteMessage.getNotification().getBody());

            // Create call-like notification
            Map<String, String> data = remoteMessage.getData();
            String callerName = data.get("caller_name");
            String callerNumber = data.get("caller_number");

            if (callerName == null) {
                callerName = remoteMessage.getNotification().getTitle();
            }
            if (callerNumber == null) {
                callerNumber = remoteMessage.getNotification().getBody();
            }

            Log.d(TAG, "Caller Name: " + callerName + ", Caller Number: " + callerNumber);
            String room  = nz(data.get("roomName"), data.get("room"));
            showIncomingCallNotification(callerName, callerNumber, room);
        }
    }

    public void subscribeToCallTopic() {
        FirebaseMessaging.getInstance().subscribeToTopic("user_1")
                .addOnCompleteListener(task -> {
                    String msg = "Subscribed to call topic";
                    if (!task.isSuccessful()) {
                        msg = "Subscribe to topic failed";
                    }
                    Log.d(TAG, msg);
                });
    }

    @Override
    public void onNewToken(String token) {
        Log.d(TAG, "Refreshed token: " + token);
        sendTokenToServer(token);
    }

    private void handleCallNotification(Map<String, String> data) {
        // Prefer CALLER fields only; do NOT prefer participant
        Log.d(TAG, "handle notification: 1");
        String caller = nz(
                data.get("caller"),        // e.g., "Care Provider, Edianon"
                data.get("caller_name"),   // alternate key
                data.get("title")          // very last fallback like "Video Call"
        );
        if (caller == null) caller = "Unknown";

        String token = data.get("token"); // JWT for video
        String room  = nz(data.get("roomName"), data.get("room"));

        Log.d(TAG, "caller=" + caller + ", room=" + room);
        showIncomingCallNotification(caller, token, room);
    }

    private static String nz(String... vals) {
        for (String v : vals) if (v != null && !v.trim().isEmpty()) return v.trim();
        return null;
    }
/*
    private void showIncomingCallNotification(String callerName, String token, @Nullable String roomName) {
        wakeUpScreen();

        // If you start the UI directly when foreground/unlocked, pass the extras too:
        if (isAppInForeground() && !isScreenLocked()) {
            launchFullScreenCallActivity(callerName, token, roomName);
            return;
        }

        createCallNotificationChannel();

        Intent fullScreenIntent = new Intent(this, IncomingCallActivity.class)
                .putExtra("caller_name", callerName)
                .putExtra("video_token", token)
                .putExtra("room_name", roomName)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK
                        | Intent.FLAG_ACTIVITY_CLEAR_TOP
                        | Intent.FLAG_ACTIVITY_SINGLE_TOP);

        PendingIntent fullScreenPendingIntent =
                PendingIntent.getActivity(this, 0, fullScreenIntent,
                        PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);

        NotificationCompat.Builder builder = new NotificationCompat.Builder(this, CALL_CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_call)
                .setCategory(NotificationCompat.CATEGORY_CALL)
                .setPriority(NotificationCompat.PRIORITY_MAX)
                .setOngoing(true)
                .setAutoCancel(true)
                .setFullScreenIntent(fullScreenPendingIntent, true)
                .setSilent(true) // ringtone plays in the activity
                .setVisibility(NotificationCompat.VISIBILITY_PUBLIC);

        NotificationManager nm = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        nm.notify(CALL_NOTIFICATION_ID, builder.build());
    }
    */

    private void launchFullScreenCallActivity(String callerName, String token, @Nullable String roomName) {
        Intent i = new Intent(this, IncomingCallActivity.class)
                .putExtra("caller_name", callerName)
                .putExtra("video_token", token)
                .putExtra("room_name", roomName)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK
                        | Intent.FLAG_ACTIVITY_CLEAR_TOP
                        | Intent.FLAG_ACTIVITY_SINGLE_TOP);
        startActivity(i);
    }

    private void showIncomingCallNotification(String callerName, String token, @Nullable String roomName) {
        // Instead of posting the notification here, start the foreground service
        Log.d(TAG, "handle notification: 2");
        Log.wtf("NOTIFICATION", "show the in coming call notification");
        Intent svc = new Intent(this, com.monitor.health.services.IncomingCallService.class)
                .putExtra(com.monitor.health.services.IncomingCallService.EXTRA_CALLER, callerName)
                .putExtra(com.monitor.health.services.IncomingCallService.EXTRA_TOKEN,  token)
                .putExtra(com.monitor.health.services.IncomingCallService.EXTRA_ROOM,   roomName);

        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
            startForegroundService(svc);
        } else {
            startService(svc);
        }
    }

    private void createCallNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(
                    CALL_CHANNEL_ID,
                    "Incoming Calls",
                    NotificationManager.IMPORTANCE_HIGH
            );

            channel.setDescription("Notifications for incoming calls");
            channel.enableLights(true);
            channel.setLightColor(Color.GREEN);
            channel.enableVibration(true);
            channel.setVibrationPattern(new long[]{0, 1000, 500, 1000});
            channel.setLockscreenVisibility(Notification.VISIBILITY_PUBLIC);

            // Set custom ringtone
            Uri ringtoneUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_RINGTONE);
            AudioAttributes audioAttributes = new AudioAttributes.Builder()
                    .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                    .setUsage(AudioAttributes.USAGE_NOTIFICATION_RINGTONE)
                    .build();
            channel.setSound(ringtoneUri, audioAttributes);

            NotificationManager manager = getSystemService(NotificationManager.class);
            manager.createNotificationChannel(channel);
            Log.d(TAG, "Notification channel created");
        }
    }

    private void wakeUpScreen() {
        PowerManager powerManager = (PowerManager) getSystemService(Context.POWER_SERVICE);
        PowerManager.WakeLock wakeLock = powerManager.newWakeLock(
                PowerManager.FULL_WAKE_LOCK | PowerManager.ACQUIRE_CAUSES_WAKEUP,
                "MyApp:IncomingCall"
        );
        wakeLock.acquire(10000); // 10 seconds
    }

    private void launchFullScreenCallActivity(String callerName, String callerNumber) {
        Intent fullScreenIntent = new Intent(this, IncomingCallActivity.class);
        fullScreenIntent.putExtra("caller_name", callerName);
        fullScreenIntent.putExtra("caller_number", callerNumber);
        fullScreenIntent.setFlags(
                Intent.FLAG_ACTIVITY_NEW_TASK |
                        Intent.FLAG_ACTIVITY_CLEAR_TOP |
                        Intent.FLAG_ACTIVITY_SINGLE_TOP
        );
        startActivity(fullScreenIntent);
    }

    private void sendTokenToServer(String token) {
        Log.d(TAG, "Sending token to server: " + token);
    }

    private boolean isScreenLocked() {
        android.app.KeyguardManager km =
                (android.app.KeyguardManager) getSystemService(KEYGUARD_SERVICE);
        return km != null && km.isKeyguardLocked();
    }

    private boolean isAppInForeground() {
        android.app.ActivityManager.RunningAppProcessInfo appProcessInfo =
                new android.app.ActivityManager.RunningAppProcessInfo();
        android.app.ActivityManager.getMyMemoryState(appProcessInfo);
        return appProcessInfo.importance
                == android.app.ActivityManager.RunningAppProcessInfo.IMPORTANCE_FOREGROUND;
    }
}