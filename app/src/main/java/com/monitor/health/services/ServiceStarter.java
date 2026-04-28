package com.monitor.health.services;

import android.Manifest;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothManager;
import android.content.Context;
import android.content.Intent;
import android.os.Build;

import androidx.core.content.ContextCompat;
import androidx.work.OneTimeWorkRequest;
import androidx.work.WorkManager;

import com.monitor.health.worker.StartupRetryWorker;

import java.util.concurrent.TimeUnit;

import static android.content.pm.PackageManager.PERMISSION_GRANTED;

public final class ServiceStarter {

    private ServiceStarter() {}

    /** Call ONLY after user is unlocked. */
    public static void startAllIfSafe(Context context) {
        // Start BLE service if Bluetooth is available + permission granted
        if (canUseBluetooth(context)) {
            startFgOrBg(context, new Intent(context, BleScanService.class));
        }

        // Fall detection typically needs BODY_SENSORS / ACTIVITY_RECOGNITION
        if (hasPermission(context, Manifest.permission.BODY_SENSORS)
                && hasPermission(context, Manifest.permission.ACTIVITY_RECOGNITION)) {
            startFgOrBg(context, new Intent(context, FallDetectionService.class));
        }

        // Your foreground service (health work). Ensure you actually post a foreground
        // notification inside onCreate()/onStartCommand() quickly (<= 10s).
        startFgOrBg(context, new Intent(context, MyForegroundService.class));
    }

    /** Use WorkManager to retry a little later after boot (system settling). */
    public static void scheduleInitRetry(Context context, int delaySeconds) {
        OneTimeWorkRequest req = new OneTimeWorkRequest.Builder(StartupRetryWorker.class)
                .setInitialDelay(delaySeconds, TimeUnit.SECONDS)
                .build();
        WorkManager.getInstance(context).enqueue(req);
    }

    private static boolean canUseBluetooth(Context ctx) {
        BluetoothManager bm = (BluetoothManager) ctx.getSystemService(Context.BLUETOOTH_SERVICE);
        BluetoothAdapter adapter = bm != null ? bm.getAdapter() : null;
        if (adapter == null) return false;

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            return hasPermission(ctx, Manifest.permission.BLUETOOTH_CONNECT)
                    && hasPermission(ctx, Manifest.permission.BLUETOOTH_SCAN);
        } else {
            // For older versions, legacy BLUETOOTH/ADMIN are enough (already in manifest)
            return true;
        }
    }

    private static boolean hasPermission(Context ctx, String perm) {
        return ContextCompat.checkSelfPermission(ctx, perm) == PERMISSION_GRANTED;
    }

    private static void startFgOrBg(Context ctx, Intent serviceIntent) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            // Foreground services must call startForeground(...) quickly.
            ContextCompat.startForegroundService(ctx, serviceIntent);
        } else {
            ctx.startService(serviceIntent);
        }
    }
}
