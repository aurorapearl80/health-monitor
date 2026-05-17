package com.monitor.health.response.bloodpressure;

public class BloodPressureData {
    private int user_id;
    private String device_id;
    private String systolic;
    private String diastolic;
    private String bpm;
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

    public String getSystolic() { return systolic; }
    public void setSystolic(String systolic) { this.systolic = systolic; }

    public String getDiastolic() { return diastolic; }
    public void setDiastolic(String diastolic) { this.diastolic = diastolic; }

    public String getBpm() { return bpm; }
    public void setBpm(String bpm) { this.bpm = bpm; }

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
