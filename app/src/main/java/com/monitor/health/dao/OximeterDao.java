package com.monitor.health.dao;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.Query;

import com.monitor.health.model.Oximeter;

import java.util.List;

@Dao
public interface OximeterDao {

    @Insert
    void insertOximeter(Oximeter oximeter);

    @Query("SELECT * FROM oximeter WHERE status = 1")
    List<Oximeter> getAllOximeter();

    @Query("SELECT * FROM oximeter WHERE status = 0")
    List<Oximeter> getAllOximeterActive();

    @Query("SELECT * FROM oximeter WHERE status = 0 ORDER BY id DESC LIMIT 1")
    Oximeter getLatestOximeter();

    @Query("UPDATE oximeter SET status = :status WHERE id = :id")
    void updateStatus(long id, int status);

    @Query("DELETE FROM oximeter WHERE status = 0")
    void deleteAll();

    @Query("DELETE FROM oximeter WHERE status = 1")
    void deleteAllHourly();

    @Query("SELECT COUNT(*) FROM oximeter")
    int getCount();

    @Query("SELECT * FROM oximeter WHERE status = :status")
    List<Oximeter> getAllOximeterByStatus(int status);

}
