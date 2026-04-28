package com.monitor.health.ui;

import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager;
import android.hardware.camera2.CameraCharacteristics;
import android.hardware.camera2.CameraManager;
import android.os.Bundle;
import android.util.Log;
import android.widget.Button;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

import com.monitor.health.R;
import com.twilio.video.AudioOptions;
import com.twilio.video.Camera2Capturer;
import com.twilio.video.ConnectOptions;
import com.twilio.video.LocalAudioTrack;
import com.twilio.video.LocalParticipant;
import com.twilio.video.LocalVideoTrack;
import com.twilio.video.RemoteAudioTrackPublication;
import com.twilio.video.RemoteDataTrack;
import com.twilio.video.RemoteDataTrackPublication;
import com.twilio.video.RemoteParticipant;
import com.twilio.video.RemoteVideoTrack;
import com.twilio.video.RemoteVideoTrackPublication;
import com.twilio.video.Room;
import com.twilio.video.TwilioException;
import com.twilio.video.Video;
import com.twilio.video.VideoView;

import java.util.Collections;

public class VideoActivity extends AppCompatActivity {
    private static final String TAG = "TwilioVideoCall";

    // Hardcoded room name and participant name
    private static final String ROOM_NAME = "DefaultVideoRoom";
    private static final String PARTICIPANT_NAME = "User";

    // Token and room from intent
    private String accessToken;
    private String roomName;
    private String callerName;

    private VideoView localRenderer;
    private VideoView remoteRenderer;

    private LocalAudioTrack localAudioTrack;
    private LocalVideoTrack localVideoTrack;
    private Camera2Capturer cameraCapturer;
    private Room room;

    private ActivityResultLauncher<String[]> permissionLauncher;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_video);

        // Get token and other data from intent
        callerName = getIntent().getStringExtra("caller_name");
        accessToken = getIntent().getStringExtra("video_token");
        roomName = getIntent().getStringExtra("room_name");


        // Use default values if not provided
        if (roomName == null || roomName.isEmpty()) {
            roomName = ROOM_NAME;
        }

        Log.d(TAG, "Access Token: " + (accessToken != null ? "RECEIVED" : "NULL"));
        Log.d(TAG, "Room Name: " + roomName);
        Log.d(TAG, "Caller Name: " + callerName);

        // Initialize views
        localRenderer = findViewById(R.id.local_video);
        remoteRenderer = findViewById(R.id.remote_video);
        localRenderer.setMirror(true);

        // Setup permission launcher
        permissionLauncher = registerForActivityResult(
                new ActivityResultContracts.RequestMultiplePermissions(),
                result -> {
                    boolean micGranted = Boolean.TRUE.equals(result.get(Manifest.permission.RECORD_AUDIO));
                    boolean camGranted = Boolean.TRUE.equals(result.get(Manifest.permission.CAMERA));
                    if (micGranted) startLocalAudio();
                    if (camGranted) startLocalVideo();
                });

        // Validate token
        if (accessToken == null || accessToken.isEmpty()) {
            Log.e(TAG, "Access token is missing!");
            return;
        }

        // Start video call automatically
        startVideoCallAutomatically();

        findViewById(R.id.decline_button).setOnClickListener(v -> declineCall());

    }

    private void declineCall() {
        if (room != null) {
            room.disconnect();
            room = null;
        }
        cleanupTracks();
        finish();
    }

    private void startVideoCallAutomatically() {
        boolean mic = ContextCompat.checkSelfPermission(this,
                Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED;
        boolean cam = ContextCompat.checkSelfPermission(this,
                Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED;

        if (!mic || !cam) {
            permissionLauncher.launch(new String[]{
                    Manifest.permission.RECORD_AUDIO,
                    Manifest.permission.CAMERA
            });
        } else {
            startLocalAudio();
            startLocalVideo();
        }

        // Connect directly with the token received from intent
        connectToRoom(accessToken, roomName);
    }

    private void startLocalAudio() {
        if (localAudioTrack == null) {
            AudioOptions audioOptions = new AudioOptions.Builder().build();
            localAudioTrack = LocalAudioTrack.create(this, true, audioOptions, "mic");
        }
    }

    private void startLocalVideo() {
        if (localVideoTrack != null) return;
        try {
            String cameraId = getFrontCameraId();
            if (cameraId == null) {
                Log.w(TAG, "No camera found, continuing audio-only");
                return;
            }
            cameraCapturer = new Camera2Capturer(this, cameraId, null);
            localVideoTrack = LocalVideoTrack.create(this, true, cameraCapturer, "camera");
            localVideoTrack.addSink(localRenderer);
        } catch (Exception e) {
            Log.w(TAG, "Camera initialization failed, audio-only mode", e);
        }
    }

    @Nullable
    private String getFrontCameraId() throws Exception {
        CameraManager cm = (CameraManager) getSystemService(Context.CAMERA_SERVICE);
        for (String id : cm.getCameraIdList()) {
            Integer facing = cm.getCameraCharacteristics(id).get(CameraCharacteristics.LENS_FACING);
            if (facing != null && facing == CameraCharacteristics.LENS_FACING_FRONT) {
                return id;
            }
        }
        // Fallback to first camera if no explicit front camera
        String[] ids = cm.getCameraIdList();
        return ids.length > 0 ? ids[0] : null;
    }

    private void connectToRoom(String accessToken, String roomName) {
        ConnectOptions.Builder builder = new ConnectOptions.Builder(accessToken)
                .roomName(roomName);
        if (localAudioTrack != null)
            builder.audioTracks(Collections.singletonList(localAudioTrack));
        if (localVideoTrack != null)
            builder.videoTracks(Collections.singletonList(localVideoTrack));
        room = Video.connect(this, builder.build(), roomListener);
        Log.i(TAG, "Connecting to room: " + roomName);
    }

    // Room callbacks
    private final Room.Listener roomListener = new Room.Listener() {
        @Override
        public void onConnected(@NonNull Room room) {
            LocalParticipant lp = room.getLocalParticipant();
            Log.i(TAG, "Connected to " + room.getName() + " as " + (lp != null ? lp.getIdentity() : "unknown"));
            for (RemoteParticipant p : room.getRemoteParticipants()) {
                attachParticipant(p);
            }
        }

        @Override
        public void onConnectFailure(@NonNull Room room, @NonNull TwilioException e) {
            Log.e(TAG, "Connect failed: " + e.getMessage());
        }

        @Override
        public void onReconnecting(@NonNull Room room, @NonNull TwilioException twilioException) {
            Log.i(TAG, "Reconnecting to room...");
        }

        @Override
        public void onReconnected(@NonNull Room room) {
            Log.i(TAG, "Reconnected to room");
        }

        @Override
        public void onDisconnected(@NonNull Room room, @Nullable TwilioException e) {
            Log.i(TAG, "Disconnected from room");
            cleanupTracks();
        }

        @Override
        public void onParticipantConnected(@NonNull Room room, @NonNull RemoteParticipant participant) {
            Log.i(TAG, "Participant connected: " + participant.getIdentity());
            attachParticipant(participant);
        }

        @Override
        public void onParticipantDisconnected(@NonNull Room room, @NonNull RemoteParticipant participant) {
            Log.i(TAG, "Participant disconnected: " + participant.getIdentity());
        }

        @Override
        public void onRecordingStarted(@NonNull Room room) {
            Log.i(TAG, "Recording started");
        }

        @Override
        public void onRecordingStopped(@NonNull Room room) {
            Log.i(TAG, "Recording stopped");
        }
    };

    private void attachParticipant(@NonNull RemoteParticipant participant) {
        participant.setListener(new RemoteParticipant.Listener() {
            @Override
            public void onVideoTrackSubscribed(@NonNull RemoteParticipant p,
                                               @NonNull RemoteVideoTrackPublication pub,
                                               @NonNull RemoteVideoTrack track) {
                runOnUiThread(() -> track.addSink(remoteRenderer));
                Log.i(TAG, "Remote video track subscribed");
            }

            @Override
            public void onVideoTrackUnsubscribed(@NonNull RemoteParticipant p,
                                                 @NonNull RemoteVideoTrackPublication pub,
                                                 @NonNull RemoteVideoTrack track) {
                runOnUiThread(() -> track.removeSink(remoteRenderer));
                Log.i(TAG, "Remote video track unsubscribed");
            }

            @Override
            public void onVideoTrackSubscriptionFailed(@NonNull RemoteParticipant p, @NonNull RemoteVideoTrackPublication pub, @NonNull TwilioException e) {
            }

            @Override
            public void onVideoTrackPublished(@NonNull RemoteParticipant p, @NonNull RemoteVideoTrackPublication pub) {
            }

            @Override
            public void onVideoTrackUnpublished(@NonNull RemoteParticipant p, @NonNull RemoteVideoTrackPublication pub) {
            }

            @Override
            public void onDataTrackSubscribed(@NonNull RemoteParticipant p, @NonNull RemoteDataTrackPublication pub, @NonNull RemoteDataTrack track) {
            }

            @Override
            public void onDataTrackUnsubscribed(@NonNull RemoteParticipant p, @NonNull RemoteDataTrackPublication pub, @NonNull RemoteDataTrack track) {
            }

            @Override
            public void onAudioTrackEnabled(@NonNull RemoteParticipant remoteParticipant, @NonNull RemoteAudioTrackPublication remoteAudioTrackPublication) {
            }

            @Override
            public void onAudioTrackDisabled(@NonNull RemoteParticipant remoteParticipant, @NonNull RemoteAudioTrackPublication remoteAudioTrackPublication) {
            }

            @Override
            public void onVideoTrackEnabled(@NonNull RemoteParticipant remoteParticipant, @NonNull RemoteVideoTrackPublication remoteVideoTrackPublication) {
            }

            @Override
            public void onVideoTrackDisabled(@NonNull RemoteParticipant remoteParticipant, @NonNull RemoteVideoTrackPublication remoteVideoTrackPublication) {
            }

            @Override
            public void onDataTrackSubscriptionFailed(@NonNull RemoteParticipant p, @NonNull RemoteDataTrackPublication pub, @NonNull TwilioException e) {
            }

            @Override
            public void onDataTrackPublished(@NonNull RemoteParticipant p, @NonNull RemoteDataTrackPublication pub) {
            }

            @Override
            public void onDataTrackUnpublished(@NonNull RemoteParticipant p, @NonNull RemoteDataTrackPublication pub) {
            }

            @Override
            public void onAudioTrackPublished(@NonNull RemoteParticipant p, @NonNull RemoteAudioTrackPublication pub) {
            }

            @Override
            public void onAudioTrackUnpublished(@NonNull RemoteParticipant p, @NonNull RemoteAudioTrackPublication pub) {
            }

            @Override
            public void onAudioTrackSubscribed(@NonNull RemoteParticipant p, @NonNull RemoteAudioTrackPublication pub, @NonNull com.twilio.video.RemoteAudioTrack track) {
            }

            @Override
            public void onAudioTrackUnsubscribed(@NonNull RemoteParticipant p, @NonNull RemoteAudioTrackPublication pub, @NonNull com.twilio.video.RemoteAudioTrack track) {
            }

            @Override
            public void onAudioTrackSubscriptionFailed(@NonNull RemoteParticipant p, @NonNull RemoteAudioTrackPublication pub, @NonNull TwilioException e) {
            }

            @Override
            public void onNetworkQualityLevelChanged(@NonNull RemoteParticipant p, @NonNull com.twilio.video.NetworkQualityLevel networkQualityLevel) {
            }
        });
    }

    private void cleanupTracks() {
        if (localVideoTrack != null) {
            localVideoTrack.removeSink(localRenderer);
            localVideoTrack.release();
            localVideoTrack = null;
        }
        if (cameraCapturer != null) {
            try {
                cameraCapturer.stopCapture();
            } catch (Exception ignored) {
            }
            cameraCapturer = null;
        }
        if (localAudioTrack != null) {
            localAudioTrack.release();
            localAudioTrack = null;
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (room != null) {
            room.disconnect();
            room = null;
        }
        cleanupTracks();
        localRenderer = null;
        remoteRenderer = null;
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (localVideoTrack != null) {
            localVideoTrack.enable(false);
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (localVideoTrack != null) {
            localVideoTrack.enable(true);
        }
    }
}