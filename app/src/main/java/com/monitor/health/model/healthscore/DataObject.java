package com.monitor.health.model.healthscore;

public class DataObject {
    private UserDrWatch data;

    public DataObject() {}
    public DataObject(UserDrWatch data) {
        this.data = data;
    }

    public UserDrWatch getData() {
        return data;
    }

    public void setData(UserDrWatch data) {
        this.data = data;
    }

    @Override
    public String toString() {
        return "DataObject{" +
                "data=" + data +
                '}';
    }
}
