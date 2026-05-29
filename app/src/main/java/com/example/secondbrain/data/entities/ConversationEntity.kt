package com.example.secondbrain.data.entities

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity
data class ConversationEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val title: String,
    val createdAt: Long
)
