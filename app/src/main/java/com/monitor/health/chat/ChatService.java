package com.monitor.health.chat;

import com.monitor.health.chat.dto.ChatMessagePageDTO;
import com.monitor.health.chat.dto.ChatSendRequestDTO;
import com.monitor.health.chat.dto.ChatSendResponseDTO;
import com.monitor.health.chat.dto.ChatSerialRequestDTO;

import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.GET;
import retrofit2.http.POST;
import retrofit2.http.Path;
import retrofit2.http.Query;

/**
 * patient-monitoring-web's serial-only doctor-watch chat API (DoctorWatchMessageController).
 * No auth headers — the watch's serial (see DeviceUtils.resolveWatchSerial) IS the credential,
 * matching the same convention as POST /api/doctor-watches/readings.
 */
public interface ChatService {

    @GET("api/doctor-watches/messages")
    Call<ChatMessagePageDTO> getMessages(@Query("serial") String serial, @Query("page") int page, @Query("per_page") int perPage);

    @POST("api/doctor-watches/messages")
    Call<ChatSendResponseDTO> sendMessage(@Body ChatSendRequestDTO request);

    @POST("api/doctor-watches/messages/{id}/read")
    Call<Void> markRead(@Path("id") long id, @Body ChatSerialRequestDTO request);
}
