package com.monitor.health.receiver;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.UserManager;
import android.util.Log;

import com.monitor.health.services.ServiceStarter;

public class BootReceiver extends BroadcastReceiver {

    private static final String TAG = "BootReceiver";

    @Override
    public void onReceive(Context context, Intent intent) {
        final String action = intent != null ? intent.getAction() : null;
        Log.d(TAG, "onReceive: " + action);

        if (Intent.ACTION_USER_UNLOCKED.equals(action)) {
            // Profile is unlocked: safe to do full init now.
            ServiceStarter.startAllIfSafe(context);
            return;
        }

        if (Intent.ACTION_LOCKED_BOOT_COMPLETED.equals(action)
                || Intent.ACTION_BOOT_COMPLETED.equals(action)) {

            if (isUserUnlocked(context)) {
                // Device already unlocked (e.g., no lock screen profile).
                ServiceStarter.startAllIfSafe(context);
            } else {
                // Too early: schedule a light retry and wait for USER_UNLOCKED.
                ServiceStarter.scheduleInitRetry(context, /*delaySeconds=*/30);
            }
        }
    }

    private boolean isUserUnlocked(Context ctx) {
        UserManager um = (UserManager) ctx.getSystemService(Context.USER_SERVICE);
        return um == null || um.isUserUnlocked();
    }
}
