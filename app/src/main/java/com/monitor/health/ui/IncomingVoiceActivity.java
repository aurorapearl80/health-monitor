package com.monitor.health.ui;

import android.app.NotificationManager;
import android.content.Context;
import android.content.Intent;
import android.media.Ringtone;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.widget.Button;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.monitor.health.R;
import com.monitor.health.voice.TwilioVoiceManager;
import com.twilio.voice.Call;
import com.twilio.voice.CallException;
import com.twilio.voice.CallInvite;

public class IncomingVoiceActivity extends AppCompatActivity {
    private static final String TAG = "IncomingVoice";
    private static final int NOTIFICATION_ID = 2001;

    private Ringtone ringtone;
    private String callSid;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_incoming_voice);

        // Dismiss any ringing notification if present
        NotificationManager nm = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        nm.cancel(NOTIFICATION_ID);

        callSid = getIntent().getStringExtra("VOICE_CALL_SID");
        CallInvite invite = TwilioVoiceManager.getInstance(getApplicationContext()).getInvite(callSid);

        TextView title = findViewById(R.id.incoming_voice_title);
        title.setText(invite != null ? ("Incoming call from " + invite.getFrom()) : "Incoming voice call");

        Button accept = findViewById(R.id.voice_accept_button);
        Button decline = findViewById(R.id.voice_decline_button);

        playRingtone();

        accept.setOnClickListener(v -> acceptCall(invite));
        decline.setOnClickListener(v -> declineCall(invite));
    }

    private void playRingtone() {
        Uri uri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_RINGTONE);
        ringtone = RingtoneManager.getRingtone(this, uri);
        if (ringtone != null) ringtone.play();
    }

    private void stopRingtone() {
        if (ringtone != null && ringtone.isPlaying()) ringtone.stop();
    }

    private void acceptCall(@Nullable CallInvite invite) {
        stopRingtone();
        if (invite == null) {
            finish();
            return;
        }
        Call call = TwilioVoiceManager.getInstance(getApplicationContext()).acceptInvite(invite, new Call.Listener() {
            @Override
            public void onConnectFailure(@NonNull Call call, @NonNull CallException callException) {

            }

            @Override
            public void onRinging(@NonNull Call call) {

            }

            @Override public void onConnected(Call call) { Log.d(TAG, "Voice connected"); }
            @Override public void onDisconnected(Call call, @Nullable CallException e) { Log.d(TAG, "Voice disconnected"); }
            @Override public void onReconnecting(Call call, CallException callException) { }
            @Override public void onReconnected(Call call) { }
        });
        if (call != null) {
            Intent i = new Intent(this, VoiceCallActivity.class);
            i.putExtra("VOICE_ACTIVE_SID", call.getSid());
            startActivity(i);
        }
        finish();
    }

    private void declineCall(@Nullable CallInvite invite) {
        stopRingtone();
        if (invite != null) {
            TwilioVoiceManager.getInstance(getApplicationContext()).rejectInvite(invite);
        }
        finish();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        stopRingtone();
    }
}
