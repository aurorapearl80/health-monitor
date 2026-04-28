package com.monitor.health.dao;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;

import com.monitor.health.model.StepEntity;

import java.util.List;

@Dao
public interface StepDao {

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    void insertStep(StepEntity step);

    @Query("SELECT * FROM steps")
    List<StepEntity> getAllSteps();

    @Query("SELECT * FROM steps WHERE status = 0")
    List<StepEntity> getPendingSteps();

    @Query("UPDATE steps SET status = :status WHERE id = :id")
    void updateStatus(long id, int status);

    @Query("DELETE FROM steps")
    void deleteAll();

    @Query("SELECT COUNT(*) FROM steps")
    int getCount();
}