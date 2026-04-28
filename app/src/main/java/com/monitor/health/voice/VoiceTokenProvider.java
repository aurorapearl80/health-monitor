package com.monitor.health.voice;

import android.util.Log;

import androidx.annotation.Nullable;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;

/**
 * Simple blocking token fetcher for Twilio Voice access tokens.
 * Replace TOKEN_URL with your backend endpoint that returns JSON: { "token": "..." }
 */
public class VoiceTokenProvider {
    private static final String TAG = "VoiceTokenProvider";

    // TODO: Replace with your server endpoint that issues a Twilio Access Token with VoiceGrant
    private static final String TOKEN_URL = "https://YOUR_SERVER/voice/token?identity=android_client";

    @Nullable
    public String fetchToken() {
        HttpURLConnection conn = null;
        try {
            URL url = new URL(TOKEN_URL);
            conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("GET");
            conn.setConnectTimeout(8000);
            conn.setReadTimeout(8000);

            int code = conn.getResponseCode();
            if (code != 200) {
                Log.e(TAG, "Token endpoint error: " + code);
                return null;
            }

            BufferedReader in = new BufferedReader(new InputStreamReader(conn.getInputStream(), StandardCharsets.UTF_8));
            StringBuilder sb = new StringBuilder();
            String line;
            while ((line = in.readLine()) != null) sb.append(line);
            in.close();

            JSONObject obj = new JSONObject(sb.toString());
            return obj.optString("token", null);
        } catch (IOException | JSONException e) {
            Log.e(TAG, "fetchToken error", e);
            return null;
        } finally {
            if (conn != null) conn.disconnect();
        }
    }
}
