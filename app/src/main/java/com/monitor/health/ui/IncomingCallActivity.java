package com.monitor.health.ui;


import android.annotation.SuppressLint;
import android.content.Intent;
import android.media.Ringtone;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.view.View;
import android.view.WindowManager;
import android.widget.ImageButton;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;

import com.monitor.health.R;
import com.monitor.health.receiver.CallActionReceiver;

public class IncomingCallActivity extends AppCompatActivity {

    private Ringtone ringtone;
    private String callerName;
    private String callerNumber;
    private String token;
    private String roomName;

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
        token      = intent.getStringExtra("video_token");  // JWT
        roomName   = intent.getStringExtra("room_name");    // optional

        TextView callerNameTV = findViewById(R.id.caller_name);
        //TextView callerNumberTV = findViewById(R.id.caller_number);

        callerNameTV.setText(callerName != null ? callerName : "Unknown");
        //callerNumberTV.setText(roomName != null ? roomName : ""); // or hide this field

        playRingtone();

        findViewById(R.id.accept_button).setOnClickListener(v -> acceptCall());
        findViewById(R.id.decline_button).setOnClickListener(v -> declineCall());
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

        //Intent callIntent = new Intent(this, CallActivity.class);
        Intent callIntent = new Intent(this, VideoActivity.class);
        callIntent.putExtra("caller_name", callerName);
        callIntent.putExtra("video_token", token);
        callIntent.putExtra("room_name", roomName);
        startActivity(callIntent);
        finish();
    }

    private void declineCall() {
        stopRingtone();
        dismissNotification();
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