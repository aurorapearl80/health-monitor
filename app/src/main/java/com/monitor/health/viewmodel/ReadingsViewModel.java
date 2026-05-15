package com.monitor.health.viewmodel;


import android.util.Log;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.monitor.health.ApiClient;
import com.monitor.health.model.ReadingValueEcg;
import com.monitor.health.response.readinghistory.ReadingHistoryItem;
import com.monitor.health.response.readinghistory.ReadingHistoryResponse;
import com.monitor.health.response.alldata.DataItem;
import com.monitor.health.response.alldata.ReadingMetricValue;
import com.monitor.health.response.alldata.ReadingValueActivity;
import com.monitor.health.response.alldata.ReadingValueBloodPressure;
import com.monitor.health.response.alldata.ReadingValueECG;
import com.monitor.health.response.alldata.ReadingValueGlucose;
import com.monitor.health.response.alldata.ReadingValueOxygen;
import com.monitor.health.response.alldata.ReadingValueTemperature;
import com.monitor.health.response.alldata.ReadingValueWeight;
import com.monitor.health.response.alldata.Root;
import com.monitor.health.response.alldata.TypesAvailability;
import com.monitor.health.utility.TimeAgo;
import com.google.gson.Gson;
import com.google.gson.JsonElement;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class ReadingsViewModel extends ViewModel {

    private final MutableLiveData<Boolean> loading = new MutableLiveData<>(false);
    private final MutableLiveData<String> error = new MutableLiveData<>(null);
    private final MutableLiveData<List<DataItem>> readings = new MutableLiveData<>(new ArrayList<>());
    private final MutableLiveData<List<DataItem>> todayReadings = new MutableLiveData<>();
    private final MutableLiveData<List<DataItem>> weekReadings = new MutableLiveData<>();
    private final MutableLiveData<List<DataItem>> monthsReadings = new MutableLiveData<>();

    // Type-specific streams (observe only what a screen needs)
    private final MutableLiveData<ReadingMetricValue> glucose = new MutableLiveData<>();
    private final MutableLiveData<String> glucoseUpdatedAt = new MutableLiveData<>();
    private final MutableLiveData<String> glucoseEventDescription = new MutableLiveData<>();
    private final MutableLiveData<List<ReadingMetricValue>> bloodPressure = new MutableLiveData<>();
    private final MutableLiveData<String> bpUpdatedAt = new MutableLiveData<>();
    private final MutableLiveData<String> bpEventDescription = new MutableLiveData<>();
    //Weight
    private final MutableLiveData<List<ReadingMetricValue>> weight = new MutableLiveData<>();
    private final MutableLiveData<String> weightUpdatedAt = new MutableLiveData<>();
    private final MutableLiveData<String> weightEventDescription = new MutableLiveData<>();
    private final MutableLiveData<List<ReadingMetricValue>> oxygen = new MutableLiveData<>();
    private final MutableLiveData<String> oxygenUpdatedAt = new MutableLiveData<>();
    private final MutableLiveData<String> oxygenEventDescription = new MutableLiveData<>();
    private final MutableLiveData<List<ReadingMetricValue>> temperature = new MutableLiveData<>();
    private final MutableLiveData<String> temperatureUpdatedAt = new MutableLiveData<>();
    private final MutableLiveData<String> temperatureEventDescription = new MutableLiveData<>();

    private final MutableLiveData<List<ReadingMetricValue>> ecg = new MutableLiveData<>();
    private final MutableLiveData<ReadingValueEcg> ecgServer = new MutableLiveData<>();
    private final MutableLiveData<String> ecgUpdatedAt = new MutableLiveData<>();
    private final MutableLiveData<String> ecgEventDescription = new MutableLiveData<>();
    private final MutableLiveData<ReadingValueActivity> activity = new MutableLiveData<>();

    // Loading states
    // Loading states
    private final MutableLiveData<Boolean> isDayLoading = new MutableLiveData<>(false);
    private final MutableLiveData<Boolean> isWeekLoading = new MutableLiveData<>(false);
    private final MutableLiveData<Boolean> isMonthLoading = new MutableLiveData<>(false);

    private final MutableLiveData<TypesAvailability> typesAvailabilityMutableLiveData = new MutableLiveData<>(null);

    // ECG reading-history (api/doctor-watches/reading-history)
    private final MutableLiveData<List<ReadingHistoryItem>> ecgHistoryDay = new MutableLiveData<>();
    private final MutableLiveData<List<ReadingHistoryItem>> ecgHistoryWeek = new MutableLiveData<>();
    private final MutableLiveData<List<ReadingHistoryItem>> ecgHistoryMonth = new MutableLiveData<>();

    // Loading state getters
    public LiveData<Boolean> getIsDayLoading() {
        return isDayLoading;
    }

    public LiveData<Boolean> getIsWeekLoading() {
        return isWeekLoading;
    }

    public LiveData<Boolean> getIsMonthLoading() {
        return isMonthLoading;
    }

    // Loading state setters
    public void setDayLoading(boolean loading) {
        isDayLoading.setValue(loading);
    }

    public void setWeekLoading(boolean loading) {
        isWeekLoading.setValue(loading);
    }

    public void setMonthLoading(boolean loading) {
        isMonthLoading.setValue(loading);
    }

    public MutableLiveData<List<DataItem>> getTodayReadings() {
        return todayReadings;
    }

    public MutableLiveData<List<DataItem>> getWeekReadings() {
        return weekReadings;
    }

    public MutableLiveData<List<DataItem>> getMonthsReadings() {
        return monthsReadings;
    }

    private final Gson gson = new Gson();

    // --- Expose LiveData ---

    public LiveData<Boolean> getLoading() { return loading; }
    public LiveData<String> getError() { return error; }
    public LiveData<List<DataItem>> getReadings() { return readings; }

    public LiveData<ReadingMetricValue> getGlucose() { return glucose; }
    public LiveData<List<ReadingMetricValue>> getBloodPressure() { return bloodPressure; }
    public LiveData<List<ReadingMetricValue>> getOxygen() { return oxygen; }
    public LiveData<List<ReadingMetricValue>> getTemperature() { return temperature; }
    public LiveData<List<ReadingMetricValue>> getWeight() { return weight; }
    public LiveData<List<ReadingMetricValue>> getEcg() { return ecg; }
    public LiveData<ReadingValueEcg> getEcgServer() { return ecgServer; }
    public LiveData<ReadingValueActivity> getActivity() { return activity; }

    public MutableLiveData<String> getGlucoseUpdatedAt() {
        return glucoseUpdatedAt;
    }

    public MutableLiveData<String> getGlucoseEventDescription() {
        return glucoseEventDescription;
    }

    public MutableLiveData<String> getBpUpdatedAt() {
        return bpUpdatedAt;
    }

    public MutableLiveData<String> getBpEventDescription() {
        return bpEventDescription;
    }

    public MutableLiveData<String> getWeightUpdatedAt() {
        return weightUpdatedAt;
    }

    public MutableLiveData<String> getWeightEventDescription() {
        return weightEventDescription;
    }

    public MutableLiveData<String> getOxygenUpdatedAt() {
        return oxygenUpdatedAt;
    }

    public MutableLiveData<String> getOxygenEventDescription() {
        return oxygenEventDescription;
    }

    public MutableLiveData<String> getTemperatureUpdatedAt() {
        return temperatureUpdatedAt;
    }

    public MutableLiveData<String> getTemperatureEventDescription() {
        return temperatureEventDescription;
    }

    public MutableLiveData<String> getEcgUpdatedAt() {
        return ecgUpdatedAt;
    }

    public MutableLiveData<String> getEcgEventDescription() {
        return ecgEventDescription;
    }

    public MutableLiveData<TypesAvailability> getTypesAvailabilityMutableLiveData() {
        return typesAvailabilityMutableLiveData;
    }

    public void setTypesAvailabilityMutableLiveData(TypesAvailability typesAvailabilityMutableLiveDataLocal){
        typesAvailabilityMutableLiveData.setValue(typesAvailabilityMutableLiveDataLocal);
    }

    // --- API call

    /**
     * Fetch latest readings and fan out per type.
     * @param baseUrl Your Constant.BASE_URL_BGM
     * @param token   API token
     * @param imei    Device IMEI (pass from Fragment/Activity)
     */
    public void fetchToday(String baseUrl, String token, String imei, String typeId, String startDate, String endDate) {
        loading.setValue(true);
        error.setValue(null);

        Call<Root> call = ApiClient.getUserService(baseUrl, token, imei).getReadingHistories(
                typeId,
                startDate,
                endDate,
                "all",
                1,
                "-createdAt"
        );
        call.enqueue(new Callback<Root>() {
            @Override
            public void onResponse(Call<Root> call, Response<Root> response) {
                Log.d("today DATA --- ", response.body()+"");
                loading.setValue(false);

                if (!response.isSuccessful() || response.body() == null) {
                    error.setValue(response.message() != null ? response.message() : "Unknown error");
                    return;
                }

                List<DataItem> data = response.body().getData();
                if (data == null) data = new ArrayList<>();
                Log.d("today DATA --- ", data.toString()+"");
                todayReadings.setValue(data);

                // Dispatch each item to the proper stream
//                for (DataItem item : data) {
//                    dispatchItem(item);
//                }
            }

            @Override
            public void onFailure(Call<Root> call, Throwable t) {
                loading.setValue(false);
                error.setValue(t != null ? t.getMessage() : "Network error");
            }
        });
    }

    public void fetchWeek(String baseUrl, String token, String imei, String typeId, String startDate, String endDate) {
        loading.setValue(true);
        error.setValue(null);

        Call<Root> call = ApiClient.getUserService(baseUrl, token, imei).getReadingHistories(
                typeId,
                startDate,
                endDate,
                "all",
                1,
                "-createdAt"
        );
        call.enqueue(new Callback<Root>() {
            @Override
            public void onResponse(Call<Root> call, Response<Root> response) {

                loading.setValue(false);

                if (!response.isSuccessful() || response.body() == null) {
                    error.setValue(response.message() != null ? response.message() : "Unknown error");
                    return;
                }

                List<DataItem> data = response.body().getData();
                if (data == null) data = new ArrayList<>();
                //Log.d("today DATA --- ", data.toString()+"");
                weekReadings.setValue(data);

                // Dispatch each item to the proper stream
//                for (DataItem item : data) {
//                    dispatchItem(item);
//                }
            }

            @Override
            public void onFailure(Call<Root> call, Throwable t) {
                loading.setValue(false);
                error.setValue(t != null ? t.getMessage() : "Network error");
            }
        });
    }

    public void fetchMonth(String baseUrl, String token, String imei, String typeId, String startDate, String endDate) {
        loading.setValue(true);
        error.setValue(null);

        Call<Root> call = ApiClient.getUserService(baseUrl, token, imei).getReadingHistories(
                typeId,
                startDate,
                endDate,
                "all",
                1,
                "-createdAt"
        );
        call.enqueue(new Callback<Root>() {
            @Override
            public void onResponse(Call<Root> call, Response<Root> response) {

                loading.setValue(false);

                if (!response.isSuccessful() || response.body() == null) {
                    error.setValue(response.message() != null ? response.message() : "Unknown error");
                    return;
                }

                List<DataItem> data = response.body().getData();
                if (data == null) data = new ArrayList<>();
                Log.d("today DATA --- ", data.toString()+"");
                monthsReadings.setValue(data);

                // Dispatch each item to the proper stream
//                for (DataItem item : data) {
//                    dispatchItem(item);
//                }
            }

            @Override
            public void onFailure(Call<Root> call, Throwable t) {
                loading.setValue(false);
                error.setValue(t != null ? t.getMessage() : "Network error");
            }
        });
    }

    public MutableLiveData<List<ReadingHistoryItem>> getEcgHistoryDay() { return ecgHistoryDay; }
    public MutableLiveData<List<ReadingHistoryItem>> getEcgHistoryWeek() { return ecgHistoryWeek; }
    public MutableLiveData<List<ReadingHistoryItem>> getEcgHistoryMonth() { return ecgHistoryMonth; }

    public void fetchEcgHistoryDay(String baseUrl, String token, String imei,
                                   String typeId, String startDate, String endDate) {
        ApiClient.getUserService(baseUrl, token, imei)
                .getReadingHistory(typeId, startDate, endDate, 100, 1)
                .enqueue(new Callback<ReadingHistoryResponse>() {
                    @Override
                    public void onResponse(Call<ReadingHistoryResponse> call, Response<ReadingHistoryResponse> response) {
                        if (response.isSuccessful() && response.body() != null && response.body().getData() != null) {
                            ecgHistoryDay.setValue(response.body().getData());
                        } else {
                            ecgHistoryDay.setValue(new ArrayList<>());
                        }
                    }
                    @Override
                    public void onFailure(Call<ReadingHistoryResponse> call, Throwable t) {
                        ecgHistoryDay.setValue(new ArrayList<>());
                    }
                });
    }

    public void fetchEcgHistoryWeek(String baseUrl, String token, String imei,
                                    String typeId, String startDate, String endDate) {
        ApiClient.getUserService(baseUrl, token, imei)
                .getReadingHistory(typeId, startDate, endDate, 100, 1)
                .enqueue(new Callback<ReadingHistoryResponse>() {
                    @Override
                    public void onResponse(Call<ReadingHistoryResponse> call, Response<ReadingHistoryResponse> response) {
                        if (response.isSuccessful() && response.body() != null && response.body().getData() != null) {
                            ecgHistoryWeek.setValue(response.body().getData());
                        } else {
                            ecgHistoryWeek.setValue(new ArrayList<>());
                        }
                    }
                    @Override
                    public void onFailure(Call<ReadingHistoryResponse> call, Throwable t) {
                        ecgHistoryWeek.setValue(new ArrayList<>());
                    }
                });
    }

    public void fetchEcgHistoryMonth(String baseUrl, String token, String imei,
                                     String typeId, String startDate, String endDate) {
        ApiClient.getUserService(baseUrl, token, imei)
                .getReadingHistory(typeId, startDate, endDate, 100, 1)
                .enqueue(new Callback<ReadingHistoryResponse>() {
                    @Override
                    public void onResponse(Call<ReadingHistoryResponse> call, Response<ReadingHistoryResponse> response) {
                        if (response.isSuccessful() && response.body() != null && response.body().getData() != null) {
                            ecgHistoryMonth.setValue(response.body().getData());
                        } else {
                            ecgHistoryMonth.setValue(new ArrayList<>());
                        }
                    }
                    @Override
                    public void onFailure(Call<ReadingHistoryResponse> call, Throwable t) {
                        ecgHistoryMonth.setValue(new ArrayList<>());
                    }
                });
    }

    /**
     * Fetch latest readings and fan out per type.
     * @param baseUrl Your Constant.BASE_URL_BGM
     * @param token   API token
     * @param imei    Device IMEI (pass from Fragment/Activity)
     */
    public void fetchLatest(String baseUrl, String token, String imei) {
        loading.setValue(true);
        error.setValue(null);

        Call<Root> call = ApiClient.getUserService(baseUrl, token, imei).getReadingsLatest(imei);
        call.enqueue(new Callback<Root>() {
            @Override
            public void onResponse(Call<Root> call, Response<Root> response) {
                Log.d("DATA", response.body()+"");
                loading.setValue(false);

                if (!response.isSuccessful() || response.body() == null) {
                    error.setValue(response.message() != null ? response.message() : "Unknown error");
                    return;
                }

                if(response.body().getPerMetricData() != null) {
                    for(int i = 0; i < response.body().getPerMetricData().size(); i++) {

                        if(response.body().getPerMetricData().get(i).getName().equals("Heart Rate")) {
                            Log.d("DATA-name", response.body().getPerMetricData().get(i).getName()+"");
                            Log.d("DATA-value", response.body().getPerMetricData().get(i).getValue()+"");
                            ReadingValueEcg ecgL = new ReadingValueEcg();
                            ecgL.setHeartRate(response.body().getPerMetricData().get(i).getValue());
                            ecgL.setEcgMeasurementLength(0);
                            ecgL.setMood(0);
                            ecgL.setDateFormat(response.body().getPerMetricData().get(i).getCreatedAt());

                            ecgServer.setValue(ecgL);
                        }
                    }
                }


                List<DataItem> data = response.body().getData();
                if (data == null) data = new ArrayList<>();
                readings.setValue(data);

                // Dispatch each item to the proper stream
                for (DataItem item : data) {
                    dispatchItem(item);
                }

                //set Type value
                typesAvailabilityMutableLiveData.setValue(response.body().getTypesAvailability());
            }

            @Override
            public void onFailure(Call<Root> call, Throwable t) {
                loading.setValue(false);
                error.setValue(t != null ? t.getMessage() : "Network error");
            }
        });
    }



    /**
     * Fetch latest readings and fan out per type.
     */
    public void getLatestInfo(String baseUrl, String token, String imei) {
        loading.setValue(true);
        error.setValue(null);

        Call<Root> call = ApiClient.getUserService(baseUrl, token, imei).getReadingsLatest(imei);
        call.enqueue(new Callback<Root>() {
            @Override
            public void onResponse(Call<Root> call, Response<Root> response) {
                Log.d("DATA", response.body()+"");
                loading.setValue(false);

                if (!response.isSuccessful() || response.body() == null) {
                    error.setValue(response.message() != null ? response.message() : "Unknown error");
                    return;
                }

                List<DataItem> data = response.body().getData();
                if (data == null) data = new ArrayList<>();
                readings.setValue(data);

                // Dispatch each item to the proper stream
                for (DataItem item : data) {
                    dispatchItem(item);
                }

                //set Type value
                typesAvailabilityMutableLiveData.setValue(response.body().getTypesAvailability());
            }

            @Override
            public void onFailure(Call<Root> call, Throwable t) {
                loading.setValue(false);
                error.setValue(t != null ? t.getMessage() : "Network error");
            }
        });
    }

    // --- Helpers ---

    private void dispatchItem(DataItem item) {
        if (item == null || item.getReadingType() == null || item.getReadingType().isEmpty()) return;

        String desc = safe(item.getReadingType().get(0).getDescription()); // e.g., "Blood Glucose"



        switch (desc) {
            case "Blood Glucose": {
                ReadingMetricValue v = convert(item.getReadingMetricValues().get(0), ReadingMetricValue.class);
                glucoseUpdatedAt.setValue(TimeAgo.relativeFromIsoUtc(item.getDeviceReadingDate()));
                if (v != null) {
                    glucose.setValue(v);
                    glucoseEventDescription.setValue(String.valueOf(item.getReadingValue()));
                    String type = item.getReadingType() != null && !item.getReadingType().isEmpty()
                            ? item.getReadingType().get(0).getDescription() : "";
                    if ("Blood Glucose".equals(type)) {
                        Gson gson = new Gson();
                        ReadingValueGlucose g = gson.fromJson(gson.toJsonTree(item.getReadingValue()),
                                ReadingValueGlucose.class);
                        if (g != null) glucoseEventDescription.setValue(String.valueOf(g.getEventDescription()));
                    }
                }


                break;
            }
            case "Blood Pressure": {
                List<ReadingMetricValue> v = Arrays.asList(convert(item.getReadingMetricValues(), ReadingMetricValue[].class));
                //if (v != null) bloodPressure.setValue(v);
                bpUpdatedAt.setValue(TimeAgo.relativeFromIsoUtc(item.getUpdatedAt()));
                if (v != null) {
                    bloodPressure.setValue(v);
                    bpEventDescription.setValue(String.valueOf(item.getReadingValue()));
                    String type = item.getReadingType() != null && !item.getReadingType().isEmpty()
                            ? item.getReadingType().get(0).getDescription() : "";
                    if ("Blood Pressure".equals(type)) {
                        Gson gson = new Gson();
                        ReadingValueGlucose g = gson.fromJson(gson.toJsonTree(item.getReadingValue()),
                                ReadingValueGlucose.class);
                        if (g != null) bpEventDescription.setValue(String.valueOf(g.getEventDescription()));
                    }
                }
                break;
            }
            case "Blood Oxygen": {
                List<ReadingMetricValue> v = Arrays.asList(convert(item.getReadingMetricValues(), ReadingMetricValue[].class));
                oxygenUpdatedAt.setValue(TimeAgo.relativeFromIsoUtc(item.getUpdatedAt()));
                oxygen.setValue(v);
                oxygenEventDescription.setValue(String.valueOf(item.getReadingValue()));
                String type = item.getReadingType() != null && !item.getReadingType().isEmpty()
                        ? item.getReadingType().get(0).getDescription() : "";
                if ("Blood Oxygen".equals(type)) {
                    Gson gson = new Gson();
                    ReadingValueGlucose g = gson.fromJson(gson.toJsonTree(item.getReadingValue()),
                            ReadingValueGlucose.class);
                    if (g != null) oxygenEventDescription.setValue(String.valueOf(g.getEventDescription()));
                }
                break;
            }
            case "Temperature": {
                List<ReadingMetricValue> v = Arrays.asList(convert(item.getReadingMetricValues(), ReadingMetricValue[].class));
                //temperature.setValue(v);
                temperatureUpdatedAt.setValue(TimeAgo.relativeFromIsoUtc(item.getUpdatedAt()));
                temperature.setValue(v);
                temperatureEventDescription.setValue(String.valueOf(item.getReadingValue()));
                String type = item.getReadingType() != null && !item.getReadingType().isEmpty()
                        ? item.getReadingType().get(0).getDescription() : "";
                if ("Temperature".equals(type)) {
                    Gson gson = new Gson();
                    ReadingValueGlucose g = gson.fromJson(gson.toJsonTree(item.getReadingValue()),
                            ReadingValueGlucose.class);
                    if (g != null) temperatureEventDescription.setValue(String.valueOf(g.getEventDescription()));
                }
                break;
            }
            case "Weight": {
                List<ReadingMetricValue> v = Arrays.asList(convert(item.getReadingMetricValues(), ReadingMetricValue[].class));
                weightUpdatedAt.setValue(TimeAgo.relativeFromIsoUtc(item.getUpdatedAt()));
                if (v != null) {
                    weight.setValue(v);
                    weightEventDescription.setValue(String.valueOf(item.getReadingValue()));
                    String type = item.getReadingType() != null && !item.getReadingType().isEmpty()
                            ? item.getReadingType().get(0).getDescription() : "";
                    if ("Weight".equals(type)) {
                        Gson gson = new Gson();
                        ReadingValueGlucose g = gson.fromJson(gson.toJsonTree(item.getReadingValue()),
                                ReadingValueGlucose.class);
                        if (g != null) weightEventDescription.setValue(String.valueOf(g.getEventDescription()));
                    }
                }
                break;
            }
            case "ECG": {

                Log.d("ECG: ", "result "+ desc + " this is the");

                //ReadingValueECG v = convert(item.getReadingValue(), ReadingValueECG.class);
//                List<ReadingMetricValue> v = Arrays.asList(convert(item.getReadingMetricValues(), ReadingMetricValue[].class));
//                if (v != null) ecg.setValue(v);
                List<ReadingMetricValue> v = Arrays.asList(convert(item.getReadingMetricValues(), ReadingMetricValue[].class));

                //temperature.setValue(v);
                Log.d("ECG: ", "result value - mao"+ item.toString()+ " this is the");
                if(item != null) {
                    if(item.getReadingMetricValues() != null) {
                        Log.d("ECG: ", "result value - mao" + item.getReadingMetricValues());
                        Log.d("ECG: ", "result value - mao" + item.getReadingMetricValues());
                    }
                }
                ecgUpdatedAt.setValue(TimeAgo.relativeFromIsoUtc(item.getUpdatedAt()));

//                Object value = item.getReadingValue();
//
//                if (value instanceof java.util.Map) {
//                    java.util.Map<?, ?> map = (java.util.Map<?, ?>) value;
//
//                    double heartRate = toDouble(map.get("heartRate"));
//                    double length = toDouble(map.get("ecgMeasurementLength"));
//                    double mood = toDouble(map.get("mood"));
//
//                    Log.d("ECG: break", "HR=" + heartRate + ", len=" + length + ", mood=" + mood);
//
//                    ReadingValueEcg ecgL = new ReadingValueEcg();
//                    ecgL.setHeartRate(heartRate);
//                    ecgL.setEcgMeasurementLength(length);
//                    ecgL.setMood(mood);
//
//                    ecgServer.setValue(ecgL);
//                } else {
//                    Log.d("ECG: break", "Not a Map. class=" + (value == null ? "null" : value.getClass().getName()));
//                }



                ecg.setValue(v);
                ecgEventDescription.setValue(String.valueOf(item.getReadingValue()));
                String type = item.getReadingType() != null && !item.getReadingType().isEmpty()
                        ? item.getReadingType().get(0).getDescription() : "";
                if ("ECG".equals(type)) {
                    Gson gson = new Gson();
                    ReadingValueGlucose g = gson.fromJson(gson.toJsonTree(item.getReadingValue()),
                            ReadingValueGlucose.class);
                    if (g != null) ecgEventDescription.setValue(String.valueOf(g.getEventDescription()));
                }
                break;
            }
            case "Activity": {
                ReadingValueActivity v = convert(item.getReadingValue(), ReadingValueActivity.class);
                if (v != null) activity.setValue(v);
                break;
            }
            default:
                // Unknown type; ignore or log
                break;
        }
    }

    private static double toDouble(Object o) {
        if (o == null) return 0.0;
        if (o instanceof Number) return ((Number) o).doubleValue();
        try { return Double.parseDouble(String.valueOf(o)); }
        catch (Exception e) { return 0.0; }
    }

    private <T> T convert(Object src, Class<T> type) {
        if (src == null) return null;
        JsonElement tree = gson.toJsonTree(src);   // handles LinkedTreeMap -> JSON
        return gson.fromJson(tree, type);
    }

    private static String safe(String s) { return s == null ? "" : s; }
}
