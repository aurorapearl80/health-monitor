package com.monitor.health.dao;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;


import com.monitor.health.entity.HeartRateJarEntity;

import java.util.List;

@Dao
public interface HeartRateJarDao {

    // Insert heart rate data
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    void insertHeartRate(HeartRateJarEntity heartRate);

    // Insert multiple heart rate records
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    void insertAll(List<HeartRateJarEntity> heartRates);

    // Get all samples recorded AFTER a given epoch (for incremental sync)
    @Query("SELECT * FROM heart_rate_jar WHERE epochMillis > :since ORDER BY epochMillis ASC")
    List<HeartRateJarEntity> getNewSamples(long since);

    // Get unsynced samples (status = 0)
    @Query("SELECT * FROM heart_rate_jar WHERE status = 0 ORDER BY epochMillis ASC")
    List<HeartRateJarEntity> getUnsyncedSamples();

    // Get samples within a time range
    @Query("SELECT * FROM heart_rate_jar WHERE epochMillis >= :startTime AND epochMillis <= :endTime ORDER BY epochMillis ASC")
    List<HeartRateJarEntity> getSamplesByTimeRange(long startTime, long endTime);

    // Delete everything we already uploaded (synced records older than cutoff)
    @Query("DELETE FROM heart_rate_jar WHERE epochMillis <= :cutoff AND status = 1")
    void deleteUpTo(long cutoff);

    // Delete all synced records
    @Query("DELETE FROM heart_rate_jar WHERE status = 1")
    void deleteSyncedRecords();

    // Get all heart rate records
    @Query("SELECT * FROM heart_rate_jar ORDER BY epochMillis DESC")
    List<HeartRateJarEntity> getAllHeartRate();

    // Get records by mode (1 = heart rate, 2 = blood oxygen)
    @Query("SELECT * FROM heart_rate_jar WHERE mode = :mode ORDER BY epochMillis DESC")
    List<HeartRateJarEntity> getRecordsByMode(int mode);

    // Get count of records with specific heart rate value
    @Query("SELECT COUNT(*) FROM heart_rate_jar WHERE value = :value")
    int getCountWithValue(double value);

    // Get count of records by mode
    @Query("SELECT COUNT(*) FROM heart_rate_jar WHERE mode = :mode")
    int getCountByMode(int mode);

    // Get count of unsynced records
    @Query("SELECT COUNT(*) FROM heart_rate_jar WHERE status = 0")
    int getUnsyncedCount();

    // Get latest heart rate record (synced)
    @Query("SELECT * FROM heart_rate_jar WHERE status = 1 ORDER BY id DESC LIMIT 1")
    HeartRateJarEntity getLatestHeartRate();

    // Get latest record by mode
    @Query("SELECT * FROM heart_rate_jar WHERE mode = :mode ORDER BY epochMillis DESC LIMIT 1")
    HeartRateJarEntity getLatestByMode(int mode);

    // Get latest unsynced record
    @Query("SELECT * FROM heart_rate_jar WHERE status = 0 ORDER BY epochMillis DESC LIMIT 1")
    HeartRateJarEntity getLatestUnsyncedRecord();

    // Update sync status for specific record
    @Query("UPDATE heart_rate_jar SET status = :status WHERE id = :id")
    void updateStatus(long id, int status);

    // Update sync status for multiple records
    @Query("UPDATE heart_rate_jar SET status = :status WHERE id IN (:ids)")
    void updateStatusBatch(List<Long> ids, int status);

    // Increment sync attempts for specific record
    @Query("UPDATE heart_rate_jar SET sync_attempts = sync_attempts + 1 WHERE id = :id")
    void incrementSyncAttempts(long id);

    // Increment sync attempts for multiple records
    @Query("UPDATE heart_rate_jar SET sync_attempts = sync_attempts + 1 WHERE id IN (:ids)")
    void incrementSyncAttemptsBatch(List<Long> ids);

    // Get records with failed sync attempts (more than maxAttempts)
    @Query("SELECT * FROM heart_rate_jar WHERE sync_attempts > :maxAttempts AND status = 0")
    List<HeartRateJarEntity> getFailedSyncRecords(int maxAttempts);

    // Get average heart rate within time range
    @Query("SELECT AVG(value) FROM heart_rate_jar WHERE mode = 1 AND epochMillis >= :startTime AND epochMillis <= :endTime")
    double getAverageHeartRate(long startTime, long endTime);

    // Get average blood oxygen within time range
    @Query("SELECT AVG(blood_oxygen) FROM heart_rate_jar WHERE mode = 2 AND epochMillis >= :startTime AND epochMillis <= :endTime")
    double getAverageBloodOxygen(long startTime, long endTime);

    // Get records for today (since midnight)
    @Query("SELECT * FROM heart_rate_jar WHERE epochMillis >= :midnightTimestamp ORDER BY epochMillis DESC")
    List<HeartRateJarEntity> getTodayRecords(long midnightTimestamp);

    // Get summary statistics
    @Query("SELECT COUNT(*) as total, " +
            "COUNT(CASE WHEN status = 1 THEN 1 END) as synced, " +
            "COUNT(CASE WHEN status = 0 THEN 1 END) as unsynced, " +
            "MIN(epochMillis) as oldest, " +
            "MAX(epochMillis) as newest " +
            "FROM heart_rate_jar")
    StatisticsSummary getStatistics();

    // Delete old records (older than specified days)
    @Query("DELETE FROM heart_rate_jar WHERE epochMillis < :cutoffTime AND status = 1")
    int deleteOldSyncedRecords(long cutoffTime);

    // Clear all data (for testing/reset purposes)
    @Query("DELETE FROM heart_rate_jar")
    void clearAll();

    @Query("UPDATE heart_rate_jar SET status = 0 WHERE status = 1")
    int updateStatusFromSyncedToUnsynced();

    // Update all records with value = 0.0 to a new value
    @Query("UPDATE heart_rate_jar SET value = :newValue WHERE value = 0.0")
    int updateZeroValues(double newValue);

    // Inner class for statistics summary
    public static class StatisticsSummary {
        public int total;
        public int synced;
        public int unsynced;
        public long oldest;
        public long newest;

        public StatisticsSummary(int total, int synced, int unsynced, long oldest, long newest) {
            this.total = total;
            this.synced = synced;
            this.unsynced = unsynced;
            this.oldest = oldest;
            this.newest = newest;
        }

        @Override
        public String toString() {
            return "StatisticsSummary{" +
                    "total=" + total +
                    ", synced=" + synced +
                    ", unsynced=" + unsynced +
                    ", oldest=" + oldest +
                    ", newest=" + newest +
                    '}';
        }
    }
}