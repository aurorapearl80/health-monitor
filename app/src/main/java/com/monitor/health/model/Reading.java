package com.monitor.health.model;


import java.util.List;

public class Reading {
    private Boolean manual;
    private String timezone;
    private String source;
    private List<Double> value;
    private String device;
    private String readingType;
    private String date;
    private String serial;

    // Constructor, getters, and setters
    public Reading() {}

    public Reading(Boolean manual, String timezone, String source, List<Double> value, String device, String readingType,
                   String date, String serial) {
        this.manual = manual;
        this.timezone = timezone;
        this.source = source;
        this.value = value;
        this.device = device;
        this.readingType = readingType;
        this.date = date;
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

    public List<Double> getValue() {
        return value;
    }

    public void setValue(List<Double> value) {
        this.value = value;
    }

    public String getDevice() {
        return device;
    }

    public void setDevice(String device) {
        this.device = device;
    }

    public String getReadingType() {
        return readingType;
    }

    public void setReadingType(String readingType) {
        this.readingType = readingType;
    }

    public String getDate() {
        return date;
    }

    public void setDate(String date) {
        this.date = date;
    }

    public String getSerial() {
        return serial;
    }

    public void setSerial(String serial) {
        this.serial = serial;
    }
}
