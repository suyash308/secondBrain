package com.example.secondbrain.data.repository

import com.example.secondbrain.data.dao.LinkItemDao
import com.example.secondbrain.data.entities.LinkItemEntity
import kotlinx.coroutines.flow.Flow

class LinkItemRepository(
    private val linkItemDao: LinkItemDao
) {
    
    fun getAllLinkItems(): Flow<List<LinkItemEntity>> {
        return linkItemDao.getAllLinkItems()
    }
    
    fun searchLinkItems(query: String): Flow<List<LinkItemEntity>> {
        return linkItemDao.searchLinkItems(query)
    }
    
    suspend fun insertLinkItem(linkItem: LinkItemEntity) {
        linkItemDao.insertLinkItem(linkItem)
    }
    
    suspend fun updateLinkItem(linkItem: LinkItemEntity) {
        linkItemDao.updateLinkItem(linkItem)
    }
    
    suspend fun deleteLinkItem(linkItem: LinkItemEntity) {
        linkItemDao.deleteLinkItem(linkItem)
    }
    
    suspend fun delete(item: LinkItemEntity) {
        linkItemDao.delete(item)
    }

    suspend fun update(item: LinkItemEntity) {
        linkItemDao.update(item)
    }

    suspend fun deleteAllLinkItems() {
        linkItemDao.deleteAllLinkItems()
    }
    
    suspend fun getLinkItemCount(): Int {
        return linkItemDao.getLinkItemCount()
    }
} 