package com.monitor.health.model;

public class UserData {
    private String uuidToken;
    private String sdkToken;
    private String deviceUuid;
    private String name;
    private String surname;
    private String userEmail;
    private String phone;
    private String model;
    private String maker;
    private String os;
    private String country;

    // Default Constructor
    public UserData() {}

    // Parameterized Constructor
    public UserData(String uuidToken, String sdkToken, String deviceUuid, String name, String surname,
                    String userEmail, String phone, String model, String maker, String os, String country) {
        this.uuidToken = uuidToken;
        this.sdkToken = sdkToken;
        this.deviceUuid = deviceUuid;
        this.name = name;
        this.surname = surname;
        this.userEmail = userEmail;
        this.phone = phone;
        this.model = model;
        this.maker = maker;
        this.os = os;
        this.country = country;
    }

    // Getters and Setters
    public String getUuidToken() {
        return uuidToken;
    }

    public void setUuidToken(String uuidToken) {
        this.uuidToken = uuidToken;
    }

    public String getSdkToken() {
        return sdkToken;
    }

    public void setSdkToken(String sdkToken) {
        this.sdkToken = sdkToken;
    }

    public String getDeviceUuid() {
        return deviceUuid;
    }

    public void setDeviceUuid(String deviceUuid) {
        this.deviceUuid = deviceUuid;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getSurname() {
        return surname;
    }

    public void setSurname(String surname) {
        this.surname = surname;
    }

    public String getUserEmail() {
        return userEmail;
    }

    public void setUserEmail(String userEmail) {
        this.userEmail = userEmail;
    }

    public String getPhone() {
        return phone;
    }

    public void setPhone(String phone) {
        this.phone = phone;
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

    // toString method for debugging
    @Override
    public String toString() {
        return "UserData{" +
                "uuidToken='" + uuidToken + '\'' +
                ", sdkToken='" + sdkToken + '\'' +
                ", deviceUuid='" + deviceUuid + '\'' +
                ", name='" + name + '\'' +
                ", surname='" + surname + '\'' +
                ", userEmail='" + userEmail + '\'' +
                ", phone='" + phone + '\'' +
                ", model='" + model + '\'' +
                ", maker='" + maker + '\'' +
                ", os='" + os + '\'' +
                ", country='" + country + '\'' +
                '}';
    }
}

