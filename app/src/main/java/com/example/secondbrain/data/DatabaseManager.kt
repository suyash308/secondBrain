package com.example.secondbrain.data

import android.content.Context
import com.example.secondbrain.data.entities.TextItemEntity
import com.example.secondbrain.data.entities.ImageItemEntity
import com.example.secondbrain.data.entities.LinkItemEntity
import com.example.secondbrain.data.mapper.DataMapper
import com.example.secondbrain.MainActivity
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class DatabaseManager(private val context: Context) {
    
    private val database = AppDatabase.getDatabase(context)
    private val textItemDao = database.textItemDao()
    private val imageItemDao = database.imageItemDao()
    private val linkItemDao = database.linkItemDao()
    private val gson = Gson()
    
    // Text Items
    fun getAllTextItems(): Flow<List<MainActivity.TextItem>> {
        return textItemDao.getAllTextItems().map { entities ->
            entities.map { DataMapper.toTextItem(it) }
        }
    }
    
    fun searchTextItems(query: String): Flow<List<MainActivity.TextItem>> {
        return textItemDao.searchTextItems(query).map { entities ->
            entities.map { DataMapper.toTextItem(it) }
        }
    }
    
    suspend fun insertTextItem(textItem: MainActivity.TextItem) {
        val entity = DataMapper.toTextItemEntity(textItem)
        textItemDao.insertTextItem(entity)
    }
    
    suspend fun getTextItemCount(): Int {
        return textItemDao.getTextItemCount()
    }
    
    // Image Items
    fun getAllImageItems(): Flow<List<MainActivity.ImageItem>> {
        return imageItemDao.getAllImageItems().map { entities ->
            entities.map { DataMapper.toImageItem(it) }
        }
    }
    
    fun searchImageItems(query: String): Flow<List<MainActivity.ImageItem>> {
        return imageItemDao.searchImageItems(query).map { entities ->
            entities.map { DataMapper.toImageItem(it) }
        }
    }
    
    suspend fun insertImageItem(imageItem: MainActivity.ImageItem) {
        val entity = DataMapper.toImageItemEntity(imageItem)
        imageItemDao.insertImageItem(entity)
    }
    
    suspend fun updateImageExtractedText(imageItem: MainActivity.ImageItem) {
        // For now, we'll use a simpler approach
        // In a real implementation, you might want to store the entity ID when inserting
        val entity = DataMapper.toImageItemEntity(imageItem)
        imageItemDao.updateImageItem(entity)
    }
    
    suspend fun updateImageExtractedTextByUri(uri: String, extractedText: String) {
        // Get all image items and find the one with matching URI
        getAllImageItems().collect { imageItems ->
            val matchingItem = imageItems.find { item ->
                item.originalUri == uri || item.localPath == uri
            }
            
            if (matchingItem != null) {
                val updatedItem = matchingItem.copy(extractedText = extractedText)
                val entity = DataMapper.toImageItemEntity(updatedItem)
                imageItemDao.updateImageItem(entity)
            }
        }
    }
    
    suspend fun getImageItemCount(): Int {
        return imageItemDao.getImageItemCount()
    }
    
    // Link Items
    fun getAllLinkItems(): Flow<List<MainActivity.LinkItem>> {
        return linkItemDao.getAllLinkItems().map { entities ->
            entities.map { DataMapper.toLinkItem(it) }
        }
    }
    
    fun searchLinkItems(query: String): Flow<List<MainActivity.LinkItem>> {
        return linkItemDao.searchLinkItems(query).map { entities ->
            entities.map { DataMapper.toLinkItem(it) }
        }
    }
    
    suspend fun insertLinkItem(linkItem: MainActivity.LinkItem) {
        val entity = DataMapper.toLinkItemEntity(linkItem)
        linkItemDao.insertLinkItem(entity)
    }
    
    suspend fun getLinkItemCount(): Int {
        return linkItemDao.getLinkItemCount()
    }
    
    // Migration from SharedPreferences
    suspend fun migrateFromSharedPreferences() {
        val prefs = context.getSharedPreferences("SecondBrainPrefs", Context.MODE_PRIVATE)
        
        // Migrate text items
        val textItemsJson = prefs.getString("text_items", "[]")
        val textType = object : TypeToken<List<MainActivity.TextItem>>() {}.type
        val textItems: List<MainActivity.TextItem> = gson.fromJson(textItemsJson, textType) ?: listOf()
        textItems.forEach { insertTextItem(it) }
        
        // Migrate image items
        val imageItemsJson = prefs.getString("image_items", "[]")
        val imageType = object : TypeToken<List<MainActivity.ImageItem>>() {}.type
        val imageItems: List<MainActivity.ImageItem> = gson.fromJson(imageItemsJson, imageType) ?: listOf()
        imageItems.forEach { insertImageItem(it) }
        
        // Migrate link items
        val linkItemsJson = prefs.getString("link_items", "[]")
        val linkType = object : TypeToken<List<MainActivity.LinkItem>>() {}.type
        val linkItems: List<MainActivity.LinkItem> = gson.fromJson(linkItemsJson, linkType) ?: listOf()
        linkItems.forEach { insertLinkItem(it) }
    }
} 