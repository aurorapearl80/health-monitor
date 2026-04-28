package com.monitor.health;

import com.monitor.health.dao.FileResponseDto;
import com.monitor.health.dto.ApiResponseDTO;
import com.monitor.health.model.MedicationSchedule;
import com.monitor.health.model.MedicinePayload;
import com.monitor.health.model.healthscore.DataObjectDto;
import com.monitor.health.request.SendAlarmRequest;
import com.monitor.health.request.UserTelemedikRequest;
import com.monitor.health.request.bledevice.AssignBleRequest;
import com.monitor.health.response.FrequencyResponse;
import com.monitor.health.response.LoginAlarmReponse;
import com.monitor.health.response.UserTelemedikResponse;
import com.monitor.health.response.alldata.Root;
import com.monitor.health.response.bledevice.DeviceResponse;
import com.monitor.health.response.bledevice.DeviceResponseList;
import com.monitor.health.response.glocuse.GlucoseServerResponse;
import com.monitor.health.response.readinghistory.ReadingHistoryResponse;
import com.monitor.health.response.user.UserProfileResponse;

import okhttp3.RequestBody;
import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.DELETE;
import retrofit2.http.GET;
import retrofit2.http.Multipart;
import retrofit2.http.POST;
import retrofit2.http.Part;
import retrofit2.http.Path;
import retrofit2.http.Query;

public interface UserService {


    @POST("api/login/")
    Call<LoginResponse> userLogin(@Body LoginRequest loginRequest);

    @POST("api/doctor-watches/readings")
    Call<Object> sendReadings(@Body ReadingsRequest readingsRequest);

    @GET("api/doctor-watches/medicine-schedules")
    Call<MedicationSchedule> getMedicineSchedule();

    @POST("api/doctor-watches/medicine-taken")
    Call<Object> saveMedicineSchedule(@Body MedicinePayload loginRequest);

    @GET("api/doctor-watches/health-score-analysis")
    Call<DataObjectDto> getHealthScore(@Query("month") int month,
                                       @Query("year") int year);

    //Send alarm
    @POST("/api/doctor-watches/third-party/telemedik/create-update")
    Call<UserTelemedikResponse> createUpdate(@Body UserTelemedikRequest userTelemedikRequest);

    @POST("/api/doctor-watches/third-party/telemedik/send-alarm")
    Call<Object> sendAlarm(@Body SendAlarmRequest sendAlarmRequest);

    @Multipart
    @POST("SDK_004_LOGIN_GET_TOKEN.php")
    Call<LoginAlarmReponse> loginGetToken(@Part("deviceUuid") RequestBody deviceUuid,
                                           @Part("sdkToken") RequestBody sdkToken,
                                           @Part("uuidToken") RequestBody uuidToken);


    @GET("api/doctor-watches/build-assets/latest")
    Call<FileResponseDto> getFileDownload();

    @GET("api/doctor-watches/frequency-settings")
    Call<FrequencyResponse> getFrequencySettings();

    @GET("api/doctor-watches/reading-history")
    Call<ReadingHistoryResponse> getReadingHistory(@Query("metric") String typeId,
                                                   @Query("start_date") String startDate,
                                                   @Query("end_date") String endDate,
                                                   @Query("per_page") int perPage,
                                                   @Query("page") int page);

    //glucose
    @GET("api/doctor-watches/readings")
    Call<GlucoseServerResponse> getReadings(@Query("reading_type_id") String typeId,
                                            @Query("start_date") String startDate,
                                            @Query("end_date") String endDate,
                                            @Query("per_page") int parPage,
                                            @Query("page") int page);

    //api/doctor-watches/readings/latest
    //Request History
    //@GET("api/doctor-watches/readings/latest?isTest=1")
    @GET("api/doctor-watches/readings/latest")
    Call<Root> getReadingsLatest();


    @GET("api/doctor-watches/readings")
    Call<Root> getReadingHistories(@Query("reading_type_id") String typeId,
                                            @Query("start_date") String startDate,
                                            @Query("end_date") String endDate,
                                            @Query("per_page") String parPage,
                                            @Query("page") int page,
                                            @Query("sort") String sort);


    @GET("api/doctor-watches/user-profile")
    Call<UserProfileResponse> getUserProfile();



    //    uuidToken:c4e471c9-64a8-4cb7-bc57-01676752ab09
//    sdkToken:a42849c99984b6e11d1226864e968fdad6d5c79c247119cd41b400dab03c4747
//    deviceUuid:24e11e9f544abaf3
//    name:testa
//    surname:testa
//    userEmail:test1001@mailinator.com.com
//    phone:+15557777888
//    model:SM-G925I
//    maker:Samsung
//    os:4
//    country:PR
    @Multipart
    @POST("SDK_003_CREATE_UPDATE_USER.php")
    Call<Object> registerDevice(
            @Part("uuidToken") RequestBody uuidToken,
            @Part("sdkToken") RequestBody sdkToken,
            @Part("deviceUuid") RequestBody deviceUuid,
            @Part("name") RequestBody name,
            @Part("surname") RequestBody surname,
            @Part("userEmail") RequestBody userEmail,
            @Part("phone") RequestBody phone,
            @Part("model") RequestBody model,
            @Part("maker") RequestBody maker,
            @Part("os") RequestBody os,
            @Part("country") RequestBody country
    );

    //    sessionToken:E46391406028240e9ed6c3749ab7123f481805a1ff261b6a83134c772eee680b
//    latitude:18.421566
//    longitude:-66.073031
//    idDev:12349876
//    type:2
//    precision:1
//    batt:50
//    cardio:0
//
//    //SEnd alarm
    @Multipart
    @POST("DRS_020_SEND_ALARM.php")
    Call<Object> sendAlarm(
            @Part("sessionToken") RequestBody sessionToken,
            @Part("latitude") RequestBody latitude,
            @Part("longitude") RequestBody longitude,
            @Part("idDev") RequestBody idDev,
            @Part("type") RequestBody type,
            @Part("precision") RequestBody precicion,
            @Part("batt") RequestBody batt,
            @Part("cardio") RequestBody cardio
    );

    @POST("api/doctor-watches/patient-devices")
    Call<DeviceResponse> assignBleDevice(@Body AssignBleRequest request);

    @DELETE("api/doctor-watches/patient-devices/{id}")
    Call<Void> deleteBleDevice(@Path("id") String id);


    @GET("api/doctor-watches/patient-devices")
    Call<DeviceResponseList> getAssignBleDevices();


    @GET("api/doctor-watches/patient-inbox")
    Call<ApiResponseDTO> getInboxMessage();

    @GET("api/doctor-watches/patient-inbox")
    Call<ApiResponseDTO> getInboxMessage(@Query("page") int page,
                                         @Query("per_page") int perPage,
                                         @Query("isRead") Boolean isRead);



}

