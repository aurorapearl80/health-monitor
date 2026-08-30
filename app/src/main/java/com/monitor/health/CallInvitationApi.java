package com.monitor.health;

import android.content.Context;
import android.util.Log;

import com.monitor.health.utility.DeviceUtils;

import java.io.IOException;
import java.util.concurrent.TimeUnit;

import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

/**
 * Accept/decline an incoming video-call invitation raised by the admin side
 * (see patient-monitoring-web's CallInvitationController). Identified by this
 * watch's serial — same serial-only auth as the doctor-watch chat/readings/
 * livekit-token endpoints, since this device never completes the email/
 * password login the bearer-authed /api/patient/call-invitations expects.
 * Fire-and-forget — failures are logged, never block the call/decline UX.
 */
public class CallInvitationApi {

    private static final String TAG = "CallInvitationApi";
    private static final MediaType JSON = MediaType.parse("application/json; charset=utf-8");
    private static final OkHttpClient CLIENT = new OkHttpClient.Builder()
            .connectTimeout(15, TimeUnit.SECONDS)
            .readTimeout(15, TimeUnit.SECONDS)
            .build();

    public static void respond(Context context, int callInvitationId, String status) {
        String serial = DeviceUtils.resolveWatchSerial(context);

        RequestBody body = RequestBody.create(
                "{\"serial\":\"" + serial + "\",\"status\":\"" + status + "\"}", JSON);
        Request request = new Request.Builder()
                .url(Constant.BASE_URL + "api/doctor-watches/call-invitations/" + callInvitationId)
                .patch(body)
                .addHeader("Accept", "application/json")
                .build();

        new Thread(() -> {
            try (Response response = CLIENT.newCall(request).execute()) {
                Log.d(TAG, "respond(" + status + ") status: " + response.code());
            } catch (IOException e) {
                Log.e(TAG, "respond error: " + e.getMessage());
            }
        }).start();
    }
}
