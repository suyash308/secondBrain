package com.example.secondbrain.data.mapper

import com.example.secondbrain.MainActivity
import com.example.secondbrain.data.entities.TextItemEntity
import com.example.secondbrain.data.entities.ImageItemEntity
import com.example.secondbrain.data.entities.LinkItemEntity

object DataMapper {
    
    // TextItem conversions
    fun toTextItemEntity(textItem: MainActivity.TextItem): TextItemEntity {
        return TextItemEntity(
            content = textItem.content ?: "",
            timestamp = textItem.timestamp
        )
    }
    
    fun toTextItem(textItemEntity: TextItemEntity): MainActivity.TextItem {
        return MainActivity.TextItem(
            content = textItemEntity.content,
            timestamp = textItemEntity.timestamp
        )
    }
    
    // ImageItem conversions
    fun toImageItemEntity(imageItem: MainActivity.ImageItem): ImageItemEntity {
        return ImageItemEntity(
            originalUri = imageItem.originalUri ?: "",
            localPath = imageItem.localPath ?: "",
            extractedText = imageItem.extractedText,
            timestamp = imageItem.timestamp
        )
    }
    
    fun toImageItem(imageItemEntity: ImageItemEntity): MainActivity.ImageItem {
        return MainActivity.ImageItem(
            originalUri = imageItemEntity.originalUri,
            localPath = imageItemEntity.localPath,
            extractedText = imageItemEntity.extractedText,
            timestamp = imageItemEntity.timestamp
        )
    }
    
    // LinkItem conversions
    fun toLinkItemEntity(linkItem: MainActivity.LinkItem): LinkItemEntity {
        return LinkItemEntity(
            url = linkItem.url ?: "",
            title = linkItem.title,
            description = linkItem.description,
            imageUrl = linkItem.imageUrl,
            timestamp = linkItem.timestamp
        )
    }
    
    fun toLinkItem(linkItemEntity: LinkItemEntity): MainActivity.LinkItem {
        return MainActivity.LinkItem(
            url = linkItemEntity.url,
            title = linkItemEntity.title,
            description = linkItemEntity.description,
            imageUrl = linkItemEntity.imageUrl,
            timestamp = linkItemEntity.timestamp
        )
    }
} 