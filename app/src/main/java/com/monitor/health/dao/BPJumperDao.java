package com.monitor.health.dao;

import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.Query;

import com.monitor.health.model.BPJumper;

import java.util.List;

@Dao
public interface BPJumperDao {

    @Insert
    void insertBPJumper(BPJumper bpJumper);

    @Query("SELECT * FROM bp_jumper WHERE status = 1")
    List<BPJumper> getAllBPJumper();

    @Query("SELECT * FROM bp_jumper WHERE status = 0")
    List<BPJumper> getAllBPJumperActive();

    @Query("SELECT COUNT(*) FROM bp_jumper WHERE systolic = :systolic AND diastolic = :diastolic AND pulseRate = :pulseRate")
    int getCompareColumns(int systolic, int diastolic, int pulseRate);

    @Query("SELECT * FROM bp_jumper WHERE status = 1 ORDER BY id DESC LIMIT 1")
    BPJumper getLatestBPJumper();

    @Query("UPDATE bp_jumper SET status = :status WHERE id = :id")
    void updateStatus(long id, int status);

    // âœ… New delete options
    @Delete
    void deleteBPJumper(BPJumper bpJumper);

    @Query("DELETE FROM bp_jumper WHERE id = :id")
    void deleteById(long id);

    @Query("DELETE FROM bp_jumper WHERE status = 1")
    void deleteAll();

    @Query("DELETE FROM bp_jumper WHERE status = 0")
    void deleteAllHourly();

    @Query("SELECT COUNT(*) FROM bp_jumper")
    int getCount();
}
