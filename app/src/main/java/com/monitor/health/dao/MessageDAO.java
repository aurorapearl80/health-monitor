package com.monitor.health.dao;

import com.monitor.health.entity.MessageEntity;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;
import androidx.room.Update;
import androidx.room.Delete;
import java.util.List;

@Dao
public interface MessageDAO {

    // REPLACE-on-conflict against the unique api_id index is what makes re-syncing the
    // same page idempotent — without it, every fetch inserted duplicate rows.
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    long insertMessage(MessageEntity message);

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insertMessages(List<MessageEntity> messages);

    @Update
    void updateMessage(MessageEntity message);

    @Delete
    void deleteMessage(MessageEntity message);

    @Query("SELECT * FROM messages WHERE id = :id")
    MessageEntity getMessageById(long id);

    @Query("SELECT * FROM messages WHERE api_id = :apiId")
    MessageEntity getMessageByApiId(long apiId);

    @Query("SELECT * FROM messages WHERE is_read = 0 ORDER BY created_at DESC")
    List<MessageEntity> getUnreadMessages();

    @Query("SELECT * FROM messages ORDER BY created_at DESC")
    List<MessageEntity> getAllMessages();

    @Query("DELETE FROM messages WHERE id = :id")
    void deleteMessageById(long id);

    @Query("DELETE FROM messages")
    void deleteAllMessages();

    @Query("SELECT COUNT(*) FROM messages WHERE is_read = 0")
    int getUnreadCount();

    @Query("SELECT COUNT(*) FROM messages")
    int getCount();

    @Query("UPDATE messages SET is_read = 1, read_at = :readAt WHERE api_id = :apiId")
    void markReadByApiId(long apiId, long readAt);
}
