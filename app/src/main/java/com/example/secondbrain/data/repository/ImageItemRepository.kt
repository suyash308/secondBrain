package com.example.secondbrain.data.repository

import com.example.secondbrain.data.dao.ImageItemDao
import com.example.secondbrain.data.entities.ImageItemEntity
import kotlinx.coroutines.flow.Flow
import java.io.File

class ImageItemRepository(
    private val imageItemDao: ImageItemDao
) {
    
    fun getAllImageItems(): Flow<List<ImageItemEntity>> {
        return imageItemDao.getAllImageItems()
    }
    
    fun searchImageItems(query: String): Flow<List<ImageItemEntity>> {
        return imageItemDao.searchImageItems(query)
    }
    
    suspend fun insertImageItem(imageItem: ImageItemEntity) {
        imageItemDao.insertImageItem(imageItem)
    }
    
    suspend fun updateImageItem(imageItem: ImageItemEntity) {
        imageItemDao.updateImageItem(imageItem)
    }
    
    suspend fun deleteImageItem(imageItem: ImageItemEntity) {
        imageItemDao.deleteImageItem(imageItem)
    }
    
    suspend fun delete(item: ImageItemEntity) {
        val localPath = item.localPath
        if (localPath.isNotEmpty()) {
            val file = File(localPath.removePrefix("file://"))
            if (file.exists()) {
                file.delete()
            }
        }
        imageItemDao.delete(item)
    }

    suspend fun deleteAllImageItems() {
        imageItemDao.deleteAllImageItems()
    }
    
    suspend fun getImageItemCount(): Int {
        return imageItemDao.getImageItemCount()
    }
    
    suspend fun updateExtractedText(id: Long, extractedText: String) {
        imageItemDao.updateExtractedText(id, extractedText)
    }
} 