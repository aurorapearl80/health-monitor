package com.monitor.health.response.readinghistory;

import java.util.List;

public class ReadingHistoryResponse {
    private List<ReadingHistoryItem> data;
    private ReadingHistoryMeta meta;

    public List<ReadingHistoryItem> getData() { return data; }
    public void setData(List<ReadingHistoryItem> data) { this.data = data; }

    public ReadingHistoryMeta getMeta() { return meta; }
    public void setMeta(ReadingHistoryMeta meta) { this.meta = meta; }
}
