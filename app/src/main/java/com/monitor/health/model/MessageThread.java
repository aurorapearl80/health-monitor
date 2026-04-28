package com.monitor.health.model;

public class MessageThread {
    public int iconResId;
    public String name;
    public String preview;
    public long timestamp;

    public MessageThread(int iconResId, String name, String preview, long timestamp) {
        this.iconResId = iconResId;
        this.name = name;
        this.preview = preview;
        this.timestamp = timestamp;
    }

    @Override
    public String toString() {
        return "MessageThread{" +
                "iconResId=" + iconResId +
                ", name='" + name + '\'' +
                ", preview='" + preview + '\'' +
                ", timestamp=" + timestamp +
                '}';
    }
}
