package com.monitor.health.entity;

import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.Ignore;
import androidx.room.Index;
import androidx.room.PrimaryKey;

/** Local cache of patient-monitoring-web's chat messages (see chat/dto/ChatMessageDTO). */
@Entity(tableName = "messages", indices = {@Index(value = "api_id", unique = true)})
public class MessageEntity {
    @PrimaryKey(autoGenerate = true)
    private long id;

    @ColumnInfo(name = "api_id")
    private long apiId;

    @ColumnInfo(name = "body")
    private String body;

    @ColumnInfo(name = "is_read")
    private boolean isRead;

    @ColumnInfo(name = "is_mine")
    private boolean isMine;

    @ColumnInfo(name = "sender_id")
    private long senderId;

    @ColumnInfo(name = "sender_name")
    private String senderName;

    @ColumnInfo(name = "recipient_id")
    private long recipientId;

    @ColumnInfo(name = "patient_id")
    private Long patientId;

    @ColumnInfo(name = "channel_id")
    private Long channelId;

    @ColumnInfo(name = "attachment_url")
    private String attachmentUrl;

    @ColumnInfo(name = "attachment_name")
    private String attachmentName;

    @ColumnInfo(name = "sender_profile_image_url")
    private String senderProfileImageUrl;

    @ColumnInfo(name = "recipient_profile_image_url")
    private String recipientProfileImageUrl;

    @ColumnInfo(name = "read_at")
    private Long readAt;

    @ColumnInfo(name = "created_at")
    private long createdAt;

    public MessageEntity() {}

    @Ignore
    public MessageEntity(long apiId, String body, boolean isRead, boolean isMine,
                          long senderId, String senderName, long recipientId,
                          Long patientId, Long channelId,
                          String attachmentUrl, String attachmentName,
                          String senderProfileImageUrl, String recipientProfileImageUrl,
                          Long readAt, long createdAt) {
        this.apiId = apiId;
        this.body = body;
        this.isRead = isRead;
        this.isMine = isMine;
        this.senderId = senderId;
        this.senderName = senderName;
        this.recipientId = recipientId;
        this.patientId = patientId;
        this.channelId = channelId;
        this.attachmentUrl = attachmentUrl;
        this.attachmentName = attachmentName;
        this.senderProfileImageUrl = senderProfileImageUrl;
        this.recipientProfileImageUrl = recipientProfileImageUrl;
        this.readAt = readAt;
        this.createdAt = createdAt;
    }

    // Getters and Setters
    public long getId() { return id; }
    public void setId(long id) { this.id = id; }

    public long getApiId() { return apiId; }
    public void setApiId(long apiId) { this.apiId = apiId; }

    public String getBody() { return body; }
    public void setBody(String body) { this.body = body; }

    public boolean isRead() { return isRead; }
    public void setRead(boolean read) { isRead = read; }

    public boolean isMine() { return isMine; }
    public void setMine(boolean mine) { isMine = mine; }

    public long getSenderId() { return senderId; }
    public void setSenderId(long senderId) { this.senderId = senderId; }

    public String getSenderName() { return senderName; }
    public void setSenderName(String senderName) { this.senderName = senderName; }

    public long getRecipientId() { return recipientId; }
    public void setRecipientId(long recipientId) { this.recipientId = recipientId; }

    public Long getPatientId() { return patientId; }
    public void setPatientId(Long patientId) { this.patientId = patientId; }

    public Long getChannelId() { return channelId; }
    public void setChannelId(Long channelId) { this.channelId = channelId; }

    public String getAttachmentUrl() { return attachmentUrl; }
    public void setAttachmentUrl(String attachmentUrl) { this.attachmentUrl = attachmentUrl; }

    public String getAttachmentName() { return attachmentName; }
    public void setAttachmentName(String attachmentName) { this.attachmentName = attachmentName; }

    public String getSenderProfileImageUrl() { return senderProfileImageUrl; }
    public void setSenderProfileImageUrl(String senderProfileImageUrl) { this.senderProfileImageUrl = senderProfileImageUrl; }

    public String getRecipientProfileImageUrl() { return recipientProfileImageUrl; }
    public void setRecipientProfileImageUrl(String recipientProfileImageUrl) { this.recipientProfileImageUrl = recipientProfileImageUrl; }

    public Long getReadAt() { return readAt; }
    public void setReadAt(Long readAt) { this.readAt = readAt; }

    public long getCreatedAt() { return createdAt; }
    public void setCreatedAt(long createdAt) { this.createdAt = createdAt; }
}
