package com.monitor.health.response.alldata;

import java.util.List;

import com.monitor.health.response.permetricdata.PerMetricDataItem;
import com.google.gson.annotations.SerializedName;
public class Root {
    private List<DataItem> data;
    @SerializedName("types_availability")
    private TypesAvailability typesAvailability;

    @SerializedName("per_metric_data")
    private List<PerMetricDataItem> perMetricData;


    public List<DataItem> getData() {
        return data;
    }

    public void setData(List<DataItem> data) {
        this.data = data;
    }

    public TypesAvailability getTypesAvailability() {
        return typesAvailability;
    }

    public void setTypesAvailability(TypesAvailability typesAvailability) {
        this.typesAvailability = typesAvailability;
    }


    public List<PerMetricDataItem> getPerMetricData() {
        return perMetricData;
    }

    public void setPerMetricData(List<PerMetricDataItem> perMetricData) {
        this.perMetricData = perMetricData;
    }

    @Override
    public String toString() {
        return "Root{" +
                "data=" + data +
                ", typesAvailability=" + typesAvailability +
                ", perMetricData=" + perMetricData +
                '}';
    }
}
