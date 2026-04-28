package com.monitor.health.response.glocuse;

public class GlucoseReadingValue {
    private int glucose;
    private int event;
    private String eventDescription;

    public int getGlucose() {
        return glucose;
    }

    public void setGlucose(int glucose) {
        this.glucose = glucose;
    }

    public int getEvent() {
        return event;
    }

    public void setEvent(int event) {
        this.event = event;
    }

    public String getEventDescription() {
        return eventDescription;
    }

    public void setEventDescription(String eventDescription) {
        this.eventDescription = eventDescription;
    }
}
