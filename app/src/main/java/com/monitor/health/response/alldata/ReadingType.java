package com.monitor.health.response.alldata;

public class ReadingType {
    private String description;
    private String subTypes;
    //private ObjectId id;

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getSubTypes() {
        return subTypes;
    }

    public void setSubTypes(String subTypes) {
        this.subTypes = subTypes;
    }

//    public ObjectId getId() {
//        return id;
//    }
//
//    public void setId(ObjectId id) {
//        this.id = id;
//    }


    @Override
    public String toString() {
        return "ReadingType{" +
                "description='" + description + '\'' +
                ", subTypes='" + subTypes + '\'' +
                '}';
    }
}
