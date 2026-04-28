package com.monitor.health.voice;

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
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.core.app.NotificationCompat;

import com.monitor.health.R;
import com.monitor.health.ui.IncomingVoiceActivity;
import com.google.firebase.messaging.FirebaseMessagingService;
import com.google.firebase.messaging.RemoteMessage;
import com.twilio.voice.CallException;
import com.twilio.voice.CallInvite;
import com.twilio.voice.CancelledCallInvite;
import com.twilio.voice.MessageListener;
import com.twilio.voice.Voice;

import java.util.Map;

/**
 * Dedicated FCM service for Twilio Voice (VoIP) push handling.
 * It forwards the data payload to Twilio SDK which yields CallInvite / CancelledCallInvite.
 */
public class VoiceFirebaseMessagingService extends FirebaseMessagingService {
    private static final String TAG = "VoiceFcmService";
    private static final String CHANNEL_ID = "voice_calls";
    private static final int NOTIFICATION_ID = 2001;

    @Override
    public void onMessageReceived(@NonNull RemoteMessage remoteMessage) {
        Map<String, String> data = remoteMessage.getData();
        if (data == null || data.isEmpty()) {
            Log.d(TAG, "No data payload; ignoring");
            return;
        }
        Log.d(TAG, "Voice push received: " + data);

        // If your server includes a Voice Access Token, register this device so Twilio can deliver invites
        String voiceToken = data.get("voice_token");
        if (voiceToken != null && !voiceToken.isEmpty()) {
            TwilioVoiceManager.getInstance(getApplicationContext())
                    .registerForVoipPush(voiceToken, new com.twilio.voice.RegistrationListener() {
                        @Override public void onRegistered(String accessToken, String fcmToken) {
                            Log.d(TAG, "Twilio Voice registered");
                        }
                        @Override public void onError(@NonNull com.twilio.voice.RegistrationException e, String accessToken, String fcmToken) {
                            Log.e(TAG, "Twilio Voice registration error: " + e.getMessage());
                        }
                    });
        }

        // Let Twilio SDK parse and deliver callbacks
        Voice.handleMessage(this, data, new MessageListener() {
            @Override
            public void onCallInvite(@NonNull CallInvite callInvite) {
                Log.d(TAG, "onCallInvite: " + callInvite.getFrom());
                TwilioVoiceManager.getInstance(getApplicationContext()).cacheInvite(callInvite);
                showIncomingNotification(callInvite);
                launchIncomingUi(callInvite);
            }

            @Override
            public void onCancelledCallInvite(@NonNull CancelledCallInvite cancelledCallInvite, @NonNull com.twilio.voice.CallException callException) {
                try {
                    String sid = cancelledCallInvite.getCallSid();
                    Log.d(TAG, "onCancelledCallInvite for: " + sid + ", reason=" + (callException != null ? callException.getMessage() : "null"));
                    if (sid != null) {
                        TwilioVoiceManager.getInstance(getApplicationContext()).removeInvite(sid);
                    }
                } catch (Throwable t) {
                    Log.w(TAG, "Error handling cancelled invite", t);
                }
                cancelIncomingNotification();
            }
        });
    }

    private void showIncomingNotification(@NonNull CallInvite invite) {
        createChannelIfNeeded();

        Intent fullScreenIntent = new Intent(this, com.monitor.health.ui.IncomingVoiceActivity.class)
                .putExtra("VOICE_CALL_SID", invite.getCallSid())
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_SINGLE_TOP);
        PendingIntent fullScreenPendingIntent = PendingIntent.getActivity(
                this, 0, fullScreenIntent,
                PendingIntent.FLAG_UPDATE_CURRENT | (Build.VERSION.SDK_INT >= 23 ? PendingIntent.FLAG_IMMUTABLE : 0)
        );

        Uri sound = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_RINGTONE);
        NotificationCompat.Builder builder = new NotificationCompat.Builder(this, CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_call)
                .setContentTitle(getString(R.string.app_name))
                .setContentText("Incoming voice call from " + invite.getFrom())
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setCategory(NotificationCompat.CATEGORY_CALL)
                .setAutoCancel(true)
                .setOngoing(true)
                .setSound(sound)
                .setFullScreenIntent(fullScreenPendingIntent, true);

        Notification notification = builder.build();
        NotificationManager nm = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        nm.notify(NOTIFICATION_ID, notification);
    }

    private void cancelIncomingNotification() {
        NotificationManager nm = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        nm.cancel(NOTIFICATION_ID);
    }

    private void createChannelIfNeeded() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(CHANNEL_ID, "Voice Calls", NotificationManager.IMPORTANCE_HIGH);
            channel.setDescription("Incoming voice calls");
            channel.enableLights(true);
            channel.setLightColor(Color.GREEN);
            channel.enableVibration(true);
            Uri sound = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_RINGTONE);
            channel.setSound(sound, new AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_NOTIFICATION_RINGTONE)
                    .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                    .build());
            NotificationManager nm = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
            nm.createNotificationChannel(channel);
        }
    }

    private void launchIncomingUi(@NonNull CallInvite invite) {
        Intent intent = new Intent(this, com.monitor.health.ui.IncomingVoiceActivity.class)
                .putExtra("VOICE_CALL_SID", invite.getCallSid())
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_SINGLE_TOP);
        startActivity(intent);
    }
}
