package com.monitor.health.chat.dto;

import com.google.gson.annotations.SerializedName;

/** Body for doctor-watch chat endpoints that only need the watch's serial (e.g. mark-as-read). */
public class ChatSerialRequestDTO {
    @SerializedName("serial")
    private final String serial;

    public ChatSerialRequestDTO(String serial) {
        this.serial = serial;
    }

    public String getSerial() { return serial; }
}
