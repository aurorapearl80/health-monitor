package com.monitor.health.worker;

import android.content.Context;
import android.os.UserManager;

import androidx.annotation.NonNull;
import androidx.work.ListenableWorker;
import androidx.work.Worker;
import androidx.work.WorkerParameters;

import com.monitor.health.services.ServiceStarter;


public class StartupRetryWorker extends Worker {

    public StartupRetryWorker(@NonNull Context context,
                              @NonNull WorkerParameters params) {
        super(context, params);
    }

    @NonNull @Override
    public ListenableWorker.Result doWork() {
        Context ctx = getApplicationContext();
        UserManager um = (UserManager) ctx.getSystemService(Context.USER_SERVICE);
        boolean unlocked = (um == null) || um.isUserUnlocked();

        if (unlocked) {
            ServiceStarter.startAllIfSafe(ctx);
            return Result.success();
        } else {
            // Still locked â€” try again a bit later.
            ServiceStarter.scheduleInitRetry(ctx, 30);
            return Result.retry();
        }
    }
}
