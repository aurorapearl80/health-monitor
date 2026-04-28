package com.monitor.health.viewmodel;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.monitor.health.graph.ReadingData;

import java.util.ArrayList;
import java.util.List;

public class SharedDataViewModel extends ViewModel {
    private final MutableLiveData<String> heartRate = new MutableLiveData<>();
    private final MutableLiveData<String> imeData = new MutableLiveData<>();
    private final MutableLiveData<Double> temperatureData = new MutableLiveData<>();

    private final MutableLiveData<String> glucoseCreatedAt = new MutableLiveData<>();

    private final MutableLiveData<Integer> glucoseData = new MutableLiveData<>();

    private final MutableLiveData<List<Double>> bloodPressureData = new MutableLiveData<>();

    private final MutableLiveData<Integer> heartRateExternal = new MutableLiveData<>();
    private final MutableLiveData<Integer> bloodValueExternal  = new MutableLiveData<>();

    private final MutableLiveData<List<String>> userInfo = new MutableLiveData<>();

    private final MutableLiveData<List<Integer>> oximeterData = new MutableLiveData<>();

    private final MutableLiveData<Double> weightData = new MutableLiveData<>();

    private final MutableLiveData<Integer> oxygen = new MutableLiveData<>();
    private final MutableLiveData<Integer> stepCount = new MutableLiveData<>();

    private final MutableLiveData<Integer> heartRateMonitor = new MutableLiveData<>();

    private final MutableLiveData<String> backgroundColor = new MutableLiveData<>("");
    private final MutableLiveData<String> chartBackgroundColor = new MutableLiveData<>("");
    private final MutableLiveData<String> gaugeArcColor = new MutableLiveData<>("");

    // Data
    private final MutableLiveData<List<ReadingData>> dayData = new MutableLiveData<>(new ArrayList<>());
    private final MutableLiveData<List<ReadingData>> weekData = new MutableLiveData<>(new ArrayList<>());
    private final MutableLiveData<List<ReadingData>> monthData = new MutableLiveData<>(new ArrayList<>());



    public LiveData<String> getBackgroundColor() {
        return backgroundColor;
    }

    public LiveData<String> getChartBackgroundColor() {
        return chartBackgroundColor;
    }

    public LiveData<String> getGaugeArcColor() {
        return gaugeArcColor;
    }

    public void setBackgroundColor(String color) {
        backgroundColor.setValue(color);
    }

    public void setChartBackgroundColor(String color) {
        chartBackgroundColor.setValue(color);
    }

    public void setGaugeArcColor(String color) {
        gaugeArcColor.setValue(color);
    }

    // Getters for data
    public LiveData<List<ReadingData>> getDayData() {
        return dayData;
    }

    public LiveData<List<ReadingData>> getWeekData() {
        return weekData;
    }

    public LiveData<List<ReadingData>> getMonthData() {
        return monthData;
    }

    // Setters for data
    public void setDayData(List<ReadingData> data) {
        dayData.setValue(data);
    }

    public void setWeekData(List<ReadingData> data) {
        weekData.setValue(data);
    }

    public void setMonthData(List<ReadingData> data) {
        monthData.setValue(data);
    }

    // Add single reading to specific tab
    public void addDayReading(ReadingData reading) {
        List<ReadingData> current = dayData.getValue();
        if (current != null) {
            current.add(0, reading); // Add to beginning
            dayData.setValue(current);
        }
    }

    public void addWeekReading(ReadingData reading) {
        List<ReadingData> current = weekData.getValue();
        if (current != null) {
            current.add(0, reading);
            weekData.setValue(current);
        }
    }

    public void addMonthReading(ReadingData reading) {
        List<ReadingData> current = monthData.getValue();
        if (current != null) {
            current.add(0, reading);
            monthData.setValue(current);
        }
    }


    public enum WearState { UNKNOWN, NOT_WORN, WORN, DISCONNECTED, BLUETOOTH_OFF, NO_PERMISSION }

    private final MutableLiveData<WearState> wearState = new MutableLiveData<>(WearState.NOT_WORN);
    public LiveData<WearState> wearState() { return wearState; }

    public void setWorn()         { wearState.postValue(WearState.WORN); }
    public void setNotWorn()      { wearState.postValue(WearState.NOT_WORN); }
    public void setDisconnected() { wearState.postValue(WearState.DISCONNECTED); }
    public void setBluetoothOff() { wearState.postValue(WearState.BLUETOOTH_OFF); }
    public void setNoPermission() { wearState.postValue(WearState.NO_PERMISSION); }


    public void setHeartRateExternal(Integer rate) {
        heartRateExternal.setValue(rate);
    }
    public void setBloodValueExternal(Integer rate) {
        bloodValueExternal.setValue(rate);
    }

    public MutableLiveData<Double> getTemperatureData() {
        return temperatureData;
    }

    public MutableLiveData<Integer> getHeartRateExternal() {
        return heartRateExternal;
    }

    public MutableLiveData<Integer> getBloodValueExternal() {
        return bloodValueExternal;
    }

    public LiveData<String> getHeartRate() {
        return heartRate;
    }

    public void setHeartRate(String rate) {
        heartRate.setValue(rate);
    }

    public LiveData<String> getImeData() {
        return imeData;
    }

    public void setImeData(String data) {
        imeData.setValue(data);
    }

    public LiveData<Double> getTemperature() {
        return temperatureData;
    }

    public void setTemperatureData(double data) {
        temperatureData.setValue(data);
    }

    public LiveData<List<Double>> getBloodPressureData() {
        return bloodPressureData;
    }

    public void setBloodPressureData(List<Double> data) {
        bloodPressureData.setValue(data);
    }

    public MutableLiveData<List<String>> getUserInfo() {
        return userInfo;
    }

    public void setUserInfo(List<String> _userInfo) {
        userInfo.setValue(_userInfo);
    }

    public MutableLiveData<String> getGlucoseCreatedAt() {
        return glucoseCreatedAt;
    }

    public void setGlucoseCreatedAt(String createdAt) {
        glucoseCreatedAt.setValue(createdAt);
    }

    public MutableLiveData<Integer> getGlucoseData() {
        return glucoseData;
    }

    public void setGlucoseData(Integer glucose) {
        glucoseData.setValue(glucose);
    }

    public MutableLiveData<List<Integer>> getOximeterData() {
        return oximeterData;
    }

    private final MutableLiveData<Boolean> isWatchWorn = new MutableLiveData<>(false);
    public LiveData<Boolean> isWatchWorn() { return isWatchWorn; }

    // Call this from your BLE/HealthManager callbacks
    public void setWatchWorn(boolean worn) { isWatchWorn.postValue(worn); }

    public void setOximeterData(List<Integer> _oximeterData) {
        oximeterData.setValue(_oximeterData);
    }

    public MutableLiveData<Double> getWeightData() {
        return weightData;
    }

    public void setWeightData(Double _weightData) {
        weightData.setValue(_weightData);
    }

    public void setHeartRateMonitor(Integer hearRate) {
        heartRateMonitor.setValue(hearRate);
    }
    public MutableLiveData<Integer> getHeartRateMonitor() {
        return heartRateMonitor;
    }

    public MutableLiveData<Integer> getOxygen() {
        return oxygen;
    }

    public void setOxygen(Integer _oxygen) {
        oxygen.setValue(_oxygen);
    }

    public MutableLiveData<Integer> getStepCount() {
        return stepCount;
    }

    public void setStepCount(Integer _step) {
        stepCount.setValue(_step);
    }
}

