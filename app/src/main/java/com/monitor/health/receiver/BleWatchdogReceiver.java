package com.monitor.health.receiver;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.util.Log;

import com.monitor.health.services.BleScanService;

public class BleWatchdogReceiver extends BroadcastReceiver {

    private static final String TAG = "BleWatchdogReceiver";
    public static final String ACTION_BLE_WATCHDOG =
            "com.monitor.health.ACTION_BLE_WATCHDOG";

    @Override
    public void onReceive(Context context, Intent intent) {
        if (intent == null || !ACTION_BLE_WATCHDOG.equals(intent.getAction())) {
            return;
        }

        Log.d(TAG, "BLE watchdog fired â€“ ensuring service & scan are running");

        // Start or wake the BLE scan service
        Intent serviceIntent = new Intent(context, BleScanService.class);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            context.startForegroundService(serviceIntent);
        } else {
            context.startService(serviceIntent);
        }
    }
}
