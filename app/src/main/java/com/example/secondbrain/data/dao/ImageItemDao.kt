package com.example.secondbrain.data.dao

import androidx.room.*
import com.example.secondbrain.data.entities.ImageItemEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface ImageItemDao {
    
    @Query("SELECT * FROM image_items ORDER BY timestamp DESC")
    fun getAllImageItems(): Flow<List<ImageItemEntity>>
    
    @Query("SELECT * FROM image_items WHERE extractedText LIKE '%' || :query || '%' ORDER BY timestamp DESC")
    fun searchImageItems(query: String): Flow<List<ImageItemEntity>>
    
    @Insert
    suspend fun insertImageItem(imageItem: ImageItemEntity)
    
    @Update
    suspend fun updateImageItem(imageItem: ImageItemEntity)
    
    @Delete
    suspend fun deleteImageItem(imageItem: ImageItemEntity)
    
    @Query("DELETE FROM image_items")
    suspend fun deleteAllImageItems()
    
    @Query("SELECT COUNT(*) FROM image_items")
    suspend fun getImageItemCount(): Int
    
    @Query("UPDATE image_items SET extractedText = :extractedText WHERE id = :id")
    suspend fun updateExtractedText(id: Long, extractedText: String)
} 