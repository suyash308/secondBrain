package com.example.secondbrain.data.entities

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity
data class ChatMessageEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val role: String,           // "user" or "assistant"
    val content: String,
    val timestamp: Long,
    val sourceIds: String? = null,    // JSON array of Long item IDs
    val sourceTypes: String? = null,  // JSON array of String content types
    val conversationId: Int = 0
)
