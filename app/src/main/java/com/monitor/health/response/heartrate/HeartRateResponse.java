package com.monitor.health.response.heartrate;

public class HeartRateResponse {
    private String message;
    private HeartRateData data;

    public String getMessage() { return message; }
    public void setMessage(String message) { this.message = message; }

    public HeartRateData getData() { return data; }
    public void setData(HeartRateData data) { this.data = data; }
}
