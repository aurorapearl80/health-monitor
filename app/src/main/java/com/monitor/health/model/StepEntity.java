package com.monitor.health.model;

import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.PrimaryKey;

import java.util.Date;

@Entity(tableName = "steps")
public class StepEntity {

    @PrimaryKey(autoGenerate = true)
    private long id;

    @ColumnInfo(name = "delta")
    private int delta;

    @ColumnInfo(name = "epoch_millis")
    private long epochMillis;

    @ColumnInfo(name = "created_at")
    private Date createdAt;

    @ColumnInfo(name = "status", defaultValue = "0")
    private int status;

    public StepEntity(int delta) {
        this.delta = delta;
        this.epochMillis = System.currentTimeMillis();
        this.createdAt = new Date();
        this.status = 0;
    }

    public long getId() { return id; }
    public void setId(long id) { this.id = id; }

    public int getDelta() { return delta; }
    public void setDelta(int delta) { this.delta = delta; }

    public long getEpochMillis() { return epochMillis; }
    public void setEpochMillis(long epochMillis) { this.epochMillis = epochMillis; }

    public Date getCreatedAt() { return createdAt; }
    public void setCreatedAt(Date createdAt) { this.createdAt = createdAt; }

    public int getStatus() { return status; }
    public void setStatus(int status) { this.status = status; }
}