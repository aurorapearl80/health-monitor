package com.monitor.health.entity;

import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.PrimaryKey;

import java.time.LocalDateTime;

@Entity(tableName = "messages")
public class MessageEntity {
    @PrimaryKey(autoGenerate = true)
    private long id;

    @ColumnInfo(name = "api_id")
    private String apiId;

    @ColumnInfo(name = "subject")
    private String subject;

    @ColumnInfo(name = "body")
    private String body;

    @ColumnInfo(name = "is_read")
    private boolean isRead;

    @ColumnInfo(name = "sender_id")
    private String senderId;

    @ColumnInfo(name = "sender_name")
    private String senderName;

    @ColumnInfo(name = "recipient_id")
    private String recipientId;

    @ColumnInfo(name = "ref_model")
    private String refModel;

    @ColumnInfo(name = "message_date")
    private long messageDate;

    @ColumnInfo(name = "is_system_notification")
    private boolean isSystemNotification;

    @ColumnInfo(name = "updated_at")
    private long updatedAt;

    @ColumnInfo(name = "created_at")
    private long createdAt;

    // Constructors
    public MessageEntity() {}

    public MessageEntity(String apiId, String subject, String body, boolean isRead,
                   String senderId, String senderName, String recipientId, String refModel,
                   long messageDate, boolean isSystemNotification,
                   long updatedAt, long createdAt) {
        this.apiId = apiId;
        this.subject = subject;
        this.body = body;
        this.isRead = isRead;
        this.senderId = senderId;
        this.senderName = senderName;
        this.recipientId = recipientId;
        this.refModel = refModel;
        this.messageDate = messageDate;
        this.isSystemNotification = isSystemNotification;
        this.updatedAt = updatedAt;
        this.createdAt = createdAt;
    }

    // Getters and Setters
    public long getId() { return id; }
    public void setId(long id) { this.id = id; }

    public String getApiId() { return apiId; }
    public void setApiId(String apiId) { this.apiId = apiId; }

    public String getSubject() { return subject; }
    public void setSubject(String subject) { this.subject = subject; }

    public String getBody() { return body; }
    public void setBody(String body) { this.body = body; }

    public boolean isRead() { return isRead; }
    public void setRead(boolean read) { isRead = read; }

    public String getSenderId() { return senderId; }
    public void setSenderId(String senderId) { this.senderId = senderId; }

    public String getSenderName() { return senderName; }
    public void setSenderName(String senderName) { this.senderName = senderName; }

    public String getRecipientId() { return recipientId; }
    public void setRecipientId(String recipientId) { this.recipientId = recipientId; }

    public String getRefModel() { return refModel; }
    public void setRefModel(String refModel) { this.refModel = refModel; }

    public long getMessageDate() { return messageDate; }
    public void setMessageDate(long messageDate) { this.messageDate = messageDate; }

    public boolean isSystemNotification() { return isSystemNotification; }
    public void setSystemNotification(boolean systemNotification) {
        isSystemNotification = systemNotification;
    }

    public long getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(long updatedAt) { this.updatedAt = updatedAt; }

    public long getCreatedAt() { return createdAt; }
    public void setCreatedAt(long createdAt) { this.createdAt = createdAt; }
}
