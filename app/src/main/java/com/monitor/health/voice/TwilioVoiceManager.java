package com.monitor.health.voice;

import android.content.Context;
import android.media.AudioManager;
import android.util.Log;

import androidx.annotation.MainThread;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.google.firebase.messaging.FirebaseMessaging;
import com.twilio.voice.Call;
import com.twilio.voice.CallException;
import com.twilio.voice.CallInvite;
import com.twilio.voice.CancelledCallInvite;
import com.twilio.voice.AcceptOptions;
import com.twilio.voice.RegistrationException;
import com.twilio.voice.RegistrationListener;
import com.twilio.voice.UnregistrationListener;
import com.twilio.voice.Voice;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Centralizes Twilio Voice registration, incoming invite, and active call handling.
 * This is a simple in-memory manager suitable for a single-process app.
 */
public class TwilioVoiceManager {
    private static final String TAG = "TwilioVoiceMgr";

    public interface IncomingInviteListener {
        @MainThread void onIncomingInvite(@NonNull CallInvite invite);
        @MainThread void onInviteCancelled(@NonNull CancelledCallInvite cancelled);
    }

    public interface CallStateListener {
        @MainThread void onConnected(@NonNull Call call);
        @MainThread void onDisconnected(@NonNull Call call, @Nullable CallException error);
        @MainThread void onReconnecting(@NonNull Call call, @NonNull CallException error);
        @MainThread void onReconnected(@NonNull Call call);
    }

    private static TwilioVoiceManager INSTANCE;

    public static synchronized TwilioVoiceManager getInstance(Context ctx) {
        if (INSTANCE == null) INSTANCE = new TwilioVoiceManager(ctx.getApplicationContext());
        return INSTANCE;
    }

    private final Context appContext;
    private final Map<String, CallInvite> pendingInvites = new HashMap<>();
    private final Map<String, Call> activeCalls = new HashMap<>();

    private final AtomicReference<String> lastFcmToken = new AtomicReference<>(null);
    private final AtomicReference<String> lastVoiceToken = new AtomicReference<>(null);

    private IncomingInviteListener incomingInviteListener;

    private TwilioVoiceManager(Context appContext) {
        this.appContext = appContext;
    }

    public void setIncomingInviteListener(@Nullable IncomingInviteListener l) {
        this.incomingInviteListener = l;
    }

    public void cacheInvite(@NonNull CallInvite invite) {
        pendingInvites.put(invite.getCallSid(), invite);
        IncomingInviteListener l = incomingInviteListener;
        if (l != null) l.onIncomingInvite(invite);
    }

    public void removeInvite(@NonNull String callSid) {
        pendingInvites.remove(callSid);
    }

    @Nullable
    public CallInvite getInvite(@NonNull String callSid) {
        return pendingInvites.get(callSid);
    }

    public void cacheActiveCall(@NonNull Call call) {
        activeCalls.put(call.getSid(), call);
    }

    public void removeActiveCall(@NonNull String callSid) {
        activeCalls.remove(callSid);
    }

    @Nullable
    public Call getActiveCall(@NonNull String callSid) { return activeCalls.get(callSid); }

    /**
     * Ensure the device is registered for VoIP push with Twilio Voice using the given Access Token.
     * It automatically obtains current FCM token.
     */
    public void registerForVoipPush(@NonNull String accessToken, @Nullable RegistrationListener listener) {
        lastVoiceToken.set(accessToken);
        FirebaseMessaging.getInstance().getToken().addOnCompleteListener(task -> {
            if (!task.isSuccessful()) {
                Log.e(TAG, "Failed to obtain FCM token for VoIP registration");
                return;
            }
            String fcmToken = task.getResult();
            lastFcmToken.set(fcmToken);
            Voice.register(accessToken, Voice.RegistrationChannel.FCM, fcmToken, listener);
        });
    }

    public void unregisterVoipPush(@Nullable UnregistrationListener listener) {
        String token = lastVoiceToken.get();
        String fcm = lastFcmToken.get();
        if (token == null || fcm == null) {
            Log.w(TAG, "Not registered: missing token or FCM");
            return;
        }
        Voice.unregister(token, Voice.RegistrationChannel.FCM, fcm, listener);
    }

    /** Accept an incoming CallInvite and start a Call. */
    @Nullable
    public Call acceptInvite(@NonNull CallInvite invite, @Nullable Call.Listener listener) {
        AcceptOptions options = new AcceptOptions.Builder().build();
        Call call = invite.accept(appContext, options, listener);
        if (call != null) cacheActiveCall(call);
        removeInvite(invite.getCallSid());
        return call;
    }

    /** Decline an incoming CallInvite. */
    public void rejectInvite(@NonNull CallInvite invite) {
        try {
            invite.reject(appContext);
        } catch (Exception e) {
            Log.w(TAG, "rejectInvite failed", e);
        } finally {
            removeInvite(invite.getCallSid());
        }
    }

    /** Simple helpers for audio route. */
    public void setSpeakerphoneOn(boolean on) {
        AudioManager am = (AudioManager) appContext.getSystemService(Context.AUDIO_SERVICE);
        if (am != null) am.setSpeakerphoneOn(on);
    }

    public Map<String, CallInvite> getPendingInvites() {
        return Collections.unmodifiableMap(pendingInvites);
    }
}
