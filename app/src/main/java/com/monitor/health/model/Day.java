package com.monitor.health.model;

public class Day {

    private boolean mon;
    private boolean tue;
    private boolean wed;
    private boolean thu;
    private boolean fri;
    private boolean sat;
    private boolean sun;

    public boolean getMon() { return mon; }
    public void setMon(boolean value) { this.mon = value; }

    public boolean getTue() { return tue; }
    public void setTue(boolean value) { this.tue = value; }

    public boolean getWed() { return wed; }
    public void setWed(boolean value) { this.wed = value; }

    public boolean getThu() { return thu; }
    public void setThu(boolean value) { this.thu = value; }

    public boolean getFri() { return fri; }
    public void setFri(boolean value) { this.fri = value; }

    public boolean getSat() { return sat; }
    public void setSat(boolean value) { this.sat = value; }

    public boolean getSun() { return sun; }
    public void setSun(boolean value) { this.sun = value; }

    // Method to get the value of the specific day
    public boolean getDayValue(String dayOfWeek) {
        switch (dayOfWeek) {
            case "mon":
                return mon;
            case "tue":
                return tue;
            case "wed":
                return wed;
            case "thu":
                return thu;
            case "fri":
                return fri;
            case "sat":
                return sat;
            case "sun":
                return sun;
            default:
                throw new IllegalArgumentException("Invalid day of week: " + dayOfWeek);
        }
    }

    @Override
    public String toString() {
        return "Day{" +
                "mon=" + mon +
                ", tue=" + tue +
                ", wed=" + wed +
                ", thu=" + thu +
                ", fri=" + fri +
                ", sat=" + sat +
                ", sun=" + sun +
                '}';
    }
}
