package com.monitor.health.chat.dto;

import com.google.gson.annotations.SerializedName;

/**
 * POST /api/doctor-watches/messages body. No recipient_id — auto-resolved server-side (by
 * resolving `serial` to a patient) to that patient's care team.
 */
public class ChatSendRequestDTO {
    @SerializedName("serial")
    private final String serial;

    @SerializedName("body")
    private final String body;

    public ChatSendRequestDTO(String serial, String body) {
        this.serial = serial;
        this.body = body;
    }

    public String getSerial() { return serial; }
    public String getBody() { return body; }
}
