package com.monitor.health.response.alldata;
public class ReadingValueBloodPressure {
    private int systolic;
    private int diastolic;
    private int pulseRate;

    // getters & setters...


    public int getSystolic() {
        return systolic;
    }

    public void setSystolic(int systolic) {
        this.systolic = systolic;
    }

    public int getDiastolic() {
        return diastolic;
    }

    public void setDiastolic(int diastolic) {
        this.diastolic = diastolic;
    }

    public int getPulseRate() {
        return pulseRate;
    }

    public void setPulseRate(int pulseRate) {
        this.pulseRate = pulseRate;
    }
}

