package com.monitor.health.dao;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;

import com.monitor.health.model.HeartRateEntity;

import java.util.List;

@Dao
public interface HeartRateDao {
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    void insertHeartRate(HeartRateEntity heartRate);  // Fixed typo

    // All samples recorded AFTER a given epoch.
    @Query("SELECT * FROM heart_rate WHERE epochMillis > :since")
    List<HeartRateEntity> getNewSamples(long since);

    // Delete everything we already uploaded.
    @Query("DELETE FROM heart_rate WHERE epochMillis <= :cutoff")
    void deleteUpTo(long cutoff);

    @Query("SELECT * FROM heart_rate WHERE status = 0")
    List<HeartRateEntity> getAllHeartRate();

    @Query("SELECT * FROM heart_rate WHERE status = 1")
    List<HeartRateEntity> getAllHeartRateActive();

    @Query("SELECT COUNT(*) FROM heart_rate WHERE value = :value")  // Changed 'value' to 'heart_rate'
    int getCountWithValue(double value);  // Changed return type and parameter to match

    @Query("SELECT * FROM heart_rate WHERE status = 0 ORDER BY id DESC LIMIT 1")
    HeartRateEntity getLatestHeartRate();

    @Query("UPDATE heart_rate SET status = :status WHERE id = :id")
    void updateStatus(long id, int status);

    @Query("DELETE FROM heart_rate WHERE status = 0")
    void deleteAll();

    @Query("DELETE FROM heart_rate WHERE status = 1")
    void deleteAllHourly();

    @Query("SELECT COUNT(*) FROM heart_rate")
    int getCount();

}
