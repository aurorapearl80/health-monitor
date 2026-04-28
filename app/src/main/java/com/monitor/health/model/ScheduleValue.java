package com.monitor.health.model;

public class ScheduleValue {
    private boolean selected;
    private String description;
    private String time;
    private String timeDisplay;
    private int quantity;

    // Getters and Setters


    public boolean isSelected() {
        return selected;
    }

    public void setSelected(boolean selected) {
        this.selected = selected;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getTime() {
        return time;
    }

    public void setTime(String time) {
        this.time = time;
    }

    public String getTimeDisplay() {
        return timeDisplay;
    }

    public void setTimeDisplay(String timeDisplay) {
        this.timeDisplay = timeDisplay;
    }

    public int getQuantity() {
        return quantity;
    }

    public void setQuantity(int quantity) {
        this.quantity = quantity;
    }
}
