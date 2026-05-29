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
    suspend fun insertImageItem(imageItem: ImageItemEntity): Long
    
    @Update
    suspend fun updateImageItem(imageItem: ImageItemEntity)
    
    @Delete
    suspend fun deleteImageItem(imageItem: ImageItemEntity)

    @Delete
    suspend fun delete(item: ImageItemEntity)
    
    @Query("DELETE FROM image_items")
    suspend fun deleteAllImageItems()
    
    @Query("SELECT COUNT(*) FROM image_items")
    suspend fun getImageItemCount(): Int
    
    @Query("UPDATE image_items SET extractedText = :extractedText WHERE id = :id")
    suspend fun updateExtractedText(id: Long, extractedText: String)

    @Query("UPDATE image_items SET extractedText = :extractedText WHERE originalUri = :uri OR localPath = :uri")
    suspend fun updateExtractedTextByUri(uri: String, extractedText: String)

    @Query("UPDATE image_items SET embedding = :embedding WHERE id = :id")
    suspend fun updateEmbedding(id: Long, embedding: String)

    @Query("SELECT id, embedding FROM image_items WHERE embedding IS NOT NULL")
    suspend fun getAllEmbeddings(): List<ItemEmbeddingProjection>

    @Query("SELECT id FROM image_items WHERE embedding IS NULL")
    suspend fun getItemsWithoutEmbedding(): List<ItemWithoutEmbedding>
} 