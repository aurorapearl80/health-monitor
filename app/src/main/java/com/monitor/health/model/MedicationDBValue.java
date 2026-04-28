package com.monitor.health.model;

import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.Ignore;
import androidx.room.PrimaryKey;

import java.util.Date;

@Entity(tableName = "medication")
public class MedicationDBValue {
    @PrimaryKey(autoGenerate = true)
    private long id;
    public String referenceMedicineId;
    public String medicineSchedule;
    public String name;
    public String type2;
    public String type;
    public int amount;
    public String unit;
    public String timeTaken;
    public String actualTimeTaken;
    public String dayTaken;
    public Date dateTaken;
    public boolean selected;
    public String parentDescription;
    public String childDescription;
    public String time;
    public String timeDisplay;
    public int quantity;
    public int tagHour;
    public int tagMinutes;
    public int hours;
    public int minutes;
    public int stateParameter;
    public int configurationParameter;

    @ColumnInfo(name = "created_at")
    private Date createdAt; // Add a field for created_at column
    @ColumnInfo(name = "status", defaultValue = "0")
    private int status;
    @Ignore
    public MedicationDBValue(String referenceMedicineId, String medicineSchedule, String name, String type2, String type, int amount, String unit, String timeTaken, String actualTimeTaken, String dayTaken, Date dateTaken, boolean selected, String parentDescription, String childDescription, String time, String timeDisplay, int quantity,
                             int tagHour, int tagMinutes, int hours, int minutes, int status, int setTagHour, int configurationParameter) {
        this.referenceMedicineId = referenceMedicineId;
        this.medicineSchedule = medicineSchedule;
        this.name = name;
        this.type2 = type2;
        this.type = type;
        this.amount = amount;
        this.unit = unit;
        this.timeTaken = timeTaken;
        this.actualTimeTaken = actualTimeTaken;
        this.dayTaken = dayTaken;
        this.dateTaken = dateTaken;
        this.selected = selected;
        this.parentDescription = parentDescription;
        this.childDescription = childDescription;
        this.time = time;
        this.timeDisplay = timeDisplay;
        this.quantity = quantity;
        this.tagHour = tagHour;
        this.tagMinutes = tagMinutes;
        this.hours = hours;
        this.minutes = minutes;
        this.createdAt = new Date(); // Initialize createdAt with current timestamp
        this.status = status;
        this.stateParameter = setTagHour;
        this.configurationParameter = configurationParameter;
    }

    public MedicationDBValue() {
    }

    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }

    public String getReferenceMedicineId() {
        return referenceMedicineId;
    }

    public void setReferenceMedicineId(String referenceMedicineId) {
        this.referenceMedicineId = referenceMedicineId;
    }

    public String getMedicineSchedule() {
        return medicineSchedule;
    }

    public void setMedicineSchedule(String medicineSchedule) {
        this.medicineSchedule = medicineSchedule;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getType2() {
        return type2;
    }

    public void setType2(String type2) {
        this.type2 = type2;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public int getAmount() {
        return amount;
    }

    public void setAmount(int amount) {
        this.amount = amount;
    }

    public String getUnit() {
        return unit;
    }

    public void setUnit(String unit) {
        this.unit = unit;
    }

    public String getTimeTaken() {
        return timeTaken;
    }

    public void setTimeTaken(String timeTaken) {
        this.timeTaken = timeTaken;
    }

    public String getActualTimeTaken() {
        return actualTimeTaken;
    }

    public void setActualTimeTaken(String actualTimeTaken) {
        this.actualTimeTaken = actualTimeTaken;
    }

    public String getDayTaken() {
        return dayTaken;
    }

    public void setDayTaken(String dayTaken) {
        this.dayTaken = dayTaken;
    }

    public Date getDateTaken() {
        return dateTaken;
    }

    public void setDateTaken(Date dateTaken) {
        this.dateTaken = dateTaken;
    }

    public boolean isSelected() {
        return selected;
    }

    public void setSelected(boolean selected) {
        this.selected = selected;
    }

    public String getParentDescription() {
        return parentDescription;
    }

    public void setParentDescription(String parentDescription) {
        this.parentDescription = parentDescription;
    }

    public String getTime() {
        return time;
    }

    public void setTime(String time) {
        this.time = time;
    }

    public String getTimeDisplay() {
        return timeDisplay;
    }

    public void setTimeDisplay(String timeDisplay) {
        this.timeDisplay = timeDisplay;
    }

    public int getQuantity() {
        return quantity;
    }

    public void setQuantity(int quantity) {
        this.quantity = quantity;
    }

    public int getTagHour() {
        return tagHour;
    }

    public void setTagHour(int tagHour) {
        this.tagHour = tagHour;
    }

    public int getTagMinutes() {
        return tagMinutes;
    }

    public void setTagMinutes(int tagMinutes) {
        this.tagMinutes = tagMinutes;
    }

    public int getHours() {
        return hours;
    }

    public void setHours(int hours) {
        this.hours = hours;
    }

    public int getMinutes() {
        return minutes;
    }

    public void setMinutes(int minutes) {
        this.minutes = minutes;
    }

    // Getter and setter methods for createdAt field
    public Date getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Date createdAt) {
        this.createdAt = createdAt;
    }

    public int getStatus() {
        return status;
    }

    public void setStatus(int status) {
        this.status = status;
    }

    public int getStateParameter() {
        return stateParameter;
    }

    public void setStateParameter(int stateParameter) {
        this.stateParameter = stateParameter;
    }

    public int getConfigurationParameter() {
        return configurationParameter;
    }

    public void setConfigurationParameter(int configurationParameter) {
        this.configurationParameter = configurationParameter;
    }

    public String getChildDescription() {
        return childDescription;
    }

    public void setChildDescription(String childDescription) {
        this.childDescription = childDescription;
    }

    @Override
    public String toString() {
        return "MedicationDBValue{" +
                "id=" + id +
                ", referenceMedicineId='" + referenceMedicineId + '\'' +
                ", medicineSchedule='" + medicineSchedule + '\'' +
                ", name='" + name + '\'' +
                ", type2='" + type2 + '\'' +
                ", type='" + type + '\'' +
                ", amount=" + amount +
                ", unit='" + unit + '\'' +
                ", timeTaken='" + timeTaken + '\'' +
                ", actualTimeTaken='" + actualTimeTaken + '\'' +
                ", dayTaken='" + dayTaken + '\'' +
                ", dateTaken=" + dateTaken +
                ", selected=" + selected +
                ", parentDescription='" + parentDescription + '\'' +
                ", parentDescription='" + childDescription + '\'' +
                ", time='" + time + '\'' +
                ", timeDisplay='" + timeDisplay + '\'' +
                ", quantity=" + quantity +
                ", tagHour=" + tagHour +
                ", tagMinutes=" + tagMinutes +
                ", hours=" + hours +
                ", minutes=" + minutes +
                ", stateParameter=" + stateParameter +
                ", configurationParameter=" + configurationParameter +
                ", createdAt=" + createdAt +
                ", status=" + status +
                '}';
    }
}
