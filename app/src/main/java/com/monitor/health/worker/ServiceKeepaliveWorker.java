package com.monitor.health.worker;

import android.content.Context;
import android.content.Intent;
import android.os.Build;

import androidx.annotation.NonNull;
import androidx.work.Worker;
import androidx.work.WorkerParameters;

import com.monitor.health.services.BleScanService;
import com.monitor.health.services.MyForegroundService;

public class ServiceKeepaliveWorker extends Worker {
    public ServiceKeepaliveWorker(@NonNull Context context, @NonNull WorkerParameters params) {
        super(context, params);
    }

    @NonNull
    @Override
    public Result doWork() {
        startIfNeeded(new Intent(getApplicationContext(), BleScanService.class));
        startIfNeeded(new Intent(getApplicationContext(), MyForegroundService.class));
        return Result.success();
    }

    private void startIfNeeded(Intent serviceIntent) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            getApplicationContext().startForegroundService(serviceIntent);
        } else {
            getApplicationContext().startService(serviceIntent);
        }
    }
}
