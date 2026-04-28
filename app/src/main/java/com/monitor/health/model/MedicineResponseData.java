package com.monitor.health.model;

import java.util.Date;

public class MedicineResponseData {
    public String _id;
    public boolean withReading;
    public String referenceMedicineId;
    public String medicineSchedule;
    public String name;
    public String type2;
    public String type;
    public int amount;
    public String unit;
    public String timeTaken;
    public String dayTaken;
    public String dateTaken;
    public String patient;
    public String createdBy;
    public Date updatedAt;
    public Date createdAt;

    public String get_id() {
        return _id;
    }

    public void set_id(String _id) {
        this._id = _id;
    }

    public boolean isWithReading() {
        return withReading;
    }

    public void setWithReading(boolean withReading) {
        this.withReading = withReading;
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

    public String getDayTaken() {
        return dayTaken;
    }

    public void setDayTaken(String dayTaken) {
        this.dayTaken = dayTaken;
    }

    public String getDateTaken() {
        return dateTaken;
    }

    public void setDateTaken(String dateTaken) {
        this.dateTaken = dateTaken;
    }

    public String getPatient() {
        return patient;
    }

    public void setPatient(String patient) {
        this.patient = patient;
    }

    public String getCreatedBy() {
        return createdBy;
    }

    public void setCreatedBy(String createdBy) {
        this.createdBy = createdBy;
    }

    public Date getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(Date updatedAt) {
        this.updatedAt = updatedAt;
    }

    public Date getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Date createdAt) {
        this.createdAt = createdAt;
    }
}
