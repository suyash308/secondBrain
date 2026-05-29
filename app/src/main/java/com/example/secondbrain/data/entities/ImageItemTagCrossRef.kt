package com.example.secondbrain.data.entities

import androidx.room.Entity

@Entity(primaryKeys = ["imageItemId", "tagId"])
data class ImageItemTagCrossRef(
    val imageItemId: Long,
    val tagId: Int
)
