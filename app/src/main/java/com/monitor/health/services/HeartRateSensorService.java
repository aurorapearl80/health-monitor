package com.monitor.health.services;

import android.annotation.SuppressLint;
import android.app.HSystemAssistManager;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
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

import com.monitor.health.Constant;
import com.monitor.health.adapter.HeartRateDataManager;
import com.monitor.health.database.DatabaseClient;
import com.monitor.health.entity.HeartRateJarEntity;

import java.util.ArrayList;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.ReentrantLock;

public class HeartRateSensorService extends Service implements HeartRateDataManager.ServerSyncListener {

    private static final String TAG = "HeartRateService";
    private static final int TYPE_HEART_RATE = 21;
    private static final int NOTIFICATION_ID = 1001;

    // Heart rate modes
    public static final int MODE_HEART_RATE = 1;
    public static final int MODE_BLOOD_OXYGEN = 2;
    public static final int MODE_HAND_OFF_DETECTION = 3;

    // Timer-based saving configuration
    private static final long SAVE_INTERVAL_MS = 5000; // 5 seconds
    private static final int MIN_VALID_HEART_RATE = 30;
    private static final int MAX_VALID_HEART_RATE = 220;
    private static final int MIN_READINGS_TO_SAVE = 1; // Reduced for testing

    // Broadcast actions
    private SensorManager mSensorManager;
    private Sensor mSensor;
    private HSystemAssistManager mHSystemAssistManager;
    private IBinder mBinder = new HeartRateServiceBinder();
    private boolean isMonitoring = false;
    private int currentMode = MODE_HEART_RATE;

    // Database components
    private DatabaseClient databaseClient;
    private ExecutorService databaseExecutor;

    // Timer-based saving components
    private Timer saveTimer;
    private List<Float> heartRateBuffer = new ArrayList<>();
    private List<Integer> bloodOxygenBuffer = new ArrayList<>();
    private final ReentrantLock bufferLock = new ReentrantLock();

    // HeartRateDataManager for server sync
    private HeartRateDataManager heartRateDataManager;

    // Statistics
    private int totalReadingsReceived = 0;
    private int validReadingsCount = 0;
    private int recordsSaved = 0;
    private int serverSyncCount = 0;
    private String lastSyncStatus = "Not synced yet";

    @Override
    public void onCreate() {
        super.onCreate();
        initializeSensor();
        initializeDatabase();
        initializeDataManager();
        createNotificationChannel();

        Log.d(TAG, "HeartRateSensorService created with timer-based saving and server sync");
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        startForeground(NOTIFICATION_ID, createNotification());
        return START_STICKY;
    }

    @Override
    public IBinder onBind(Intent intent) {
        return mBinder;
    }

    @SuppressLint("WrongConstant")
    private void initializeSensor() {
        Log.d(TAG, "Initializing sensors");
        mSensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);
        mSensor = mSensorManager.getDefaultSensor(TYPE_HEART_RATE);
        mHSystemAssistManager = (HSystemAssistManager) getSystemService("hsystemassist");
    }

    private void initializeDatabase() {
        databaseClient = DatabaseClient.getInstance(this);
        databaseExecutor = Executors.newSingleThreadExecutor();
        Log.d(TAG, "Database components initialized");
    }

    // Initialize HeartRateDataManager for server sync
    private void initializeDataManager() {
        heartRateDataManager = new HeartRateDataManager(this);
        heartRateDataManager.setSyncListener(this); // Set this service as the sync listener
        heartRateDataManager.setCurrentMode(currentMode);
        Log.d(TAG, "HeartRateDataManager initialized for server sync");
    }

    @SuppressLint("DiscouragedApi")
    private void startPeriodicSave() {
        if (saveTimer != null) {
            saveTimer.cancel();
        }

        saveTimer = new Timer("HeartRateSaveTimer");
        // FIXED: Use scheduleAtFixedRate instead of schedule
        saveTimer.scheduleAtFixedRate(new TimerTask() {
            @Override
            public void run() {
                Log.d(TAG, "â° Timer triggered - calling saveBufferedData()");
                saveBufferedData();
            }
        }, SAVE_INTERVAL_MS, SAVE_INTERVAL_MS);

        Log.d(TAG, "Started periodic save timer (every " + SAVE_INTERVAL_MS + "ms)");
    }

    private void stopPeriodicSave() {
        if (saveTimer != null) {
            saveTimer.cancel();
            saveTimer = null;
            Log.d(TAG, "Stopped periodic save timer");
        }
    }

    // Binder class for local binding
    public class HeartRateServiceBinder extends Binder {
        public HeartRateSensorService getService() {
            return HeartRateSensorService.this;
        }
    }

    // Public methods to control the service
    public void startHeartRateMonitoring() {
        startMonitoring(MODE_HEART_RATE);
        //startMonitoring(MODE_BLOOD_OXYGEN);
    }

    public void startBloodOxygenMonitoring() {
        startMonitoring(MODE_BLOOD_OXYGEN);
    }

    public void startHandOffDetection() {
        startMonitoring(MODE_HAND_OFF_DETECTION);
    }

    private void startMonitoring(int mode) {
        currentMode = mode;

        if (mHSystemAssistManager != null) {
            mHSystemAssistManager.setHeartrateMode(mode);
            Log.d(TAG, "Set HSystemAssistManager mode to: " + mode);
        }

        // Update data manager mode
        if (heartRateDataManager != null) {
            heartRateDataManager.setCurrentMode(mode);
        }

        if (mSensor != null && !isMonitoring) {
            boolean registered = mSensorManager.registerListener(
                    mHeartRateListener,
                    mSensor,
                    SensorManager.SENSOR_DELAY_NORMAL
            );

            if (registered) {
                isMonitoring = true;
                startPeriodicSave(); // Start the timer
                Log.d(TAG, "Started monitoring mode: " + mode);
            } else {
                sendErrorBroadcast("Failed to register sensor listener");
            }
        } else if (mSensor == null) {
            sendErrorBroadcast("Heart rate sensor not available");
        }
    }

    public void stopMonitoring() {
        if (mSensorManager != null && mHeartRateListener != null && isMonitoring) {
            mSensorManager.unregisterListener(mHeartRateListener);
            isMonitoring = false;
            stopPeriodicSave();

            // Save any remaining buffered data
            saveBufferedData();

            Log.d(TAG, "Stopped monitoring");
            logStatistics();
        }
    }

    public boolean isMonitoring() {
        return isMonitoring;
    }

    public int getCurrentMode() {
        return currentMode;
    }

    // Sensor event listener with buffering
    private final SensorEventListener mHeartRateListener = new SensorEventListener() {
        @Override
        public void onSensorChanged(SensorEvent event) {
            if (event.sensor.getType() == TYPE_HEART_RATE && event.values.length > 0) {
                float heartRate = event.values[0];
                totalReadingsReceived++;

                // Validate heart rate value
                if (isValidHeartRate(heartRate)) {
                    validReadingsCount++;

                    // Buffer the reading
                    bufferLock.lock();
                    try {
                        heartRateBuffer.add(heartRate);

                        // Buffer blood oxygen if available
                        int bloodOxygen = getCurrentBloodOxygen();
                        if (bloodOxygen > 0) {
                            bloodOxygenBuffer.add(bloodOxygen);
                        }

                    } finally {
                        bufferLock.unlock();
                    }
                    // Send immediate UI update
                    sendHeartRateDataBroadcast(heartRate);
                    //update the UI sending to main activity
                    Intent fallIntent = new Intent(Constant.ACTION_HEART_RATE_MONITOR_FROM_JAR);
                    fallIntent.putExtra(Constant.VALUE_HEART_RATE_MONITOR_FROM_JAR, (int)heartRate);
                    sendBroadcast(fallIntent);

                    Log.v(TAG, String.format("Valid HR buffered: %.1f (Buffer: %d, Total: %d)",
                            heartRate, heartRateBuffer.size(), totalReadingsReceived));
                } else {
                    //Log.v(TAG, String.format("Invalid HR ignored: %.1f", heartRate));
                }
            }
        }

        @Override
        public void onAccuracyChanged(Sensor sensor, int accuracy) {
            Log.d(TAG, "Sensor accuracy changed: " + accuracy);
        }
    };

    // Save buffered data (called by timer every 5 seconds)
    private void saveBufferedData() {
        Log.d(TAG, "ðŸ’¾ saveBufferedData() called");

        bufferLock.lock();
        List<Float> hrCopy = new ArrayList<>();
        List<Integer> boCopy = new ArrayList<>();

        try {
            Log.d(TAG, "Current buffer size: " + heartRateBuffer.size());

            if (heartRateBuffer.size() >= MIN_READINGS_TO_SAVE) {
                Log.d(TAG, "âœ… Sufficient readings (" + heartRateBuffer.size() + " >= " + MIN_READINGS_TO_SAVE + ")");
                hrCopy.addAll(heartRateBuffer);
                boCopy.addAll(bloodOxygenBuffer);
                heartRateBuffer.clear();
                bloodOxygenBuffer.clear();
                Log.d(TAG, "Copied " + hrCopy.size() + " HR readings, " + boCopy.size() + " BO readings");
            } else {
                Log.d(TAG, "ðŸ“­ No readings in buffer, skipping save");
                return;
            }
        } finally {
            bufferLock.unlock();
        }

        Log.d(TAG, "ðŸš€ Processing " + hrCopy.size() + " readings");

        // Save in background thread
        databaseExecutor.execute(() -> {
            try {
                // Use BOTH approaches:
                // 1. Direct database save for immediate storage
                saveToDatabase(hrCopy, boCopy);

                // 2. HeartRateDataManager for server sync
                addToDataManager(hrCopy, boCopy);

            } catch (Exception e) {
                Log.e(TAG, "ðŸ’¥ Error saving buffered data", e);
            }
        });
    }

    // Direct database save (for immediate storage)
    private void saveToDatabase(List<Float> heartRates, List<Integer> bloodOxygens) {
        try {
            // Calculate averages
            float avgHeartRate = calculateAverageFloat(heartRates);
            int avgBloodOxygen = bloodOxygens.isEmpty() ? 0 : calculateAverageInt(bloodOxygens);

            // Create entity
            HeartRateJarEntity entity = new HeartRateJarEntity();
            entity.setValue(avgHeartRate);
            entity.setBloodOxygen(avgBloodOxygen);
            entity.setEpochMillis(System.currentTimeMillis());
            entity.setMode(currentMode);
            entity.setStatus(0); // Not synced
            entity.setSyncAttempts(0);

            // Insert into database
            databaseClient.getAppDatabase().heartRateJarDao().insertHeartRate(entity);
            recordsSaved++;

            Log.d(TAG, String.format("ðŸ’¾ Saved to DB: HR=%.1f (from %d readings), BO=%d, Mode=%d, Records saved: %d",
                    avgHeartRate, heartRates.size(), avgBloodOxygen, currentMode, recordsSaved));

        } catch (Exception e) {
            Log.e(TAG, "ðŸ’¥ Error saving to database", e);
        }
    }

    // Add to HeartRateDataManager for server sync
    private void addToDataManager(List<Float> heartRates, List<Integer> bloodOxygens) {
        try {
            // Calculate averages
            int avgHeartRate = (int) calculateAverageFloat(heartRates);
            int avgBloodOxygen = bloodOxygens.isEmpty() ? 0 : calculateAverageInt(bloodOxygens);

            Log.d(TAG, String.format("ðŸ“¤ Adding to DataManager: HR=%d, BO=%d", avgHeartRate, avgBloodOxygen));

            // Add to HeartRateDataManager (this handles server sync timing)
            heartRateDataManager.addHeartRateReading(avgHeartRate, avgBloodOxygen);

        } catch (Exception e) {
            Log.e(TAG, "ðŸ’¥ Error adding to data manager", e);
        }
    }

    // Server sync listener callbacks from HeartRateDataManager
    @Override
    public void onSyncSuccess(int recordCount) {
        serverSyncCount++;
        lastSyncStatus = "Success: " + recordCount + " records synced";
        Log.i(TAG, "âœ… Server sync successful! Synced " + recordCount + " records. Total syncs: " + serverSyncCount);

        // Send broadcast to notify UI about successful sync
        sendSyncStatusBroadcast(true, recordCount, null);
    }

    @Override
    public void onSyncError(String error, int recordCount) {
        lastSyncStatus = "Failed: " + error + " (" + recordCount + " records)";
        Log.e(TAG, "âŒ Server sync failed! Error: " + error + ", Records: " + recordCount);

        // Send broadcast to notify UI about sync failure
        sendSyncStatusBroadcast(false, recordCount, error);
    }

    // Validation method
    private boolean isValidHeartRate(float heartRate) {
        return heartRate > 0 &&
                heartRate >= MIN_VALID_HEART_RATE &&
                heartRate <= MAX_VALID_HEART_RATE &&
                !Float.isNaN(heartRate) &&
                !Float.isInfinite(heartRate);
    }

    // Calculate average for float values
    private float calculateAverageFloat(List<Float> values) {
        if (values.isEmpty()) return 0f;

        double sum = 0;
        for (Float value : values) {
            sum += value;
        }
        return (float) (sum / values.size());
    }

    // Calculate average for integer values
    private int calculateAverageInt(List<Integer> values) {
        if (values.isEmpty()) return 0;

        long sum = 0;
        for (Integer value : values) {
            sum += value;
        }
        return (int) (sum / values.size());
    }

    // Get current blood oxygen value (implement based on your sensor)
    private int getCurrentBloodOxygen() {
        if (currentMode == MODE_BLOOD_OXYGEN) {
            return 98; // Placeholder value
        }
        return 0;
    }

    // Manual save trigger (for testing or special cases)
    public void forceSave() {
        Log.d(TAG, "ðŸ”§ Force save triggered");
        saveBufferedData();
    }

    // Force server sync (for testing or manual trigger)
    public void forceServerSync() {
        Log.d(TAG, "ðŸ”§ Force server sync triggered");
        if (heartRateDataManager != null) {
            heartRateDataManager.forceSyncToServer();
        }
    }

    // Debug sync timing
    public void debugSyncTiming() {
        Log.d(TAG, "ðŸ› Debug sync timing triggered");
        if (heartRateDataManager != null) {
            // Add debug method to HeartRateDataManager if needed
            heartRateDataManager.forceSyncToServer();
        }
    }

    // Get comprehensive service statistics
    @SuppressLint("DefaultLocale")
    public String getServiceStatistics() {
        bufferLock.lock();
        try {
            String dataManagerStats = heartRateDataManager != null ?
                    heartRateDataManager.getStatistics() : "DataManager not initialized";

            return String.format("Service Stats:\nTotal readings: %d, Valid: %d\nBuffer: HR=%d/BO=%d\nSaved records: %d\nServer syncs: %d\nLast sync: %s\nMode: %d\n\nDataManager: %s",
                    totalReadingsReceived, validReadingsCount,
                    heartRateBuffer.size(), bloodOxygenBuffer.size(),
                    recordsSaved, serverSyncCount, lastSyncStatus, currentMode,
                    dataManagerStats);
        } finally {
            bufferLock.unlock();
        }
    }

    private void logStatistics() {
        Log.i(TAG, "=== Service Statistics ===");
        Log.i(TAG, getServiceStatistics());
        Log.i(TAG, "=========================");
    }

    // Broadcast methods
    private void sendHeartRateDataBroadcast(float heartRate) {
        Intent intent = new Intent(Constant.ACTION_HEART_RATE_DATA);
        intent.putExtra(Constant.EXTRA_HEART_RATE, heartRate);
        intent.putExtra(Constant.EXTRA_SENSOR_MODE, currentMode);
        sendBroadcast(intent);
    }

    private void sendSyncStatusBroadcast(boolean success, int recordCount, String error) {
        Intent intent = new Intent("com.monitor.health.ACTION_SYNC_STATUS");
        intent.putExtra("sync_success", success);
        intent.putExtra("record_count", recordCount);
        if (error != null) {
            intent.putExtra("error_message", error);
        }
        sendBroadcast(intent);
    }

    private void sendErrorBroadcast(String errorMessage) {
        Intent intent = new Intent(Constant.ACTION_SENSOR_ERROR);
        intent.putExtra(Constant.EXTRA_ERROR_MESSAGE, errorMessage);
        sendBroadcast(intent);
    }

    // Notification methods
    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(
                    "HEART_RATE_CHANNEL",
                    "Heart Rate Monitoring",
                    NotificationManager.IMPORTANCE_LOW
            );
            channel.setDescription("Heart rate sensor monitoring service");

            NotificationManager manager = getSystemService(NotificationManager.class);
            manager.createNotificationChannel(channel);
        }
    }

    private Notification createNotification() {
        String contentText = isMonitoring ?
                "Monitoring heart rate (Mode: " + currentMode + ") - Syncs: " + serverSyncCount :
                "Heart rate service ready";

        return new NotificationCompat.Builder(this, "HEART_RATE_CHANNEL")
                .setContentTitle("Heart Rate Monitor")
                .setContentText(contentText)
                .setSmallIcon(android.R.drawable.ic_menu_info_details)
                .setOngoing(true)
                .build();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();

        Log.d(TAG, "Service destroying...");

        // Stop monitoring and save remaining data
        stopMonitoring();

        // Cleanup HeartRateDataManager
        if (heartRateDataManager != null) {
            heartRateDataManager.cleanup();
        }

        // Cleanup database executor
        if (databaseExecutor != null && !databaseExecutor.isShutdown()) {
            databaseExecutor.shutdown();
            try {
                if (!databaseExecutor.awaitTermination(5, TimeUnit.SECONDS)) {
                    databaseExecutor.shutdownNow();
                    Log.w(TAG, "Database executor forced shutdown");
                }
            } catch (InterruptedException e) {
                databaseExecutor.shutdownNow();
                Thread.currentThread().interrupt();
            }
        }

        // Cleanup resources
        mSensorManager = null;
        mSensor = null;
        mHSystemAssistManager = null;

        Log.d(TAG, "Service destroyed and cleaned up");
        logStatistics();
    }
}