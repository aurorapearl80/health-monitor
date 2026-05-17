package com.monitor.health.response.ble;

import com.google.gson.annotations.SerializedName;

public class MeasurementUnit {

    @SerializedName("value")
    private String value;

    @SerializedName("label")
    private String label;

    public String getValue() { return value; }
    public void setValue(String value) { this.value = value; }

    public String getLabel() { return label; }
    public void setLabel(String label) { this.label = label; }
}
