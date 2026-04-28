package com.monitor.health.model.healthscore;

public class Reading {

    public String groupName;
    public int readingCount;
    public double avgReadingValue;
    public String unit;
    public Double avgHealthScore;
    public int month;
    public int year;
    public String monthYear;

    public Reading() {}

    public Reading(String groupName, int readingCount, double avgReadingValue, String unit, Double avgHealthScore, int month, int year, String monthYear) {
        this.groupName = groupName;
        this.readingCount = readingCount;
        this.avgReadingValue = avgReadingValue;
        this.unit = unit;
        this.avgHealthScore = avgHealthScore;
        this.month = month;
        this.year = year;
        this.monthYear = monthYear;
    }

    public String getGroupName() {
        return groupName;
    }

    public void setGroupName(String groupName) {
        this.groupName = groupName;
    }

    public int getReadingCount() {
        return readingCount;
    }

    public void setReadingCount(int readingCount) {
        this.readingCount = readingCount;
    }

    public double getAvgReadingValue() {
        return avgReadingValue;
    }

    public void setAvgReadingValue(double avgReadingValue) {
        this.avgReadingValue = avgReadingValue;
    }

    public String getUnit() {
        return unit;
    }

    public void setUnit(String unit) {
        this.unit = unit;
    }

    public Double getAvgHealthScore() {
        return avgHealthScore;
    }

    public void setAvgHealthScore(Double avgHealthScore) {
        this.avgHealthScore = avgHealthScore;
    }

    public int getMonth() {
        return month;
    }

    public void setMonth(int month) {
        this.month = month;
    }

    public int getYear() {
        return year;
    }

    public void setYear(int year) {
        this.year = year;
    }

    public String getMonthYear() {
        return monthYear;
    }

    public void setMonthYear(String monthYear) {
        this.monthYear = monthYear;
    }

    @Override
    public String toString() {
        return "Reading{" +
                "groupName='" + groupName + '\'' +
                ", readingCount=" + readingCount +
                ", avgReadingValue=" + avgReadingValue +
                ", unit='" + unit + '\'' +
                ", avgHealthScore=" + avgHealthScore +
                ", month=" + month +
                ", year=" + year +
                ", monthYear='" + monthYear + '\'' +
                '}';
    }
}
