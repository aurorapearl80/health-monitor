package com.monitor.health.services;

import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.util.Log;

import com.onesignal.OneSignal;
import com.onesignal.notifications.INotificationClickEvent;
import com.onesignal.notifications.INotificationClickListener;
import com.onesignal.notifications.INotificationLifecycleListener;
import com.onesignal.notifications.INotificationWillDisplayEvent;

import org.json.JSONObject;

/**
 * Bridges OneSignal's `type: incoming_call` push (see CallRingNotifier on the
 * backend) into the same {@link IncomingCallService} foreground-service /
 * full-screen-Activity flow that used to be driven by raw FCM messages in
 * MyFirebaseMessagingService — that class is left in place but is now
 * dormant, since the backend no longer sends this app raw FCM pushes.
 *
 * NOTE: written against the OneSignal Android SDK v5 public API but not
 * compiled locally — verify import paths/method names against the SDK
 * version pinned in app/build.gradle before shipping.
 */
public final class OneSignalCallListener {

    private static final String TAG = "OneSignalCallListener";

    private OneSignalCallListener() {}

    /** Call once from PersApp.onCreate(), after OneSignal.initWithContext(...). */
    public static void register(Context appContext) {
        OneSignal.getNotifications().addForegroundLifecycleListener(new INotificationLifecycleListener() {
            @Override
            public void onWillDisplay(INotificationWillDisplayEvent event) {
                JSONObject data = event.getNotification().getAdditionalData();
                if (isIncomingCall(data)) {
                    event.preventDefault();
                    startIncomingCallService(appContext, data);
                }
            }
        });

        OneSignal.getNotifications().addClickListener(new INotificationClickListener() {
            @Override
            public void onClick(INotificationClickEvent event) {
                JSONObject data = event.getNotification().getAdditionalData();
                if (isIncomingCall(data)) {
                    startIncomingCallService(appContext, data);
                }
            }
        });
    }

    private static boolean isIncomingCall(JSONObject data) {
        return data != null && "incoming_call".equals(data.optString("type"));
    }

    private static void startIncomingCallService(Context context, JSONObject data) {
        String callerName = data.optString("caller_name", "Unknown");
        String roomName = data.optString("room_name", null);
        String avatarUrl = data.optString("caller_avatar_url", null);
        int callInvitationId = data.optInt("call_invitation_id", 0);

        Log.d(TAG, "Incoming call push: caller=" + callerName + " room=" + roomName + " invitation=" + callInvitationId);

        Intent svc = new Intent(context, IncomingCallService.class)
                .putExtra(IncomingCallService.EXTRA_CALLER, callerName)
                .putExtra(IncomingCallService.EXTRA_ROOM, roomName)
                .putExtra(IncomingCallService.EXTRA_CALLER_AVATAR_URL, avatarUrl)
                .putExtra(IncomingCallService.EXTRA_CALL_INVITATION_ID, callInvitationId);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            context.startForegroundService(svc);
        } else {
            context.startService(svc);
        }
    }
}
