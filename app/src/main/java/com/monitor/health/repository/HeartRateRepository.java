package com.monitor.health.repository;

import android.content.Context;
import android.os.AsyncTask;
import android.util.Log;

import com.monitor.health.dao.HeartRateJarDao;
import com.monitor.health.database.DatabaseClient;
import com.monitor.health.entity.HeartRateJarEntity;
import com.monitor.health.model.HeartRateEntity;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class HeartRateRepository {

    private static final String TAG = "HeartRateRepository";
    private static final int THREAD_POOL_SIZE = 4;

    private HeartRateJarDao heartRateDao;
    private ExecutorService executorService;
    private static HeartRateRepository instance;

    // Repository callback interfaces
    public interface DataCallback<T> {
        void onSuccess(T data);
        void onError(String error);
    }

    public interface SimpleCallback {
        void onSuccess();
        void onError(String error);
    }

    // Singleton pattern
    public static HeartRateRepository getInstance(Context context) {
        if (instance == null) {
            synchronized (HeartRateRepository.class) {
                if (instance == null) {
                    instance = new HeartRateRepository(context);
                }
            }
        }
        return instance;
    }

    private HeartRateRepository(Context context) {
        DatabaseClient database = DatabaseClient.getInstance(context);
        //HeartRateDatabase database = HeartRateDatabase.getInstance(context);
        heartRateDao = database.getAppDatabase().heartRateJarDao();
        executorService = Executors.newFixedThreadPool(THREAD_POOL_SIZE);
    }

    // Insert single heart rate record
    public void insertHeartRate(HeartRateJarEntity heartRate, SimpleCallback callback) {
        executorService.execute(() -> {
            try {
                heartRateDao.insertHeartRate(heartRate);
                Log.d(TAG, "Inserted heart rate record: " + heartRate);
                if (callback != null) {
                    callback.onSuccess();
                }
            } catch (Exception e) {
                Log.e(TAG, "Error inserting heart rate", e);
                if (callback != null) {
                    callback.onError(e.getMessage());
                }
            }
        });
    }

    // Insert multiple records
    public void insertAll(List<HeartRateJarEntity> heartRates, SimpleCallback callback) {
        executorService.execute(() -> {
            try {
                heartRateDao.insertAll(heartRates);
                Log.d(TAG, "Inserted " + heartRates.size() + " heart rate records");
                if (callback != null) {
                    callback.onSuccess();
                }
            } catch (Exception e) {
                Log.e(TAG, "Error inserting heart rate batch", e);
                if (callback != null) {
                    callback.onError(e.getMessage());
                }
            }
        });
    }

    // Get unsynced samples for server upload
    public void getUnsyncedSamples(DataCallback<List<HeartRateJarEntity>> callback) {
        executorService.execute(() -> {
            try {
                List<HeartRateJarEntity> unsyncedSamples = heartRateDao.getUnsyncedSamples();
                Log.d(TAG, "Retrieved " + unsyncedSamples.size() + " unsynced samples");
                if (callback != null) {
                    callback.onSuccess(unsyncedSamples);
                }
            } catch (Exception e) {
                Log.e(TAG, "Error getting unsynced samples", e);
                if (callback != null) {
                    callback.onError(e.getMessage());
                }
            }
        });
    }

    // Get new samples after a specific timestamp
    public void getNewSamples(long since, DataCallback<List<HeartRateJarEntity>> callback) {
        executorService.execute(() -> {
            try {
                List<HeartRateJarEntity> newSamples = heartRateDao.getNewSamples(since);
                Log.d(TAG, "Retrieved " + newSamples.size() + " new samples since " + since);
                if (callback != null) {
                    callback.onSuccess(newSamples);
                }
            } catch (Exception e) {
                Log.e(TAG, "Error getting new samples", e);
                if (callback != null) {
                    callback.onError(e.getMessage());
                }
            }
        });
    }

    // Mark records as synced
    public void markAsSynced(List<Long> recordIds, SimpleCallback callback) {
        executorService.execute(() -> {
            try {
                heartRateDao.updateStatusBatch(recordIds, 1); // 1 = synced
                Log.d(TAG, "Marked " + recordIds.size() + " records as synced");
                if (callback != null) {
                    callback.onSuccess();
                }
            } catch (Exception e) {
                Log.e(TAG, "Error marking records as synced", e);
                if (callback != null) {
                    callback.onError(e.getMessage());
                }
            }
        });
    }

    // Increment sync attempts for failed records
    public void incrementSyncAttempts(List<Long> recordIds, SimpleCallback callback) {
        executorService.execute(() -> {
            try {
                heartRateDao.incrementSyncAttemptsBatch(recordIds);
                Log.d(TAG, "Incremented sync attempts for " + recordIds.size() + " records");
                if (callback != null) {
                    callback.onSuccess();
                }
            } catch (Exception e) {
                Log.e(TAG, "Error incrementing sync attempts", e);
                if (callback != null) {
                    callback.onError(e.getMessage());
                }
            }
        });
    }

    // Get all heart rate records
    public void getAllHeartRate(DataCallback<List<HeartRateJarEntity>> callback) {
        executorService.execute(() -> {
            try {
                List<HeartRateJarEntity> allRecords = heartRateDao.getAllHeartRate();
                if (callback != null) {
                    callback.onSuccess(allRecords);
                }
            } catch (Exception e) {
                Log.e(TAG, "Error getting all heart rate records", e);
                if (callback != null) {
                    callback.onError(e.getMessage());
                }
            }
        });
    }

    // Get latest heart rate record
    public void getLatestHeartRate(DataCallback<HeartRateJarEntity> callback) {
        executorService.execute(() -> {
            try {
                HeartRateJarEntity latest = heartRateDao.getLatestHeartRate();
                if (callback != null) {
                    callback.onSuccess(latest);
                }
            } catch (Exception e) {
                Log.e(TAG, "Error getting latest heart rate", e);
                if (callback != null) {
                    callback.onError(e.getMessage());
                }
            }
        });
    }

    // Get records by mode (heart rate or blood oxygen)
    public void getRecordsByMode(int mode, DataCallback<List<HeartRateJarEntity>> callback) {
        executorService.execute(() -> {
            try {
                List<HeartRateJarEntity> records = heartRateDao.getRecordsByMode(mode);
                Log.d(TAG, "Retrieved " + records.size() + " records for mode " + mode);
                if (callback != null) {
                    callback.onSuccess(records);
                }
            } catch (Exception e) {
                Log.e(TAG, "Error getting records by mode", e);
                if (callback != null) {
                    callback.onError(e.getMessage());
                }
            }
        });
    }

    // Delete old synced records
    public void deleteUpTo(long cutoff, SimpleCallback callback) {
        executorService.execute(() -> {
            try {
                heartRateDao.deleteUpTo(cutoff);
                Log.d(TAG, "Deleted synced records up to " + cutoff);
                if (callback != null) {
                    callback.onSuccess();
                }
            } catch (Exception e) {
                Log.e(TAG, "Error deleting old records", e);
                if (callback != null) {
                    callback.onError(e.getMessage());
                }
            }
        });
    }

    // Get statistics summary
    public void getStatistics(DataCallback<HeartRateJarDao.StatisticsSummary> callback) {
        executorService.execute(() -> {
            try {
                HeartRateJarDao.StatisticsSummary stats = heartRateDao.getStatistics();
                Log.d(TAG, "Retrieved statistics: " + stats);
                if (callback != null) {
                    callback.onSuccess(stats);
                }
            } catch (Exception e) {
                Log.e(TAG, "Error getting statistics", e);
                if (callback != null) {
                    callback.onError(e.getMessage());
                }
            }
        });
    }

    // Get average heart rate for time range
    public void getAverageHeartRate(long startTime, long endTime, DataCallback<Double> callback) {
        executorService.execute(() -> {
            try {
                double average = heartRateDao.getAverageHeartRate(startTime, endTime);
                Log.d(TAG, "Average heart rate from " + startTime + " to " + endTime + ": " + average);
                if (callback != null) {
                    callback.onSuccess(average);
                }
            } catch (Exception e) {
                Log.e(TAG, "Error getting average heart rate", e);
                if (callback != null) {
                    callback.onError(e.getMessage());
                }
            }
        });
    }

    // Cleanup old records (older than specified days)
    public void cleanupOldRecords(int daysOld, SimpleCallback callback) {
        executorService.execute(() -> {
            try {
                long cutoffTime = System.currentTimeMillis() - (daysOld * 24 * 60 * 60 * 1000L);
                int deletedCount = heartRateDao.deleteOldSyncedRecords(cutoffTime);
                Log.d(TAG, "Cleaned up " + deletedCount + " old records (older than " + daysOld + " days)");
                if (callback != null) {
                    callback.onSuccess();
                }
            } catch (Exception e) {
                Log.e(TAG, "Error cleaning up old records", e);
                if (callback != null) {
                    callback.onError(e.getMessage());
                }
            }
        });
    }

    // Create a heart rate entity with current timestamp
    public HeartRateJarEntity createHeartRateEntity(double heartRateValue, int bloodOxygenValue, int mode) {
        return new HeartRateJarEntity(heartRateValue, bloodOxygenValue, System.currentTimeMillis(), mode);
    }

    // Shutdown executor service
    public void shutdown() {
        if (executorService != null && !executorService.isShutdown()) {
            executorService.shutdown();
            Log.d(TAG, "Repository executor service shut down");
        }
    }
}
