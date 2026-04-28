package com.monitor.health.entity;


import androidx.room.Entity;
import androidx.room.PrimaryKey;
@Entity(tableName = "TypeAvailability")
public class TypeAvailabilityEntity {

    @PrimaryKey(autoGenerate = true)
    private long id;
    private boolean bloodGlucose;
    private boolean bloodPressure;

    private boolean weight;

    private boolean bloodOxygen;

    private boolean electrocardiogram;

    private boolean temperature;

    public TypeAvailabilityEntity() {
    }

    public TypeAvailabilityEntity(boolean bloodGlucose, boolean bloodPressure, boolean weight, boolean bloodOxygen, boolean electrocardiogram, boolean temperature) {
        this.bloodGlucose = bloodGlucose;
        this.bloodPressure = bloodPressure;
        this.weight = weight;
        this.bloodOxygen = bloodOxygen;
        this.electrocardiogram = electrocardiogram;
        this.temperature = temperature;
    }


    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }

    public boolean isBloodGlucose() {
        return bloodGlucose;
    }

    public void setBloodGlucose(boolean bloodGlucose) {
        this.bloodGlucose = bloodGlucose;
    }

    public boolean isBloodPressure() {
        return bloodPressure;
    }

    public void setBloodPressure(boolean bloodPressure) {
        this.bloodPressure = bloodPressure;
    }

    public boolean isWeight() {
        return weight;
    }

    public void setWeight(boolean weight) {
        this.weight = weight;
    }

    public boolean isBloodOxygen() {
        return bloodOxygen;
    }

    public void setBloodOxygen(boolean bloodOxygen) {
        this.bloodOxygen = bloodOxygen;
    }

    public boolean isElectrocardiogram() {
        return electrocardiogram;
    }

    public void setElectrocardiogram(boolean electrocardiogram) {
        this.electrocardiogram = electrocardiogram;
    }

    public boolean isTemperature() {
        return temperature;
    }

    public void setTemperature(boolean temperature) {
        this.temperature = temperature;
    }

    @Override
    public String toString() {
        return "ModelTypeAvailability{" +
                "bloodGlucose=" + bloodGlucose +
                ", bloodPressure=" + bloodPressure +
                ", weight=" + weight +
                ", bloodOxygen=" + bloodOxygen +
                ", electrocardiogram=" + electrocardiogram +
                ", temperature=" + temperature +
                '}';
    }
}
