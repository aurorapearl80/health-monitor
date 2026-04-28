package com.monitor.health.response.bledevice;


import com.google.gson.annotations.SerializedName;

public class DeviceResponse {

    @SerializedName("newly_created")
    private boolean newlyCreated;

    @SerializedName("ownership_status")
    private String ownershipStatus;

    @SerializedName("data")
    private BLEData data;

    public boolean isNewlyCreated() {
        return newlyCreated;
    }

    public void setNewlyCreated(boolean newlyCreated) {
        this.newlyCreated = newlyCreated;
    }

    public String getOwnershipStatus() {
        return ownershipStatus;
    }

    public void setOwnershipStatus(String ownershipStatus) {
        this.ownershipStatus = ownershipStatus;
    }

    public BLEData getData() {
        return data;
    }

    public void setData(BLEData data) {
        this.data = data;
    }

    @Override
    public String toString() {
        return "DeviceResponse{" +
                "newlyCreated=" + newlyCreated +
                ", ownershipStatus='" + ownershipStatus + '\'' +
                ", data=" + data +
                '}';
    }
}
