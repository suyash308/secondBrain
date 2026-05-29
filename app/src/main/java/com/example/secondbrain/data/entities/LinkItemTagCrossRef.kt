package com.example.secondbrain.data.entities

import androidx.room.Entity

@Entity(primaryKeys = ["linkItemId", "tagId"])
data class LinkItemTagCrossRef(
    val linkItemId: Long,
    val tagId: Int
)
