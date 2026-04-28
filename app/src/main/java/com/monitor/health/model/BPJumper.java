package com.monitor.health.model;

import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.PrimaryKey;

import com.monitor.health.utility.DateUtils;

import java.util.Date;

@Entity(tableName = "bp_jumper")
public class BPJumper {
    @PrimaryKey(autoGenerate = true)
    private long id;
    private int systolic;
    private int diastolic;
    private int pulseRate;
    private String serial;

    @ColumnInfo(name = "created_at")
    private Date createdAt; // Add a field for created_at column
    @ColumnInfo(name = "status", defaultValue = "0")
    private int status;


    public BPJumper(int systolic, int diastolic, int pulseRate, int status, String serial) {
        this.systolic = systolic;
        this.diastolic = diastolic;
        this.pulseRate = pulseRate;
        this.createdAt = new Date(); // Initialize createdAt with current timestamp
        this.status = status;
        this.serial = serial;
    }


    public int getSystolic() {
        return systolic;
    }

    public void setSystolic(int systolic) {
        this.systolic = systolic;
    }

    public int getDiastolic() {
        return diastolic;
    }

    public void setDiastolic(int diastolic) {
        this.diastolic = diastolic;
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
                ", systolic=" + systolic +
                ", diastolic=" + diastolic +
                ", pulseRate=" + pulseRate +
                ", createdAt=" + createdAt +
                ", status=" + status+
                ", serial=" + serial+
                '}';
    }
}
