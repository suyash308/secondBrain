package com.example.secondbrain.data.dao

import androidx.room.*
import com.example.secondbrain.data.entities.TextItemEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface TextItemDao {
    
    @Query("SELECT * FROM text_items ORDER BY timestamp DESC")
    fun getAllTextItems(): Flow<List<TextItemEntity>>
    
    @Query("SELECT * FROM text_items WHERE content LIKE '%' || :query || '%' ORDER BY timestamp DESC")
    fun searchTextItems(query: String): Flow<List<TextItemEntity>>
    
    @Insert
    suspend fun insertTextItem(textItem: TextItemEntity)
    
    @Update
    suspend fun updateTextItem(textItem: TextItemEntity)
    
    @Delete
    suspend fun deleteTextItem(textItem: TextItemEntity)
    
    @Query("DELETE FROM text_items")
    suspend fun deleteAllTextItems()
    
    @Query("SELECT COUNT(*) FROM text_items")
    suspend fun getTextItemCount(): Int
} 