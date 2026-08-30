package com.monitor.health.chat.dto;

import com.google.gson.annotations.SerializedName;

public class ChatAttachmentDTO {
    @SerializedName("url")
    private String url;

    @SerializedName("name")
    private String name;

    @SerializedName("mime")
    private String mime;

    @SerializedName("size")
    private long size;

    @SerializedName("is_image")
    private boolean isImage;

    public String getUrl() { return url; }
    public String getName() { return name; }
    public String getMime() { return mime; }
    public long getSize() { return size; }
    public boolean isImage() { return isImage; }
}
