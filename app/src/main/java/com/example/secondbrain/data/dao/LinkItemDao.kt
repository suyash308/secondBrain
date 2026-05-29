package com.example.secondbrain.data.dao

import androidx.room.*
import com.example.secondbrain.data.entities.LinkItemEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface LinkItemDao {
    
    @Query("SELECT * FROM link_items ORDER BY timestamp DESC")
    fun getAllLinkItems(): Flow<List<LinkItemEntity>>
    
    @Query("SELECT * FROM link_items WHERE title LIKE '%' || :query || '%' OR description LIKE '%' || :query || '%' OR url LIKE '%' || :query || '%' ORDER BY timestamp DESC")
    fun searchLinkItems(query: String): Flow<List<LinkItemEntity>>
    
    @Insert
    suspend fun insertLinkItem(linkItem: LinkItemEntity)
    
    @Update
    suspend fun updateLinkItem(linkItem: LinkItemEntity)

    @Update
    suspend fun update(item: LinkItemEntity)

    @Delete
    suspend fun deleteLinkItem(linkItem: LinkItemEntity)

    @Delete
    suspend fun delete(item: LinkItemEntity)
    
    @Query("DELETE FROM link_items")
    suspend fun deleteAllLinkItems()
    
    @Query("SELECT COUNT(*) FROM link_items")
    suspend fun getLinkItemCount(): Int

    @Query("UPDATE link_items SET title = :title, description = :description, imageUrl = :imageUrl WHERE url = :url")
    suspend fun updateLinkMetadata(url: String, title: String?, description: String?, imageUrl: String?)
} 