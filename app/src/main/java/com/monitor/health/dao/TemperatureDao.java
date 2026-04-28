package com.monitor.health.dao;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.Query;

import com.monitor.health.model.Temperature;

import java.util.List;

@Dao
public interface TemperatureDao {
    @Insert
    void insertTemperature(Temperature temperature);

    @Query("SELECT * FROM temperature ORDER BY id DESC")
    List<Temperature> getAllTemperature();

    @Query("SELECT COUNT(*) FROM temperature WHERE temperature = :temperature")
    int getTemperature(long temperature);

    @Query("SELECT * FROM temperature WHERE status = 1 ORDER BY id DESC LIMIT 1")
    Temperature getLatestTemperature();

    @Query("UPDATE temperature SET status = :status WHERE id = :id")
    void updateStatus(long id, int status);

    @Query("DELETE FROM temperature")
    void deleteAll();

    // Delete a specific record by ID
    @Query("DELETE FROM temperature WHERE id = :tempId")
    void deleteById(long tempId);

    @Query("SELECT COUNT(*) FROM temperature")
    int getCount();
}
