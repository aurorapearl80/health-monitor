package com.monitor.health.response.glocuse;

import com.google.gson.annotations.SerializedName;

public class GlucoseOid {
    @SerializedName("$oid")
    private String oid;

    public String getOid() {
        return oid;
    }

    public void setOid(String oid) {
        this.oid = oid;
    }
}
