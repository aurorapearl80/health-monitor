package com.monitor.health.request.bledevice;


import com.google.gson.annotations.SerializedName;

public class AssignBleRequest {

    @SerializedName("device_id")
    private String deviceId;

    @SerializedName("serial")
    private String serial;

    public AssignBleRequest(String deviceId, String serial) {
        this.deviceId = deviceId;
        this.serial = serial;
    }

    public String getDeviceId() {
        return deviceId;
    }

    public String getSerial() {
        return serial;
    }
}