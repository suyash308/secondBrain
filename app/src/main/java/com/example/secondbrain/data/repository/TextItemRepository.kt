package com.example.secondbrain.data.repository

import com.example.secondbrain.data.dao.TextItemDao
import com.example.secondbrain.data.entities.TextItemEntity
import kotlinx.coroutines.flow.Flow

class TextItemRepository(
    private val textItemDao: TextItemDao
) {
    
    fun getAllTextItems(): Flow<List<TextItemEntity>> {
        return textItemDao.getAllTextItems()
    }
    
    fun searchTextItems(query: String): Flow<List<TextItemEntity>> {
        return textItemDao.searchTextItems(query)
    }
    
    suspend fun insertTextItem(textItem: TextItemEntity) {
        textItemDao.insertTextItem(textItem)
    }
    
    suspend fun updateTextItem(textItem: TextItemEntity) {
        textItemDao.updateTextItem(textItem)
    }
    
    suspend fun deleteTextItem(textItem: TextItemEntity) {
        textItemDao.deleteTextItem(textItem)
    }
    
    suspend fun delete(item: TextItemEntity) {
        textItemDao.delete(item)
    }

    suspend fun update(item: TextItemEntity) {
        textItemDao.update(item)
    }

    suspend fun deleteAllTextItems() {
        textItemDao.deleteAllTextItems()
    }
    
    suspend fun getTextItemCount(): Int {
        return textItemDao.getTextItemCount()
    }
} 