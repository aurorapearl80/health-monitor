package com.monitor.health.viewmodel;


import android.util.Log;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.monitor.health.ApiClient;
import com.monitor.health.model.ReadingValueEcg;
import com.monitor.health.response.readinghistory.ReadingHistoryItem;
import com.monitor.health.response.readinghistory.ReadingHistoryResponse;
import com.monitor.health.response.alldata.ConvertedValue;
import com.monitor.health.response.alldata.DataItem;
import com.monitor.health.response.alldata.ReadingMetricValue;
import com.monitor.health.response.alldata.ReadingValueActivity;
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
        if (item == null) return;

        String type = item.getType();
        if (type == null || type.isEmpty()) return;

        // Items with no data: leave LiveData at its current value (fragments show "--" on null)
        if ("no_data".equals(item.getStatus()) || item.getValue() == null || item.getValue().isEmpty()) {
            Log.d("ViewModel", "no_data for type=" + type);
            return;
        }

        List<Object> rawValues = item.getValue();
        String timeAgo = TimeAgo.relativeFromIsoUtc(item.getDeviceReadingDate());

        switch (type) {
            case "blood_glucose": {
                double glucoseVal = toDouble(rawValues.get(0));
                ReadingMetricValue m = new ReadingMetricValue();
                m.setValue(glucoseVal);
                m.setUnit("mg/dL");
                glucoseUpdatedAt.setValue(timeAgo);
                glucose.setValue(m);
                glucoseEventDescription.setValue("");
                break;
            }
            case "blood_pressure": {
                if (rawValues.size() < 3) return;
                double systolic  = toDouble(rawValues.get(0));
                double diastolic = toDouble(rawValues.get(1));
                double bpm       = toDouble(rawValues.get(2));

                ReadingMetricValue sys = new ReadingMetricValue();
                sys.setValue(systolic);
                sys.setUnit("mmHg");

                ReadingMetricValue dia = new ReadingMetricValue();
                dia.setValue(diastolic);
                dia.setUnit("mmHg");

                ReadingMetricValue pulse = new ReadingMetricValue();
                pulse.setValue(bpm);
                pulse.setUnit("bpm");

                bpUpdatedAt.setValue(timeAgo);
                bloodPressure.setValue(Arrays.asList(sys, dia, pulse));
                bpEventDescription.setValue("");
                break;
            }
            case "weight": {
                double kgs = toDouble(rawValues.get(0));
                double lbs = kgs * 2.20462;

                ConvertedValue lbsCV = new ConvertedValue();
                lbsCV.setValue(Math.round(lbs * 10.0) / 10.0);
                lbsCV.setUnit("lbs");

                ConvertedValue kgsCV = new ConvertedValue();
                kgsCV.setValue(kgs);
                kgsCV.setUnit("kgs");

                ReadingMetricValue wm = new ReadingMetricValue();
                wm.setValue(kgs);
                wm.setUnit("kgs");
                wm.setShould_convert(true);
                wm.setConvertedValues(Arrays.asList(lbsCV, kgsCV));

                weightUpdatedAt.setValue(timeAgo);
                weight.setValue(Arrays.asList(wm));
                weightEventDescription.setValue("");
                break;
            }
            case "blood_oxygen": {
                if (rawValues.size() < 2) return;
                double spo2  = toDouble(rawValues.get(0));
                double pulse = toDouble(rawValues.get(1));

                ConvertedValue pulseCV = new ConvertedValue();
                pulseCV.setValue(pulse);
                pulseCV.setUnit("bpm");

                ReadingMetricValue pulseMetric = new ReadingMetricValue();
                pulseMetric.setValue(pulse);
                pulseMetric.setUnit("bpm");
                pulseMetric.setConvertedValues(Arrays.asList(pulseCV));

                ConvertedValue spo2CV = new ConvertedValue();
                spo2CV.setValue(spo2);
                spo2CV.setUnit("%");

                ReadingMetricValue spo2Metric = new ReadingMetricValue();
                spo2Metric.setValue(spo2);
                spo2Metric.setUnit("%");
                spo2Metric.setConvertedValues(Arrays.asList(spo2CV));

                oxygenUpdatedAt.setValue(timeAgo);
                oxygen.setValue(Arrays.asList(pulseMetric, spo2Metric));
                oxygenEventDescription.setValue("");
                break;
            }
            case "temperature": {
                double celsius    = toDouble(rawValues.get(0));
                double fahrenheit = Math.round(((celsius * 9.0 / 5.0) + 32.0) * 10.0) / 10.0;

                ConvertedValue fahrenheitCV = new ConvertedValue();
                fahrenheitCV.setValue(fahrenheit);
                fahrenheitCV.setUnit("°F");

                ConvertedValue celsiusCV = new ConvertedValue();
                celsiusCV.setValue(celsius);
                celsiusCV.setUnit("°C");

                ReadingMetricValue tm = new ReadingMetricValue();
                tm.setValue(celsius);
                tm.setUnit("°C");
                tm.setConvertedValues(Arrays.asList(fahrenheitCV, celsiusCV));

                temperatureUpdatedAt.setValue(timeAgo);
                temperature.setValue(Arrays.asList(tm));
                temperatureEventDescription.setValue("");
                break;
            }
            default:
                Log.d("ViewModel", "unhandled type=" + type);
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
