package com.example.secondbrain.data.entities

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "text_items")
data class TextItemEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val content: String,
    val timestamp: Long,
    val embedding: String? = null
) 