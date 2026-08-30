package com.monitor.health.chat.dto;

import com.google.gson.annotations.SerializedName;

/**
 * Message shape returned by every patient-monitoring-web chat endpoint — see CHAT_API.md
 * "Message object shape". {@code channel_id} is always null for this app's 1:1-only usage.
 */
public class ChatMessageDTO {
    @SerializedName("id")
    private long id;

    @SerializedName("sender_id")
    private long senderId;

    @SerializedName("recipient_id")
    private long recipientId;

    @SerializedName("patient_id")
    private Long patientId;

    @SerializedName("channel_id")
    private Long channelId;

    @SerializedName("body")
    private String body;

    @SerializedName("attachment")
    private ChatAttachmentDTO attachment;

    @SerializedName("is_read")
    private boolean isRead;

    @SerializedName("read_at")
    private String readAt;

    @SerializedName("is_mine")
    private boolean isMine;

    @SerializedName("created_at")
    private String createdAt;

    @SerializedName("sender")
    private ChatUserDTO sender;

    @SerializedName("recipient")
    private ChatUserDTO recipient;

    public long getId() { return id; }
    public long getSenderId() { return senderId; }
    public long getRecipientId() { return recipientId; }
    public Long getPatientId() { return patientId; }
    public Long getChannelId() { return channelId; }
    public String getBody() { return body; }
    public ChatAttachmentDTO getAttachment() { return attachment; }
    public boolean isRead() { return isRead; }
    public String getReadAt() { return readAt; }
    public boolean isMine() { return isMine; }
    public String getCreatedAt() { return createdAt; }
    public ChatUserDTO getSender() { return sender; }
    public ChatUserDTO getRecipient() { return recipient; }
}
