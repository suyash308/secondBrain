package com.example.secondbrain.data.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import com.example.secondbrain.data.entities.ChatMessageEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface ChatMessageDao {

    @Insert
    suspend fun insert(message: ChatMessageEntity)

    // Per-conversation queries
    @Query("SELECT * FROM ChatMessageEntity WHERE conversationId = :conversationId ORDER BY timestamp ASC")
    fun getMessages(conversationId: Int): Flow<List<ChatMessageEntity>>

    @Query("SELECT * FROM ChatMessageEntity WHERE conversationId = :conversationId ORDER BY timestamp DESC LIMIT :limit")
    suspend fun getRecentMessages(conversationId: Int, limit: Int): List<ChatMessageEntity>

    @Query("DELETE FROM ChatMessageEntity WHERE conversationId = :conversationId")
    suspend fun deleteByConversation(conversationId: Int)

    // Legacy — kept for backward compat
    @Query("SELECT * FROM ChatMessageEntity ORDER BY timestamp ASC")
    fun getAllMessages(): Flow<List<ChatMessageEntity>>

    @Query("DELETE FROM ChatMessageEntity")
    suspend fun deleteAll()
}
