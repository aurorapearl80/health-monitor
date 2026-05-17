package com.monitor.health.response.oximeter;

public class OximeterData {
    private int user_id;
    private String device_id;
    private String reading_type_id;
    private String oxygen;
    private int pulse_rate;
    private boolean manual;
    private String measured_at;
    private String serial;
    private String timezone;
    private String updated_at;
    private String created_at;
    private int id;

    public int getUser_id() { return user_id; }
    public void setUser_id(int user_id) { this.user_id = user_id; }

    public String getDevice_id() { return device_id; }
    public void setDevice_id(String device_id) { this.device_id = device_id; }

    public String getReading_type_id() { return reading_type_id; }
    public void setReading_type_id(String reading_type_id) { this.reading_type_id = reading_type_id; }

    public String getOxygen() { return oxygen; }
    public void setOxygen(String oxygen) { this.oxygen = oxygen; }

    public int getPulse_rate() { return pulse_rate; }
    public void setPulse_rate(int pulse_rate) { this.pulse_rate = pulse_rate; }

    public boolean isManual() { return manual; }
    public void setManual(boolean manual) { this.manual = manual; }

    public String getMeasured_at() { return measured_at; }
    public void setMeasured_at(String measured_at) { this.measured_at = measured_at; }

    public String getSerial() { return serial; }
    public void setSerial(String serial) { this.serial = serial; }

    public String getTimezone() { return timezone; }
    public void setTimezone(String timezone) { this.timezone = timezone; }

    public String getUpdated_at() { return updated_at; }
    public void setUpdated_at(String updated_at) { this.updated_at = updated_at; }

    public String getCreated_at() { return created_at; }
    public void setCreated_at(String created_at) { this.created_at = created_at; }

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }
}
