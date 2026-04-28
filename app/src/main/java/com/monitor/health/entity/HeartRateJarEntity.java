package com.monitor.health.entity;

import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.PrimaryKey;

@Entity(tableName = "heart_rate_jar")
public class HeartRateJarEntity {

    @PrimaryKey(autoGenerate = true)
    private long id;

    @ColumnInfo(name = "value")
    private double value; // Heart rate value in BPM

    @ColumnInfo(name = "blood_oxygen")
    private int bloodOxygen; // Blood oxygen percentage

    @ColumnInfo(name = "epochMillis")
    private long epochMillis; // Timestamp in milliseconds

    @ColumnInfo(name = "mode")
    private int mode; // 1 = heart rate, 2 = blood oxygen

    @ColumnInfo(name = "status")
    private int status; // 0 = not synced, 1 = synced

    @ColumnInfo(name = "sync_attempts")
    private int syncAttempts; // Number of sync attempts

    // Default constructor
    public HeartRateJarEntity() {
    }

    // Constructor with essential fields
    public HeartRateJarEntity(double value, int bloodOxygen, long epochMillis, int mode) {
        this.value = value;
        this.bloodOxygen = bloodOxygen;
        this.epochMillis = epochMillis;
        this.mode = mode;
        this.status = 0; // Default to not synced
        this.syncAttempts = 0;
    }

    // Constructor with all fields
    public HeartRateJarEntity(long id, double value, int bloodOxygen, long epochMillis,
                              int mode, int status, int syncAttempts) {
        this.id = id;
        this.value = value;
        this.bloodOxygen = bloodOxygen;
        this.epochMillis = epochMillis;
        this.mode = mode;
        this.status = status;
        this.syncAttempts = syncAttempts;
    }

    // Getters and Setters
    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }

    public double getValue() {
        return value;
    }

    public void setValue(double value) {
        this.value = value;
    }

    public int getBloodOxygen() {
        return bloodOxygen;
    }

    public void setBloodOxygen(int bloodOxygen) {
        this.bloodOxygen = bloodOxygen;
    }

    public long getEpochMillis() {
        return epochMillis;
    }

    public void setEpochMillis(long epochMillis) {
        this.epochMillis = epochMillis;
    }

    public int getMode() {
        return mode;
    }

    public void setMode(int mode) {
        this.mode = mode;
    }

    public int getStatus() {
        return status;
    }

    public void setStatus(int status) {
        this.status = status;
    }

    public int getSyncAttempts() {
        return syncAttempts;
    }

    public void setSyncAttempts(int syncAttempts) {
        this.syncAttempts = syncAttempts;
    }

    @Override
    public String toString() {
        return "HeartRateJarEntity{" +
                "id=" + id +
                ", value=" + value +
                ", bloodOxygen=" + bloodOxygen +
                ", epochMillis=" + epochMillis +
                ", mode=" + mode +
                ", status=" + status +
                ", syncAttempts=" + syncAttempts +
                '}';
    }
}