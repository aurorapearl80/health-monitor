package com.monitor.health.viewmodel;

import android.app.Application;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.monitor.health.ApiClient;
import com.monitor.health.Constant;
import com.monitor.health.database.DatabaseClient;
import com.monitor.health.model.BleDeviceModel;
import com.monitor.health.model.healthscore.UserDrWatch;
import com.monitor.health.response.ble.BleUserData;
import com.monitor.health.response.ble.BleUserProfileResponse;
import com.monitor.health.utility.PreferenceHelper;

import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class ProfileViewModel extends AndroidViewModel {

    private static final String TAG = "ProfileViewModel";

    private final MutableLiveData<UserDrWatch> profileData = new MutableLiveData<>();
    private final MutableLiveData<Boolean> loading = new MutableLiveData<>(false);
    private final MutableLiveData<String> error = new MutableLiveData<>();

    private final DatabaseClient databaseClient;
    private final ExecutorService executor = Executors.newSingleThreadExecutor();

    public ProfileViewModel(@NonNull Application application) {
        super(application);
        databaseClient = DatabaseClient.getInstance(application);
    }

    public LiveData<UserDrWatch> getProfileData() { return profileData; }
    public LiveData<Boolean> getLoading() { return loading; }
    public LiveData<String> getError() { return error; }

    /** Load cached profile from Room DB immediately (shows stale data while API refreshes). */
    public void loadCachedProfile() {
        executor.execute(() -> {
            List<UserDrWatch> list = databaseClient.getAppDatabase().userDrWatchDao().getAllDrWatch();
            if (list != null && !list.isEmpty()) {
                profileData.postValue(list.get(0));
            }
        });
    }

    /**
     * Fetch user profile from the BLE device endpoint.
     * If serial is null/empty, falls back to the first stored BLE device serial.
     */
    public void fetchBleDeviceUserProfile(String serial, String imei) {
        loading.postValue(true);

        if (serial == null || serial.isEmpty()) {
            executor.execute(() -> {
                List<BleDeviceModel> devices = databaseClient.getAppDatabase().bleDeviceDao().getAllBleDevices();
                String resolvedSerial = (!devices.isEmpty() && devices.get(0).getSerial() != null)
                        ? devices.get(0).getSerial()
                        : imei; // fallback: use Android device ID as serial
                callApi(resolvedSerial, imei);
            });
        } else {
            callApi(serial, imei);
        }
    }

    private void callApi(String serial, String imei) {
        String token = PreferenceHelper.getInstance(getApplication())
                .getString(Constant.AUTH_TOKEN, "");

        // API requires uppercase serial (DB stores lowercase for BLE matching)
        String upperSerial = (serial != null) ? serial.toUpperCase() : "";
        Log.d(TAG, "Calling profile API — serial=" + upperSerial
                + "  token_present=" + !token.isEmpty());

        Call<BleUserProfileResponse> call = ApiClient
                .getUserService(Constant.BASE_URL, token, imei)
                .getBleDeviceUserProfile(upperSerial);

        call.enqueue(new Callback<BleUserProfileResponse>() {
            @Override
            public void onResponse(@NonNull Call<BleUserProfileResponse> call,
                                   @NonNull Response<BleUserProfileResponse> response) {
                loading.postValue(false);
                if (response.isSuccessful() && response.body() != null
                        && response.body().getUser() != null) {
                    Log.d(TAG, "Profile API success — user=" + response.body().getUser().getFullName());
                    saveAndPost(response.body().getUser());
                } else {
                    String errBody = "";
                    try { if (response.errorBody() != null) errBody = response.errorBody().string(); } catch (Exception ignored) {}
                    Log.e(TAG, "Profile fetch failed: HTTP " + response.code() + " body=" + errBody);
                    error.postValue("HTTP " + response.code() + ": " + errBody);
                    loadCachedProfile();
                }
            }

            @Override
            public void onFailure(@NonNull Call<BleUserProfileResponse> call,
                                  @NonNull Throwable t) {
                loading.postValue(false);
                Log.e(TAG, "Profile fetch network error: " + t.getMessage(), t);
                error.postValue("Network error: " + t.getMessage());
                loadCachedProfile();
            }
        });
    }

    private void saveAndPost(BleUserData user) {
        executor.execute(() -> {
            UserDrWatch entity = mapToEntity(user);
            databaseClient.getAppDatabase().userDrWatchDao().clearAllUserDrWatch();
            databaseClient.getAppDatabase().userDrWatchDao().insertUserDrWatch(entity);

            // Persist measurement units globally so every screen can read them
            PreferenceHelper.getInstance(getApplication())
                    .saveMeasurementUnits(user.getMeasurementUnits());

            profileData.postValue(entity);
        });
    }

    private UserDrWatch mapToEntity(BleUserData user) {
        UserDrWatch entity = new UserDrWatch();
        entity.set_id(String.valueOf(user.getId()));
        entity.setFullname(user.getFullName());
        entity.setUsername(user.getUsername());
        entity.setEmail(user.getEmail());
        entity.setPhone(user.getPhone());
        entity.setGender(user.getGender());
        entity.setOrganization(user.getOrganization());
        entity.setSubOrganization(user.getSubOrganization());
        entity.setMemberId(user.getMemberId());
        entity.setBday(user.getDateOfBirth());
        entity.setCreatedAt(user.getCreatedAt());
        entity.setActive("active".equalsIgnoreCase(user.getStatus()));
        entity.setPatient_conditions(
                user.getConditions() != null ? String.join(", ", user.getConditions()) : "");
        entity.setPractitioners(
                user.getPractitioners() != null ? String.join(", ", user.getPractitioners()) : "");

        // New fields
        entity.setFirstName(user.getFirstName());
        entity.setLastName(user.getLastName());
        entity.setState(user.getState());
        entity.setCountry(user.getCountry());
        entity.setZipCode(user.getZipCode());
        entity.setCompleteAddress(user.getCompleteAddress());
        entity.setHeight(user.getHeight());
        entity.setWeight(user.getWeight());
        entity.setProfileImageUrl(user.getProfileImageUrl());
        entity.setStatus(user.getStatus());
        entity.setGeneralPractitioner(user.getGeneralPractitioner());
        entity.setPrimaryInsuranceName(user.getPrimaryInsuranceName());
        entity.setHomeNumber(user.getHomeNumber());
        entity.setAngelSupport(user.getAngelSupport());
        return entity;
    }

    @Override
    protected void onCleared() {
        super.onCleared();
        executor.shutdown();
    }
}
