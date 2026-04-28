package com.monitor.health.response.permetricdata;

import java.util.List;
import com.google.gson.annotations.SerializedName;

public class PerMetricDataItem {
    private String name;
    private Double value;
    private String unit;

    @SerializedName("convertedValues")
    private List<ConvertedValue> convertedValues;

    private String loincCode;
    private String loincDescription;
    private String patientId;
    private String readingId;
    private String patientDeviceId;
    private String deviceSource;
    private String transmittingDeviceId;
    private String deviceSerial;
    private String date;
    private String processor;
    private String updatedAt;
    private String createdAt;
    private Integer healthScore;

    private RatingInfo ratingInfo;

    private String id;

    // Getters/Setters

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public Double getValue() { return value; }
    public void setValue(Double value) { this.value = value; }

    public String getUnit() { return unit; }
    public void setUnit(String unit) { this.unit = unit; }

    public List<ConvertedValue> getConvertedValues() { return convertedValues; }
    public void setConvertedValues(List<ConvertedValue> convertedValues) { this.convertedValues = convertedValues; }

    public String getLoincCode() { return loincCode; }
    public void setLoincCode(String loincCode) { this.loincCode = loincCode; }

    public String getLoincDescription() { return loincDescription; }
    public void setLoincDescription(String loincDescription) { this.loincDescription = loincDescription; }

    public String getPatientId() { return patientId; }
    public void setPatientId(String patientId) { this.patientId = patientId; }

    public String getReadingId() { return readingId; }
    public void setReadingId(String readingId) { this.readingId = readingId; }

    public String getPatientDeviceId() { return patientDeviceId; }
    public void setPatientDeviceId(String patientDeviceId) { this.patientDeviceId = patientDeviceId; }

    public String getDeviceSource() { return deviceSource; }
    public void setDeviceSource(String deviceSource) { this.deviceSource = deviceSource; }

    public String getTransmittingDeviceId() { return transmittingDeviceId; }
    public void setTransmittingDeviceId(String transmittingDeviceId) { this.transmittingDeviceId = transmittingDeviceId; }

    public String getDeviceSerial() { return deviceSerial; }
    public void setDeviceSerial(String deviceSerial) { this.deviceSerial = deviceSerial; }

    public String getDate() { return date; }
    public void setDate(String date) { this.date = date; }

    public String getProcessor() { return processor; }
    public void setProcessor(String processor) { this.processor = processor; }

    public String getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(String updatedAt) { this.updatedAt = updatedAt; }

    public String getCreatedAt() { return createdAt; }
    public void setCreatedAt(String createdAt) { this.createdAt = createdAt; }

    public Integer getHealthScore() { return healthScore; }
    public void setHealthScore(Integer healthScore) { this.healthScore = healthScore; }

    public RatingInfo getRatingInfo() { return ratingInfo; }
    public void setRatingInfo(RatingInfo ratingInfo) { this.ratingInfo = ratingInfo; }

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    @Override
    public String toString() {
        return "PerMetricDataItem{" +
                "name='" + name + '\'' +
                ", value=" + value +
                ", unit='" + unit + '\'' +
                ", convertedValues=" + convertedValues +
                ", loincCode='" + loincCode + '\'' +
                ", loincDescription='" + loincDescription + '\'' +
                ", patientId='" + patientId + '\'' +
                ", readingId='" + readingId + '\'' +
                ", patientDeviceId='" + patientDeviceId + '\'' +
                ", deviceSource='" + deviceSource + '\'' +
                ", transmittingDeviceId='" + transmittingDeviceId + '\'' +
                ", deviceSerial='" + deviceSerial + '\'' +
                ", date='" + date + '\'' +
                ", processor='" + processor + '\'' +
                ", updatedAt='" + updatedAt + '\'' +
                ", createdAt='" + createdAt + '\'' +
                ", healthScore=" + healthScore +
                ", ratingInfo=" + ratingInfo +
                ", id='" + id + '\'' +
                '}';
    }
}
