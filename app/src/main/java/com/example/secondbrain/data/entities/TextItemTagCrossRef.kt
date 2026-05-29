package com.example.secondbrain.data.entities

import androidx.room.Entity

@Entity(primaryKeys = ["textItemId", "tagId"])
data class TextItemTagCrossRef(
    val textItemId: Long,
    val tagId: Int
)
