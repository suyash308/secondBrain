package com.example.secondbrain.data.dao

import androidx.room.*
import com.example.secondbrain.data.entities.TextItemEntity
import kotlinx.coroutines.flow.Flow

// Shared projections used by all three item DAOs and DatabaseManager
data class ItemEmbeddingProjection(val id: Long, val embedding: String)
data class ItemWithoutEmbedding(val id: Long)

@Dao
interface TextItemDao {
    
    @Query("SELECT * FROM text_items ORDER BY timestamp DESC")
    fun getAllTextItems(): Flow<List<TextItemEntity>>
    
    @Query("SELECT * FROM text_items WHERE content LIKE '%' || :query || '%' ORDER BY timestamp DESC")
    fun searchTextItems(query: String): Flow<List<TextItemEntity>>
    
    @Insert
    suspend fun insertTextItem(textItem: TextItemEntity): Long
    
    @Update
    suspend fun updateTextItem(textItem: TextItemEntity)

    @Update
    suspend fun update(item: TextItemEntity)

    @Delete
    suspend fun deleteTextItem(textItem: TextItemEntity)

    @Delete
    suspend fun delete(item: TextItemEntity)
    
    @Query("DELETE FROM text_items")
    suspend fun deleteAllTextItems()
    
    @Query("SELECT COUNT(*) FROM text_items")
    suspend fun getTextItemCount(): Int

    @Query("UPDATE text_items SET embedding = :embedding WHERE id = :id")
    suspend fun updateEmbedding(id: Long, embedding: String)

    @Query("SELECT id, embedding FROM text_items WHERE embedding IS NOT NULL")
    suspend fun getAllEmbeddings(): List<ItemEmbeddingProjection>

    @Query("SELECT id FROM text_items WHERE embedding IS NULL")
    suspend fun getItemsWithoutEmbedding(): List<ItemWithoutEmbedding>
} 