package com.monitor.health.response.permetricdata;

import com.google.gson.annotations.SerializedName;

public class OidWrapper {
    @SerializedName("$oid")
    private String oid;

    public String getOid() { return oid; }
    public void setOid(String oid) { this.oid = oid; }

    @Override
    public String toString() {
        return "OidWrapper{" +
                "oid='" + oid + '\'' +
                '}';
    }
}
