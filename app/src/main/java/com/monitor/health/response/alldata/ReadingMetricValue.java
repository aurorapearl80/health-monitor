package com.monitor.health.response.alldata;

import java.util.List;

public class ReadingMetricValue {
    private String name;
    private double value;
    private String unit;
    private List<ConvertedValue> convertedValues;
    private Integer healthScore;
    private RatingInfo ratingInfo;

    private boolean should_convert;

    public boolean isShould_convert() {
        return should_convert;
    }

    public void setShould_convert(boolean should_convert) {
        this.should_convert = should_convert;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

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

    public List<ConvertedValue> getConvertedValues() {
        return convertedValues;
    }

    public void setConvertedValues(List<ConvertedValue> convertedValues) {
        this.convertedValues = convertedValues;
    }

    public Integer getHealthScore() {
        return healthScore;
    }

    public void setHealthScore(Integer healthScore) {
        this.healthScore = healthScore;
    }

    public RatingInfo getRatingInfo() {
        return ratingInfo;
    }

    public void setRatingInfo(RatingInfo ratingInfo) {
        this.ratingInfo = ratingInfo;
    }

    @Override
    public String toString() {
        return "ReadingMetricValue{" +
                "name='" + name + '\'' +
                ", value=" + value +
                ", unit='" + unit + '\'' +
                ", convertedValues=" + convertedValues +
                ", healthScore=" + healthScore +
                ", ratingInfo=" + ratingInfo +
                ", should_convert=" + should_convert +
                '}';
    }
}

