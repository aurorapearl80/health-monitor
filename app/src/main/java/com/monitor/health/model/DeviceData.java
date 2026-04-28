package com.monitor.health.model;

public class DeviceData {
    private String sessionToken;
    private double latitude;
    private double longitude;
    private String idDev;
    private int type;
    private int precision;
    private int batt;
    private int cardio;

    // Default Constructor
    public DeviceData() {}

    // Parameterized Constructor
    public DeviceData(String sessionToken, double latitude, double longitude, String idDev,
                      int type, int precision, int batt, int cardio) {
        this.sessionToken = sessionToken;
        this.latitude = latitude;
        this.longitude = longitude;
        this.idDev = idDev;
        this.type = type;
        this.precision = precision;
        this.batt = batt;
        this.cardio = cardio;
    }

    // Getters and Setters
    public String getSessionToken() {
        return sessionToken;
    }

    public void setSessionToken(String sessionToken) {
        this.sessionToken = sessionToken;
    }

    public double getLatitude() {
        return latitude;
    }

    public void setLatitude(double latitude) {
        this.latitude = latitude;
    }

    public double getLongitude() {
        return longitude;
    }

    public void setLongitude(double longitude) {
        this.longitude = longitude;
    }

    public String getIdDev() {
        return idDev;
    }

    public void setIdDev(String idDev) {
        this.idDev = idDev;
    }

    public int getType() {
        return type;
    }

    public void setType(int type) {
        this.type = type;
    }

    public int getPrecision() {
        return precision;
    }

    public void setPrecision(int precision) {
        this.precision = precision;
    }

    public int getBatt() {
        return batt;
    }

    public void setBatt(int batt) {
        this.batt = batt;
    }

    public int getCardio() {
        return cardio;
    }

    public void setCardio(int cardio) {
        this.cardio = cardio;
    }

    // toString Method
    @Override
    public String toString() {
        return "DeviceData{" +
                "sessionToken='" + sessionToken + '\'' +
                ", latitude=" + latitude +
                ", longitude=" + longitude +
                ", idDev='" + idDev + '\'' +
                ", type=" + type +
                ", precision=" + precision +
                ", batt=" + batt +
                ", cardio=" + cardio +
                '}';
    }
}

