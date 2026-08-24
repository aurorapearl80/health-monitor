package com.monitor.health;

import android.content.Context;
import android.util.Log;

import com.monitor.health.utility.PreferenceHelper;

import java.io.IOException;
import java.util.concurrent.TimeUnit;

import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

/**
 * Accept/decline an incoming video-call invitation raised by the admin side
 * (see patient-monitoring-web's CallInvitationController). Fire-and-forget —
 * failures are logged, never block the call/decline UX.
 */
public class CallInvitationApi {

    private static final String TAG = "CallInvitationApi";
    private static final MediaType JSON = MediaType.parse("application/json; charset=utf-8");
    private static final OkHttpClient CLIENT = new OkHttpClient.Builder()
            .connectTimeout(15, TimeUnit.SECONDS)
            .readTimeout(15, TimeUnit.SECONDS)
            .build();

    public static void respond(Context context, int callInvitationId, String status) {
        String authToken = PreferenceHelper.getInstance(context).getString(Constant.AUTH_TOKEN, null);
        if (authToken == null) {
            Log.w(TAG, "No auth token stored; cannot respond to call invitation.");
            return;
        }

        RequestBody body = RequestBody.create("{\"status\":\"" + status + "\"}", JSON);
        Request request = new Request.Builder()
                .url(Constant.BASE_URL + "api/patient/call-invitations/" + callInvitationId)
                .patch(body)
                .addHeader("Authorization", "Bearer " + authToken)
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
