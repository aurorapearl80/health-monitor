package com.monitor.health.model;

import androidx.room.Entity;
import androidx.room.Index;
import androidx.room.PrimaryKey;

@Entity(
        tableName = "ble_device",
        indices = {@Index(value = {"serial"}, unique = true)}
)
public class BleDeviceModel {
    @PrimaryKey(autoGenerate = true)
    private long id;

    private String serial;
    private String deviceId;
    private String serverId;
    private String deviceName;
    private String deviceAddress;
    private boolean isConnected; // NEW: Track if device is connected

    public BleDeviceModel() {
         // Default to not connected
    }

    public BleDeviceModel(long id, String serial, String deviceId, String serverId, String deviceName, String deviceAddress) {
        this.id = id;
        this.serial = serial;
        this.deviceId = deviceId;
        this.serverId = serverId;
        this.deviceName = deviceName;
        this.deviceAddress = deviceAddress;
    }

    public BleDeviceModel(long id, String serial, String deviceId, String serverId, String deviceName, String deviceAddress, boolean isConnected) {
        this.id = id;
        this.serial = serial;
        this.deviceId = deviceId;
        this.serverId = serverId;
        this.deviceName = deviceName;
        this.deviceAddress = deviceAddress;
        this.isConnected = isConnected;
    }

    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }

    public String getSerial() {
        return serial;
    }

    public void setSerial(String serial) {
        // Normalize to lowercase to avoid case-sensitivity issues
        this.serial = (serial != null) ? serial.toLowerCase() : null;
    }

    public String getDeviceId() {
        return deviceId;
    }

    public void setDeviceId(String deviceId) {
        this.deviceId = deviceId;
    }

    public String getServerId() {
        return serverId;
    }

    public void setServerId(String serverId) {
        this.serverId = serverId;
    }

    public String getDeviceName() {
        return deviceName;
    }

    public void setDeviceName(String deviceName) {
        this.deviceName = deviceName;
    }

    public String getDeviceAddress() {
        return deviceAddress;
    }

    public void setDeviceAddress(String deviceAddress) {
        // Normalize to lowercase to avoid case-sensitivity issues
        this.deviceAddress = (deviceAddress != null) ? deviceAddress.toLowerCase() : null;
    }

    public boolean isConnected() {
        return isConnected;
    }

    public void setConnected(boolean connected) {
        isConnected = connected;
    }

    @Override
    public String toString() {
        return "BleDeviceModel{" +
                "id=" + id +
                ", serial='" + serial + '\'' +
                ", deviceId='" + deviceId + '\'' +
                ", serverId='" + serverId + '\'' +
                ", deviceName='" + deviceName + '\'' +
                ", deviceAddress='" + deviceAddress + '\'' +
                ", isConnected=" + isConnected +
                '}';
    }
}