package com.monitor.health.utility;


import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.os.PowerManager;
import android.provider.Settings;

public class BatteryOptimizationHelper {

    /**
     * Check if battery optimization is disabled for this app
     */
    public static boolean isBatteryOptimizationDisabled(Context context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            PowerManager pm = (PowerManager) context.getSystemService(Context.POWER_SERVICE);
            return pm != null && pm.isIgnoringBatteryOptimizations(context.getPackageName());
        }
        return true;
    }

    /**
     * Request to disable battery optimization
     * Call this from your Activity
     */
    public static void requestDisableBatteryOptimization(Context context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (!isBatteryOptimizationDisabled(context)) {
                Intent intent = new Intent();
                intent.setAction(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS);
                intent.setData(Uri.parse("package:" + context.getPackageName()));
                context.startActivity(intent);
            }
        }
    }
}
