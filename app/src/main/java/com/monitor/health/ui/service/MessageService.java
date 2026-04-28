package com.monitor.health.ui.service;

import android.content.Context;
import android.util.Log;

import com.monitor.health.dao.MessageDAO;
import com.monitor.health.database.DatabaseClient;
import com.monitor.health.dto.ApiResponseDTO;
import com.monitor.health.dto.MessageDTO;
import com.monitor.health.entity.MessageEntity;

import java.util.ArrayList;
import java.util.List;

public class MessageService {
    private static final String TAG = "MessageService";
    private MessageDAO messageDAO;

    public MessageService(Context context) {
        DatabaseClient database = DatabaseClient.getInstance(context);
        this.messageDAO = database.getAppDatabase().messageDAO();
    }

    /**
     * Save API messages to database with deduplication
     * Only inserts new messages, skips duplicates based on apiId
     */
    public void saveApiMessages(ApiResponseDTO apiResponse) {
        Log.w(TAG, "API response is null yawa"+apiResponse.toString());
        if (apiResponse == null) {
            Log.w(TAG, "API response is null");
            return;
        }
        if (apiResponse.getData() == null) {
            Log.w(TAG, "API response data is null");
            return;
        }

        int payloadSize = apiResponse.getData().size();
        Log.d(TAG, "[Save] Incoming messages count=" + payloadSize);
        if (payloadSize == 0) {
            Log.w(TAG, "[Save] Empty data list from API â€” nothing to persist");
        }

        List<MessageEntity> newMessages = new ArrayList<>();
        int duplicateCount = 0;
        int inspected = 0;

        for (MessageDTO dto : apiResponse.getData()) {
            Log.w(TAG, "API response is null body: "+dto.getBody());
            try {


                MessageEntity message = convertDTOToEntity(dto);
                newMessages.add(message);
                Log.w(TAG, "API response is null body: "+message.getBody());
                // Normalize id to String to match entity/DAO contract
//                String apiId = dto.getId();
//                MessageEntity existingMessage = messageDAO.getMessageByApiId(apiId);

//                if (existingMessage == null) {
//                    MessageEntity message = convertDTOToEntity(dto);
//                    newMessages.add(message);
//                    if (inspected < 5) {
//                        Log.d(TAG, "[Save] NEW id=" + apiId);
//                    }
//                } else {
//                    duplicateCount++;
//                    if (inspected < 5) {
//                        Log.d(TAG, "[Save] DUP id=" + apiId);
//                    }
//                }
//                inspected++;
            } catch (Exception e) {
                Log.e(TAG, "Error processing DTO: " + (dto != null ? dto.getId() : "<null>") , e);
            }
        }
        Log.w(TAG, "[Save] Empty data to be save "+newMessages.toString());
        messageDAO.insertMessages(newMessages);

//        if (!newMessages.isEmpty()) {
//            try {
//                messageDAO.insertMessages(newMessages);
//                Log.d(TAG, "âœ… Saved " + newMessages.size() + " new messages (Skipped " + duplicateCount + " duplicates)");
//            } catch (Exception e) {
//                Log.e(TAG, "âŒ Insert failed for new messages batch of size " + newMessages.size(), e);
//            }
//        } else {
//            Log.w(TAG, "No new messages to save. All " + duplicateCount + " messages are duplicates.");
//        }
    }

    /**
     * Alternative: Clear all messages and insert fresh data
     * Use this if you want a complete refresh
     */
    public void saveApiMessagesWithClearAll(ApiResponseDTO apiResponse) {
        if (apiResponse == null || apiResponse.getData() == null) {
            Log.w(TAG, "API response or data is null");
            return;
        }

        try {
            // Clear all existing messages
            messageDAO.deleteAllMessages();
            Log.d(TAG, "ðŸ—‘ï¸  Cleared all messages from database");

            List<MessageEntity> messages = new ArrayList<>();
            for (MessageDTO dto : apiResponse.getData()) {
                try {
                    MessageEntity message = convertDTOToEntity(dto);
                    messages.add(message);
                } catch (Exception e) {
                    Log.e(TAG, "Error converting DTO to entity", e);
                }
            }

            if (!messages.isEmpty()) {
                messageDAO.insertMessages(messages);
                Log.d(TAG, "âœ… Saved " + messages.size() + " fresh messages to database");
            } else {
                Log.w(TAG, "No messages to save");
            }
        } catch (Exception e) {
            Log.e(TAG, "âŒ Error saving API messages", e);
        }
    }

    public void saveSingleMessage(MessageDTO messageDTO) {
        if (messageDTO == null) {
            Log.w(TAG, "MessageDTO is null");
            return;
        }

        try {
            // Check if message already exists
            MessageEntity existingMessage = messageDAO.getMessageByApiId(messageDTO.getId());

            if (existingMessage == null) {
                MessageEntity message = convertDTOToEntity(messageDTO);
                messageDAO.insertMessage(message);
                Log.d(TAG, "âœ… Saved single message to database");
            } else {
                Log.d(TAG, "â­ï¸  Message already exists, skipping: " + messageDTO.getId());
            }
        } catch (Exception e) {
            Log.e(TAG, "âŒ Error saving single message", e);
        }
    }

    private MessageEntity convertDTOToEntity(MessageDTO dto) {
        long messageDateTime = parseIso8601ToMillis(dto.getMessageDate());
        long updatedDateTime = parseIso8601ToMillis(dto.getUpdatedAt());
        long createdDateTime = parseIso8601ToMillis(dto.getCreatedAt());

        return new MessageEntity(
                dto.getId(),
                dto.getSubject(),
                dto.getBody(),
                dto.isRead(),
                dto.getSender() != null ? dto.getSender().getId() : null,
                dto.getSender() != null ? dto.getSender().getFullname() : null,
                dto.getRecepient(),
                dto.getRefModel(),
                messageDateTime,
                dto.isSystemNotification(),
                updatedDateTime,
                createdDateTime
        );
    }

    private long parseIso8601ToMillis(String iso8601String) {
        if (iso8601String == null || iso8601String.isEmpty()) {
            return System.currentTimeMillis();
        }
        try {
            // Parse ISO 8601 format: "2026-03-08T13:02:05.699000Z"
            String cleanedDate = iso8601String.replace("Z", "+00:00");
            java.time.OffsetDateTime offsetDateTime = java.time.OffsetDateTime.parse(cleanedDate);
            return offsetDateTime.toInstant().toEpochMilli();
        } catch (Exception e) {
            Log.e(TAG, "Error parsing date: " + iso8601String, e);
            return System.currentTimeMillis();
        }
    }

    public MessageEntity getMessageById(long id) {
        return messageDAO.getMessageById(id);
    }

    public MessageEntity getMessageByApiId(String apiId) {
        return messageDAO.getMessageByApiId(apiId);
    }

    public List<MessageEntity> getMessagesByRecipient(String recipientId) {
        return messageDAO.getMessagesByRecipient(recipientId);
    }

    public List<MessageEntity> getUnreadMessages() {
        return messageDAO.getUnreadMessages();
    }

    public List<MessageEntity> getMessagesBySender(String senderId) {
        return messageDAO.getMessagesBySender(senderId);
    }

    public List<MessageEntity> getAllMessages() {
        return messageDAO.getAllMessages();
    }

    public List<MessageEntity> getSystemNotifications() {
        return messageDAO.getSystemNotifications();
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