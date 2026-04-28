package com.monitor.health.response.alldata;

public class Source {
    private String description;
    private ObjectId id;

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public ObjectId getId() {
        return id;
    }

    public void setId(ObjectId id) {
        this.id = id;
    }

    @Override
    public String toString() {
        return "Source{" +
                "description='" + description + '\'' +
                ", id=" + id +
                '}';
    }
}
