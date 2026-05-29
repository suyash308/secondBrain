package com.example.secondbrain.data.entities

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "image_items")
data class ImageItemEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val originalUri: String,
    val localPath: String,
    val extractedText: String?,
    val timestamp: Long,
    val embedding: String? = null
) 