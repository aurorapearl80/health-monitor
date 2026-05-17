package com.monitor.health.response.oximeter;

public class OximeterResponse {
    private String message;
    private OximeterData data;

    public String getMessage() { return message; }
    public void setMessage(String message) { this.message = message; }

    public OximeterData getData() { return data; }
    public void setData(OximeterData data) { this.data = data; }
}
