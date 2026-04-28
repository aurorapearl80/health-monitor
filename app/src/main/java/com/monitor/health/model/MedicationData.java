package com.monitor.health.model;

import java.util.List;

public class MedicationData {
    private String id;
    private List<String> additionalSchedule;
    private String name;
    private String description;
    private String type;
    private String unit;
    private String photo;
    private String notes;
    private boolean allowReminder;
    private MainSchedule mainSchedule;
    private String timezone;
    private String startDate;
    private String endDate;
    private String createdAt;
    private String updatedAt;

    // Getters and Setters

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public List<String> getAdditionalSchedule() {
        return additionalSchedule;
    }

    public void setAdditionalSchedule(List<String> additionalSchedule) {
        this.additionalSchedule = additionalSchedule;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getUnit() {
        return unit;
    }

    public void setUnit(String unit) {
        this.unit = unit;
    }

    public String getPhoto() {
        return photo;
    }

    public void setPhoto(String photo) {
        this.photo = photo;
    }

    public String getNotes() {
        return notes;
    }

    public void setNotes(String notes) {
        this.notes = notes;
    }

    public boolean isAllowReminder() {
        return allowReminder;
    }

    public void setAllowReminder(boolean allowReminder) {
        this.allowReminder = allowReminder;
    }

    public MainSchedule getMainSchedule() {
        return mainSchedule;
    }

    public void setMainSchedule(MainSchedule mainSchedule) {
        this.mainSchedule = mainSchedule;
    }

    public String getTimezone() {
        return timezone;
    }

    public void setTimezone(String timezone) {
        this.timezone = timezone;
    }

    public String getStartDate() {
        return startDate;
    }

    public void setStartDate(String startDate) {
        this.startDate = startDate;
    }

    public String getEndDate() {
        return endDate;
    }

    public void setEndDate(String endDate) {
        this.endDate = endDate;
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
}
