package com.monitor.health.response.weight;

public class WeightResponse {
    private String message;
    private WeightData data;

    public String getMessage() { return message; }
    public void setMessage(String message) { this.message = message; }

    public WeightData getData() { return data; }
    public void setData(WeightData data) { this.data = data; }
}
