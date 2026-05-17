package com.monitor.health.response.bloodpressure;

public class BloodPressureResponse {
    private String message;
    private BloodPressureData data;

    public String getMessage() { return message; }
    public void setMessage(String message) { this.message = message; }

    public BloodPressureData getData() { return data; }
    public void setData(BloodPressureData data) { this.data = data; }
}
