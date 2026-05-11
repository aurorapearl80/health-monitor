package com.monitor.health.request;


public class ReadingRequest {
    private Boolean manual;
    private String timezone;
    private String source;
    private Double temperature;
    private String device_id;
    private String readingType;
    private String measured_at;
    private String serial;

    // Constructor, getters, and setters
    public ReadingRequest() {}

    public ReadingRequest(Boolean manual, String timezone, String source, Double temperature, String device_id, String readingType,
                          String measured_at, String serial) {
        this.manual = manual;
        this.timezone = timezone;
        this.source = source;
        this.temperature = temperature;
        this.device_id = device_id;
        this.readingType = readingType;
        this.measured_at = measured_at;
        this.serial = serial;
    }

    // Getters and setters
    // Omitted for brevity, can be generated using IDE or manually


    public Boolean getManual() {
        return manual;
    }

    public void setManual(Boolean manual) {
        this.manual = manual;
    }

    public String getTimezone() {
        return timezone;
    }

    public void setTimezone(String timezone) {
        this.timezone = timezone;
    }

    public String getSource() {
        return source;
    }

    public void setSource(String source) {
        this.source = source;
    }

    public Double getTemperature() {
        return temperature;
    }

    public void setTemperature(Double temperature) {
        this.temperature = temperature;
    }

    public String getReadingType() {
        return readingType;
    }

    public void setReadingType(String readingType) {
        this.readingType = readingType;
    }

    public String getDevice_id() {
        return device_id;
    }

    public void setDevice_id(String device_id) {
        this.device_id = device_id;
    }

    public String getMeasured_at() {
        return measured_at;
    }

    public void setMeasured_at(String measured_at) {
        this.measured_at = measured_at;
    }

    public String getSerial() {
        return serial;
    }

    public void setSerial(String serial) {
        this.serial = serial;
    }
}
