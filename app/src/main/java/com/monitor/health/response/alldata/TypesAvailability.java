package com.monitor.health.response.alldata;

import com.google.gson.annotations.SerializedName;

public class TypesAvailability {

    @SerializedName("blood_glucose")
    private boolean bloodGlucose;

    @SerializedName("blood_pressure")
    private boolean bloodPressure;

    @SerializedName("weight")
    private boolean weight;

    @SerializedName("blood_oxygen")
    private boolean bloodOxygen;

    @SerializedName("electrocardiogram")
    private boolean electrocardiogram;

    @SerializedName("temperature")
    private boolean temperature;

    public TypesAvailability() {}


    public TypesAvailability(boolean bloodGlucose, boolean bloodPressure, boolean weight, boolean bloodOxygen, boolean electrocardiogram, boolean temperature) {
        this.bloodGlucose = bloodGlucose;
        this.bloodPressure = bloodPressure;
        this.weight = weight;
        this.bloodOxygen = bloodOxygen;
        this.electrocardiogram = electrocardiogram;
        this.temperature = temperature;
    }

    public boolean isBloodGlucose() { return bloodGlucose; }
    public void setBloodGlucose(boolean bloodGlucose) { this.bloodGlucose = bloodGlucose; }

    public boolean isBloodPressure() { return bloodPressure; }
    public void setBloodPressure(boolean bloodPressure) { this.bloodPressure = bloodPressure; }

    public boolean isWeight() { return weight; }
    public void setWeight(boolean weight) { this.weight = weight; }

    public boolean isBloodOxygen() { return bloodOxygen; }
    public void setBloodOxygen(boolean bloodOxygen) { this.bloodOxygen = bloodOxygen; }

    public boolean isElectrocardiogram() { return electrocardiogram; }
    public void setElectrocardiogram(boolean electrocardiogram) { this.electrocardiogram = electrocardiogram; }

    public boolean isTemperature() { return temperature; }
    public void setTemperature(boolean temperature) { this.temperature = temperature; }

    @Override
    public String toString() {
        return "TypesAvailability{" +
                "bloodGlucose=" + bloodGlucose +
                ", bloodPressure=" + bloodPressure +
                ", weight=" + weight +
                ", bloodOxygen=" + bloodOxygen +
                ", electrocardiogram=" + electrocardiogram +
                ", temperature=" + temperature +
                '}';
    }
}
