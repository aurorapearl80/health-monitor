package com.monitor.health.dao;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.Query;

import com.monitor.health.entity.TypeAvailabilityEntity;

import java.util.List;

@Dao
public interface TypeAvailabilityDao {
    @Insert
    void insertTypeAvailability(TypeAvailabilityEntity modelTypeAvailability);

    @Query("SELECT * FROM TypeAvailability")
    List<TypeAvailabilityEntity> getAllTypeAvailability();

    @Query("SELECT * FROM TypeAvailability LIMIT 1")
    TypeAvailabilityEntity getSingleTypeAvailability();

    @Query("DELETE FROM TypeAvailability")
    void deleteAll();

    @Query("SELECT COUNT(*) FROM TypeAvailability")
    int getCount();

}
