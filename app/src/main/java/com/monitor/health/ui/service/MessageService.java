package com.monitor.health.ui.service;

import android.content.Context;
import android.util.Log;

import com.monitor.health.chat.dto.ChatMessageDTO;
import com.monitor.health.dao.MessageDAO;
import com.monitor.health.database.DatabaseClient;
import com.monitor.health.entity.MessageEntity;

import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;

/** Persists patient-monitoring-web chat messages (chat/dto/ChatMessageDTO) into the local Room cache. */
public class MessageService {
    private static final String TAG = "MessageService";
    private final MessageDAO messageDAO;

    public MessageService(Context context) {
        DatabaseClient database = DatabaseClient.getInstance(context);
        this.messageDAO = database.getAppDatabase().messageDAO();
    }

    /** Upsert a fetched page. Safe to call repeatedly — REPLACE-on-conflict on api_id dedups. */
    public void saveMessages(List<ChatMessageDTO> messages) {
        if (messages == null || messages.isEmpty()) {
            Log.d(TAG, "No messages to save");
            return;
        }
        List<MessageEntity> entities = new ArrayList<>();
        for (ChatMessageDTO dto : messages) {
            try {
                entities.add(toEntity(dto));
            } catch (Exception e) {
                Log.e(TAG, "Error converting message dto id=" + dto.getId(), e);
            }
        }
        messageDAO.insertMessages(entities);
        Log.d(TAG, "Upserted " + entities.size() + " messages");
    }

    public void saveMessage(ChatMessageDTO dto) {
        if (dto == null) return;
        messageDAO.insertMessage(toEntity(dto));
    }

    /** Optimistically flip a message read locally, ahead of the next full re-sync. */
    public void markReadLocally(long apiId) {
        messageDAO.markReadByApiId(apiId, System.currentTimeMillis());
    }

    private MessageEntity toEntity(ChatMessageDTO dto) {
        return new MessageEntity(
                dto.getId(),
                dto.getBody(),
                dto.isRead(),
                dto.isMine(),
                dto.getSenderId(),
                dto.getSender() != null ? dto.getSender().getName() : null,
                dto.getRecipientId(),
                dto.getPatientId(),
                dto.getChannelId(),
                dto.getAttachment() != null ? dto.getAttachment().getUrl() : null,
                dto.getAttachment() != null ? dto.getAttachment().getName() : null,
                dto.getSender() != null ? dto.getSender().getProfileImageUrl() : null,
                dto.getRecipient() != null ? dto.getRecipient().getProfileImageUrl() : null,
                parseIso8601ToMillisOrNull(dto.getReadAt()),
                parseIso8601ToMillis(dto.getCreatedAt())
        );
    }

    private Long parseIso8601ToMillisOrNull(String iso8601String) {
        if (iso8601String == null || iso8601String.isEmpty()) return null;
        return parseIso8601ToMillis(iso8601String);
    }

    private long parseIso8601ToMillis(String iso8601String) {
        if (iso8601String == null || iso8601String.isEmpty()) {
            return System.currentTimeMillis();
        }
        try {
            // Handles both "...Z" and "...+00:00" offset suffixes.
            String cleanedDate = iso8601String.replace("Z", "+00:00");
            OffsetDateTime offsetDateTime = OffsetDateTime.parse(cleanedDate);
            return offsetDateTime.toInstant().toEpochMilli();
        } catch (Exception e) {
            Log.e(TAG, "Error parsing date: " + iso8601String, e);
            return System.currentTimeMillis();
        }
    }

    public List<MessageEntity> getAllMessages() {
        return messageDAO.getAllMessages();
    }

    public int getUnreadCount() {
        return messageDAO.getUnreadCount();
    }

    public void updateMessage(MessageEntity message) {
        messageDAO.updateMessage(message);
    }

    public void deleteMessage(long id) {
        messageDAO.deleteMessageById(id);
    }

    public void deleteAllMessages() {
        messageDAO.deleteAllMessages();
    }
}
