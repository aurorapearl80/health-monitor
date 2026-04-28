package com.monitor.health.model;

import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.PrimaryKey;

import com.monitor.health.utility.DateUtils;

import java.util.Date;

@Entity(tableName = "oximeter")
public class Oximeter {
    @PrimaryKey(autoGenerate = true)
    private long id;
    private int pulseRate;
    private int oxygen;
    private String serial;

    @ColumnInfo(name = "created_at")
    private Date createdAt; // Add a field for created_at column
    @ColumnInfo(name = "status", defaultValue = "0")
    private int status;


    public Oximeter(int pulseRate, int oxygen, int status, String serial) {
        this.oxygen= oxygen;
        this.pulseRate = pulseRate;
        this.createdAt = new Date(); // Initialize createdAt with current timestamp
        this.status = status;
        this.serial = serial;
    }


    public int getPulseRate() {
        return pulseRate;
    }

    public void setPulseRate(int pulseRate) {
        this.pulseRate = pulseRate;
    }

    // Getter and setter methods for createdAt field
    public Date getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Date createdAt) {
        this.createdAt = createdAt;
    }

    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }

    public int getStatus() {
        return status;
    }

    public void setStatus(int status) {
        this.status = status;
    }

    public String getSerial() {
        return serial;
    }

    public void setSerial(String serial) {
        this.serial = serial;
    }

    public int getOxygen() {
        return oxygen;
    }

    public void setOxygen(int oxygen) {
        this.oxygen = oxygen;
    }

    // Getter for human readable time
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


    @Override
    public String toString() {
        return "BPJumper{" +
                "id=" + id +
                ", oxygen=" + oxygen +
                ", pulseRate=" + pulseRate +
                ", createdAt=" + createdAt +
                ", status=" + status+
                ", serial=" + serial+
                '}';
    }
}
