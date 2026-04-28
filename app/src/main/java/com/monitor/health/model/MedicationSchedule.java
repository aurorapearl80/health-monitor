package com.monitor.health.model;

import java.util.List;

public class MedicationSchedule {
    private List<MedicationData> data;

    public List<MedicationData> getData() {
        return data;
    }

    public void setData(List<MedicationData> data) {
        this.data = data;
    }
}
