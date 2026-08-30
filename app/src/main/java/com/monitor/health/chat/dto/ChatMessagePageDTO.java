package com.monitor.health.chat.dto;

import com.google.gson.annotations.SerializedName;
import com.monitor.health.dto.MetadataDTO;

import java.util.List;

public class ChatMessagePageDTO {
    @SerializedName("data")
    private List<ChatMessageDTO> data;

    @SerializedName("meta")
    private MetadataDTO meta;

    public List<ChatMessageDTO> getData() { return data; }
    public MetadataDTO getMeta() { return meta; }
}
