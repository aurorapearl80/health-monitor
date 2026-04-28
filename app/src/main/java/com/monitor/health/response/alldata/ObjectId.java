package com.monitor.health.response.alldata;

import com.google.gson.annotations.SerializedName;

public class ObjectId {
    @SerializedName("$oid")
    private String oid;

    public String getOid() { return oid; }
    public void setOid(String oid) { this.oid = oid; }
}



