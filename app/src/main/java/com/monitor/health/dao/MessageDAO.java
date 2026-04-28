package com.monitor.health.dao;

import com.monitor.health.entity.MessageEntity;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.Query;
import androidx.room.Update;
import androidx.room.Delete;
import java.util.List;

@Dao
public interface MessageDAO {

    @Insert
    long insertMessage(MessageEntity message);

    @Insert
    void insertMessages(List<MessageEntity> messages);

    @Update
    void updateMessage(MessageEntity message);

    @Delete
    void deleteMessage(MessageEntity message);

    @Query("SELECT * FROM messages WHERE id = :id")
    MessageEntity getMessageById(long id);

    @Query("SELECT * FROM messages WHERE api_id = :apiId")
    MessageEntity getMessageByApiId(String apiId);

    @Query("SELECT * FROM messages WHERE recipient_id = :recipientId ORDER BY message_date DESC")
    List<MessageEntity> getMessagesByRecipient(String recipientId);

    @Query("SELECT * FROM messages WHERE is_read = 0 ORDER BY message_date DESC")
    List<MessageEntity> getUnreadMessages();

    @Query("SELECT * FROM messages WHERE sender_id = :senderId ORDER BY message_date DESC")
    List<MessageEntity> getMessagesBySender(String senderId);

    @Query("SELECT * FROM messages ORDER BY message_date DESC")
    List<MessageEntity> getAllMessages();

    @Query("SELECT * FROM messages WHERE is_system_notification = 1 ORDER BY message_date DESC")
    List<MessageEntity> getSystemNotifications();

    @Query("DELETE FROM messages WHERE id = :id")
    void deleteMessageById(long id);

    @Query("DELETE FROM messages")
    void deleteAllMessages();

    @Query("SELECT COUNT(*) FROM messages WHERE is_read = 0")
    int getUnreadCount();

    @Query("SELECT COUNT(*) FROM messages")
    int getCount();
}