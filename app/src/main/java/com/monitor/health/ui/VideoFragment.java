package com.monitor.health.ui;

import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager;
import android.hardware.camera2.CameraCharacteristics;
import android.hardware.camera2.CameraManager;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;

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

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Collections;

public class VideoFragment extends Fragment {

    private static final String TAG = "WearTwilioVideoFragment";
//    private static final String TOKEN_URL =
//            "https://YOUR_SERVER.example.com/token?identity=wear_user&room=wear-room";
    private static final String TOKEN_URL =
            "https://8fff2cb6dac1.ngrok-free.app/token?identity=wear_user&room=wear-room";
    private VideoView localRenderer;
    private VideoView remoteRenderer;

    private LocalAudioTrack localAudioTrack;
    private LocalVideoTrack localVideoTrack;
    private Camera2Capturer cameraCapturer;
    private Room room;

    private ActivityResultLauncher<String[]> permissionLauncher;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_video, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        localRenderer = view.findViewById(R.id.local_video);
        remoteRenderer = view.findViewById(R.id.remote_video);
        localRenderer.setMirror(true);

        Button join = view.findViewById(R.id.joinButton);
        Button leave = view.findViewById(R.id.leaveButton);

        join.setOnClickListener(v -> ensurePermissionsAndJoin());
        leave.setOnClickListener(v -> disconnectFromRoom());

        // Updated logging API
        //Video.setLogLevel(Video.LogLevel.INFO);

        permissionLauncher = registerForActivityResult(
                new ActivityResultContracts.RequestMultiplePermissions(),
                result -> {
                    boolean micGranted = Boolean.TRUE.equals(result.get(Manifest.permission.RECORD_AUDIO));
                    boolean camGranted = Boolean.TRUE.equals(result.get(Manifest.permission.CAMERA));
                    if (micGranted) startLocalAudio();
                    if (camGranted) startLocalVideo();
                    if (!micGranted && !camGranted) {
                        Toast.makeText(requireContext(), "Mic/Camera denied", Toast.LENGTH_SHORT).show();
                    }
                });
    }

    private void ensurePermissionsAndJoin() {
        boolean mic = ContextCompat.checkSelfPermission(requireContext(),
                Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED;
        boolean cam = ContextCompat.checkSelfPermission(requireContext(),
                Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED;

        if (!mic || !cam) {
            permissionLauncher.launch(new String[]{Manifest.permission.RECORD_AUDIO, Manifest.permission.CAMERA});
        } else {
            startLocalAudio();
            startLocalVideo();
        }

        new FetchTokenTask(token -> {
            if (token == null || token.isEmpty()) {
                Toast.makeText(requireContext(), "Token fetch failed", Toast.LENGTH_SHORT).show();
                return;
            }
            connectToRoom(token, "wear-room");
        }).execute(TOKEN_URL);
    }

    private void startLocalAudio() {
        if (localAudioTrack == null) {
            AudioOptions audioOptions = new AudioOptions.Builder().build();
            localAudioTrack = LocalAudioTrack.create(requireContext(), true, audioOptions, "mic");
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
            cameraCapturer = new Camera2Capturer(requireContext(), cameraId, null);
            localVideoTrack = LocalVideoTrack.create(requireContext(), true, cameraCapturer, "camera");
            localVideoTrack.addSink(localRenderer);
        } catch (Exception e) {
            Log.w(TAG, "Camera initialization failed, audio-only mode", e);
        }
    }

    @Nullable
    private String getFrontCameraId() throws Exception {
        CameraManager cm = (CameraManager) requireContext().getSystemService(Context.CAMERA_SERVICE);
        for (String id : cm.getCameraIdList()) {
            Integer facing = cm.getCameraCharacteristics(id).get(CameraCharacteristics.LENS_FACING);
            if (facing != null && facing == CameraCharacteristics.LENS_FACING_FRONT) {
                return id;
            }
        }
        // fallback to first camera if no explicit front camera
        String[] ids = cm.getCameraIdList();
        return ids.length > 0 ? ids[0] : null;
    }

    private void connectToRoom(String accessToken, String roomName) {
        ConnectOptions.Builder builder = new ConnectOptions.Builder(accessToken)
                .roomName(roomName);
        if (localAudioTrack != null) builder.audioTracks(Collections.singletonList(localAudioTrack));
        if (localVideoTrack != null) builder.videoTracks(Collections.singletonList(localVideoTrack));
        room = Video.connect(requireContext(), builder.build(), roomListener);
    }

    // ---- Room callbacks for SDK 7.x ----
    private final Room.Listener roomListener = new Room.Listener() {
        @Override public void onConnected(@NonNull Room room) {
            LocalParticipant lp = room.getLocalParticipant();
            Log.i(TAG, "Connected to " + room.getName() + " as " + (lp != null ? lp.getIdentity() : "unknown"));
            for (RemoteParticipant p : room.getRemoteParticipants()) {
                attachParticipant(p);
            }
        }

        @Override public void onConnectFailure(@NonNull Room room, @NonNull TwilioException e) {
            Toast.makeText(requireContext(), "Connect failed: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }

        @Override
        public void onReconnecting(@NonNull Room room, @NonNull TwilioException twilioException) {

        }

        @Override
        public void onReconnected(@NonNull Room room) {

        }

        @Override public void onDisconnected(@NonNull Room room, @Nullable TwilioException e) {
            cleanupTracks();
        }

        @Override public void onParticipantConnected(@NonNull Room room, @NonNull RemoteParticipant participant) {
            attachParticipant(participant);
        }

        @Override public void onParticipantDisconnected(@NonNull Room room, @NonNull RemoteParticipant participant) { }

        @Override public void onRecordingStarted(@NonNull Room room) { }

        @Override public void onRecordingStopped(@NonNull Room room) { }
    };

    private void attachParticipant(@NonNull RemoteParticipant participant) {
        participant.setListener(new RemoteParticipant.Listener() {
            @Override
            public void onVideoTrackSubscribed(@NonNull RemoteParticipant p,
                                               @NonNull RemoteVideoTrackPublication pub,
                                               @NonNull RemoteVideoTrack track) {
                // render remote video
                track.addSink(remoteRenderer);
            }

            @Override
            public void onVideoTrackUnsubscribed(@NonNull RemoteParticipant p,
                                                 @NonNull RemoteVideoTrackPublication pub,
                                                 @NonNull RemoteVideoTrack track) {
                track.removeSink(remoteRenderer);
            }

            // ---- Other callbacks weâ€™ll no-op for now ----
            @Override public void onVideoTrackSubscriptionFailed(@NonNull RemoteParticipant p, @NonNull RemoteVideoTrackPublication pub, @NonNull TwilioException e) { }
            @Override public void onVideoTrackPublished(@NonNull RemoteParticipant p, @NonNull RemoteVideoTrackPublication pub) { }
            @Override public void onVideoTrackUnpublished(@NonNull RemoteParticipant p, @NonNull RemoteVideoTrackPublication pub) { }
            @Override public void onDataTrackSubscribed(@NonNull RemoteParticipant p, @NonNull RemoteDataTrackPublication pub, @NonNull RemoteDataTrack track) { }
            @Override public void onDataTrackUnsubscribed(@NonNull RemoteParticipant p, @NonNull RemoteDataTrackPublication pub, @NonNull RemoteDataTrack track) { }

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

            @Override public void onDataTrackSubscriptionFailed(@NonNull RemoteParticipant p, @NonNull RemoteDataTrackPublication pub, @NonNull TwilioException e) { }
            @Override public void onDataTrackPublished(@NonNull RemoteParticipant p, @NonNull RemoteDataTrackPublication pub) { }
            @Override public void onDataTrackUnpublished(@NonNull RemoteParticipant p, @NonNull RemoteDataTrackPublication pub) { }
            @Override public void onAudioTrackPublished(@NonNull RemoteParticipant p, @NonNull com.twilio.video.RemoteAudioTrackPublication pub) { }
            @Override public void onAudioTrackUnpublished(@NonNull RemoteParticipant p, @NonNull com.twilio.video.RemoteAudioTrackPublication pub) { }
            @Override public void onAudioTrackSubscribed(@NonNull RemoteParticipant p, @NonNull com.twilio.video.RemoteAudioTrackPublication pub, @NonNull com.twilio.video.RemoteAudioTrack track) { }
            @Override public void onAudioTrackUnsubscribed(@NonNull RemoteParticipant p, @NonNull com.twilio.video.RemoteAudioTrackPublication pub, @NonNull com.twilio.video.RemoteAudioTrack track) { }
            @Override public void onAudioTrackSubscriptionFailed(@NonNull RemoteParticipant p, @NonNull com.twilio.video.RemoteAudioTrackPublication pub, @NonNull TwilioException e) { }
            @Override public void onNetworkQualityLevelChanged(@NonNull RemoteParticipant p, @NonNull com.twilio.video.NetworkQualityLevel networkQualityLevel) { }

        });
    }

    private void disconnectFromRoom() {
        if (room != null) {
            room.disconnect();
            room = null;
        }
        cleanupTracks();
    }

    private void cleanupTracks() {
        if (localVideoTrack != null) {
            localVideoTrack.removeSink(localRenderer);
            localVideoTrack.release();
            localVideoTrack = null;
        }
        if (cameraCapturer != null) {
            try { cameraCapturer.stopCapture(); } catch (Exception ignored) {}
            cameraCapturer = null;
        }
        if (localAudioTrack != null) {
            localAudioTrack.release();
            localAudioTrack = null;
        }
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        // VideoView doesn't need release(); clear refs to avoid leaks
        localRenderer = null;
        remoteRenderer = null;
    }

    /** Simple async token fetch */
    static class FetchTokenTask extends AsyncTask<String, Void, String> {
        interface Callback { void onToken(String token); }
        private final Callback cb;
        FetchTokenTask(Callback cb) { this.cb = cb; }
        @Override protected String doInBackground(String... urls) {
            try {
                URL u = new URL(urls[0]);
                HttpURLConnection c = (HttpURLConnection) u.openConnection();
                c.setConnectTimeout(5000);
                c.setReadTimeout(5000);
                c.setRequestMethod("GET");
                int code = c.getResponseCode();
                if (code != 200) return null;
                BufferedReader br = new BufferedReader(new InputStreamReader(c.getInputStream()));
                StringBuilder sb = new StringBuilder();
                String line;
                while ((line = br.readLine()) != null) sb.append(line);
                br.close();
                return sb.toString();
            } catch (Exception e) {
                Log.e(TAG, "Token fetch error", e);
                return null;
            }
        }
        @Override protected void onPostExecute(String token) { if (cb != null) cb.onToken(token); }
    }
}
