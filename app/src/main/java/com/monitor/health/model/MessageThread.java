package com.monitor.health.model;

public class MessageThread {
    public int iconResId;
    public String name;
    public String preview;
    public long timestamp;
    public long apiId;
    public boolean isMine;
    public boolean isRead;
    public String avatarUrl;

    public MessageThread(int iconResId, String name, String preview, long timestamp,
                          long apiId, boolean isMine, boolean isRead, String avatarUrl) {
        this.iconResId = iconResId;
        this.name = name;
        this.preview = preview;
        this.timestamp = timestamp;
        this.apiId = apiId;
        this.isMine = isMine;
        this.isRead = isRead;
        this.avatarUrl = avatarUrl;
    }

    @Override
    public String toString() {
        return "MessageThread{" +
                "iconResId=" + iconResId +
                ", name='" + name + '\'' +
                ", preview='" + preview + '\'' +
                ", timestamp=" + timestamp +
                ", apiId=" + apiId +
                ", isMine=" + isMine +
                ", isRead=" + isRead +
                '}';
    }
}
