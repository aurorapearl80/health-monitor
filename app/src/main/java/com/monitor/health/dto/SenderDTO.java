package com.monitor.health.dto;

import com.google.gson.annotations.SerializedName;

public class SenderDTO {
    @SerializedName("fullname")
    private String fullname;
    
    @SerializedName("id")
    private String id;

    // Getters and setters
    public String getFullname() { return fullname; }
    public void setFullname(String fullname) { this.fullname = fullname; }

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    @Override
    public String toString() {
        return "SenderDTO{" +
                "fullname='" + fullname + '\'' +
                ", id='" + id + '\'' +
                '}';
    }
}