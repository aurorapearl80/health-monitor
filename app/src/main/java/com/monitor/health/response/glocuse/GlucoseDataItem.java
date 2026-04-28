package com.monitor.health.response.glocuse;

import java.util.List;

public class GlucoseDataItem {

    private List<Integer> value;
    private GlucoseReadingValue readingValue;
    private String status;
    private String timezone;
    private boolean checkIfNumber;
    private String additionalInformation;
    private String user;
    private String serial;
    private String patientDevice;
    private List<GlucoseReadingType> readingType;
    private boolean manual;
    private String deviceReadingDate;
    private List<GlucoseSource> source;
    private String createdAt;
    private String updatedAt;
    private String id;

    public List<Integer> getValue() {
        return value;
    }

    public void setValue(List<Integer> value) {
        this.value = value;
    }

    public GlucoseReadingValue getReadingValue() {
        return readingValue;
    }

    public void setReadingValue(GlucoseReadingValue readingValue) {
        this.readingValue = readingValue;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
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

    public String getAdditionalInformation() {
        return additionalInformation;
    }

    public void setAdditionalInformation(String additionalInformation) {
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

    public List<GlucoseReadingType> getReadingType() {
        return readingType;
    }

    public void setReadingType(List<GlucoseReadingType> readingType) {
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

    public List<GlucoseSource> getSource() {
        return source;
    }

    public void setSource(List<GlucoseSource> source) {
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

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }
}
