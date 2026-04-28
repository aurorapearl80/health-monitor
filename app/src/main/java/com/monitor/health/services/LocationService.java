package com.monitor.health.services;

import android.annotation.SuppressLint;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.Service;
import android.content.pm.ServiceInfo;
import android.content.Intent;
import android.os.Build;
import android.os.IBinder;
import android.location.Location;
import android.os.Looper;
import android.util.Log;

import androidx.annotation.Nullable;
import androidx.core.app.NotificationCompat;

import com.google.android.gms.location.*;

public class LocationService extends Service {

    private static final String CHANNEL_ID = "location_channel";
    private FusedLocationProviderClient fusedLocationClient;
    private LocationCallback locationCallback;

    @Override
    public void onCreate() {
        super.onCreate();
        createNotificationChannel();
        Notification notification = getNotification("Getting location...");
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            startForeground(1, notification, ServiceInfo.FOREGROUND_SERVICE_TYPE_LOCATION);
        } else {
            startForeground(1, notification);
        }

        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this);

        locationCallback = new LocationCallback() {
            @Override
            public void onLocationResult(LocationResult locationResult) {
                if (locationResult == null) return;

                for (Location location : locationResult.getLocations()) {
                    double lat = location.getLatitude();
                    double lng = location.getLongitude();
                    //Log.d("LocationService", "Lat: " + lat + " Lng: " + lng);

                    // Send broadcast to MainActivity
                    Intent intent = new Intent("LOCATION_UPDATE");
                    intent.putExtra("lat", lat);
                    intent.putExtra("lng", lng);
                    sendBroadcast(intent);
                }
            }
        };

        startLocationUpdates();
    }

    @SuppressLint("NewApi")
    private void startLocationUpdates() {
        LocationRequest request = LocationRequest.create();
        request.setInterval(60000);        // 60 seconds
        request.setFastestInterval(30000); // 30 seconds
        request.setPriority(LocationRequest.PRIORITY_BALANCED_POWER_ACCURACY);

        if (checkSelfPermission(android.Manifest.permission.ACCESS_FINE_LOCATION) == android.content.pm.PackageManager.PERMISSION_GRANTED) {
            fusedLocationClient.requestLocationUpdates(request, locationCallback, Looper.getMainLooper());
        }
    }

    private Notification getNotification(String content) {
        return new NotificationCompat.Builder(this, CHANNEL_ID)
                .setContentTitle("Location Service")
                .setContentText(content)
                .setSmallIcon(android.R.drawable.ic_menu_mylocation)
                .build();
    }

    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel serviceChannel = new NotificationChannel(
                    CHANNEL_ID,
                    "Location Service Channel",
                    NotificationManager.IMPORTANCE_DEFAULT
            );
            NotificationManager manager = getSystemService(NotificationManager.class);
            if (manager != null) {
                manager.createNotificationChannel(serviceChannel);
            }
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        fusedLocationClient.removeLocationUpdates(locationCallback);
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }
}
