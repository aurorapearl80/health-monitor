package com.monitor.health.response.bledevice;


import com.google.gson.annotations.SerializedName;

public class DeviceDetails {

    @SerializedName("name")
    private String name;

    @SerializedName("description")
    private String description;

    @SerializedName("brand")
    private String brand;

    @SerializedName("brandModel")
    private String brandModel;

    @SerializedName("isWatch")
    private boolean isWatch;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getBrand() {
        return brand;
    }

    public void setBrand(String brand) {
        this.brand = brand;
    }

    public String getBrandModel() {
        return brandModel;
    }

    public void setBrandModel(String brandModel) {
        this.brandModel = brandModel;
    }

    public boolean isWatch() {
        return isWatch;
    }

    public void setWatch(boolean watch) {
        isWatch = watch;
    }

    @Override
    public String toString() {
        return "DeviceDetails{" +
                "name='" + name + '\'' +
                ", description='" + description + '\'' +
                ", brand='" + brand + '\'' +
                ", brandModel='" + brandModel + '\'' +
                ", isWatch=" + isWatch +
                '}';
    }
}
