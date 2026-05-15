package com.monitor.health.services;


import static com.monitor.health.utility.AppUtils.getTodayDate;

import android.annotation.SuppressLint;
import android.app.AlarmManager;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.ConnectivityManager;
import android.net.Network;
import android.net.NetworkCapabilities;
import android.net.NetworkInfo;
import android.content.pm.ServiceInfo;
import android.os.Build;
import android.os.IBinder;
import android.os.SystemClock;
import android.provider.Settings;
import android.util.Log;

import androidx.annotation.Nullable;
import androidx.core.app.NotificationCompat;

import com.monitor.health.ApiClient;
import com.monitor.health.Constant;
import com.monitor.health.NetworkUtils;
import com.monitor.health.R;
import com.monitor.health.ReadingsRequest;
import com.monitor.health.adapter.HealthManager;
import com.monitor.health.dao.BPJumperDao;
import com.monitor.health.dao.HeartRateDao;
import com.monitor.health.dao.OximeterDao;
import com.monitor.health.dao.TemperatureDao;
import com.monitor.health.database.DatabaseClient;
import com.monitor.health.model.BPJumper;
import com.monitor.health.model.HeartRateEntity;
import com.monitor.health.model.Oximeter;
import com.monitor.health.model.Reading;
import com.monitor.health.model.ReadingValue;
import com.monitor.health.model.Temperature;
import com.monitor.health.model.WeighingScale;
import com.monitor.health.utility.DateUtils;
import com.monitor.health.utility.DeviceUtils;
import com.monitor.health.utility.PreferenceHelper;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.TimeZone;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class MyForegroundService extends Service {

    private static final String TAG = "MyForegroundService";
    private static final String CHANNEL_ID = "ForegroundServiceChannel";
    DatabaseClient databaseClient;
    String androidId;
    private String token = "";
    private ConnectivityManager connectivityManager;
    private ConnectivityManager.NetworkCallback networkCallback;

    @Override
    public void onCreate() {
        super.onCreate();

        databaseClient = DatabaseClient.getInstance(getApplicationContext());
        //androidId = Settings.Secure.getString(getContentResolver(), Settings.Secure.ANDROID_ID);
        androidId = DeviceUtils.getIMEI(this);

        createNotificationChannel();
        Notification notification = new NotificationCompat.Builder(this, CHANNEL_ID)
                .setContentTitle("Internet Check Service")
                .setContentText("Checking internet connectivity...")
                .setSmallIcon(R.drawable.ic_launcher_background)
                .build();
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            startForeground(1, notification, ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC);
        } else {
            startForeground(1, notification);
        }

        // React immediately when network becomes available
        connectivityManager = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            networkCallback = new ConnectivityManager.NetworkCallback() {
                @Override
                public void onAvailable(android.net.Network network) {
                    Log.d(TAG, "Network became available â€” syncing data");
                    syncData();
                }
            };
            connectivityManager.registerDefaultNetworkCallback(networkCallback);
        }

        // Start a thread to check for internet connectivity and sync data
        new Thread(() -> {
            while (true) {
                NetworkUtils.ConnectionQuality quality =
                        NetworkUtils.getConnectionQuality(getApplicationContext());
                if (quality != NetworkUtils.ConnectionQuality.NONE) {
                    Log.d(TAG, "Connection available (quality: " + quality + ") â€” syncing data");
                    syncData();
                } else {
                    Log.d(TAG, "No internet connection â€” skipping sync");
                }
                try {
                    Thread.sleep(10 * 60 * 1000); // Check every 10 minutes
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        }).start();
    }

    public void syncData() {
        syncBloodPressureData();
        syncSpo2();
        syncTemperature();
        syncGlucose();
        // Heart Rate
        //to review
        //syncHeartRate();
        //to review
        // Steps (delta-based) â€” use only one path to avoid double-counting
        startFetchingSteps();
    }

    private void syncHeartRate() {
        HeartRateDao dao = databaseClient.getAppDatabase().heartRateDao();
        List<HeartRateEntity> entities = dao.getAllHeartRate();
        if (entities == null || entities.isEmpty()) return;

        List<Reading> readingsList = new ArrayList<>();
        for (HeartRateEntity entity : entities) {
            Reading reading = new Reading(
                    false, "Asia/Manila", "jtm00025b94050c",
                    Arrays.asList(0.0, entity.getValue()),
                    "66437be266c8833a1c42d7aa",
                    "5bb306382598931ffbd1b626",
                    getDate(),
                    "jtm00025b94050c"
            );
            readingsList.add(reading);
        }

        ReadingsRequest req = new ReadingsRequest(readingsList);
        Call<Object> call = ApiClient.getUserService(Constant.BASE_URL_BGM,
                Constant.TOKEN_DR_WATCH_API, androidId).sendReadings(req);
        call.enqueue(new Callback<Object>() {
            @Override
            public void onResponse(Call<Object> call, Response<Object> response) {
                Log.d(TAG, "Heart rate sync: " + response.code());
            }
            @Override
            public void onFailure(Call<Object> call, Throwable t) {
                Log.e(TAG, "Heart rate sync failed: " + t.getMessage());
            }
        });
    }

    private void syncSteps() {
        HealthManager.getInstance(getApplicationContext()).getSteps(new HealthManager.ValueCallback<Integer>() {
            @Override
            public void onValue(Integer steps) {
                if (steps == null || steps <= 0) return;
                android.content.SharedPreferences prefs =
                        getSharedPreferences("steps_prefs", MODE_PRIVATE);
                int lastStepCount = prefs.getInt("last_step_count", 0);
                int delta;

                Log.e(TAG, "Steps available: " + lastStepCount);
                if (steps < lastStepCount) {
                    // Device restarted â€” step counter reset to zero
                    delta = steps;
                } else {
                    delta = steps - lastStepCount;
                }
                Log.e(TAG, "Steps available delta: " + delta);
                if (delta > 0) {
                    Log.e(TAG, "Steps available send delta: " + delta);
                    sendStepsToServer(delta, steps, prefs);
                }
            }
            @Override
            public void onError(String error) {
                Log.e(TAG, "Steps unavailable: " + error);
            }
        });
    }

    private void sendStepsToServer(int delta, int newStepCount, android.content.SharedPreferences prefs) {
        Log.d(TAG, "Sending steps synchronously: " + delta);
        try {
            String token = "bNWZsV#BeZvaNb*gF@3Z^7tCNhCT29Vw8Vi%4T%";

            Reading reading = new Reading(
                    false,
                    "Asia/Manila",
                    "jtm00025b94050c",
                    // API expects a list; here we send [steps]
                    Arrays.asList((double) delta),
                    "66437be266c8833a1c42d7aa",
                    "5bb306382598931ffbd1b629",
                    getTodayDate(),
                    DeviceUtils.getIMEI(this)
            );

            ReadingsRequest payload = new ReadingsRequest(List.of(reading));

            ApiClient.getUserService(Constant.BASE_URL_BGM, token, DeviceUtils.getIMEI(this))
                    .sendReadings(payload)
                    .enqueue(new Callback<Object>() {
                        @Override
                        public void onResponse(Call<Object> call, Response<Object> response) {
                            if (response.isSuccessful()) {
                                prefs.edit().putInt("last_step_count", newStepCount).apply();
                                Log.d(TAG, "âœ… Steps data sent successfully");
                            } else {
                                Log.e(TAG, "âŒ Server returned error: " + response.code() + " - " + response.message() + " â€” steps will retry next sync");
                            }
                        }

                        @Override
                        public void onFailure(Call<Object> call, Throwable t) {
                            Log.e(TAG, "âŒ Sync failed â€” steps will retry next sync", t);
                        }
                    });

        } catch (Exception e) {
            Log.e(TAG, "âŒ Exception during server sync", e);
        }
    }

    private boolean isInternetAvailable() {
        ConnectivityManager connectivityManager =
                (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
        if (connectivityManager != null) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                Network network = connectivityManager.getActiveNetwork();
                if (network != null) {
                    NetworkCapabilities capabilities = connectivityManager.getNetworkCapabilities(network);
                    return capabilities != null && capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET);
                }
            } else {
                NetworkInfo activeNetworkInfo = connectivityManager.getActiveNetworkInfo();
                return activeNetworkInfo != null && activeNetworkInfo.isConnected();
            }
        }
        return false;
    }

    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel serviceChannel = new NotificationChannel(
                    CHANNEL_ID,
                    "Foreground Service Channel",
                    NotificationManager.IMPORTANCE_DEFAULT
            );
            NotificationManager manager = getSystemService(NotificationManager.class);
            if (manager != null) {
                manager.createNotificationChannel(serviceChannel);
            }
        }
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        return START_STICKY;
    }

    @SuppressLint("ScheduleExactAlarm")
    @Override
    public void onTaskRemoved(Intent rootIntent) {
        Log.d(TAG, "onTaskRemoved - restarting service");
        Intent restartIntent = new Intent(getApplicationContext(), MyForegroundService.class);
        restartIntent.setPackage(getPackageName());
        @SuppressLint("UnspecifiedImmutableFlag")
        PendingIntent restartPendingIntent = PendingIntent.getService(
                getApplicationContext(), 1, restartIntent,
                PendingIntent.FLAG_ONE_SHOT);
        AlarmManager alarmManager = (AlarmManager) getSystemService(Context.ALARM_SERVICE);
        if (alarmManager != null) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                alarmManager.setExactAndAllowWhileIdle(AlarmManager.ELAPSED_REALTIME_WAKEUP,
                        SystemClock.elapsedRealtime() + 1000,
                        restartPendingIntent);
            } else {
                alarmManager.setExact(AlarmManager.ELAPSED_REALTIME_WAKEUP,
                        SystemClock.elapsedRealtime() + 1000,
                        restartPendingIntent);
            }
        }
        super.onTaskRemoved(rootIntent);
    }

    @Override
    public void onDestroy() {
        if (networkCallback != null && connectivityManager != null) {
            try {
                connectivityManager.unregisterNetworkCallback(networkCallback);
            } catch (Exception ignored) {}
        }
        super.onDestroy();
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    public void sendBPJumper(double systolic, double diastolic, double bpm, String serial, long id) {

        token = "bNWZsV#BeZvaNb*gF@3Z^7tCNhCT29Vw8Vi%4T%";
        Reading reading = new Reading(
                false,
                "Asia/Manila",
                "5be9a0e03d320b73e5f7aa71",
                Arrays.asList(systolic,diastolic, bpm),
                "62e42fce170f8985e63754bb",
                "5bb306382598931ffbd1b624",
                getDate(),
                serial
        );
        List<Reading> readingsList = Arrays.asList(reading);
        ReadingsRequest readingsRequest = new ReadingsRequest(readingsList);

        Call<Object> call = ApiClient.getUserService(Constant.BASE_URL_BGM,token, androidId).sendReadings(readingsRequest);
        call.enqueue(new Callback<Object>() {
            @Override
            public void onResponse(Call<Object> call, Response<Object> response) {
                if (response.isSuccessful()) {
                    databaseClient.getAppDatabase().bpJumperDao().updateStatus(id, 0);
                }
            }

            @Override
            public void onFailure(Call<Object> call, Throwable t) {
                // Request failed
                // Handle failure
                Log.d(TAG, "Fail "+t);
            }
        });
    }

    public String getDate() {
        // Get the current date and time
        Calendar calendar = Calendar.getInstance();
        Date currentDate = calendar.getTime();

        // Set the desired time zone offset (e.g., +08:00)
        TimeZone timeZone = TimeZone.getTimeZone("GMT+08:00");
        calendar.setTimeZone(timeZone);

        // Format the date and time
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss");
        sdf.setTimeZone(timeZone);
        String formattedDate = sdf.format(currentDate);

        // Get the offset in hours and minutes
        int offsetMillis = timeZone.getOffset(currentDate.getTime());
        int offsetHours = offsetMillis / (60 * 60 * 1000);
        int offsetMinutes = Math.abs((offsetMillis / (60 * 1000)) % 60);

        // Format the offset
        // String offset = String.format("%s%02d:%02d", offsetHours >= 0 ? "+" : "-", Math.abs(offsetHours), offsetMinutes);


        // Combine formatted date and offset
        String finalFormattedDate = formattedDate + "+00:00";
        return finalFormattedDate;

    }

    public void sendGlucose(long glucose, String serial, long id) {
        // Usage example in your activity or service
        token = "bNWZsV#BeZvaNb*gF@3Z^7tCNhCT29Vw8Vi%4T%";
        Reading reading = new Reading(
                false,
                "Asia/Manila",
                "5be9a0e03d320b73e5f7aa71",
                Arrays.asList((double)glucose),
                "5e4c0db6bc20236a64ca3467",
                "5bb306382598931ffbd1b623",
                getDate(),
                serial
        );
        List<Reading> readingsList = Arrays.asList(reading);
        ReadingsRequest readingsRequest = new ReadingsRequest(readingsList);

        Call<Object> call = ApiClient.getUserService(Constant.BASE_URL_BGM,token, androidId).sendReadings(readingsRequest);
        call.enqueue(new Callback<Object>() {
            @Override
            public void onResponse(Call<Object> call, Response<Object> response) {
                if (response.isSuccessful()) {
                    databaseClient.getAppDatabase().readingValueDao().updateStatus(id, 0);
                }
            }

            @Override
            public void onFailure(Call<Object> call, Throwable t) {
                // Request failed
                // Handle failure

            }
        });
    }

    public void sendTemperature(double temperature, String serial, long id) {
        Log.d(TAG, "sending temperature "+ temperature);
        // Usage example in your activity or service
        token = "bNWZsV#BeZvaNb*gF@3Z^7tCNhCT29Vw8Vi%4T%";
        Reading reading = new Reading(
                false,
                "Asia/Manila",
                "jtm00025b94050c",
                Arrays.asList(temperature),
                "5bc3cb14cba82b066cae7bc1",
                "5bb306382598931ffbd1b628",
                getDate(),
                serial
        );
        List<Reading> readingsList = Arrays.asList(reading);
        ReadingsRequest readingsRequest = new ReadingsRequest(readingsList);

        Call<Object> call = ApiClient.getUserService(Constant.BASE_URL_BGM,token, androidId).sendReadings(readingsRequest);
        call.enqueue(new Callback<Object>() {
            @Override
            public void onResponse(Call<Object> call, Response<Object> response) {

                if (response.isSuccessful()) {
                    // Request successful
                    databaseClient.getAppDatabase().temperatureDao().updateStatus(id, 0);
                }
            }

            @Override
            public void onFailure(Call<Object> call, Throwable t) {
                // Request failed
            }
        });
    }

    public void sendOximeter(double pulseRate, double oxygen, String serial, long id) {
        // Usage example in your activity or service
        Log.d(TAG, "Eximeter serial: "+serial);
        token = "bNWZsV#BeZvaNb*gF@3Z^7tCNhCT29Vw8Vi%4T%";
        Reading reading = new Reading(
                false,
                "Asia/Manila",
                serial,
                Arrays.asList(pulseRate, oxygen),
                "5bc3cb14cba82b066cae7bc2",
                "5bb306382598931ffbd1b626",
                getDate(),
                serial
        );
        List<Reading> readingsList = Arrays.asList(reading);
        ReadingsRequest readingsRequest = new ReadingsRequest(readingsList);

        Call<Object> call = ApiClient.getUserService(Constant.BASE_URL_BGM,token, androidId).sendReadings(readingsRequest);
        call.enqueue(new Callback<Object>() {
            @Override
            public void onResponse(Call<Object> call, Response<Object> response) {
                Log.d(TAG, "this is the result "+ response.code());
                Log.d(TAG, "this is the result "+ response.body());
                Log.d(TAG, "this is the result "+ response.toString());
                Log.d(TAG, "this is the result "+ response.message());
                if (response.isSuccessful()) {
                    // Request successful
                    // Handle response if needed
                    //saveTemperatureData(temperature);
                    databaseClient.getAppDatabase().oximeterDao().updateStatus(id, 0);
                }
            }

            @Override
            public void onFailure(Call<Object> call, Throwable t) {
                // Request failed
            }
        });
    }

    public void sendWeightScale(double weight, String serial, long id) {
        // Usage example in your activity or service
        Log.d(TAG, "Eximeter serial: " + serial);
        token = "bNWZsV#BeZvaNb*gF@3Z^7tCNhCT29Vw8Vi%4T%";
        Reading reading = new Reading(
                false,
                "Asia/Manila",
                serial,
                Arrays.asList(weight),
                "5d2cac72ed5d7122d4044f0f",
                "5bb306382598931ffbd1b625",
                getDate(),
                serial
        );
        List<Reading> readingsList = Arrays.asList(reading);
        ReadingsRequest readingsRequest = new ReadingsRequest(readingsList);

        Call<Object> call = ApiClient.getUserService(Constant.BASE_URL_BGM, token, androidId).sendReadings(readingsRequest);
        call.enqueue(new Callback<Object>() {
            @Override
            public void onResponse(Call<Object> call, Response<Object> response) {
                Log.d(TAG, "this is the result " + response.code());
                Log.d(TAG, "this is the result " + response.body());
                Log.d(TAG, "this is the result " + response.toString());
                Log.d(TAG, "this is the result " + response.message());
                if (response.isSuccessful()) {
                    databaseClient.getAppDatabase().weighingScaleDao().updateStatus(id, 0);
                }
            }

            @Override
            public void onFailure(Call<Object> call, Throwable t) {
                // Request failed
                Log.d(TAG, "Error Failure!..");
            }
        });
    }


    private void syncBloodPressureData() {

        BPJumperDao dao = databaseClient.getAppDatabase().bpJumperDao();
        List<BPJumper> heartRateEntities = dao.getAllBPJumper();
        List<Reading> readingsList = new ArrayList<>();
        for (BPJumper bp : heartRateEntities) {
            Reading reading = new Reading();            // NEW object each iteration
            reading.setManual(false);
            reading.setTimezone("Asia/Manila");
            reading.setSource("jtm00025b94050c");
            reading.setValue(Arrays.asList(
                    (double) bp.getSystolic(),
                    (double) bp.getDiastolic(),
                    (double) bp.getPulseRate()
            ));
            reading.setDevice("66437be266c8833a1c42d7aa");
            reading.setReadingType("5bb306382598931ffbd1b624");
            //reading.setDate(DateUtils.getDate());       // or bp.getCreatedAt() if you want per-row time
            reading.setDate(DateUtils.toIso8601Manila(bp.getCreatedAt()));
            reading.setSerial("jtm00025b94050c");
            readingsList.add(reading);                  // append, don't replace
        }

// set once, after the loop
        ReadingsRequest readingsRequest = new ReadingsRequest();
        readingsRequest.setReadings(readingsList);
        if(!readingsRequest.getReadings().isEmpty()) {
            sendBPJumper(readingsRequest);
        }


    }

    public void sendBPJumper(ReadingsRequest readingsRequest) {
        Log.d(TAG, "Success : sending the BP");

        Call<Object> call = ApiClient.getUserService(Constant.BASE_URL_BGM,Constant.TOKEN_DR_WATCH_API, androidId).sendReadings(readingsRequest);
        call.enqueue(new Callback<Object>() {
            @Override
            public void onResponse(Call<Object> call, Response<Object> response) {
                Log.d(TAG, "Bulk Success : "+response);
                Log.d(TAG, "Bulk Success : "+response.code());
                Log.d(TAG, "Bulk Success : "+response.message());
                Log.d(TAG, "Bulk Success : "+response.toString());
                BPJumperDao dao = databaseClient.getAppDatabase().bpJumperDao();
                dao.deleteAll();

            }

            @Override
            public void onFailure(Call<Object> call, Throwable t) {
                Log.d(TAG, "Bulk error : "+t.toString());
            }
        });
    }

    @SuppressLint("WrongConstant")
    private void syncSpo2() {

        OximeterDao dao = databaseClient.getAppDatabase().oximeterDao();
        List<Oximeter> oximeterEntities = dao.getAllOximeterByStatus(1);
        Log.d("Oximeter----", String.valueOf(oximeterEntities.size()));
        List<Reading> readingsList = new ArrayList<>();

        for (Oximeter bp : oximeterEntities) {
            Log.d("Oximeter----", bp.getPulseRate() + "");

            Reading reading = new Reading();            // NEW object each iteration
            reading.setManual(false);
            reading.setTimezone("Asia/Manila");
            reading.setSource(bp.getSerial());
            reading.setValue(Arrays.asList(
                    (double) bp.getPulseRate(),
                    (double) bp.getOxygen()
            ));
            reading.setDevice(Constant.DEVICE_OXIMETER);
            reading.setReadingType("5bb306382598931ffbd1b626");
            reading.setDate(DateUtils.toIso8601Manila(bp.getCreatedAt()));
            reading.setSerial(bp.getSerial());
            readingsList.add(reading);                  // append, don't replace
        }

// set once, after the loop
        ReadingsRequest readingsRequest = new ReadingsRequest();
        readingsRequest.setReadings(readingsList);
        if(!readingsRequest.getReadings().isEmpty()) {
            sendOximeterServer(readingsRequest);
        }
    }
    public void sendOximeterServer(ReadingsRequest readingsRequest) {
        Log.d(TAG, "Success : sending the BP");

        Call<Object> call = ApiClient.getUserService(Constant.BASE_URL_BGM,Constant.TOKEN_DR_WATCH_API, androidId).sendReadings(readingsRequest);
        call.enqueue(new Callback<Object>() {
            @Override
            public void onResponse(Call<Object> call, Response<Object> response) {
                Log.d(TAG, "Bulk Success : "+response);
                Log.d(TAG, "Bulk Success : "+response.code());
                Log.d(TAG, "Bulk Success : "+response.message());
                OximeterDao dao = databaseClient.getAppDatabase().oximeterDao();
                dao.deleteAll();
            }

            @Override
            public void onFailure(Call<Object> call, Throwable t) {
                Log.d(TAG, "Bulk error : "+t.toString());
            }
        });
    }

    @SuppressLint("WrongConstant")
    private void syncTemperature() {


        TemperatureDao dao = databaseClient.getAppDatabase().temperatureDao();
        List<Temperature> temperatureEntities = dao.getAllTemperature();
        List<Reading> readingsList = new ArrayList<>();

        for (Temperature bp : temperatureEntities) {
            Log.d("temperature----", bp.getTemperature() + "");

            Reading reading = new Reading();            // NEW object each iteration
            reading.setManual(false);
            reading.setTimezone("Asia/Manila");
            reading.setSource("jtm00025b94050c");
            reading.setValue(Arrays.asList(
                    bp.getTemperature()
            ));
            reading.setDevice(Constant.DEVICE_TEMPERATURE);
            reading.setReadingType("5bb306382598931ffbd1b628");
            reading.setDate(DateUtils.toIso8601Manila(bp.getCreatedAt()));
            reading.setSerial("5bc3cb14cba82b066cae7bc1");
            readingsList.add(reading);                  // append, don't replace
        }

// set once, after the loop
        ReadingsRequest readingsRequest = new ReadingsRequest();
        readingsRequest.setReadings(readingsList);
        if(!readingsRequest.getReadings().isEmpty()) {
            sendStepServer(readingsRequest);
        }
    }
    public void sendStepServer(ReadingsRequest readingsRequest) {
        Log.d(TAG, "Success : sending the BP");

        Call<Object> call = ApiClient.getUserService(Constant.BASE_URL_BGM,Constant.TOKEN_DR_WATCH_API, androidId).sendReadings(readingsRequest);
        call.enqueue(new Callback<Object>() {
            @Override
            public void onResponse(Call<Object> call, Response<Object> response) {
                Log.d(TAG, "Bulk Success : "+response);
                Log.d(TAG, "Bulk Success : "+response.code());
                Log.d(TAG, "Bulk Success : "+response.message());
                TemperatureDao dao  = databaseClient.getAppDatabase().temperatureDao();
                dao.deleteAll();
            }

            @Override
            public void onFailure(Call<Object> call, Throwable t) {
                Log.d(TAG, "Bulk error : "+t.toString());
            }
        });
    }

    public void startFetchingSteps(){
        HealthManager.getInstance(this).getSteps(new HealthManager.ValueCallback<Integer>() {
            @Override
            public void onValue(Integer steps) {
                if (steps == null || steps <= 0) return;

                SharedPreferences prefs = getSharedPreferences("steps_prefs", MODE_PRIVATE);
                int lastStepCount = prefs.getInt("last_step_count", 0);

                int delta;
                if (steps < lastStepCount) {
                    // Device restarted â€” step counter was reset to zero
                    delta = steps;
                } else {
                    delta = steps - lastStepCount;
                }

                if (delta > 0) {
                    prefs.edit().putInt("last_step_count", steps).apply();
                    sendStepsSync(delta);
                }
            }

            @Override
            public void onError(String error) {

            }
        });
    }

    private void sendStepsSync(Integer steps) {
        Log.d(TAG, "Sending steps synchronously: " + steps);
        try {
            String token = "bNWZsV#BeZvaNb*gF@3Z^7tCNhCT29Vw8Vi%4T%";

            Reading reading = new Reading(
                    false,
                    "Asia/Manila",
                    "jtm00025b94050c",
                    // API expects a list; here we send [steps]
                    Arrays.asList((double) steps),
                    "66437be266c8833a1c42d7aa",
                    "5bb306382598931ffbd1b629",
                    getTodayDate(),
                    DeviceUtils.getIMEI(this)
            );

            ReadingsRequest payload = new ReadingsRequest(List.of(reading));

            ApiClient.getUserService(Constant.BASE_URL_BGM, token, DeviceUtils.getIMEI(this))
                    .sendReadings(payload)
                    .enqueue(new Callback<Object>() {
                        @Override
                        public void onResponse(Call<Object> call, Response<Object> response) {
                            if (response.isSuccessful()) {
                                Log.d(TAG, "âœ… Steps data sent successfully");
                            } else {
                                Log.e(TAG, "âŒ Server returned error: " + response.code() + " - " + response.message());
                            }
                        }

                        @Override
                        public void onFailure(Call<Object> call, Throwable t) {
                            Log.e(TAG, "âŒ Sync failed", t);
                        }
                    });

        } catch (Exception e) {
            Log.e(TAG, "âŒ Exception during server sync", e);
        }
    }

    public void syncGlucose() {

        List<ReadingValue> readingValueList =  databaseClient.getAppDatabase().readingValueDao().getAllReadingValues();
        if(readingValueList != null && !readingValueList.isEmpty()) {
            for (ReadingValue readingValue: readingValueList
            ) {
                sendGlucose(readingValue.getGlucose(), readingValue.getMailValue(), readingValue.getId());
            }
        }
    }


    public void sendGlucose(long glucose, int mailValue, long id) {
        // Usage example in your activity or service

        Log.d(TAG, "The data of glucose Reading list count : "+glucose);

        String serial = PreferenceHelper.getInstance(this).getString("EMPECS", "");
        Reading reading = new Reading(
                false,
                "Asia/Manila",
                "5be9a0e03d320b73e5f7aa71",
                Arrays.asList((double)glucose, (double)mailValue),
                "5e4c0db6bc20236a64ca3467",
                "5bb306382598931ffbd1b623",
                DateUtils.getDate(),
                serial
        );
        List<Reading> readingsList = Arrays.asList(reading);
        ReadingsRequest readingsRequest = new ReadingsRequest(readingsList);

        Call<Object> call = ApiClient.getUserService(Constant.BASE_URL_BGM,"bNWZsV#BeZvaNb*gF@3Z^7tCNhCT29Vw8Vi%4T%", DeviceUtils.getIMEI(this)).sendReadings(readingsRequest);
        call.enqueue(new Callback<Object>() {
            @Override
            public void onResponse(Call<Object> call, Response<Object> response) {

                if (response.isSuccessful()) {
                    // Request successful
                    // Handle response if needed
                    //saveData(glucose);
                    databaseClient.getAppDatabase().readingValueDao().deleteById(id);


                }
            }

            @Override
            public void onFailure(Call<Object> call, Throwable t) {
                // Request failed
                // Handle failure

            }
        });
    }


}
