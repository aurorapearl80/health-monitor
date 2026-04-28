package com.monitor.health.response.alldata;

public class RatingInfo {
    private String readingOutcome;
    private String criteria;
    private String readingValue;
    private int score;
    private String thumbs;

    public String getReadingOutcome() {
        return readingOutcome;
    }

    public void setReadingOutcome(String readingOutcome) {
        this.readingOutcome = readingOutcome;
    }

    public String getCriteria() {
        return criteria;
    }

    public void setCriteria(String criteria) {
        this.criteria = criteria;
    }

    public String getReadingValue() {
        return readingValue;
    }

    public void setReadingValue(String readingValue) {
        this.readingValue = readingValue;
    }

    public int getScore() {
        return score;
    }

    public void setScore(int score) {
        this.score = score;
    }

    public String getThumbs() {
        return thumbs;
    }

    public void setThumbs(String thumbs) {
        this.thumbs = thumbs;
    }
}
