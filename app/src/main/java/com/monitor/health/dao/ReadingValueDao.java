package com.monitor.health.dao;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.Query;

import com.monitor.health.model.ReadingValue;

import java.util.List;

@Dao
public interface ReadingValueDao {

    @Insert
    void insertReadingValue(ReadingValue readingValue);

    @Query("SELECT * FROM reading_values ORDER BY id DESC")
    List<ReadingValue> getAllReadingValues();

    @Query("SELECT COUNT(*) FROM reading_values WHERE glucose = :name")
    int getReadingValueCountByGlucose(int name);

    @Query("SELECT * FROM reading_values WHERE status = 1 ORDER BY id DESC LIMIT 1")
    ReadingValue getLatestReadingValue();

    @Query("UPDATE reading_values SET status = :status WHERE id = :id")
    void updateStatus(long id, int status);


    @Query("DELETE FROM reading_values")
    void deleteAll();

    // Delete a specific record by ID
    @Query("DELETE FROM reading_values WHERE id = :tempId")
    void deleteById(long tempId);
}
