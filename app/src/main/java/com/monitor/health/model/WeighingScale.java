package com.monitor.health.model;

import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.PrimaryKey;

import com.monitor.health.utility.DateUtils;

import java.util.Date;

@Entity(tableName = "weighing_scale")
public class WeighingScale {
    @PrimaryKey(autoGenerate = true)
    private long id;
    private double weight;
    private String serial;

    @ColumnInfo(name = "created_at")
    private Date createdAt; // Add a field for created_at column
    @ColumnInfo(name = "status", defaultValue = "0")
    private int status;

    public WeighingScale(double weight, int status, String serial) {
        this.weight = weight;
        this.status = status;
        this.serial = serial;
        this.createdAt = new Date(); // Initialize createdAt with current timestamp
    }

    public double getWeight() {
        return weight;
    }

    public void setWeight(long weight) {
        this.weight = weight;
    }

    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }

    public Date getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Date createdAt) {
        this.createdAt = createdAt;
    }

    public String getSerial() {
        return serial;
    }

    public void setSerial(String serial) {
        this.serial = serial;
    }

    public int getStatus() {
        return status;
    }

    public void setStatus(int status) {
        this.status = status;
    }
    public String getCreatedAtFormatted() {
        return DateUtils.getTimeAgo(createdAt);
    }

    public String getCreatedAtShort() {
        return DateUtils.getTimeAgoShort(createdAt.getTime());
    }

    // For debugging - see actual date
    public String getCreatedAtDebug() {
        return DateUtils.timestampToDate(createdAt.getTime());
    }

}
