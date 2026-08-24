package com.monitor.health.ui;


import android.annotation.SuppressLint;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.media.Ringtone;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.WindowManager;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;

import com.monitor.health.CallInvitationApi;
import com.monitor.health.R;
import com.monitor.health.receiver.CallActionReceiver;
import com.monitor.health.services.IncomingCallService;

import java.net.URL;

public class IncomingCallActivity extends AppCompatActivity {

    /** Set when launched from the notification's Accept button. */
    public static final String EXTRA_AUTO_ACCEPT = "auto_accept";

    private Ringtone ringtone;
    private String callerName;
    private String callerNumber;
    private String token;
    private String roomName;
    private int callInvitationId;
    private String callerAvatarUrl;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        getWindow().addFlags(
                android.view.WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED
                        | android.view.WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON
                        | android.view.WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON
        );

        // Modern APIs to appear over the lock screen and turn the screen on
//        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O_MR1) {
//            setShowWhenLocked(true);
//            setTurnScreenOn(true);
//            // Optionally dismiss the keyguard to allow interaction
//            android.app.KeyguardManager km = (android.app.KeyguardManager) getSystemService(KEYGUARD_SERVICE);
//            if (km != null) km.requestDismissKeyguard(this, null);
//        } else {
//            getWindow().addFlags(
//                    WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED
//                            | WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON
//                            | WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON
//            );
//        }

        setContentView(R.layout.activity_incoming_call);

        // Cancel the posted full-screen notification so it doesnâ€™t linger in the shade
        android.app.NotificationManager nm =
                (android.app.NotificationManager) getSystemService(NOTIFICATION_SERVICE);
        nm.cancel(1001); // CALL_NOTIFICATION_ID

//        // Show over lock screen
//        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O_MR1) {
//            setShowWhenLocked(true);
//            setTurnScreenOn(true);
//        } else {
//            getWindow().addFlags(
//                    WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED |
//                            WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON |
//                            WindowManager.LayoutParams.FLAG_DISMISS_KEYGUARD
//            );
//        }

        Intent intent = getIntent();
        callerName = intent.getStringExtra("caller_name");  // from participant
        token      = intent.getStringExtra("video_token");  // JWT (Twilio path only)
        roomName   = intent.getStringExtra("room_name");    // optional
        callInvitationId = intent.getIntExtra(IncomingCallService.EXTRA_CALL_INVITATION_ID, 0);
        callerAvatarUrl = intent.getStringExtra(IncomingCallService.EXTRA_CALLER_AVATAR_URL);

        if (intent.getBooleanExtra(EXTRA_AUTO_ACCEPT, false)) {
            // Launched from the notification's Accept button — skip the ring
            // UI entirely and jump straight into the call.
            acceptCall();
            return;
        }

        TextView callerNameTV = findViewById(R.id.caller_name);
        //TextView callerNumberTV = findViewById(R.id.caller_number);

        callerNameTV.setText(callerName != null ? callerName : "Unknown");
        //callerNumberTV.setText(roomName != null ? roomName : ""); // or hide this field

        loadAvatar();
        playRingtone();

        findViewById(R.id.accept_button).setOnClickListener(v -> acceptCall());
        findViewById(R.id.decline_button).setOnClickListener(v -> declineCall());
    }


    private void loadAvatar() {
        if (callerAvatarUrl == null || callerAvatarUrl.isEmpty()) return;
        new Thread(() -> {
            try (java.io.InputStream stream = new URL(callerAvatarUrl).openStream()) {
                Bitmap bitmap = BitmapFactory.decodeStream(stream);
                if (bitmap != null) {
                    runOnUiThread(() -> {
                        ImageView avatar = findViewById(R.id.caller_avatar);
                        if (avatar != null) avatar.setImageBitmap(bitmap);
                    });
                }
            } catch (Exception e) {
                Log.w("IncomingCallActivity", "avatar load failed: " + e.getMessage());
            }
        }).start();
    }

    private void playRingtone() {
        Uri ringtoneUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_RINGTONE);
        ringtone = RingtoneManager.getRingtone(this, ringtoneUri);
        if (ringtone != null) {
            ringtone.play();
        }
    }

    private void stopRingtone() {
        if (ringtone != null && ringtone.isPlaying()) {
            ringtone.stop();
        }
    }

    private void acceptCall() {
        stopRingtone();
        dismissNotification();

        if (callInvitationId > 0) {
            // New LiveKit flow (call-invitations raised from the admin/web side) —
            // fetches its own join token, so no video_token extra needed.
            CallInvitationApi.respond(this, callInvitationId, "accepted");
            Intent callIntent = new Intent(this, LiveKitCallActivity.class);
            callIntent.putExtra("caller_name", callerName);
            startActivity(callIntent);
        } else {
            // Legacy Twilio path (raw FCM push, no call_invitation_id).
            Intent callIntent = new Intent(this, VideoActivity.class);
            callIntent.putExtra("caller_name", callerName);
            callIntent.putExtra("video_token", token);
            callIntent.putExtra("room_name", roomName);
            startActivity(callIntent);
        }
        finish();
    }

    private void declineCall() {
        stopRingtone();
        dismissNotification();
        if (callInvitationId > 0) {
            CallInvitationApi.respond(this, callInvitationId, "declined");
        }
        finish();
    }

    private void dismissNotification() {
        Intent intent = new Intent(this, CallActionReceiver.class);
        intent.setAction("DISMISS_CALL");
        sendBroadcast(intent);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        stopRingtone();
    }


}