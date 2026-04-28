package com.monitor.health.response.alldata;

public class ConvertedValue {
    private double value;
    private String unit;

    public double getValue() {
        return value;
    }

    public void setValue(double value) {
        this.value = value;
    }

    public String getUnit() {
        return unit;
    }

    public void setUnit(String unit) {
        this.unit = unit;
    }

    @Override
    public String toString() {
        return "ConvertedValue{" +
                "value=" + value +
                ", unit='" + unit + '\'' +
                '}';
    }
}

