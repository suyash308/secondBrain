package com.example.secondbrain.data.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import com.example.secondbrain.data.entities.ConversationEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface ConversationDao {

    @Insert
    suspend fun insert(conversation: ConversationEntity): Long

    @Query("SELECT * FROM ConversationEntity ORDER BY createdAt DESC")
    fun getAllConversations(): Flow<List<ConversationEntity>>

    @Query("UPDATE ConversationEntity SET title = :title WHERE id = :id")
    suspend fun updateTitle(id: Int, title: String)

    @Query("DELETE FROM ConversationEntity WHERE id = :id")
    suspend fun deleteById(id: Int)
}
