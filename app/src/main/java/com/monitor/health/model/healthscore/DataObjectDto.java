package com.monitor.health.model.healthscore;

public class DataObjectDto {
    private UserDrWatchDto data;

    public DataObjectDto() {}
    public DataObjectDto(UserDrWatchDto data) {
        this.data = data;
    }

    public UserDrWatchDto getData() {
        return data;
    }

    public void setData(UserDrWatchDto data) {
        this.data = data;
    }

    @Override
    public String toString() {
        return "DataObject{" +
                "data=" + data +
                '}';
    }
}
