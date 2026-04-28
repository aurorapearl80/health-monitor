package com.monitor.health.worker;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.core.app.NotificationCompat;
import androidx.core.content.ContextCompat;
import androidx.work.ForegroundInfo;
import androidx.work.Worker;
import androidx.work.WorkerParameters;

import com.monitor.health.R;
import com.monitor.health.services.TestService;
import com.monitor.health.MainActivity;

public class HeartRateMonitorWorker extends Worker {

    private static final String TAG = "HeartRateWorker";
    private static final String CHANNEL_ID = "hrm_worker_channel";
    private static final int NOTIF_ID = 6401;

    public HeartRateMonitorWorker(@NonNull Context context, @NonNull WorkerParameters params) {
        super(context, params);
    }

    @NonNull
    @Override
    public Result doWork() {
        final long start = System.currentTimeMillis();
        Log.d(TAG, "Executing (attempt=" + getRunAttemptCount() + ") at " + start);

        try {
            // 1) Promote worker to foreground ASAP (improves reliability under Doze)
            setForegroundAsync(createForegroundInfo("Preparing heart-rate syncâ€¦"));

            // 2) Start your long-running ForegroundService (BLE, uploads, etc.)
            Intent svc = new Intent(getApplicationContext(), TestService.class);
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                ContextCompat.startForegroundService(getApplicationContext(), svc);
            } else {
                getApplicationContext().startService(svc);
            }

            Log.d(TAG, "Foreground service dispatched. Worker done. elapsed=" +
                    (System.currentTimeMillis() - start) + "ms");
            return Result.success();

        } catch (SecurityException se) {
            // Likely missing runtime permissions â†’ surface as failure so you can see it
            Log.e(TAG, "SecurityException (permissions?): " + se.getMessage(), se);
            return Result.failure();

        } catch (Throwable t) {
            // Transient issues (BT stack busy, scanner null right after BT ON, etc.)
            Log.w(TAG, "Transient failure: " + t.getMessage(), t);
            return Result.retry();
        }
    }

    // ---------- Foreground (notification) ----------

    private ForegroundInfo createForegroundInfo(String text) {
        final Context ctx = getApplicationContext();
        ensureChannel();

        // Optional: tap opens app
        PendingIntent contentIntent = PendingIntent.getActivity(
                ctx,
                0,
                new Intent(ctx, MainActivity.class),
                (Build.VERSION.SDK_INT >= 23)
                        ? PendingIntent.FLAG_IMMUTABLE | PendingIntent.FLAG_UPDATE_CURRENT
                        : PendingIntent.FLAG_UPDATE_CURRENT
        );

        Notification notif = new NotificationCompat.Builder(ctx, CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_profile)         // replace with your icon
                .setContentTitle("Health sync")
                .setContentText(text)
                .setContentIntent(contentIntent)
                .setOnlyAlertOnce(true)
                .setOngoing(true)
                .setPriority(NotificationCompat.PRIORITY_LOW)
                .build();

        return new ForegroundInfo(NOTIF_ID, notif);
    }

    private void ensureChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationManager nm =
                    (NotificationManager) getApplicationContext().getSystemService(Context.NOTIFICATION_SERVICE);
            if (nm == null) return;
            NotificationChannel ch = nm.getNotificationChannel(CHANNEL_ID);
            if (ch == null) {
                ch = new NotificationChannel(
                        CHANNEL_ID,
                        "Heart-rate background work",
                        NotificationManager.IMPORTANCE_LOW
                );
                nm.createNotificationChannel(ch);
            }
        }
    }
}
