package com.monitor.health.response.ble;

import com.google.gson.annotations.SerializedName;

public class MeasurementUnits {

    @SerializedName("unit_height")
    private MeasurementUnit unitHeight;

    @SerializedName("unit_weight")
    private MeasurementUnit unitWeight;

    @SerializedName("unit_temperature")
    private MeasurementUnit unitTemperature;

    @SerializedName("unit_glucose")
    private MeasurementUnit unitGlucose;

    @SerializedName("unit_blood_pressure")
    private MeasurementUnit unitBloodPressure;

    public MeasurementUnit getUnitHeight() { return unitHeight; }
    public MeasurementUnit getUnitWeight() { return unitWeight; }
    public MeasurementUnit getUnitTemperature() { return unitTemperature; }
    public MeasurementUnit getUnitGlucose() { return unitGlucose; }
    public MeasurementUnit getUnitBloodPressure() { return unitBloodPressure; }
}
