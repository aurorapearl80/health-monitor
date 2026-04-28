package com.monitor.health.model.healthscore;

import java.util.List;

public class HealthRating {
    public String _id;
    public int month;
    public int year;
    public String patient_id;
    public List<Reading> readings;
    public int overallReadingCount;
    public String fullname;
    public String enrolledBy;
    public String practitioners;
    public String patientId;
    public double overallAvgHealthScore;
    public String timezone;
    public String updated_at;
    public String created_at;

    public HealthRating() {}

    public HealthRating(String _id, int month, int year, String patient_id, List<Reading> readings, int overallReadingCount, String fullname, String enrolledBy, String practitioners, String patientId, double overallAvgHealthScore, String timezone, String updated_at, String created_at) {
        this._id = _id;
        this.month = month;
        this.year = year;
        this.patient_id = patient_id;
        this.readings = readings;
        this.overallReadingCount = overallReadingCount;
        this.fullname = fullname;
        this.enrolledBy = enrolledBy;
        this.practitioners = practitioners;
        this.patientId = patientId;
        this.overallAvgHealthScore = overallAvgHealthScore;
        this.timezone = timezone;
        this.updated_at = updated_at;
        this.created_at = created_at;
    }

    public String get_id() {
        return _id;
    }

    public void set_id(String _id) {
        this._id = _id;
    }

    public int getMonth() {
        return month;
    }

    public void setMonth(int month) {
        this.month = month;
    }

    public int getYear() {
        return year;
    }

    public void setYear(int year) {
        this.year = year;
    }

    public String getPatient_id() {
        return patient_id;
    }

    public void setPatient_id(String patient_id) {
        this.patient_id = patient_id;
    }

    public List<Reading> getReadings() {
        return readings;
    }

    public void setReadings(List<Reading> readings) {
        this.readings = readings;
    }

    public int getOverallReadingCount() {
        return overallReadingCount;
    }

    public void setOverallReadingCount(int overallReadingCount) {
        this.overallReadingCount = overallReadingCount;
    }

    public String getFullname() {
        return fullname;
    }

    public void setFullname(String fullname) {
        this.fullname = fullname;
    }

    public String getEnrolledBy() {
        return enrolledBy;
    }

    public void setEnrolledBy(String enrolledBy) {
        this.enrolledBy = enrolledBy;
    }

    public String getPractitioners() {
        return practitioners;
    }

    public void setPractitioners(String practitioners) {
        this.practitioners = practitioners;
    }

    public String getPatientId() {
        return patientId;
    }

    public void setPatientId(String patientId) {
        this.patientId = patientId;
    }

    public double getOverallAvgHealthScore() {
        return overallAvgHealthScore;
    }

    public void setOverallAvgHealthScore(double overallAvgHealthScore) {
        this.overallAvgHealthScore = overallAvgHealthScore;
    }

    public String getTimezone() {
        return timezone;
    }

    public void setTimezone(String timezone) {
        this.timezone = timezone;
    }

    public String getUpdated_at() {
        return updated_at;
    }

    public void setUpdated_at(String updated_at) {
        this.updated_at = updated_at;
    }

    public String getCreated_at() {
        return created_at;
    }

    public void setCreated_at(String created_at) {
        this.created_at = created_at;
    }

    @Override
    public String toString() {
        return "HealthRating{" +
                "_id='" + _id + '\'' +
                ", month=" + month +
                ", year=" + year +
                ", patient_id='" + patient_id + '\'' +
                ", readings=" + readings +
                ", overallReadingCount=" + overallReadingCount +
                ", fullname='" + fullname + '\'' +
                ", enrolledBy='" + enrolledBy + '\'' +
                ", practitioners='" + practitioners + '\'' +
                ", patientId='" + patientId + '\'' +
                ", overallAvgHealthScore=" + overallAvgHealthScore +
                ", timezone='" + timezone + '\'' +
                ", updated_at='" + updated_at + '\'' +
                ", created_at='" + created_at + '\'' +
                '}';
    }
}
