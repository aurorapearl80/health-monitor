package com.monitor.health.services;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.BatteryManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.provider.Settings;
import android.util.Log;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.core.app.ActivityCompat;

import com.monitor.health.ApiClient;
import com.monitor.health.Constant;
import com.monitor.health.database.DatabaseClient;
import com.monitor.health.request.SendAlarmRequest;

import java.util.Locale;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class InactivityMonitorService extends Service {
    private Handler handler = new Handler();
    private Runnable inactivityCheck;
    private static final long CHECK_INTERVAL = 60000; // 1 minute
    private static final long INACTIVITY_THRESHOLD = 300000; // 5 minutes
    private long lastActivityTime;
    DatabaseClient databaseClient;

    public double latitude = 18.421566;
    public double longitude = -66.073031;
    String _model;                     // e.g., SM-G925I
    String _maker;              // e.g., Samsung
    String osVersion;       // e.g., 4.4, 12, 13
    String _country;


    private static final String TAG = "InactivityMonitorService";
    String androidId;

    //Please convert this to services:
    private static final int LOCATION_PERMISSION_REQUEST_CODE = 100;
    private LocationManager locationManager;

    private static final int MAX_RETRY_COUNT = 3; // Set max retry attempts
    private int retryCount = 0;
    int batteryPercent = 0;
    @Override
    public void onCreate() {
        super.onCreate();
        lastActivityTime = System.currentTimeMillis();
        Log.d("InactivityMonitor", "running here..... ");
        //Toast.makeText(getApplicationContext(), "Running here", Toast.LENGTH_LONG).show();
        databaseClient = DatabaseClient.getInstance(getApplicationContext());
        androidId = Settings.Secure.getString(getContentResolver(), Settings.Secure.ANDROID_ID);

        _model = Build.MODEL;                     // e.g., SM-G925I
        _maker = Build.MANUFACTURER;              // e.g., Samsung
        osVersion = Build.VERSION.RELEASE;       // e.g., 4.4, 12, 13
        _country = Locale.getDefault().getCountry(); // e.g., PR empty for now

        Log.wtf("BatteryPercent", "connecting------------------");
        BatteryManager batteryManager = (BatteryManager) getSystemService(BATTERY_SERVICE);
        IntentFilter ifilter = new IntentFilter(Intent.ACTION_BATTERY_CHANGED);
        Intent batteryStatus = registerReceiver(null, ifilter);
        int level = batteryStatus.getIntExtra(BatteryManager.EXTRA_LEVEL, -1);
        int scale = batteryStatus.getIntExtra(BatteryManager.EXTRA_SCALE, -1);
        batteryPercent = (level * 100) / scale;
        Log.wtf("BatteryPercent", String.valueOf(batteryPercent));


        startInactivityCheck();

        getLocation();
    }

    private void startInactivityCheck() {
        inactivityCheck = new Runnable() {
            @Override
            public void run() {
                long currentTime = System.currentTimeMillis();
                if (currentTime - lastActivityTime > INACTIVITY_THRESHOLD) {
                    // Trigger fall alert or send notification
                    Log.d("InactivityMonitor", "Potential fall detected due to inactivity");
                    Toast.makeText(getApplicationContext(), "Potential fall detected due to inactivity", Toast.LENGTH_LONG).show();
                    // Notify or alert user

                    //_LOGIN_GET_TOKEN(TokenGenerator.token(), androidId);

                    //SDK_003_CREATE_UPDATE_USER();
                    sendAlarm();
                }
                handler.postDelayed(inactivityCheck, CHECK_INTERVAL);
            }
        };
        handler.postDelayed(inactivityCheck, CHECK_INTERVAL);
    }

    @Override
    public void onDestroy() {
        //handler.removeCallbacks(inactivityCheck);
        super.onDestroy();
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }
    @SuppressLint("LongLogTag")
    public void sendAlarm() {
        SendAlarmRequest sendAlarmRequest = new SendAlarmRequest(148.752, 87588.701, androidId, 1, 1, batteryPercent, true,
                _model, _maker,
                "0", _country);
        Call<Object> call = ApiClient.getUserService(Constant.BASE_URL_BGM, Constant.TOKEN_DR_WATCH_API, androidId)
                .sendAlarm(sendAlarmRequest);

        call.enqueue(new Callback<Object>() {
            @Override
            public void onResponse(Call<Object> call, Response<Object> response) {
                Log.d(TAG, "SDK_003_CREATE_UPDATE_USER DRS_020_SEND_ALARM code " + response.code());
                Log.d(TAG, "SDK_003_CREATE_UPDATE_USER DRS_020_SEND_ALARM body " + response.body());
                Log.d(TAG, "SDK_003_CREATE_UPDATE_USER DRS_020_SEND_ALARM toString " + response.toString());
                Log.d(TAG, "SDK_003_CREATE_UPDATE_USER DRS_020_SEND_ALARM message " + response.message());

                if (response.code() == 200) {
                    Log.d(TAG, "Alarm sent successfully!");
                    retryCount = 0; // Reset retry count on success
                } else {
                    handleRetry();
                }
            }

            @Override
            public void onFailure(Call<Object> call, Throwable t) {
                Log.d(TAG, "SDK_003_CREATE_UPDATE_USER Error " + t.toString());
                handleRetry();
            }
        });
    }
    @SuppressLint("LongLogTag")
    private void handleRetry() {
        if (retryCount < MAX_RETRY_COUNT) {
            retryCount++;
            Log.d(TAG, "Retrying sendAlarm... Attempt " + retryCount);
            sendAlarm();
        } else {
            Log.d(TAG, "Max retry attempts reached. Failed to send alarm.");
        }
    }



    // Initialize and get location

    public void getLocation() {
        locationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
        //Log.d(TAG, "latitude:  ---  ");
        // Check if GPS or Network Provider is enabled
        boolean isGPSEnabled = locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER);
        boolean isNetworkEnabled = locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER);

        if (!isGPSEnabled && !isNetworkEnabled) {
            Toast.makeText(this, "No location provider available. Enable GPS or Network.", Toast.LENGTH_SHORT).show();
            return;
        }

        // Get location using GPS Provider

        //Log.d(TAG, "latitude:  ---  "+isGPSEnabled );
        if (isGPSEnabled) {
            requestLocation(LocationManager.GPS_PROVIDER);
        }
        // Get location using Network Provider as a fallback
        else if (isNetworkEnabled) {
            requestLocation(LocationManager.NETWORK_PROVIDER);
        }
    }

    public void requestLocation(String provider) {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED &&
                ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            //Log.d(TAG, "Permissions not granted. Requesting permissions...");
            //ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION}, 1);
            return;
        }

        // Get the last known location
        Location lastKnownLocation = locationManager.getLastKnownLocation(provider);

        if (lastKnownLocation != null) {
            latitude = lastKnownLocation.getLatitude();
            longitude = lastKnownLocation.getLongitude();


        } else {

        }

        // Check if provider is enabled
        if (!locationManager.isProviderEnabled(provider)) {
            //Log.d(TAG, "Provider " + provider + " is not enabled. Prompting user to enable GPS...");
            Intent intent = new Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS);
            startActivity(intent);
            return;
        }

        // Request location updates
        locationManager.requestLocationUpdates(
                provider,
                1000, // Minimum time interval between updates (in milliseconds)
                1, // Minimum distance interval between updates (in meters)
                new LocationListener() {
                    @Override
                    public void onLocationChanged(@NonNull Location location) {
                        double latitude = location.getLatitude();
                        double longitude = location.getLongitude();

//                        Log.d(TAG, "Provider Latitude: " + latitude);
//                        Log.d(TAG, "Provider Longitude: " + longitude);

                        // Stop updates after obtaining location
                        locationManager.removeUpdates(this);
                    }

                    @Override
                    public void onStatusChanged(String provider, int status, Bundle extras) {
                        //Log.d(TAG, "Provider Status changed: " + status);
                    }

                    @Override
                    public void onProviderEnabled(@NonNull String provider) {
                        //Log.d(TAG, "Provider " + provider + " Enabled");
                    }

                    @Override
                    public void onProviderDisabled(@NonNull String provider) {
                        //Log.d(TAG, "Provider " + provider + " Disabled");
                    }
                }
        );
    }




}

