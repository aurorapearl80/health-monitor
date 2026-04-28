package com.monitor.health.model;

public class ReadingValueEcg {
    private double heartRate;
    private double ecgMeasurementLength;
    private double mood;

    private String dateFormat;

    public void setHeartRate(double heartRate) {
        this.heartRate = heartRate;
    }

    public void setEcgMeasurementLength(double ecgMeasurementLength) {
        this.ecgMeasurementLength = ecgMeasurementLength;
    }

    public void setMood(double mood) {
        this.mood = mood;
    }

    public double getHeartRate() { return heartRate; }
    public double getEcgMeasurementLength() { return ecgMeasurementLength; }
    public double getMood() { return mood; }

    public String getDateFormat() {
        return dateFormat;
    }

    public void setDateFormat(String dateFormat) {
        this.dateFormat = dateFormat;
    }
}

