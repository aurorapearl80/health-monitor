package com.monitor.health.response.readinghistory;

import com.google.gson.annotations.SerializedName;

import java.util.List;

public class ReadingHistoryItem {
    private String name;
    private double value;
    private String unit;

    @SerializedName("convertedValues")
    private List<ReadingHistoryConvertedValue> convertedValues;

    @SerializedName("loincCode")
    private String loincCode;

    @SerializedName("loincDescription")
    private String loincDescription;

    @SerializedName("patientId")
    private String patientId;

    @SerializedName("readingId")
    private String readingId;

    @SerializedName("patientDeviceId")
    private String patientDeviceId;

    @SerializedName("deviceSource")
    private String deviceSource;

    @SerializedName("transmittingDeviceId")
    private String transmittingDeviceId;

    @SerializedName("deviceSerial")
    private String deviceSerial;

    private String date;
    private String processor;

    @SerializedName("updatedAt")
    private String updatedAt;

    @SerializedName("createdAt")
    private String createdAt;

    private String id;

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public double getValue() { return value; }
    public void setValue(double value) { this.value = value; }

    public String getUnit() { return unit; }
    public void setUnit(String unit) { this.unit = unit; }

    public List<ReadingHistoryConvertedValue> getConvertedValues() { return convertedValues; }
    public void setConvertedValues(List<ReadingHistoryConvertedValue> convertedValues) { this.convertedValues = convertedValues; }

    public String getDate() { return date; }
    public void setDate(String date) { this.date = date; }

    public String getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(String updatedAt) { this.updatedAt = updatedAt; }

    public String getCreatedAt() { return createdAt; }
    public void setCreatedAt(String createdAt) { this.createdAt = createdAt; }

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getDeviceSource() { return deviceSource; }
    public void setDeviceSource(String deviceSource) { this.deviceSource = deviceSource; }

    public String getDeviceSerial() { return deviceSerial; }
    public void setDeviceSerial(String deviceSerial) { this.deviceSerial = deviceSerial; }
}
