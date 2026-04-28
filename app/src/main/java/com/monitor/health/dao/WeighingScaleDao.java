package com.monitor.health.dao;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.Query;

import com.monitor.health.model.WeighingScale;

import java.util.List;

@Dao
public interface WeighingScaleDao {
    @Insert
    void insertWeighingScale(WeighingScale weighingScale);

    @Query("SELECT * FROM weighing_scale ORDER BY id DESC")
    List<WeighingScale> getAllWeighingScale();

    @Query("SELECT COUNT(*) FROM weighing_scale WHERE weight = :weight")
    int getWeight(long weight);

    @Query("SELECT * FROM weighing_scale WHERE status = 1 ORDER BY id DESC LIMIT 1")
    WeighingScale getLatestWeighingScale();

    @Query("UPDATE weighing_scale SET status = :status WHERE id = :id")
    void updateStatus(long id, int status);

    @Query("DELETE FROM weighing_scale")
    void deleteAll();
}
