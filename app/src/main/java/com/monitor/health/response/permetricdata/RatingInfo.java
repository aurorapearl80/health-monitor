package com.monitor.health.response.permetricdata;

public class RatingInfo {
    private String readingOutcome;
    private String criteria;
    private String readingValue;
    private Integer score;
    private String thumbs;
    private OidWrapper id;

    public String getReadingOutcome() { return readingOutcome; }
    public void setReadingOutcome(String readingOutcome) { this.readingOutcome = readingOutcome; }

    public String getCriteria() { return criteria; }
    public void setCriteria(String criteria) { this.criteria = criteria; }

    public String getReadingValue() { return readingValue; }
    public void setReadingValue(String readingValue) { this.readingValue = readingValue; }

    public Integer getScore() { return score; }
    public void setScore(Integer score) { this.score = score; }

    public String getThumbs() { return thumbs; }
    public void setThumbs(String thumbs) { this.thumbs = thumbs; }

    public OidWrapper getId() { return id; }
    public void setId(OidWrapper id) { this.id = id; }

    @Override
    public String toString() {
        return "RatingInfo{" +
                "readingOutcome='" + readingOutcome + '\'' +
                ", criteria='" + criteria + '\'' +
                ", readingValue='" + readingValue + '\'' +
                ", score=" + score +
                ", thumbs='" + thumbs + '\'' +
                ", id=" + id +
                '}';
    }
}
