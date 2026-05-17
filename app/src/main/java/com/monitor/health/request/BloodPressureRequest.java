package com.monitor.health.request;

public class BloodPressureRequest {
    private String measured_at;
    private String serial;
    private double systolic;
    private double diastolic;
    private String device_id;
    private double bpm;
    private String timezone;

    public BloodPressureRequest() {}

    public BloodPressureRequest(String measured_at, String serial, double systolic,
                                double diastolic, String device_id, double bpm, String timezone) {
        this.measured_at = measured_at;
        this.serial = serial;
        this.systolic = systolic;
        this.diastolic = diastolic;
        this.device_id = device_id;
        this.bpm = bpm;
        this.timezone = timezone;
    }

    public String getMeasured_at() { return measured_at; }
    public void setMeasured_at(String measured_at) { this.measured_at = measured_at; }

    public String getSerial() { return serial; }
    public void setSerial(String serial) { this.serial = serial; }

    public double getSystolic() { return systolic; }
    public void setSystolic(double systolic) { this.systolic = systolic; }

    public double getDiastolic() { return diastolic; }
    public void setDiastolic(double diastolic) { this.diastolic = diastolic; }

    public String getDevice_id() { return device_id; }
    public void setDevice_id(String device_id) { this.device_id = device_id; }

    public double getBpm() { return bpm; }
    public void setBpm(double bpm) { this.bpm = bpm; }

    public String getTimezone() { return timezone; }
    public void setTimezone(String timezone) { this.timezone = timezone; }
}
