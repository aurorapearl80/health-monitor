package com.monitor.health.ui;

import android.os.Bundle;
import android.util.Log;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.gson.Gson;
import com.monitor.health.ApiClient;
import com.monitor.health.R;
import com.monitor.health.chat.ChatSocketManager;
import com.monitor.health.chat.dto.ChatMessageDTO;
import com.monitor.health.chat.dto.ChatSendRequestDTO;
import com.monitor.health.chat.dto.ChatSendResponseDTO;
import com.monitor.health.ui.service.MessageService;
import com.monitor.health.utility.DeviceUtils;

import org.json.JSONObject;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

/** Sends a text message to the patient's care team via POST /api/doctor-watches/messages. */
public class ComposeMessageActivity extends AppCompatActivity {
    private static final String TAG = "ComposeMessageActivity";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_compose_message);

        EditText edtMessage = findViewById(R.id.edt_message);
        Button btnSend = findViewById(R.id.btn_send);
        ProgressBar progressSend = findViewById(R.id.progress_send);

        String watchSerial = DeviceUtils.resolveWatchSerial(this);

        btnSend.setOnClickListener(v -> {
            String msg = edtMessage.getText().toString().trim();
            if (msg.isEmpty()) {
                Toast.makeText(this, "Please enter a message.", Toast.LENGTH_SHORT).show();
                return;
            }

            btnSend.setEnabled(false);
            if (progressSend != null) progressSend.setVisibility(android.view.View.VISIBLE);

            ApiClient.getChatService().sendMessage(new ChatSendRequestDTO(watchSerial, msg))
                    .enqueue(new Callback<ChatSendResponseDTO>() {
                        @Override
                        public void onResponse(Call<ChatSendResponseDTO> call, Response<ChatSendResponseDTO> response) {
                            btnSend.setEnabled(true);
                            if (progressSend != null) progressSend.setVisibility(android.view.View.GONE);

                            if (response.isSuccessful() && response.body() != null && response.body().getData() != null) {
                                ChatMessageDTO sent = response.body().getData();
                                new Thread(() -> new MessageService(ComposeMessageActivity.this).saveMessage(sent)).start();
                                pushLive(sent);
                                Toast.makeText(ComposeMessageActivity.this, "Message sent!", Toast.LENGTH_SHORT).show();
                                finish();
                            } else if (response.code() == 422) {
                                Log.w(TAG, "Send rejected: this watch isn't linked to a patient account yet.");
                                Toast.makeText(ComposeMessageActivity.this, "This watch isn't linked to a patient account yet.", Toast.LENGTH_LONG).show();
                            } else {
                                Log.e(TAG, "Send failed: HTTP " + response.code());
                                Toast.makeText(ComposeMessageActivity.this, "Failed to send message.", Toast.LENGTH_SHORT).show();
                            }
                        }

                        @Override
                        public void onFailure(Call<ChatSendResponseDTO> call, Throwable t) {
                            btnSend.setEnabled(true);
                            if (progressSend != null) progressSend.setVisibility(android.view.View.GONE);
                            Log.e(TAG, "Send network error", t);
                            Toast.makeText(ComposeMessageActivity.this, "Connection error. Try again.", Toast.LENGTH_SHORT).show();
                        }
                    });
        });
    }

    /** Best-effort live push over the socketio relay, per CHAT_API.md — REST history already has it. */
    private void pushLive(ChatMessageDTO sent) {
        try {
            JSONObject json = new JSONObject(new Gson().toJson(sent));
            ChatSocketManager.getInstance().emitPrivateMessage(sent.getRecipientId(), json);
        } catch (Exception e) {
            Log.w(TAG, "Failed to push sent message live", e);
        }
    }
}
