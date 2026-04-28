package com.monitor.health.response.readinghistory;

import com.google.gson.annotations.SerializedName;

public class ReadingHistoryMeta {
    private int total;

    @SerializedName("per_page")
    private int perPage;

    @SerializedName("current_page")
    private int currentPage;

    @SerializedName("last_page")
    private int lastPage;

    @SerializedName("start_date")
    private String startDate;

    @SerializedName("end_date")
    private String endDate;

    public int getTotal() { return total; }
    public void setTotal(int total) { this.total = total; }

    public int getPerPage() { return perPage; }
    public void setPerPage(int perPage) { this.perPage = perPage; }

    public int getCurrentPage() { return currentPage; }
    public void setCurrentPage(int currentPage) { this.currentPage = currentPage; }

    public int getLastPage() { return lastPage; }
    public void setLastPage(int lastPage) { this.lastPage = lastPage; }

    public String getStartDate() { return startDate; }
    public void setStartDate(String startDate) { this.startDate = startDate; }

    public String getEndDate() { return endDate; }
    public void setEndDate(String endDate) { this.endDate = endDate; }
}
