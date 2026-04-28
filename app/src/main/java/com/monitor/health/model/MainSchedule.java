package com.monitor.health.model;

import java.util.List;

public class MainSchedule {
    private Day days; // Map to hold day names like "mon", "tue", etc.
    private List<ScheduleValue> value;

    // Getters and Setters


    public Day getDays() {
        return days;
    }

    public void setDays(Day days) {
        this.days = days;
    }

    public List<ScheduleValue> getValue() {
        return value;
    }

    public void setValue(List<ScheduleValue> value) {
        this.value = value;
    }
}
