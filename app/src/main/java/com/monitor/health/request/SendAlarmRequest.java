package com.monitor.health.request;

public class SendAlarmRequest {
    private String model;
    private String maker;
    private String os;
    private String country;
    private double latitude;
    private double longitude;
    private String deviceUuid;
    private int type;
    private int precision;
    private int batt;
    private boolean cardio;

    public SendAlarmRequest(double latitude, double longitude, String deviceUuid, int type, int precision, int batt, boolean cardio,String model, String maker,  String os, String country) {
        this.latitude = latitude;
        this.longitude = longitude;
        this.deviceUuid = deviceUuid;
        this.type = type;
        this.precision = precision;
        this.batt = batt;
        this.cardio = cardio;
        this.model = model;
        this.maker = maker;
        this.os = os;
        this.country = country;
    }

    public String getModel() {
        return model;
    }

    public void setModel(String model) {
        this.model = model;
    }

    public String getMaker() {
        return maker;
    }

    public void setMaker(String maker) {
        this.maker = maker;
    }

    public String getOs() {
        return os;
    }

    public void setOs(String os) {
        this.os = os;
    }

    public String getCountry() {
        return country;
    }

    public void setCountry(String country) {
        this.country = country;
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

    public String getDeviceUuid() {
        return deviceUuid;
    }

    public void setDeviceUuid(String deviceUuid) {
        this.deviceUuid = deviceUuid;
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

    public boolean isCardio() {
        return cardio;
    }

    public void setCardio(boolean cardio) {
        this.cardio = cardio;
    }

    @Override
    public String toString() {
        return "LocationData{" +
                "latitude=" + latitude +
                ", longitude=" + longitude +
                ", deviceUuid='" + deviceUuid + '\'' +
                ", type=" + type +
                ", precision=" + precision +
                ", batt=" + batt +
                ", cardio=" + cardio +
                '}';
    }
}
