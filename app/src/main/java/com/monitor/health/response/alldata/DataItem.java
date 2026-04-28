package com.monitor.health.response.alldata;

import java.util.List;

public class DataItem {
    private List<Object> value; // holds raw numbers
    private Object readingValue; // one of the ReadingValue* classes
    private String timezone;
    private boolean checkIfNumber;
    private Object additionalInformation;
    private String user;
    private String serial;
    private String patientDevice;
    private List<ReadingType> readingType;
    private boolean manual;
    private String deviceReadingDate;
    private List<Source> source;
    private String createdAt;
    private String updatedAt;
    private String status;
    private String id;
    private List<ReadingMetricValue> readingMetricValues;

    // getters & setters...

    public List<Object> getValue() {
        return value;
    }

    public void setValue(List<Object> value) {
        this.value = value;
    }

    public Object getReadingValue() {
        return readingValue;
    }

    public void setReadingValue(Object readingValue) {
        this.readingValue = readingValue;
    }

    public String getTimezone() {
        return timezone;
    }

    public void setTimezone(String timezone) {
        this.timezone = timezone;
    }

    public boolean isCheckIfNumber() {
        return checkIfNumber;
    }

    public void setCheckIfNumber(boolean checkIfNumber) {
        this.checkIfNumber = checkIfNumber;
    }

    public Object getAdditionalInformation() {
        return additionalInformation;
    }

    public void setAdditionalInformation(Object additionalInformation) {
        this.additionalInformation = additionalInformation;
    }

    public String getUser() {
        return user;
    }

    public void setUser(String user) {
        this.user = user;
    }

    public String getSerial() {
        return serial;
    }

    public void setSerial(String serial) {
        this.serial = serial;
    }

    public String getPatientDevice() {
        return patientDevice;
    }

    public void setPatientDevice(String patientDevice) {
        this.patientDevice = patientDevice;
    }

    public List<ReadingType> getReadingType() {
        return readingType;
    }

    public void setReadingType(List<ReadingType> readingType) {
        this.readingType = readingType;
    }

    public boolean isManual() {
        return manual;
    }

    public void setManual(boolean manual) {
        this.manual = manual;
    }

    public String getDeviceReadingDate() {
        return deviceReadingDate;
    }

    public void setDeviceReadingDate(String deviceReadingDate) {
        this.deviceReadingDate = deviceReadingDate;
    }

    public List<Source> getSource() {
        return source;
    }

    public void setSource(List<Source> source) {
        this.source = source;
    }

    public String getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(String createdAt) {
        this.createdAt = createdAt;
    }

    public String getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(String updatedAt) {
        this.updatedAt = updatedAt;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public List<ReadingMetricValue> getReadingMetricValues() {
        return readingMetricValues;
    }

    public void setReadingMetricValues(List<ReadingMetricValue> readingMetricValues) {
        this.readingMetricValues = readingMetricValues;
    }

    @Override
    public String toString() {
        return "DataItem{" +
                "value=" + value +
                ", readingValue=" + readingValue +
                ", timezone='" + timezone + '\'' +
                ", checkIfNumber=" + checkIfNumber +
                ", additionalInformation=" + additionalInformation +
                ", user='" + user + '\'' +
                ", serial='" + serial + '\'' +
                ", patientDevice='" + patientDevice + '\'' +
                ", readingType=" + readingType +
                ", manual=" + manual +
                ", deviceReadingDate='" + deviceReadingDate + '\'' +
                ", source=" + source +
                ", createdAt='" + createdAt + '\'' +
                ", updatedAt='" + updatedAt + '\'' +
                ", status='" + status + '\'' +
                ", id='" + id + '\'' +
                ", readingMetricValues=" + readingMetricValues +
                '}';
    }
}
