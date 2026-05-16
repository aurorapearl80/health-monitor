package com.monitor.health.services;

import static com.monitor.health.Constant.ACTION_HEALTH_UPDATE;
import static com.monitor.health.utility.AppUtils.getTodayDate;

import android.annotation.SuppressLint;
import android.app.HSystemAssistManager;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ServiceInfo;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Build;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.util.Log;

import androidx.annotation.Nullable;
import androidx.core.app.NotificationCompat;
import androidx.core.content.ContextCompat;

import com.monitor.health.ApiClient;
import com.monitor.health.Constant;
import com.monitor.health.NetworkUtils;
import com.monitor.health.receiver.HourlyKickReceiver;
import com.monitor.health.ReadingsRequest;
import com.monitor.health.database.DatabaseClient;
import com.monitor.health.model.BPJumper;
import com.monitor.health.model.HeartRateEntity;
import com.monitor.health.model.Oximeter;
import com.monitor.health.model.StepEntity;
import com.monitor.health.model.Reading;
import com.monitor.health.utility.BPReading;
import com.monitor.health.utility.BloodPressureEstimator;
import com.monitor.health.utility.DateUtils;
import com.monitor.health.utility.DeviceUtils;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.TimeZone;
import java.util.concurrent.atomic.AtomicInteger;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class TestService extends Service {

    private static final String TAG = "TestService";
    private static final int NOTIF_ID = 44;
    private static final String NOTIF_CH_ID = "health_upload";
    private static final int TYPE_HEART_RATE = 21;

    private HSystemAssistManager systemAssistManager;
    private SensorManager mSensorManager;
    private Sensor mSensor;

    private int heartRateValue = 0;
    private int bloodRateValue = 0;

    private final Handler handler = new Handler(Looper.getMainLooper());
    private Runnable stopAndUploadRunnable;

    // NEW: track when all uploads complete (HR, SpO2, Steps, BP, Sleep) = 5
    private final AtomicInteger pendingUploads = new AtomicInteger(0);
    private final AtomicInteger failedUploads = new AtomicInteger(0);
    private boolean measurementComplete = false;

    private static final long RETRY_INTERVAL_MS = 5 * 60 * 1000L; // 5 minutes

    private final SensorEventListener mHeartRateListener = new SensorEventListener() {
        @Override public void onSensorChanged(SensorEvent e) {
            int hr = 0;
            if (e.values != null && e.values.length > 0 && !Float.isNaN(e.values[0])) {
                hr = Math.round(e.values[0]);
                if (hr < 25 || hr > 240) hr = 0;
            }
            // Only overwrite with a valid reading â€” do NOT reset to 0 if mode-2
            // sensor events stop reporting HR (e.values[0] == 0 in SpO2 mode).
            if (hr > 0) heartRateValue = hr;

            // SpO2 derivation from [1] or [2]
            Integer spo2 = null;
            if (e.values != null) {
                if (e.values.length > 1 && !Float.isNaN(e.values[1])) {
                    spo2 = Math.round(e.values[1]);
                } else if (e.values.length > 2 && !Float.isNaN(e.values[2])) {
                    int alt = Math.round(e.values[2]);
                    if (alt > 0 && alt != heartRateValue) spo2 = alt;
                }
            }
            if (spo2 != null && spo2 > 0) {
                if (spo2 > 100) spo2 = 100;
                bloodRateValue = spo2;
            }

            int stepsNow = -1;
            try { stepsNow = systemAssistManager.getSetpCount(); } catch (Throwable ignored) {}
            broadcastToUI(heartRateValue, bloodRateValue, stepsNow, /*uploaded=*/false, "live");
        }
        @Override public void onAccuracyChanged(Sensor s, int a) {}
    };

    @Override public void onCreate() {
        super.onCreate();
        startInForeground();
        initSystem();
        enableBothMeasurements();
        registerSensor();

        // give sensors time, then upload
        stopAndUploadRunnable = this::uploadAndStop;
        handler.postDelayed(stopAndUploadRunnable, 75_000);
    }

    // IMPORTANT: sticky so the system restarts us if killed
    @Override public int onStartCommand(Intent intent, int flags, int startId) {
        return START_STICKY;
    }

    @SuppressLint("WrongConstant")
    private void initSystem() {
        systemAssistManager = (HSystemAssistManager) getSystemService("hsystemassist");
        mSensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);
        try { systemAssistManager.isEnableAccelerate(this); } catch (Throwable ignored) {}
    }

    private void enableBothMeasurements() {
        try {
            systemAssistManager.setHeartrateMode(1); // HR â€” needs several seconds to produce a reading
            // Switch to SpO2 mode only once we have a valid HR reading, or after 50 s max.
            // This prevents the 2pm/Doze scenario where mode-1 never fires an event within 30 s,
            // leaving heartRateValue = 0 for the rest of the run.
            scheduleSwitchToMode2(0);
        } catch (Exception e) {
            Log.e(TAG, "Error enabling heart rate mode: " + e.getMessage());
        }
    }

    // Polls every 10 s (up to 50 s total) for a valid HR reading, then switches to SpO2 mode.
    private void scheduleSwitchToMode2(int attemptCount) {
        final int MAX_ATTEMPTS = 5; // 5 Ã— 10 s = 50 s max wait
        handler.postDelayed(() -> {
            if (heartRateValue > 0 || attemptCount >= MAX_ATTEMPTS) {
                try {
                    systemAssistManager.setHeartrateMode(2); // Blood/SpO2
                    Log.d(TAG, "Switched to SpO2 mode (attempt=" + (attemptCount + 1)
                            + ", heartRate=" + heartRateValue + ")");
                } catch (Exception e) {
                    Log.e(TAG, "Error enabling SpO2 mode: " + e.getMessage());
                }
            } else {
                Log.d(TAG, "HR not yet captured (attempt=" + (attemptCount + 1) + "), waitingâ€¦");
                scheduleSwitchToMode2(attemptCount + 1);
            }
        }, 10_000);
    }

    private void registerSensor() {
        mSensor = mSensorManager.getDefaultSensor(TYPE_HEART_RATE);
        if (mSensor != null) {
            mSensorManager.registerListener(mHeartRateListener, mSensor, SensorManager.SENSOR_DELAY_NORMAL);
        } else {
            Log.w(TAG, "No HR sensor; will still upload placeholder values.");
        }
    }

    private void startInForeground() {
        // Android 14+ enforces that foregroundServiceType=health requires at least one
        // prerequisite runtime permission (ACTIVITY_RECOGNITION, health.READ_HEART_RATE, etc.).
        // After clearing app data, these are revoked — stop gracefully instead of crashing.
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q &&
                ContextCompat.checkSelfPermission(this, android.Manifest.permission.ACTIVITY_RECOGNITION)
                        != PackageManager.PERMISSION_GRANTED) {
            Log.w(TAG, "ACTIVITY_RECOGNITION not granted — skipping TestService startup");
            stopSelf();
            return;
        }

        NotificationManager nm = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
        NotificationChannel ch = new NotificationChannel(
                NOTIF_CH_ID, "Health Uploads", NotificationManager.IMPORTANCE_LOW);
        nm.createNotificationChannel(ch);

        Notification notif = new NotificationCompat.Builder(this, NOTIF_CH_ID)
                .setContentTitle("Health sync running")
                .setContentText("Collecting & uploading data")
                .setSmallIcon(android.R.drawable.stat_sys_upload)
                .setOngoing(true)
                .build();

        try {
            // Android 14+ (API 34) requires the service type in the startForeground() call
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
                startForeground(NOTIF_ID, notif, ServiceInfo.FOREGROUND_SERVICE_TYPE_HEALTH);
            } else {
                startForeground(NOTIF_ID, notif);
            }
        } catch (SecurityException e) {
            Log.e(TAG, "startForeground failed — prerequisite permission not granted", e);
            stopSelf();
        }
    }

    private void uploadAndStop() {
        measurementComplete = true;
        try { mSensorManager.unregisterListener(mHeartRateListener); } catch (Throwable ignored) {}

        int steps = -1;
        try { steps = systemAssistManager.getSetpCount(); } catch (Throwable ignored) {}

        // 4 live measurements (HR, SpO2, Steps, BP) + 1 DB flush (Steps only).
        // sendSleepSync removed: it sent [0.0, heartRateValue] with the same readingType as
        // sendHeartRateSync (b626), causing a duplicate b626 entry every hour on the server.
        // HR/SpO2/BP send*Sync already bundle their own offline DB records in the same batch,
        // so syncStoredHeartRate / syncStoredSpo2 / syncStoredBloodPressure are not called here.
        pendingUploads.set(5);
        failedUploads.set(0);

        try {
            sendHeartRateSync();
            sendBloodOxygenSync();
            sendStepSync(steps);
            sendBloodPressureSync();
            syncStoredSteps();
        } catch (Exception e) {
            Log.e(TAG, "âŒ Exception during server sync", e);
            // Fail fast: mark all remaining as done so we don't hang.
            while (pendingUploads.getAndDecrement() > 0) {}
            checkAndStopService();
        }
    }

    private void checkAndStopService() {
        if (measurementComplete && pendingUploads.get() <= 0) {
            if (failedUploads.get() > 0) {
                Log.d(TAG, failedUploads.get() + " upload(s) saved locally â€” scheduling retry in 5 min");
                HourlyKickReceiver.scheduleRetry(getApplicationContext(), RETRY_INTERVAL_MS);
            } else {
                // Always re-arm the normal hourly alarm here as well.
                // onReceive already set the alarm at the start of this run, but anything
                // between then and now (OS memory pressure, doze, permission change) could
                // have silently cleared it.  Calling scheduleNext again is harmless and
                // guarantees the hourly chain never breaks after a successful sync.
                Log.d(TAG, "All uploads sent to server â€” re-arming next hourly run");
                HourlyKickReceiver.scheduleNext(getApplicationContext());
            }
            handler.postDelayed(() -> {
                stopForeground(true);
                stopSelf();
            }, 500);
        }
    }

    private void onUploadComplete() {
        int left = pendingUploads.decrementAndGet();
        Log.d(TAG, "Upload done. Remaining: " + Math.max(left, 0));
        checkAndStopService();
    }

    private void broadcastToUI(int heart, int blood, int steps, boolean uploaded, @Nullable String note) {
        Intent i = new Intent(ACTION_HEALTH_UPDATE);
        i.putExtra("heartRate", heart);
        i.putExtra("bloodRate", blood);
        i.putExtra("steps", steps);
        i.putExtra("uploaded", uploaded);
        if (note != null) i.putExtra("note", note);
        sendBroadcast(i);
    }

    // === Individual uploads ===
    private void sendHeartRateSync() {
        if (heartRateValue == 0) { onUploadComplete(); return; }

        if (!NetworkUtils.isInternetConnected(getApplicationContext())) {
            Log.d(TAG, "No internet â€” saving heart rate locally: " + heartRateValue);
            HeartRateEntity entity = new HeartRateEntity(heartRateValue);
            entity.setStatus(1);
            DatabaseClient.getInstance(getApplicationContext())
                    .getAppDatabase().heartRateDao()
                    .insertHeartRate(entity);
            failedUploads.incrementAndGet();
            onUploadComplete();
            return;
        }

        List<HeartRateEntity> stored = DatabaseClient.getInstance(getApplicationContext())
                .getAppDatabase().heartRateDao().getAllHeartRateActive();

        String deviceTz = TimeZone.getDefault().getID();
        List<Reading> readingsList = new ArrayList<>();
        if (stored != null) {
            for (HeartRateEntity e : stored) {
                Reading r = new Reading();
                r.setManual(false);
                r.setTimezone(deviceTz);
                r.setSource("jtm00025b94050c");
                r.setValue(Arrays.asList(0.0, e.getValue()));
                r.setDevice("66437be266c8833a1c42d7aa");
                r.setReadingType("5bb306382598931ffbd1b626");
                r.setDate(DateUtils.toIso8601(e.getCreatedAt(), deviceTz));
                r.setSerial("jtm00025b94050c");
                readingsList.add(r);
            }
        }

        List<Double> vals = new ArrayList<>(2);
        vals.add(0.0);
        vals.add((double) heartRateValue);
        readingsList.add(new Reading(
                false, deviceTz, "jtm00025b94050c",
                vals, "66437be266c8833a1c42d7aa",
                "5bb306382598931ffbd1b626",
                getTodayDate(),
                DeviceUtils.getIMEI(getApplicationContext())
        ));

        ReadingsRequest req = new ReadingsRequest(readingsList);

        ApiClient.getUserService(Constant.BASE_URL_BGM, Constant.TOKEN_DR_WATCH_API,
                        DeviceUtils.getIMEI(getApplicationContext()))
                .sendReadings(req)
                .enqueue(new Callback<Object>() {
                    @Override public void onResponse(Call<Object> call, Response<Object> resp) {
                        if (resp.isSuccessful()) {
                            DatabaseClient.getInstance(getApplicationContext())
                                    .getAppDatabase().heartRateDao().deleteAllHourly();
                        } else {
                            Log.e(TAG, "âŒ Heart rate server error: " + resp.code());
                            failedUploads.incrementAndGet();
                        }
                        onUploadComplete();
                    }
                    @Override public void onFailure(Call<Object> call, Throwable t) {
                        Log.e(TAG, "âŒ Heart rate upload failed", t);
                        failedUploads.incrementAndGet();
                        onUploadComplete();
                    }
                });
    }

    private void sendBloodOxygenSync() {
        if (bloodRateValue == 0) { onUploadComplete(); return; }

        if (!NetworkUtils.isInternetConnected(getApplicationContext())) {
            Log.d(TAG, "No internet â€” saving blood oxygen locally: " + bloodRateValue);
            DatabaseClient.getInstance(getApplicationContext())
                    .getAppDatabase().oximeterDao()
                    .insertOximeter(new Oximeter(0, bloodRateValue, 0,
                            DeviceUtils.getIMEI(getApplicationContext())));
            failedUploads.incrementAndGet();
            onUploadComplete();
            return;
        }

        List<Oximeter> stored = DatabaseClient.getInstance(getApplicationContext())
                .getAppDatabase().oximeterDao().getAllOximeterActive();

        List<Reading> readingsList = new ArrayList<>();
        if (stored != null) {
            for (Oximeter e : stored) {
                Reading r = new Reading();
                r.setManual(false);
                r.setTimezone("Asia/Manila");
                r.setSource("jtm00025b94050c");
                r.setValue(Arrays.asList(0.0, (double) e.getOxygen()));
                r.setDevice("66437be266c8833a1c42d7aa");
                r.setReadingType("5bb306382598931ffbd1b626");
                r.setDate(DateUtils.toIso8601Manila(e.getCreatedAt()));
                r.setSerial("jtm00025b94050c");
                readingsList.add(r);
            }
        }

        readingsList.add(new Reading(
                false, "Asia/Manila", "jtm00025b94050c",
                Arrays.asList(0.0, (double) bloodRateValue),
                "66437be266c8833a1c42d7aa",
                "5bb306382598931ffbd1b626",
                getTodayDate(),
                DeviceUtils.getIMEI(getApplicationContext())
        ));

        ReadingsRequest req = new ReadingsRequest(readingsList);

        ApiClient.getUserService(Constant.BASE_URL_BGM, Constant.TOKEN_DR_WATCH_API,
                        DeviceUtils.getIMEI(getApplicationContext()))
                .sendReadings(req)
                .enqueue(new Callback<Object>() {
                    @Override public void onResponse(Call<Object> call, Response<Object> resp) {
                        if (resp.isSuccessful()) {
                            // deleteAll() removes status=0 records â€” the ones we actually fetched and sent.
                            // (deleteAllHourly() removes status=1 records, which is wrong here.)
                            DatabaseClient.getInstance(getApplicationContext())
                                    .getAppDatabase().oximeterDao().deleteAll();
                        } else {
                            Log.e(TAG, "âŒ SpO2 server error: " + resp.code());
                            failedUploads.incrementAndGet();
                        }
                        onUploadComplete();
                    }
                    @Override public void onFailure(Call<Object> call, Throwable t) {
                        Log.e(TAG, "âŒ SpO2 upload failed", t);
                        failedUploads.incrementAndGet();
                        onUploadComplete();
                    }
                });
    }

    private void sendBloodPressureSync() {
        if (heartRateValue == 0) { onUploadComplete(); return; }

        BloodPressureEstimator est = new BloodPressureEstimator();
        BPReading bp = est.estimateBloodPressure(heartRateValue);
        int adjustedSystolic = bp.systolic + 10;
        int adjustedDiastolic = bp.diastolic + 5;

        if (!NetworkUtils.isInternetConnected(getApplicationContext())) {
            Log.d(TAG, "No internet â€” saving blood pressure locally: systolic=" + adjustedSystolic + ", diastolic=" + adjustedDiastolic);
            DatabaseClient.getInstance(getApplicationContext())
                    .getAppDatabase().bpJumperDao()
                    .insertBPJumper(new BPJumper(adjustedSystolic, adjustedDiastolic, 0, 0,
                            DeviceUtils.getIMEI(getApplicationContext())));
            failedUploads.incrementAndGet();
            onUploadComplete();
            return;
        }

        List<BPJumper> stored = DatabaseClient.getInstance(getApplicationContext())
                .getAppDatabase().bpJumperDao().getAllBPJumperActive();

        List<Reading> readingsList = new ArrayList<>();
        if (stored != null) {
            for (BPJumper e : stored) {
                Reading r = new Reading();
                r.setManual(false);
                r.setTimezone("Asia/Manila");
                r.setSource("jtm00025b94050c");
                r.setValue(Arrays.asList((double) e.getSystolic(), (double) e.getDiastolic()));
                r.setDevice("66437be266c8833a1c42d7aa");
                r.setReadingType("5bb306382598931ffbd1b624");
                r.setDate(DateUtils.toIso8601Manila(e.getCreatedAt()));
                r.setSerial("jtm00025b94050c");
                readingsList.add(r);
            }
        }

        readingsList.add(new Reading(
                false, "Asia/Manila", "jtm00025b94050c",
                Arrays.asList((double) adjustedSystolic, (double) adjustedDiastolic),
                "66437be266c8833a1c42d7aa",
                "5bb306382598931ffbd1b624",
                getTodayDate(),
                DeviceUtils.getIMEI(getApplicationContext())
        ));

        ReadingsRequest req = new ReadingsRequest(readingsList);

        ApiClient.getUserService(Constant.BASE_URL_BGM, Constant.TOKEN_DR_WATCH_API,
                        DeviceUtils.getIMEI(getApplicationContext()))
                .sendReadings(req)
                .enqueue(new Callback<Object>() {
                    @Override public void onResponse(Call<Object> call, Response<Object> resp) {
                        if (resp.isSuccessful()) {
                            DatabaseClient.getInstance(getApplicationContext())
                                    .getAppDatabase().bpJumperDao().deleteAllHourly();
                        } else {
                            Log.e(TAG, "âŒ Blood pressure server error: " + resp.code());
                            failedUploads.incrementAndGet();
                        }
                        onUploadComplete();
                    }
                    @Override public void onFailure(Call<Object> call, Throwable t) {
                        Log.e(TAG, "âŒ Blood pressure upload failed", t);
                        failedUploads.incrementAndGet();
                        onUploadComplete();
                    }
                });
    }

    private void sendStepSync(Integer steps) {
        Log.d(TAG, "Sending steps synchronously: " + steps);
        if (steps == null || steps <= 0) { onUploadComplete(); return; }

        try {
            android.content.SharedPreferences prefs = getApplicationContext()
                    .getSharedPreferences("steps_prefs", android.content.Context.MODE_PRIVATE);
            int lastStepCount = prefs.getInt("last_step_count", 0);

            int delta;
            if (steps < lastStepCount) {
                // Device restarted â€” step counter was reset to zero
                delta = steps;
            } else {
                delta = steps - lastStepCount;
            }

            if (delta <= 0) { onUploadComplete(); return; }

            prefs.edit().putInt("last_step_count", steps).apply();

            if (!NetworkUtils.isInternetConnected(getApplicationContext())) {
                Log.d(TAG, "No internet â€” saving steps locally: delta=" + delta);
                DatabaseClient.getInstance(getApplicationContext())
                        .getAppDatabase().stepDao()
                        .insertStep(new StepEntity(delta));
                failedUploads.incrementAndGet();
                onUploadComplete();
                return;
            }

            List<Double> vals = Arrays.asList((double) delta);

            Reading reading = new Reading(
                    false, "Asia/Manila", "jtm00025b94050c",
                    vals, "66437be266c8833a1c42d7aa",
                    "5bb306382598931ffbd1b629",
                    getTodayDate(),
                    DeviceUtils.getIMEI(getApplicationContext())
            );
            ReadingsRequest req = new ReadingsRequest(Arrays.asList(reading));

            ApiClient.getUserService(Constant.BASE_URL_BGM, Constant.TOKEN_DR_WATCH_API,
                            DeviceUtils.getIMEI(getApplicationContext()))
                    .sendReadings(req)
                    .enqueue(new Callback<Object>() {
                        @Override
                        public void onResponse(Call<Object> call, Response<Object> resp) {
                            if (resp.isSuccessful()) {
                                Log.d(TAG, "âœ… Steps data sent successfully");
                            } else {
                                Log.e(TAG, "âŒ Server returned error: " + resp.code() + " - " + resp.message());
                            }
                            onUploadComplete();
                        }

                        @Override
                        public void onFailure(Call<Object> call, Throwable t) {
                            Log.e(TAG, "âŒ Sync failed", t);
                            onUploadComplete();
                        }
                    });
        } catch (Exception e) {
            Log.e(TAG, "âŒ Exception during server sync", e);
            onUploadComplete();
        }
    }

    private void sendSleepSync() {
        if (heartRateValue == 0) { onUploadComplete(); return; }

        List<Double> hr = Arrays.asList(0.0, (double) heartRateValue);
        Reading reading = new Reading(
                false, "Asia/Manila", "jtm00025b94050c",
                hr, "66437be266c8833a1c42d7aa",
                "5bb306382598931ffbd1b626",
                getTodayDate(),
                DeviceUtils.getIMEI(getApplicationContext())
        );
        ReadingsRequest req = new ReadingsRequest(Arrays.asList(reading));

        ApiClient.getUserService(Constant.BASE_URL_BGM, Constant.TOKEN_DR_WATCH_API,
                        DeviceUtils.getIMEI(getApplicationContext()))
                .sendReadings(req)
                .enqueue(new Callback<Object>() {
                    @Override public void onResponse(Call<Object> call, Response<Object> resp) { onUploadComplete(); }
                    @Override public void onFailure(Call<Object> call, Throwable t) { onUploadComplete(); }
                });
    }

    // === DB flush: send previously saved offline data when connection is available ===

    private void syncStoredHeartRate() {
        if (!NetworkUtils.isInternetConnected(getApplicationContext())) { onUploadComplete(); return; }

        // Collect ALL pending heart rate records regardless of status:
        // status=0 comes from ECG/HeartRateService offline saves,
        // status=1 comes from TestService offline saves (sendHeartRateSync bail path).
        List<HeartRateEntity> entities0 = DatabaseClient.getInstance(getApplicationContext())
                .getAppDatabase().heartRateDao().getAllHeartRate();
        List<HeartRateEntity> entities1 = DatabaseClient.getInstance(getApplicationContext())
                .getAppDatabase().heartRateDao().getAllHeartRateActive();

        List<HeartRateEntity> entities = new ArrayList<>();
        if (entities0 != null) entities.addAll(entities0);
        if (entities1 != null) entities.addAll(entities1);

        if (entities.isEmpty()) { onUploadComplete(); return; }

        List<Reading> readingsList = new ArrayList<>();
        for (HeartRateEntity e : entities) {
            Reading r = new Reading();
            r.setManual(false);
            r.setTimezone("Asia/Manila");
            r.setSource("jtm00025b94050c");
            r.setValue(Arrays.asList(0.0, e.getValue()));
            r.setDevice("66437be266c8833a1c42d7aa");
            r.setReadingType("5bb306382598931ffbd1b626");
            r.setDate(DateUtils.toIso8601Manila(e.getCreatedAt()));
            r.setSerial("jtm00025b94050c");
            readingsList.add(r);
        }
        ReadingsRequest req = new ReadingsRequest(readingsList);
        ApiClient.getUserService(Constant.BASE_URL_BGM, Constant.TOKEN_DR_WATCH_API,
                        DeviceUtils.getIMEI(getApplicationContext()))
                .sendReadings(req)
                .enqueue(new Callback<Object>() {
                    @Override public void onResponse(Call<Object> call, Response<Object> resp) {
                        DatabaseClient.getInstance(getApplicationContext())
                                .getAppDatabase().heartRateDao().deleteAll();
                        DatabaseClient.getInstance(getApplicationContext())
                                .getAppDatabase().heartRateDao().deleteAllHourly();
                        Log.d(TAG, "âœ… Stored heart rate synced and cleared");
                        onUploadComplete();
                    }
                    @Override public void onFailure(Call<Object> call, Throwable t) {
                        Log.e(TAG, "âŒ Stored heart rate sync failed", t);
                        onUploadComplete();
                    }
                });
    }

    private void syncStoredSpo2() {
        if (!NetworkUtils.isInternetConnected(getApplicationContext())) { onUploadComplete(); return; }

        // Collect ALL pending SpO2 records regardless of status:
        // status=0 comes from TestService offline saves (sendBloodOxygenSync bail path),
        // status=1 comes from BloodOxygenFragment offline saves.
        List<Oximeter> entities0 = DatabaseClient.getInstance(getApplicationContext())
                .getAppDatabase().oximeterDao().getAllOximeterActive();
        List<Oximeter> entities1 = DatabaseClient.getInstance(getApplicationContext())
                .getAppDatabase().oximeterDao().getAllOximeterByStatus(1);

        List<Oximeter> entities = new ArrayList<>();
        if (entities0 != null) entities.addAll(entities0);
        if (entities1 != null) entities.addAll(entities1);

        if (entities.isEmpty()) { onUploadComplete(); return; }

        List<Reading> readingsList = new ArrayList<>();
        for (Oximeter e : entities) {
            Reading r = new Reading();
            r.setManual(false);
            r.setTimezone("Asia/Manila");
            r.setSource(e.getSerial());
            r.setValue(Arrays.asList((double) e.getPulseRate(), (double) e.getOxygen()));
            r.setDevice(Constant.DEVICE_OXIMETER);
            r.setReadingType("5bb306382598931ffbd1b626");
            r.setDate(DateUtils.toIso8601Manila(e.getCreatedAt()));
            r.setSerial(e.getSerial());
            readingsList.add(r);
        }
        ReadingsRequest req = new ReadingsRequest(readingsList);
        ApiClient.getUserService(Constant.BASE_URL_BGM, Constant.TOKEN_DR_WATCH_API,
                        DeviceUtils.getIMEI(getApplicationContext()))
                .sendReadings(req)
                .enqueue(new Callback<Object>() {
                    @Override public void onResponse(Call<Object> call, Response<Object> resp) {
                        DatabaseClient.getInstance(getApplicationContext())
                                .getAppDatabase().oximeterDao().deleteAll();
                        DatabaseClient.getInstance(getApplicationContext())
                                .getAppDatabase().oximeterDao().deleteAllHourly();
                        Log.d(TAG, "âœ… Stored SpO2 synced and cleared");
                        onUploadComplete();
                    }
                    @Override public void onFailure(Call<Object> call, Throwable t) {
                        Log.e(TAG, "âŒ Stored SpO2 sync failed", t);
                        onUploadComplete();
                    }
                });
    }

    private void syncStoredBloodPressure() {
        if (!NetworkUtils.isInternetConnected(getApplicationContext())) { onUploadComplete(); return; }
        List<BPJumper> entities = DatabaseClient.getInstance(getApplicationContext())
                .getAppDatabase().bpJumperDao().getAllBPJumper();
        if (entities == null || entities.isEmpty()) { onUploadComplete(); return; }

        List<Reading> readingsList = new ArrayList<>();
        for (BPJumper e : entities) {
            Reading r = new Reading();
            r.setManual(false);
            r.setTimezone("Asia/Manila");
            r.setSource("jtm00025b94050c");
            r.setValue(Arrays.asList((double) e.getSystolic(), (double) e.getDiastolic(), (double) e.getPulseRate()));
            r.setDevice("66437be266c8833a1c42d7aa");
            r.setReadingType("5bb306382598931ffbd1b624");
            r.setDate(DateUtils.toIso8601Manila(e.getCreatedAt()));
            r.setSerial("jtm00025b94050c");
            readingsList.add(r);
        }
        ReadingsRequest req = new ReadingsRequest(readingsList);
        ApiClient.getUserService(Constant.BASE_URL_BGM, Constant.TOKEN_DR_WATCH_API,
                        DeviceUtils.getIMEI(getApplicationContext()))
                .sendReadings(req)
                .enqueue(new Callback<Object>() {
                    @Override public void onResponse(Call<Object> call, Response<Object> resp) {
                        DatabaseClient.getInstance(getApplicationContext())
                                .getAppDatabase().bpJumperDao().deleteAll();
                        Log.d(TAG, "âœ… Stored blood pressure synced and cleared");
                        onUploadComplete();
                    }
                    @Override public void onFailure(Call<Object> call, Throwable t) {
                        Log.e(TAG, "âŒ Stored blood pressure sync failed", t);
                        onUploadComplete();
                    }
                });
    }

    private void syncStoredSteps() {
        if (!NetworkUtils.isInternetConnected(getApplicationContext())) { onUploadComplete(); return; }
        List<StepEntity> entities = DatabaseClient.getInstance(getApplicationContext())
                .getAppDatabase().stepDao().getAllSteps();
        if (entities == null || entities.isEmpty()) { onUploadComplete(); return; }

        List<Reading> readingsList = new ArrayList<>();
        for (StepEntity e : entities) {
            Reading r = new Reading();
            r.setManual(false);
            r.setTimezone("Asia/Manila");
            r.setSource("jtm00025b94050c");
            r.setValue(Arrays.asList((double) e.getDelta()));
            r.setDevice("66437be266c8833a1c42d7aa");
            r.setReadingType("5bb306382598931ffbd1b629");
            r.setDate(DateUtils.toIso8601Manila(e.getCreatedAt()));
            r.setSerial("jtm00025b94050c");
            readingsList.add(r);
        }
        ReadingsRequest req = new ReadingsRequest(readingsList);
        ApiClient.getUserService(Constant.BASE_URL_BGM, Constant.TOKEN_DR_WATCH_API,
                        DeviceUtils.getIMEI(getApplicationContext()))
                .sendReadings(req)
                .enqueue(new Callback<Object>() {
                    @Override public void onResponse(Call<Object> call, Response<Object> resp) {
                        DatabaseClient.getInstance(getApplicationContext())
                                .getAppDatabase().stepDao().deleteAll();
                        Log.d(TAG, "âœ… Stored steps synced and cleared");
                        onUploadComplete();
                    }
                    @Override public void onFailure(Call<Object> call, Throwable t) {
                        Log.e(TAG, "âŒ Stored steps sync failed", t);
                        onUploadComplete();
                    }
                });
    }

    @Override public void onDestroy() {
        super.onDestroy();
        try { mSensorManager.unregisterListener(mHeartRateListener); } catch (Throwable ignored) {}
        if (stopAndUploadRunnable != null) handler.removeCallbacks(stopAndUploadRunnable);
        Log.d(TAG, "Service destroyed");
    }

    @Override public IBinder onBind(Intent intent) { return null; }
}
