package com.monitor.health.dto;

import com.google.gson.annotations.SerializedName;

public class MetadataDTO {
    @SerializedName("current_page")
    private int currentPage;

    @SerializedName("from")
    private int from;

    @SerializedName("last_page")
    private int lastPage;

    @SerializedName("path")
    private String path;

    @SerializedName("per_page")
    private int perPage;

    @SerializedName("to")
    private int to;

    @SerializedName("total")
    private int total;

    // Getters and Setters
    public int getCurrentPage() { return currentPage; }
    public void setCurrentPage(int currentPage) { this.currentPage = currentPage; }

    public int getFrom() { return from; }
    public void setFrom(int from) { this.from = from; }

    public int getLastPage() { return lastPage; }
    public void setLastPage(int lastPage) { this.lastPage = lastPage; }

    public String getPath() { return path; }
    public void setPath(String path) { this.path = path; }

    public int getPerPage() { return perPage; }
    public void setPerPage(int perPage) { this.perPage = perPage; }

    public int getTo() { return to; }
    public void setTo(int to) { this.to = to; }

    public int getTotal() { return total; }
    public void setTotal(int total) { this.total = total; }

    @Override
    public String toString() {
        return "MetadataDTO{" +
                "currentPage=" + currentPage +
                ", from=" + from +
                ", lastPage=" + lastPage +
                ", path='" + path + '\'' +
                ", perPage=" + perPage +
                ", to=" + to +
                ", total=" + total +
                '}';
    }
}
