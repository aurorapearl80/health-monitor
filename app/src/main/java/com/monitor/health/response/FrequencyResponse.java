package com.monitor.health.response;

import com.monitor.health.model.Frequency;

import java.util.List;

public class FrequencyResponse {
    private List<Frequency> data;

    public FrequencyResponse(List<Frequency> data) {
        this.data = data;
    }

    public List<Frequency> getData() {
        return data;
    }

    public void setData(List<Frequency> data) {
        this.data = data;
    }

    @Override
    public String toString() {
        return "FrequencyResponse{" +
                "data=" + data +
                '}';
    }
}
