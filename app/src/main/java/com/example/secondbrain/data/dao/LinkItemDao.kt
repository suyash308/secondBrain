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
    suspend fun insertLinkItem(linkItem: LinkItemEntity): Long
    
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

    @Query("SELECT * FROM link_items WHERE url = :url LIMIT 1")
    suspend fun getLinkItemByUrl(url: String): LinkItemEntity?

    @Query("UPDATE link_items SET embedding = :embedding WHERE id = :id")
    suspend fun updateEmbedding(id: Long, embedding: String)

    @Query("SELECT id, embedding FROM link_items WHERE embedding IS NOT NULL")
    suspend fun getAllEmbeddings(): List<ItemEmbeddingProjection>

    @Query("SELECT id FROM link_items WHERE embedding IS NULL")
    suspend fun getItemsWithoutEmbedding(): List<ItemWithoutEmbedding>
} 