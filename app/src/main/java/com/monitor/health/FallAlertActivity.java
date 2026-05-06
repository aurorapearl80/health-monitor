package com.monitor.health;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.media.Ringtone;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.BatteryManager;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.monitor.health.request.SendAlarmRequest;
import com.monitor.health.utility.DeviceUtils;
import com.monitor.health.utility.SmartWatchAlertDialog;

import java.util.Locale;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class FallAlertActivity extends AppCompatActivity {

    public static volatile boolean isShowing = false;

    SharedPreferences prefs;
    private static final String TAG = "FallAlertActivity";
    String _model;                     // e.g., SM-G925I
    String _maker;              // e.g., Samsung
    String osVersion;       // e.g., 4.4, 12, 13
    String _country;
    String androidId;
    private static final int MAX_RETRY_COUNT = 3; // Set max retry attempts
    private int retryCount = 0;
    int batteryPercent = 0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // savedInstanceState != null means Android is restoring the activity from a saved state
        // (e.g., process death/recreation). The original intent still carries sos_triggered=true,
        // so we must explicitly reject restorations — user did not re-trigger SOS.
        if (savedInstanceState != null || !getIntent().getBooleanExtra("sos_triggered", false)) {
            finish();
            return;
        }
        isShowing = true;
        prefs = this.getSharedPreferences("LocationPrefs", MODE_PRIVATE);
        androidId =  DeviceUtils.getIMEI(this);

        _model = Build.MODEL;                     // e.g., SM-G925I
        _maker = Build.MANUFACTURER;              // e.g., Samsung
        osVersion = Build.VERSION.RELEASE;       // e.g., 4.4, 12, 13
        _country = Locale.getDefault().getCountry(); // e.g., PR empty for now
        IntentFilter ifilter = new IntentFilter(Intent.ACTION_BATTERY_CHANGED);
        Intent batteryStatus = registerReceiver(null, ifilter);
        int level = batteryStatus.getIntExtra(BatteryManager.EXTRA_LEVEL, -1);
        int scale = batteryStatus.getIntExtra(BatteryManager.EXTRA_SCALE, -1);
        batteryPercent = (level * 100) / scale;

        SmartWatchAlertDialog.showSaveDialog(
                this, // âœ… Activity context
                "We detected you may have fell or requested Emergency Call?",
                new SmartWatchAlertDialog.DialogListener() {
                    @Override public void onOkClicked() {
                        isShowing = false;
                        sendAlarm();
                        finish();
                    }

                    @Override public void onCancelClicked() {
                        isShowing = false;
                        finish();
                    }
                }
        );
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        //Log.d(TAG, “onNewIntent — dialog already visible, ignoring re-trigger”);
    }



    public void sendAlarm() {

        double latitude = Double.longBitsToDouble(prefs.getLong("latitude", Double.doubleToRawLongBits(0)));
        double longitude = Double.longBitsToDouble(prefs.getLong("longitude", Double.doubleToRawLongBits(0)));
//        SendAlarmRequest sendAlarmRequest = new SendAlarmRequest(latitude, longitude, androidId, 1, 1, batteryPercent, true,
//                _model, _maker,
//                "0", _country);
        // Location sample: lat: 148.752, Long: 87588.701
        SendAlarmRequest sendAlarmRequest = new SendAlarmRequest(latitude, longitude, androidId, 1, 1, batteryPercent, true,
                _model, _maker,
                "0", _country);
        Call<Object> call = ApiClient.getUserService(Constant.BASE_URL_BGM, Constant.TOKEN_DR_WATCH_API, androidId)
                .sendAlarm(sendAlarmRequest);

        call.enqueue(new Callback<Object>() {
            @Override
            public void onResponse(Call<Object> call, Response<Object> response) {
                Log.d(TAG, "SDK_003_CREATE_UPDATE_USER DRS_020_SEND_ALARM code " + response.code());
                Log.d(TAG, "SDK_003_CREATE_UPDATE_USER DRS_020_SEND_ALARM body " + response.body());
                Log.d(TAG, "SDK_003_CREATE_UPDATE_USER DRS_020_SEND_ALARM toString " + response.toString());
                Log.d(TAG, "SDK_003_CREATE_UPDATE_USER DRS_020_SEND_ALARM message " + response.message());
                // Hide progress
                if (response.code() == 200) {
                    Log.d(TAG, "Alarm sent successfully!");
                    retryCount = 0; // Reset retry count on success
                    playNotificationSound();
                } else {
                    handleRetry();
                }
            }

            @Override
            public void onFailure(Call<Object> call, Throwable t) {
                handleRetry();
            }
        });
    }
    @SuppressLint("LongLogTag")
    private void handleRetry() {
        if (retryCount < MAX_RETRY_COUNT) {
            retryCount++;
            Log.d(TAG, "Retrying sendAlarm... Attempt " + retryCount);
            sendAlarm();
        } else {
            Log.d(TAG, "Max retry attempts reached. Failed to send alarm.");
        }
    }
    @Override
    protected void onDestroy() {
        isShowing = false;
        super.onDestroy();
    }

    private void playNotificationSound() {
        try {
            Uri notification = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION);
            Ringtone ringtone = RingtoneManager.getRingtone(this, notification);
            ringtone.play();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}