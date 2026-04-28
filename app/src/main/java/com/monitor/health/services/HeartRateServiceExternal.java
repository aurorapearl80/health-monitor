package com.monitor.health.services;

import android.app.Service;
import android.content.Intent;
import android.os.IBinder;

public class HeartRateServiceExternal extends Service {

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {

        Intent it = new Intent("com.hsciot.healthy.action.start");
        it.setPackage("com.hsc.heartrate"); // Make sure this is correct
        it.putExtra("action", 1 << 11); // ACTION_HEART
        it.putExtra("count", 1);

        startService(it);

        return START_NOT_STICKY;
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }
}
