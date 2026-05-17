package com.monitor.health.request;

public class OximeterRequest {
    private String measured_at;
    private String serial;
    private double oxygen;
    private String device_id;
    private String timezone;
    private int pulse_rate;

    public OximeterRequest() {}

    public OximeterRequest(String measured_at, String serial, double oxygen, String device_id,
                           String timezone, int pulse_rate) {
        this.measured_at = measured_at;
        this.serial = serial;
        this.oxygen = oxygen;
        this.device_id = device_id;
        this.timezone = timezone;
        this.pulse_rate = pulse_rate;
    }

    public String getMeasured_at() { return measured_at; }
    public void setMeasured_at(String measured_at) { this.measured_at = measured_at; }

    public String getSerial() { return serial; }
    public void setSerial(String serial) { this.serial = serial; }

    public double getOxygen() { return oxygen; }
    public void setOxygen(double oxygen) { this.oxygen = oxygen; }

    public String getDevice_id() { return device_id; }
    public void setDevice_id(String device_id) { this.device_id = device_id; }

    public String getTimezone() { return timezone; }
    public void setTimezone(String timezone) { this.timezone = timezone; }

    public int getPulse_rate() { return pulse_rate; }
    public void setPulse_rate(int pulse_rate) { this.pulse_rate = pulse_rate; }
}
