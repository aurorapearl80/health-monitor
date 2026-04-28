package com.monitor.health.dto;

import com.google.gson.annotations.SerializedName;
import java.util.List;

public class ApiResponseDTO {
    @SerializedName("data")
    private List<MessageDTO> data;

    @SerializedName("meta")
    private MetadataDTO meta;

    // Getters and Setters
    public List<MessageDTO> getData() { return data; }
    public void setData(List<MessageDTO> data) { this.data = data; }

    public MetadataDTO getMeta() { return meta; }
    public void setMeta(MetadataDTO meta) { this.meta = meta; }

    @Override
    public String toString() {
        return "ApiResponseDTO{" +
                "data=" + data +
                ", meta=" + meta +
                '}';
    }
}
