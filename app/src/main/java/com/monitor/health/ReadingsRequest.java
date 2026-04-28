package com.monitor.health;

import com.monitor.health.model.Reading;

import java.util.List;

public class ReadingsRequest {
    private List<Reading> readings;

    // Constructor, getters, and setters
    public ReadingsRequest() {}

    public ReadingsRequest(List<Reading> readings) {
        this.readings = readings;
    }

    // Getters and setters
    // Omitted for brevity, can be generated using IDE or manually


    public List<Reading> getReadings() {
        return readings;
    }

    public void setReadings(List<Reading> readings) {
        this.readings = readings;
    }

    @Override
    public String toString() {
        return "ReadingsRequest{" +
                "readings=" + readings +
                '}';
    }
}