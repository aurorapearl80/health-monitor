package com.monitor.health.response.glocuse;

import java.util.List;

public class GlucoseServerResponse {
    private List<GlucoseDataItem> data;
    private GlucoseMeta meta;

    public List<GlucoseDataItem> getData() {
        return data;
    }

    public void setData(List<GlucoseDataItem> data) {
        this.data = data;
    }

    public GlucoseMeta getMeta() {
        return meta;
    }

    public void setMeta(GlucoseMeta meta) {
        this.meta = meta;
    }

    @Override
    public String toString() {
        return "GlucoseServerResponse{" +
                "data=" + data +
                ", meta=" + meta +
                '}';
    }
}
