package com.monitor.health.model;


import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.PrimaryKey;

import java.util.Date;

@Entity(tableName = "heart_rate")
public class HeartRateEntity {
    @PrimaryKey(autoGenerate = true)
    private long id;
    @ColumnInfo(name = "value")
    private double value;
    private long epochMillis;          // UTC timestamp
    @ColumnInfo(name = "created_at", defaultValue = "CURRENT_TIMESTAMP")
    private Date createdAt; // Add a field for created_at column
    @ColumnInfo(name = "status", defaultValue = "0")
    private int status;

    public HeartRateEntity(double value) {
        this.value = value;
        this.createdAt = new Date(); // Set current date/time
        this.epochMillis = System.currentTimeMillis();
    }

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

    public Date getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Date createdAt) {
        this.createdAt = createdAt;
    }

    public int getStatus() {
        return status;
    }

    public void setStatus(int status) {
        this.status = status;
    }

    public long getEpochMillis() {
        return epochMillis;
    }

    public void setEpochMillis(long epochMillis) {
        this.epochMillis = epochMillis;
    }
}
