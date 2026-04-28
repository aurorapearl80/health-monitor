package com.monitor.health.dao;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.Query;

import com.monitor.health.model.MedicationDBValue;

import java.util.List;

@Dao
public interface MedicationDBValueDao {
    @Insert
    void insertMedication(MedicationDBValue medicationDBValue);

    @Query("SELECT * FROM medication")
    List<MedicationDBValue> getAllMedications();

    @Query("SELECT * FROM medication WHERE tagHour = :tagHour AND tagMinutes = :tagMinutes AND hours = :hours AND minutes = :minutes")
    List<MedicationDBValue> getMedicationsByTime(int tagHour, int tagMinutes, int hours, int minutes);
    //use: List<MedicationDBValue> medications = medicationDBValueDao.getMedicationsByTime(8, 30, 8, 30);

    @Query("SELECT * FROM medication WHERE stateParameter = :setAlarmTag")
    List<MedicationDBValue> getStateParameter(int setAlarmTag);

    @Query("SELECT * FROM medication WHERE configurationParameter = :configuration")
    List<MedicationDBValue> getConfigurationParameter(int configuration);

    // Add this to clear all data in the medication table
    @Query("DELETE FROM medication")
    void clearAllMedications();
}
