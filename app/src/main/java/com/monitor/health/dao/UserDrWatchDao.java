package com.monitor.health.dao;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.Query;

import com.monitor.health.model.healthscore.UserDrWatch;

import java.util.List;

@Dao
public interface UserDrWatchDao {
    @Insert
    void insertUserDrWatch(UserDrWatch userDrWatch);
    @Query("SELECT * FROM user_drwatch")
    List<UserDrWatch> getAllDrWatch();
    @Query("SELECT * FROM user_drwatch WHERE _id = :id")
    UserDrWatch getDrWatch(int id);
    // Add this to clear all data in the medication table
    @Query("DELETE FROM user_drwatch")
    void clearAllUserDrWatch();
}
