package com.monitor.health.ui;

import android.os.Bundle;
import android.util.Log;
import android.widget.Button;
import android.widget.TextView;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.monitor.health.R;
import com.monitor.health.voice.TwilioVoiceManager;
import com.twilio.voice.Call;
import com.twilio.voice.CallException;

public class VoiceCallActivity extends AppCompatActivity {
    private static final String TAG = "VoiceCallActivity";

    private Call call;
    private boolean muted = false;
    private boolean speakerOn = false;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_voice_call);

        String callSid = getIntent().getStringExtra("VOICE_ACTIVE_SID");
        call = TwilioVoiceManager.getInstance(getApplicationContext()).getActiveCall(callSid);

        TextView status = findViewById(R.id.voice_call_status);
        Button mute = findViewById(R.id.voice_mute_button);
        Button speaker = findViewById(R.id.voice_speaker_button);
        Button hangup = findViewById(R.id.voice_hangup_button);

        if (call != null) {
            status.setText("Connected: " + call.getSid());
        } else {
            status.setText("No active call");
        }

        mute.setOnClickListener(v -> toggleMute());
        speaker.setOnClickListener(v -> toggleSpeaker());
        hangup.setOnClickListener(v -> endCall());
    }

    private void toggleMute() {
        if (call == null) return;
        try {
            muted = !muted;
            call.mute(muted);
        } catch (Exception e) {
            Log.w(TAG, "mute toggle failed", e);
        }
    }

    private void toggleSpeaker() {
        speakerOn = !speakerOn;
        TwilioVoiceManager.getInstance(getApplicationContext()).setSpeakerphoneOn(speakerOn);
    }

    private void endCall() {
        if (call != null) {
            try {
                call.disconnect();
            } catch (Exception e) {
                Log.w(TAG, "disconnect failed", e);
            }
            TwilioVoiceManager.getInstance(getApplicationContext()).removeActiveCall(call.getSid());
        }
        finish();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
    }
}