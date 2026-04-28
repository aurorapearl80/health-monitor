package com.monitor.health.graph;

public class ReadingData {
    public String date;
    public String time;
    public float weight;
    public float bmi;
    public String status;
    public int viewLayout;

    public ReadingData() {
    }

    public ReadingData(String date, String time, float weight, float bmi, String status, int viewLayout) {
        this.date = date;
        this.time = time;
        this.weight = weight;
        this.bmi = bmi;
        this.status = status;
        this.viewLayout = viewLayout;
    }

    public String getDate() {
        return date;
    }

    public void setDate(String date) {
        this.date = date;
    }

    public String getTime() {
        return time;
    }

    public void setTime(String time) {
        this.time = time;
    }

    public float getWeight() {
        return weight;
    }

    public void setWeight(float weight) {
        this.weight = weight;
    }

    public float getBmi() {
        return bmi;
    }

    public void setBmi(float bmi) {
        this.bmi = bmi;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String formatData() {
        return getWeight()+"/"+getBmi();
    }

    public int getViewLayout() {
        return viewLayout;
    }

    public void setViewLayout(int viewLayout) {
        this.viewLayout = viewLayout;
    }

    @Override
    public String toString() {
        return "ReadingData{" +
                "date='" + date + '\'' +
                ", time='" + time + '\'' +
                ", weight=" + weight +
                ", bmi=" + bmi +
                ", status='" + status + '\'' +
                ", viewLayout='" + viewLayout + '\'' +
                '}';
    }
}
