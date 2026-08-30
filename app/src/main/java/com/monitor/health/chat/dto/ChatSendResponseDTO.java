package com.monitor.health.chat.dto;

import com.google.gson.annotations.SerializedName;

public class ChatSendResponseDTO {
    @SerializedName("message")
    private String message;

    @SerializedName("data")
    private ChatMessageDTO data;

    public String getMessage() { return message; }
    public ChatMessageDTO getData() { return data; }
}
