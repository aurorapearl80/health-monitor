package com.monitor.health.dto;


import com.google.gson.annotations.SerializedName;
import java.util.List;

public class MessageDTO {
    @SerializedName("subject")
    private String subject;
    
    @SerializedName("body")
    private String body;
    
    @SerializedName("isRead")
    private boolean isRead;

    @SerializedName("otherData")
    private List<Object> otherData;

    @SerializedName("sender")
    private SenderDTO sender;
    
    @SerializedName("recepient")
    private String recepient;

    @SerializedName("refModel")
    private String refModel;

    @SerializedName("messageDate")
    private String messageDate;

    @SerializedName("isSystemNotification")
    private boolean isSystemNotification;

    @SerializedName("updatedAt")
    private String updatedAt;

    @SerializedName("createdAt")
    private String createdAt;
    
    @SerializedName("id")
    private String id;

    // Getters and Setters
    public String getSubject() { return subject; }
    public void setSubject(String subject) { this.subject = subject; }

    public String getBody() { return body; }
    public void setBody(String body) { this.body = body; }

    public boolean isRead() { return isRead; }
    public void setRead(boolean read) { isRead = read; }

    public List<Object> getOtherData() { return otherData; }
    public void setOtherData(List<Object> otherData) { this.otherData = otherData; }

    public SenderDTO getSender() { return sender; }
    public void setSender(SenderDTO sender) { this.sender = sender; }

    public String getRecepient() { return recepient; }
    public void setRecepient(String recepient) { this.recepient = recepient; }

    public String getRefModel() { return refModel; }
    public void setRefModel(String refModel) { this.refModel = refModel; }

    public String getMessageDate() { return messageDate; }
    public void setMessageDate(String messageDate) { this.messageDate = messageDate; }

    public boolean isSystemNotification() { return isSystemNotification; }
    public void setSystemNotification(boolean systemNotification) {
        isSystemNotification = systemNotification;
    }

    public String getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(String updatedAt) { this.updatedAt = updatedAt; }

    public String getCreatedAt() { return createdAt; }
    public void setCreatedAt(String createdAt) { this.createdAt = createdAt; }

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    @Override
    public String toString() {
        return "MessageDTO{" +
                "subject='" + subject + '\'' +
                ", body='" + body + '\'' +
                ", isRead=" + isRead +
                ", sender=" + (sender != null ? sender.toString() : "null") +
                ", recepient='" + recepient + '\'' +
                ", refModel='" + refModel + '\'' +
                ", messageDate='" + messageDate + '\'' +
                ", isSystemNotification=" + isSystemNotification +
                ", id='" + id + '\'' +
                '}';
    }
}