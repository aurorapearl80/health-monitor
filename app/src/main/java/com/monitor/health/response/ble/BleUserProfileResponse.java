package com.monitor.health.response.ble;

import com.google.gson.annotations.SerializedName;

public class BleUserProfileResponse {

    @SerializedName("serial")
    private String serial;

    @SerializedName("user")
    private BleUserData user;

    public String getSerial() { return serial; }
    public void setSerial(String serial) { this.serial = serial; }

    public BleUserData getUser() { return user; }
    public void setUser(BleUserData user) { this.user = user; }
}
