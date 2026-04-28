package com.monitor.health.response.bledevice;

import com.google.gson.annotations.SerializedName;

public class BLEData {

    @SerializedName("id")
    private String id;

    @SerializedName("serial")
    private String serial;

    @SerializedName("createdAt")
    private String createdAt;

    @SerializedName("assignedSource")
    private String assignedSource;

    @SerializedName("device_id")
    private String deviceId;

    @SerializedName("device_details")
    private DeviceDetails deviceDetails;

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getSerial() {
        return serial;
    }

    public void setSerial(String serial) {
        this.serial = serial;
    }

    public String getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(String createdAt) {
        this.createdAt = createdAt;
    }

    public DeviceDetails getDeviceDetails() {
        return deviceDetails;
    }

    public void setDeviceDetails(DeviceDetails deviceDetails) {
        this.deviceDetails = deviceDetails;
    }

    public String getAssignedSource() {
        return assignedSource;
    }

    public void setAssignedSource(String assignedSource) {
        this.assignedSource = assignedSource;
    }


    public String getDeviceId() {
        return deviceId;
    }

    public void setDeviceId(String deviceId) {
        this.deviceId = deviceId;
    }

    @Override
    public String toString() {
        return "BLEData{" +
                "id='" + id + '\'' +
                ", serial='" + serial + '\'' +
                ", createdAt='" + createdAt + '\'' +
                ", deviceDetails=" + deviceDetails +
                '}';
    }
}
