package com.monitor.health.response.glocuse;

public class GlucoseReadingType {
    private String description;
    private String subTypes;
    private GlucoseOid id;

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

    public GlucoseOid getId() {
        return id;
    }

    public void setId(GlucoseOid id) {
        this.id = id;
    }
}
