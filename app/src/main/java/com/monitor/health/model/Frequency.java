package com.monitor.health.model;

public class Frequency {
    private String vitalName;
    private Integer measurementInterval;
    private String intervalUnit;

    public Frequency() {
    }

    public Frequency(String vitalName, Integer measurementInterval, String intervalUnit) {
        this.vitalName = vitalName;
        this.measurementInterval = measurementInterval;
        this.intervalUnit = intervalUnit;
    }

    public String getVitalName() {
        return vitalName;
    }

    public void setVitalName(String vitalName) {
        this.vitalName = vitalName;
    }

    public Integer getMeasurementInterval() {
        return measurementInterval;
    }

    public void setMeasurementInterval(Integer measurementInterval) {
        this.measurementInterval = measurementInterval;
    }

    public String getIntervalUnit() {
        return intervalUnit;
    }

    public void setIntervalUnit(String intervalUnit) {
        this.intervalUnit = intervalUnit;
    }
}
