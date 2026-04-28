package com.monitor.health.response.alldata;

public class ReadingValueWeight {
    private double weight;
    private double bmi;
    private String bmiDescription;

    // getters & setters...


    public double getWeight() {
        return weight;
    }

    public void setWeight(double weight) {
        this.weight = weight;
    }

    public double getBmi() {
        return bmi;
    }

    public void setBmi(double bmi) {
        this.bmi = bmi;
    }

    public String getBmiDescription() {
        return bmiDescription;
    }

    public void setBmiDescription(String bmiDescription) {
        this.bmiDescription = bmiDescription;
    }
}
