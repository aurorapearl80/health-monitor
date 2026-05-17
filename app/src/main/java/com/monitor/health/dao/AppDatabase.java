package com.monitor.health.dao;

import androidx.room.Database;
import androidx.room.RoomDatabase;
import androidx.room.TypeConverters;

import com.monitor.health.entity.HeartRateJarEntity;
import com.monitor.health.entity.MessageEntity;
import com.monitor.health.entity.TypeAvailabilityEntity;
import com.monitor.health.model.BPJumper;
import com.monitor.health.model.BleDeviceModel;
import com.monitor.health.model.HeartRateEntity;
import com.monitor.health.model.MedicationDBValue;
import com.monitor.health.model.Oximeter;
import com.monitor.health.model.ReadingValue;
import com.monitor.health.model.StepEntity;
import com.monitor.health.model.Temperature;
import com.monitor.health.model.WeighingScale;
import com.monitor.health.model.healthscore.UserDrWatch;

@Database(entities = {
        ReadingValue.class,
        BPJumper.class,
        Temperature.class,
        Oximeter.class,
        WeighingScale.class,
        MedicationDBValue.class,
        UserDrWatch.class,
        HeartRateEntity.class,
        HeartRateJarEntity.class,
        TypeAvailabilityEntity.class,
        BleDeviceModel.class,
        MessageEntity.class,
        StepEntity.class,
}, version = 12, exportSchema = false)
@TypeConverters({Converters.class}) // Add TypeConverters annotation
public abstract class AppDatabase extends RoomDatabase {
    public abstract ReadingValueDao readingValueDao();
    public abstract BPJumperDao bpJumperDao();
    public abstract TemperatureDao temperatureDao();
    public abstract OximeterDao oximeterDao();
    public abstract WeighingScaleDao weighingScaleDao();
    public abstract MedicationDBValueDao medicationDBValueDao();
    public abstract UserDrWatchDao userDrWatchDao();
    public abstract HeartRateDao heartRateDao();
    public abstract HeartRateJarDao heartRateJarDao();
    public abstract TypeAvailabilityDao typeAvailabilityDao();
    public abstract BleDeviceDao bleDeviceDao();
    public abstract MessageDAO messageDAO();
    public abstract StepDao stepDao();
}
