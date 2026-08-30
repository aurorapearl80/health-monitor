package com.monitor.health.chat.dto;

import com.google.gson.annotations.SerializedName;

public class ChatUserDTO {
    @SerializedName("id")
    private long id;

    @SerializedName("name")
    private String name;

    @SerializedName("email")
    private String email;

    @SerializedName("profile_image_url")
    private String profileImageUrl;

    public long getId() { return id; }
    public String getName() { return name; }
    public String getEmail() { return email; }
    public String getProfileImageUrl() { return profileImageUrl; }
}
