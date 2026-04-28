package com.monitor.health.model;

import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.Ignore;
import androidx.room.PrimaryKey;

import com.monitor.health.utility.DateUtils;

import java.util.Date;

@Entity(tableName = "reading_values")
public class ReadingValue {

    @PrimaryKey(autoGenerate = true)
    private long id;
    private long glucose;
    private int event;
    private String eventDescription;
    private String serial;
    private int mailValue;
    private String unitValue;

    @ColumnInfo(name = "created_at")
    private Date createdAt; // Add a field for created_at column
    @ColumnInfo(name = "status", defaultValue = "0")
    private int status;

    @Ignore
    public ReadingValue() {
        this.createdAt = new Date(); // Initialize createdAt with current timestamp
    }
    public ReadingValue(long glucose, int event, String eventDescription, int status, String serial, int mailValue, String unitValue) {
        this.glucose = glucose;
        this.event = event;
        this.eventDescription = eventDescription;
        this.createdAt = new Date(); // Initialize createdAt with current timestamp
        this.status = status;
        this.serial = serial;
        this.mailValue = mailValue;
        this.unitValue = unitValue;
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

    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }

    public long getGlucose() {
        return glucose;
    }

    public void setGlucose(long glucose) {
        this.glucose = glucose;
    }

    public int getEvent() {
        return event;
    }

    public void setEvent(int event) {
        this.event = event;
    }

    public String getEventDescription() {
        return eventDescription;
    }

    public void setEventDescription(String eventDescription) {
        this.eventDescription = eventDescription;
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

    // Getter and setter methods for createdAt field
    public Date getCreatedAt() {
        return createdAt;
    }
    public void setCreatedAt(Date createdAt) {
        this.createdAt = createdAt;
    }

    public int getMailValue() {
        return mailValue;
    }

    public void setMailValue(int mailValue) {
        this.mailValue = mailValue;
    }

    public String getUnitValue() {
        return unitValue;
    }

    public void setUnitValue(String unitValue) {
        this.unitValue = unitValue;
    }

    @Override
    public String toString() {
        return "ReadingValue{" +
                "id=" + id +
                ", glucose=" + glucose +
                ", event=" + event +
                ", eventDescription='" + eventDescription + '\'' +
                ", serial='" + serial + '\'' +
                ", createdAt=" + createdAt +
                ", status=" + status +
                '}';
    }
}
