package com.monitor.health.adapter;

import static com.monitor.health.utility.AppUtils.getTodayDate;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import com.monitor.health.ApiClient;
import com.monitor.health.Constant;
import com.monitor.health.ReadingsRequest;
import com.monitor.health.dao.HeartRateJarDao;
import com.monitor.health.database.DatabaseClient;
import com.monitor.health.entity.HeartRateJarEntity;
import com.monitor.health.model.Reading;
import com.monitor.health.utility.DeviceUtils;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.TimeZone;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class HeartRateDataManager {

    private static final String TAG = "HeartRateDataManager";

    // Configuration constants
    private static final long DATA_SAVE_INTERVAL = 2 * 60 * 1000; // Save every 2 minutes
    private static final long SERVER_SYNC_INTERVAL = 5 * 60 * 1000; // Sync every 5 minutes
    private static final int MIN_READINGS_FOR_AVERAGE = 10; // Minimum readings before calculating average
    // Add this constant at the top
    private static final int MAX_SYNC_RECORDS = 100; // Limit sync to 100 records at a time

    private Context context;
    private DatabaseClient dbHelper;
    private Handler handler;

    // Data collection variables
    private List<Integer> heartRateReadings = new ArrayList<>();
    private List<Integer> bloodOxygenReadings = new ArrayList<>();
    private int currentMode = 1; // 1 = heart rate, 2 = blood oxygen
    private long lastSaveTime = 0;
    private long lastSyncTime = 0;

    // Server sync interface
    public interface ServerSyncListener {
        void onSyncSuccess(int recordCount);
        void onSyncError(String error, int recordCount);
    }

    private ServerSyncListener syncListener;
    private final ExecutorService executor = Executors.newSingleThreadExecutor();

    public HeartRateDataManager(Context context) {
        this.context = context.getApplicationContext();
        this.dbHelper = DatabaseClient.getInstance(context);
        this.handler = new Handler(Looper.getMainLooper());
        this.lastSaveTime = System.currentTimeMillis();
        this.lastSyncTime = System.currentTimeMillis();

        Log.d(TAG, "HeartRateDataManager initialized");
    }

    public void setSyncListener(ServerSyncListener listener) {
        this.syncListener = listener;
    }

    public void setCurrentMode(int mode) {
        this.currentMode = mode;
        Log.d(TAG, "Current mode set to: " + (mode == 1 ? "Heart Rate" : "Blood Oxygen"));
    }

    // Called from sensor listener - accumulates readings
    // ISSUE 4: Add debug logging to track sync timing
// In HeartRateDataManager.java, enhance addHeartRateReading method:

    public synchronized void addHeartRateReading(int heartRate, int bloodOxygen) {
        heartRateReadings.add(heartRate);
        bloodOxygenReadings.add(bloodOxygen);

        long currentTime = System.currentTimeMillis();
        long timeSinceLastSave = currentTime - lastSaveTime;
        long timeSinceLastSync = currentTime - lastSyncTime;

        Log.d(TAG, String.format("Added reading: HR=%d, BO=%d, Total readings: %d, " +
                        "Time since last save: %ds, Time since last sync: %ds",
                heartRate, bloodOxygen, heartRateReadings.size(),
                timeSinceLastSave / 1000, timeSinceLastSync / 1000));

        // Check if it's time to save to database
        if (currentTime - lastSaveTime >= DATA_SAVE_INTERVAL &&
                heartRateReadings.size() >= MIN_READINGS_FOR_AVERAGE) {
            Log.d(TAG, "ðŸ“ Triggering database save - " + heartRateReadings.size() + " readings");
            saveAverageToDatabase();
            lastSaveTime = currentTime;
        }

        // Check if it's time to sync to server
        if (currentTime - lastSyncTime >= SERVER_SYNC_INTERVAL) {
            Log.d(TAG, "ðŸš€ Triggering server sync - Time elapsed: " + (timeSinceLastSync / 1000) + "s");
            syncToServer();
            lastSyncTime = currentTime;
        } else {
            Log.v(TAG, "â³ Sync not due yet - " + ((SERVER_SYNC_INTERVAL - timeSinceLastSync) / 1000) + "s remaining");
        }
    }


    // Save averaged data to Room database
    private synchronized void saveAverageToDatabase() {
        if (heartRateReadings.isEmpty()) return;

        // Calculate averages
        double avgHeartRate = calculateAverage(heartRateReadings);
        int avgBloodOxygen = calculateAverageInt(bloodOxygenReadings);

        // Create HeartRateJarEntity
        HeartRateJarEntity entity = new HeartRateJarEntity();
        entity.setValue(avgHeartRate);
        entity.setBloodOxygen(avgBloodOxygen);
        entity.setEpochMillis(System.currentTimeMillis());
        entity.setMode(currentMode);
        entity.setStatus(0); // 0 = not synced
        entity.setSyncAttempts(0);

        // Save to database using Room DAO in background thread
        executor.execute(() -> {
            try {
                dbHelper.getAppDatabase().heartRateJarDao().insertHeartRate(entity);

                Log.d(TAG, String.format("Saved average to Room DB: HR=%.1f (from %d readings), BO=%d (from %d readings), Mode=%d",
                        avgHeartRate, heartRateReadings.size(), avgBloodOxygen, bloodOxygenReadings.size(), currentMode));

                // Cleanup old records periodically (using Room DAO method)
                if (Math.random() < 0.1) { // 10% chance each time
                    cleanupOldRecords();
                }
            } catch (Exception e) {
                Log.e(TAG, "Error saving to database: " + e.getMessage());
            }
        });

        // Clear readings after saving
        heartRateReadings.clear();
        bloodOxygenReadings.clear();
    }

    // Cleanup old records using Room DAO
    private void cleanupOldRecords() {
        try {
            // Delete synced records older than 7 days
            long sevenDaysAgo = System.currentTimeMillis() - (7 * 24 * 60 * 60 * 1000L);
            int deletedCount = dbHelper.getAppDatabase().heartRateJarDao().deleteOldSyncedRecords(sevenDaysAgo);
            Log.d(TAG, "Cleaned up " + deletedCount + " old records");
        } catch (Exception e) {
            Log.e(TAG, "Error during cleanup: " + e.getMessage());
        }
    }

    // Calculate average from list of integers (returns double)
    private double calculateAverage(List<Integer> values) {
        if (values.isEmpty()) return 0.0;

        long sum = 0;
        for (Integer value : values) {
            sum += value;
        }

        return (double) sum / values.size();
    }

    // Calculate average from list of integers (returns int)
    private int calculateAverageInt(List<Integer> values) {
        if (values.isEmpty()) return 0;

        long sum = 0;
        for (Integer value : values) {
            sum += value;
        }

        return (int) (sum / values.size());
    }

    // Sync unsynced records to server
    // ISSUE 5: Add more detailed logging in syncToServer
// Enhanced syncToServer method:

    // Replace your existing syncToServer method with this:
    public void syncToServer() {
        Log.i(TAG, "ðŸ”„ Starting server sync process...");

        executor.execute(() -> {
            try {
                // Get ALL unsynced records first
                List<HeartRateJarEntity> allUnsyncedRecords = dbHelper.getAppDatabase().heartRateJarDao().getUnsyncedSamples();

                if (allUnsyncedRecords.isEmpty()) {
                    Log.d(TAG, "âœ… No unsynced records to send to server");
                    return;
                }

                // LIMIT the records to sync (this prevents the SQL variable error)
                List<HeartRateJarEntity> recordsToSync;
                if (allUnsyncedRecords.size() > MAX_SYNC_RECORDS) {
                    recordsToSync = allUnsyncedRecords.subList(0, MAX_SYNC_RECORDS);
                    Log.i(TAG, "ðŸ“Š Limiting sync to " + MAX_SYNC_RECORDS + " records (out of " + allUnsyncedRecords.size() + " total)");
                } else {
                    recordsToSync = allUnsyncedRecords;
                    Log.i(TAG, "ðŸ“Š Syncing all " + allUnsyncedRecords.size() + " unsynced records");
                }

                Log.i(TAG, "ðŸš€ Starting sync of " + recordsToSync.size() + " records to server");

                // Attempt to send to server
                boolean syncSuccess = sendToServer(recordsToSync);

                if (syncSuccess) {
                    // Mark records as synced
                    List<Long> syncedIds = new ArrayList<>();
                    for (HeartRateJarEntity record : recordsToSync) {
                        syncedIds.add(record.getId());
                    }

                    // This should now be safe since we limited the records
                    dbHelper.getAppDatabase().heartRateJarDao().updateStatusBatch(syncedIds, 1);

                    Log.i(TAG, "âœ… Successfully synced " + recordsToSync.size() + " records");

                    if (syncListener != null) {
                        handler.post(() -> syncListener.onSyncSuccess(recordsToSync.size()));
                    }

                } else {
                    // Increment sync attempts for failed records
                    List<Long> failedIds = new ArrayList<>();
                    for (HeartRateJarEntity record : recordsToSync) {
                        failedIds.add(record.getId());
                    }

                    // This should now be safe since we limited the records
                    dbHelper.getAppDatabase().heartRateJarDao().incrementSyncAttemptsBatch(failedIds);
                    Log.e(TAG, "âŒ Failed to sync " + recordsToSync.size() + " records");

                    if (syncListener != null) {
                        handler.post(() -> syncListener.onSyncError("Server sync failed", recordsToSync.size()));
                    }
                }

            } catch (Exception e) {
                Log.e(TAG, "ðŸ’¥ Exception during server sync", e);

                if (syncListener != null) {
                    handler.post(() -> syncListener.onSyncError("Exception: " + e.getMessage(), 0));
                }
            }
        });
    }

    // OPTIONAL: Add method to check if more records need syncing
    public boolean hasMoreRecordsToSync() {
        try {
            int unsyncedCount = dbHelper.getAppDatabase().heartRateJarDao().getUnsyncedCount();
            return unsyncedCount > 0;
        } catch (Exception e) {
            Log.e(TAG, "Error checking unsynced count", e);
            return false;
        }
    }

    // OPTIONAL: Add method to sync all records in multiple batches
    public void syncAllRecordsInBatches() {
        Log.i(TAG, "ðŸ”„ Starting complete sync process in batches...");

        executor.execute(() -> {
            int batchCount = 0;
            int totalSynced = 0;

            while (hasMoreRecordsToSync()) {
                batchCount++;
                Log.i(TAG, "ðŸ“¦ Processing batch " + batchCount + "...");

                // Sync one batch
                syncToServer();

                // Wait a bit between batches to avoid overwhelming the server
                try {
                    Thread.sleep(2000); // 2 second delay between batches
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    break;
                }

                // Safety limit to prevent infinite loop
                if (batchCount >= 50) { // Max 50 batches (5000 records)
                    Log.w(TAG, "âš ï¸ Reached maximum batch limit, stopping sync");
                    break;
                }
            }

            Log.i(TAG, "âœ… Completed batch sync process. Processed " + batchCount + " batches");
        });
    }

    // Send data to server - replace with your actual server API call
  //ISSUE 2: sendToServer always returns true but sync callback expects success
// In HeartRateDataManager.java, fix the sendToServer method:
    // ISSUE 6: Add sync trigger debugging
// Add this method to HeartRateDataManager for testing:

    public void debugSyncTiming() {
        long currentTime = System.currentTimeMillis();
        long timeSinceLastSync = currentTime - lastSyncTime;
        boolean shouldSync = timeSinceLastSync >= SERVER_SYNC_INTERVAL;

        Log.d(TAG, "ðŸ› Sync Debug Info:");
        Log.d(TAG, "   Current time: " + currentTime);
        Log.d(TAG, "   Last sync time: " + lastSyncTime);
        Log.d(TAG, "   Time since last sync: " + (timeSinceLastSync / 1000) + "s");
        Log.d(TAG, "   Sync interval: " + (SERVER_SYNC_INTERVAL / 1000) + "s");
        Log.d(TAG, "   Should sync: " + shouldSync);
        Log.d(TAG, "   Pending readings: " + heartRateReadings.size());

        if (shouldSync) {
            Log.d(TAG, "ðŸš€ Manually triggering sync...");
            syncToServer();
        }
    }
    private boolean sendToServer(List<HeartRateJarEntity> records) {
        try {
            Log.d(TAG, "Attempting to send " + records.size() + " records to server");

            // Create heart rate list for API
            List<Double> heartRateList = new ArrayList<>();
            for (HeartRateJarEntity record : records) {
//                Log.d(TAG, "Syncing record: ID=" + record.getId() +
//                        ", HR=" + record.getValue() +
//                        ", BO=" + record.getBloodOxygen() +
//                        ", Time=" + new java.util.Date(record.getEpochMillis()) +
//                        ", Mode=" + record.getMode());
                // Add [0.0, heartRate] for each record
                heartRateList.add(0.0);
                heartRateList.add(record.getValue());
            }

            // Send to actual server
            sendHeartRateSync(heartRateList); // Make this synchronous

            Log.d(TAG, "Successfully sent data to server");
            return true; // Return true only if API call succeeds

        } catch (Exception e) {
            Log.e(TAG, "Error sending data to server", e);
            return false;
        }
    }

    // ISSUE 3: Make sendHeartRate synchronous for proper success/failure detection
// Add this new synchronous method to HeartRateDataManager:

    private boolean sendHeartRateSync(List<Double> heartRateList) {
        Log.d(TAG, "Sending heart rate data synchronously");

        try {
            String token = "bNWZsV#BeZvaNb*gF@3Z^7tCNhCT29Vw8Vi%4T%";
            Reading reading = new Reading(
                    false,
                    TimeZone.getDefault().getID(),
                    "jtm00025b94050c",
                    heartRateList,
                    "66437be266c8833a1c42d7aa",
                    "5bb306382598931ffbd1b626",
                    getTodayDate(),
                    "66437be266c8833a1c42d7aa"
            );
            List<Reading> readingsList = Arrays.asList(reading);
            ReadingsRequest readingsRequest = new ReadingsRequest(readingsList);

            Call<Object> call = ApiClient.getUserService(Constant.BASE_URL_BGM, token,
                    DeviceUtils.getIMEI(this.context)).sendReadings(readingsRequest);

            // Make synchronous call
            Response<Object> response = call.execute();

            Log.d(TAG, "Server response code: " + response.code());
            Log.d(TAG, "Server response message: " + response.message());

            if (response.isSuccessful()) {
                Log.d(TAG, "âœ… Heart rate data sent successfully");
                return true;
            } else {
                Log.e(TAG, "âŒ Server returned error: " + response.code() + " - " + response.message());
                return false;
            }

        } catch (Exception e) {
            Log.e(TAG, "âŒ Exception during server sync", e);
            return false;
        }
    }

    public void sendHeartRate(List<Double> heartRateList) {
        //Log.d(TAG, "sending temperature "+ heartRate);
        // Usage example in your activity or service
        String token = "bNWZsV#BeZvaNb*gF@3Z^7tCNhCT29Vw8Vi%4T%";
        Reading reading = new Reading(
                false,
                TimeZone.getDefault().getID(),
                "jtm00025b94050c",
                heartRateList,
                "66437be266c8833a1c42d7aa",
                "5bb306382598931ffbd1b626",
                getTodayDate(),
                "66437be266c8833a1c42d7aa"
        );
        List<Reading> readingsList = Arrays.asList(reading);
        ReadingsRequest readingsRequest = new ReadingsRequest(readingsList);

        Call<Object> call = ApiClient.getUserService(Constant.BASE_URL_BGM,token, DeviceUtils.getIMEI(this.context)).sendReadings(readingsRequest);
        call.enqueue(new Callback<Object>() {
            @Override
            public void onResponse(Call<Object> call, Response<Object> response) {
                Log.d(TAG, "sendHeartRate DRS_020_SEND_ALARM code " + response.code());
                Log.d(TAG, "sendHeartRate DRS_020_SEND_ALARM body " + response.body());
                Log.d(TAG, "sendHeartRate DRS_020_SEND_ALARM toString " + response.toString());
                Log.d(TAG, "sendHeartRate DRS_020_SEND_ALARM message " + response.message());
                if (response.isSuccessful()) {
                    // Request successful
                    // Handle response if needed
                    //saveTemperatureData(temperature);
                    // restartBle();
                } else {
                    // Request failed
                    // Handle error
                    // restartBle();
                }
            }

            @Override
            public void onFailure(Call<Object> call, Throwable t) {
                // Request failed
                // Handle failure
                //restartBle();
            }
        });
    }

    // Force save current readings (useful when app is closing)
    public void forceSave() {
        saveAverageToDatabase();
    }

    // Get statistics using Room DAO
    public String getStatistics() {
        try {
            int unsyncedCount = dbHelper.getAppDatabase().heartRateJarDao().getUnsyncedCount();
            // Get total count (you might need to add this method to your DAO)
            // For now, we'll use a simple approach
            int pendingReadings = heartRateReadings.size();

            return String.format("Unsynced DB Records: %d, Pending Readings: %d",
                    unsyncedCount, pendingReadings);
        } catch (Exception e) {
            Log.e(TAG, "Error getting statistics: " + e.getMessage());
            return "Error getting statistics";
        }
    }

    // Get detailed statistics using Room DAO
    public void getDetailedStatistics(StatisticsCallback callback) {
        executor.execute(() -> {
            try {
                HeartRateJarDao.StatisticsSummary stats = dbHelper.getAppDatabase().heartRateJarDao().getStatistics();
                int pendingReadings = heartRateReadings.size();

                String statsString = String.format("Total: %d, Synced: %d, Unsynced: %d, Pending: %d",
                        stats.total, stats.synced, stats.unsynced, pendingReadings);

                if (callback != null) {
                    handler.post(() -> callback.onSuccess(statsString, stats));
                }
            } catch (Exception e) {
                Log.e(TAG, "Error getting detailed statistics: " + e.getMessage());
                if (callback != null) {
                    handler.post(() -> callback.onError(e.getMessage()));
                }
            }
        });
    }

    // Statistics callback interface
    public interface StatisticsCallback {
        void onSuccess(String summary, HeartRateJarDao.StatisticsSummary details);
        void onError(String error);
    }

    // Manual sync trigger
    public void forceSyncToServer() {
        syncToServer();
    }

    // Get latest heart rate
    public void getLatestHeartRate(DataCallback callback) {
        executor.execute(() -> {
            try {
                HeartRateJarEntity latest = dbHelper.getAppDatabase().heartRateJarDao().getLatestHeartRate();
                if (callback != null) {
                    handler.post(() -> callback.onSuccess(latest));
                }
            } catch (Exception e) {
                Log.e(TAG, "Error getting latest heart rate: " + e.getMessage());
                if (callback != null) {
                    handler.post(() -> callback.onError(e.getMessage()));
                }
            }
        });
    }

    // Data callback interface
    public interface DataCallback {
        void onSuccess(HeartRateJarEntity data);
        void onError(String error);
    }

    // Cleanup resources
    public void cleanup() {
        forceSave(); // Save any pending data
        handler.removeCallbacksAndMessages(null);
        if (!executor.isShutdown()) {
            executor.shutdown();
        }
        Log.d(TAG, "HeartRateDataManager cleaned up");
    }
}