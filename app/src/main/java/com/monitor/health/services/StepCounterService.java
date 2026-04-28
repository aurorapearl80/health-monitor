package com.monitor.health.services;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Binder;
import android.os.Build;
import android.os.IBinder;
import android.util.Log;

import androidx.core.app.NotificationCompat;

import com.monitor.health.MainActivity;
import com.monitor.health.R;

public class StepCounterService extends Service {
    private static final String TAG = "StepCounterService";
    private static final String CHANNEL_ID = "step_counter_channel";
    private SensorManager sensorManager;
    private Sensor stepSensor;
    private SensorEventListener stepListener;
    private IBinder binder = new LocalBinder();
    private int steps = 0;

    public class LocalBinder extends Binder {
        public StepCounterService getService() {
            return StepCounterService.this;
        }
    }

    @Override
    public void onCreate() {
        super.onCreate();
        sensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);
        stepSensor = sensorManager.getDefaultSensor(Sensor.TYPE_STEP_DETECTOR);

        stepListener = new SensorEventListener() {
            @Override
            public void onSensorChanged(SensorEvent event) {
                steps += (int) event.values[0];
                Log.d(TAG, "Steps: " + steps);
                Intent intent = new Intent("STEP_COUNT_UPDATED");
                intent.putExtra("steps", steps);
                sendBroadcast(intent);
            }

            @Override
            public void onAccuracyChanged(Sensor sensor, int accuracy) {
                // Handle accuracy changes
            }
        };

        sensorManager.registerListener(stepListener, stepSensor, SensorManager.SENSOR_DELAY_NORMAL);
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Intent notificationIntent = new Intent(this, MainActivity.class);
        PendingIntent pendingIntent = PendingIntent.getActivity(this, 0, notificationIntent, PendingIntent.FLAG_IMMUTABLE);
        Notification notification = new NotificationCompat.Builder(this, CHANNEL_ID)
                .setContentTitle("Step Counter")
                .setContentText("Running in background")
                .setSmallIcon(R.drawable.ic_launcher_foreground)
                .setContentIntent(pendingIntent)
                .build();

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(CHANNEL_ID, "Step Counter", NotificationManager.IMPORTANCE_DEFAULT);
            NotificationManager manager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
            manager.createNotificationChannel(channel);
        }

        startForeground(1, notification);
        return super.onStartCommand(intent, flags, startId);
    }

    @Override
    public IBinder onBind(Intent intent) {
        return binder;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        sensorManager.unregisterListener(stepListener);
    }

    public int getSteps() {
        return steps;
    }
}