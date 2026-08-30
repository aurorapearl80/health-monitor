package com.monitor.health.chat;

import android.util.Log;

import com.monitor.health.Constant;

import org.json.JSONException;
import org.json.JSONObject;

import java.net.URISyntaxException;

import io.socket.client.IO;
import io.socket.client.Socket;
import io.socket.emitter.Emitter;

/**
 * Wraps the socketio relay's chat events (see patient-monitoring-web/CHAT_API.md). The relay has
 * no rooms and no auth of its own — every connected client receives every broadcast, so inbound
 * events must be filtered by recipient_id here before being treated as "addressed to me".
 * REST history (ChatService) is always the source of truth; this is delivery-only.
 */
public class ChatSocketManager {

    private static final String TAG = "ChatSocketManager";
    private static ChatSocketManager instance;

    private Socket socket;
    private long myUserId = -1;
    private IncomingMessageListener listener;

    public interface IncomingMessageListener {
        void onPrivateMessage(JSONObject message);
    }

    private ChatSocketManager() {}

    public static synchronized ChatSocketManager getInstance() {
        if (instance == null) {
            instance = new ChatSocketManager();
        }
        return instance;
    }

    public void setListener(IncomingMessageListener listener) {
        this.listener = listener;
    }

    public synchronized void connect(long userId) {
        this.myUserId = userId;
        if (socket != null && socket.connected()) {
            return;
        }
        try {
            if (socket == null) {
                socket = IO.socket(Constant.BASE_URL);
                socket.on(Socket.EVENT_CONNECT, onConnect);
                socket.on("private_message", onPrivateMessage);
            }
            socket.connect();
        } catch (URISyntaxException e) {
            Log.e(TAG, "Invalid socket URL: " + Constant.BASE_URL, e);
        }
    }

    public synchronized void disconnect() {
        if (socket != null) {
            socket.disconnect();
        }
    }

    /** Best-effort — if not connected, the recipient still gets the message via REST history. */
    public void emitPrivateMessage(long recipientId, JSONObject messageJson) {
        if (socket == null || !socket.connected()) {
            Log.d(TAG, "Socket not connected, skipping live push (REST history still has it)");
            return;
        }
        try {
            JSONObject payload = new JSONObject();
            payload.put("to", recipientId);
            payload.put("message", messageJson);
            socket.emit("private_message", payload);
        } catch (JSONException e) {
            Log.e(TAG, "Failed to build private_message payload", e);
        }
    }

    private final Emitter.Listener onConnect = args -> {
        Log.d(TAG, "Socket connected, registering user " + myUserId);
        if (myUserId > 0) {
            socket.emit("register", (int) myUserId);
        }
    };

    private final Emitter.Listener onPrivateMessage = args -> {
        if (args.length == 0 || !(args[0] instanceof JSONObject)) return;
        JSONObject payload = (JSONObject) args[0];
        JSONObject message = payload.optJSONObject("message");
        if (message == null) return;

        long recipientId = message.optLong("recipient_id", -1);
        if (recipientId != myUserId) {
            return;
        }
        if (listener != null) {
            listener.onPrivateMessage(message);
        }
    };
}
