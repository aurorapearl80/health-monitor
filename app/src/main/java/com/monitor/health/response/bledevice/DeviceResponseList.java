package com.monitor.health.response.bledevice;

import com.google.gson.annotations.SerializedName;
import java.util.List;

public class DeviceResponseList {

    @SerializedName("newly_created")
    private boolean newlyCreated;

    @SerializedName("ownership_status")
    private String ownershipStatus;

    @SerializedName("data")
    private List<BLEData> data;   // âœ… array

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

    public List<BLEData> getData() {
        return data;
    }

    public void setData(List<BLEData> data) {
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