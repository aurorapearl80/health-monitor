package com.monitor.health.request;

public class HeartRateRequest {
    private String measured_at;
    private String serial;
    private double bpm;
    private String device_id;
    private String timezone;

    public HeartRateRequest(String measured_at, String serial, double bpm,
                            String device_id, String timezone) {
        this.measured_at = measured_at;
        this.serial = serial;
        this.bpm = bpm;
        this.device_id = device_id;
        this.timezone = timezone;
    }

    public String getMeasured_at() { return measured_at; }
    public void setMeasured_at(String measured_at) { this.measured_at = measured_at; }

    public String getSerial() { return serial; }
    public void setSerial(String serial) { this.serial = serial; }

    public double getBpm() { return bpm; }
    public void setBpm(double bpm) { this.bpm = bpm; }

    public String getDevice_id() { return device_id; }
    public void setDevice_id(String device_id) { this.device_id = device_id; }

    public String getTimezone() { return timezone; }
    public void setTimezone(String timezone) { this.timezone = timezone; }
}
