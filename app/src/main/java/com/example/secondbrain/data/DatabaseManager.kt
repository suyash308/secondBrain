package com.example.secondbrain.data

import android.content.Context
import com.example.secondbrain.data.dao.ItemEmbeddingProjection
import com.example.secondbrain.data.dao.ItemWithoutEmbedding
import com.example.secondbrain.data.entities.ChatMessageEntity
import com.example.secondbrain.data.entities.ConversationEntity
import com.example.secondbrain.data.entities.ImageItemTagCrossRef
import com.example.secondbrain.data.entities.LinkItemTagCrossRef
import com.example.secondbrain.data.entities.TagEntity
import com.example.secondbrain.data.entities.TextItemEntity
import com.example.secondbrain.data.entities.ImageItemEntity
import com.example.secondbrain.data.entities.LinkItemEntity
import com.example.secondbrain.data.entities.TextItemTagCrossRef
import com.example.secondbrain.data.mapper.DataMapper
import com.example.secondbrain.data.repository.TextItemRepository
import com.example.secondbrain.data.repository.ImageItemRepository
import com.example.secondbrain.data.repository.LinkItemRepository
import com.example.secondbrain.MainActivity
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext

enum class ContentType { TEXT, IMAGE, LINK }

class DatabaseManager(private val context: Context) {
    
    private val database = AppDatabase.getDatabase(context)
    private val textItemDao = database.textItemDao()
    private val imageItemDao = database.imageItemDao()
    private val linkItemDao = database.linkItemDao()
    private val tagDao = database.tagDao()
    private val chatMessageDao = database.chatMessageDao()
    private val conversationDao = database.conversationDao()
    private val gson = Gson()
    private val textItemRepository = TextItemRepository(textItemDao)
    private val imageItemRepository = ImageItemRepository(imageItemDao)
    private val linkItemRepository = LinkItemRepository(linkItemDao)
    
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
    
    suspend fun insertTextItem(textItem: MainActivity.TextItem): Long {
        val entity = DataMapper.toTextItemEntity(textItem)
        return textItemDao.insertTextItem(entity)
    }
    
    suspend fun deleteTextItem(item: TextItemEntity) {
        tagDao.deleteTagsForTextItem(item.id)
        textItemRepository.delete(item)
    }

    suspend fun updateTextItem(item: TextItemEntity) {
        textItemRepository.update(item)
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
    
    suspend fun insertImageItem(imageItem: MainActivity.ImageItem): Long {
        val entity = DataMapper.toImageItemEntity(imageItem)
        return imageItemDao.insertImageItem(entity)
    }
    
    suspend fun updateImageExtractedText(imageItem: MainActivity.ImageItem) {
        // For now, we'll use a simpler approach
        // In a real implementation, you might want to store the entity ID when inserting
        val entity = DataMapper.toImageItemEntity(imageItem)
        imageItemDao.updateImageItem(entity)
    }
    
    suspend fun updateImageExtractedTextByUri(uri: String, extractedText: String) {
        imageItemDao.updateExtractedTextByUri(uri, extractedText)
    }
    
    suspend fun deleteImageItem(item: ImageItemEntity) {
        tagDao.deleteTagsForImageItem(item.id)
        imageItemRepository.delete(item)
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
    
    suspend fun insertLinkItem(linkItem: MainActivity.LinkItem): Long {
        val entity = DataMapper.toLinkItemEntity(linkItem)
        return linkItemDao.insertLinkItem(entity)
    }
    
    suspend fun deleteLinkItem(item: LinkItemEntity) {
        tagDao.deleteTagsForLinkItem(item.id)
        linkItemRepository.delete(item)
    }

    suspend fun updateLinkItem(item: LinkItemEntity) {
        linkItemRepository.update(item)
    }

    suspend fun getLinkItemCount(): Int {
        return linkItemDao.getLinkItemCount()
    }

    suspend fun updateLinkMetadata(url: String, title: String?, description: String?, imageUrl: String?) {
        linkItemDao.updateLinkMetadata(url, title, description, imageUrl)
    }

    suspend fun getLinkItemByUrl(url: String) = linkItemDao.getLinkItemByUrl(url)
    
    // Tags
    suspend fun addTagToItem(tagName: String, itemId: Long, contentType: ContentType) {
        val existingOrNew = tagDao.searchTags(tagName).firstOrNull { it.name == tagName }
        val tagId = if (existingOrNew != null) {
            existingOrNew.id
        } else {
            tagDao.insertTag(TagEntity(name = tagName)).toInt()
        }
        when (contentType) {
            ContentType.TEXT  -> tagDao.addTagToTextItem(TextItemTagCrossRef(itemId, tagId))
            ContentType.IMAGE -> tagDao.addTagToImageItem(ImageItemTagCrossRef(itemId, tagId))
            ContentType.LINK  -> tagDao.addTagToLinkItem(LinkItemTagCrossRef(itemId, tagId))
        }
    }

    suspend fun removeTagFromItem(tagId: Int, itemId: Long, contentType: ContentType) {
        when (contentType) {
            ContentType.TEXT  -> tagDao.removeTagFromTextItem(TextItemTagCrossRef(itemId, tagId))
            ContentType.IMAGE -> tagDao.removeTagFromImageItem(ImageItemTagCrossRef(itemId, tagId))
            ContentType.LINK  -> tagDao.removeTagFromLinkItem(LinkItemTagCrossRef(itemId, tagId))
        }
    }

    fun getTagsForItem(itemId: Long, contentType: ContentType): Flow<List<TagEntity>> {
        return when (contentType) {
            ContentType.TEXT  -> tagDao.getTagsForTextItem(itemId)
            ContentType.IMAGE -> tagDao.getTagsForImageItem(itemId)
            ContentType.LINK  -> tagDao.getTagsForLinkItem(itemId)
        }
    }

    fun getAllTags(): Flow<List<TagEntity>> = tagDao.getAllTags()

    suspend fun searchTagSuggestions(query: String): List<TagEntity> = tagDao.searchTags(query)

    fun getItemIdsByTag(tagId: Int, contentType: ContentType): Flow<List<Long>> {
        return when (contentType) {
            ContentType.TEXT  -> tagDao.getTextItemIdsByTag(tagId)
            ContentType.IMAGE -> tagDao.getImageItemIdsByTag(tagId)
            ContentType.LINK  -> tagDao.getLinkItemIdsByTag(tagId)
        }
    }

    // Embeddings
    suspend fun updateTextItemEmbedding(id: Long, embedding: String) {
        withContext(Dispatchers.IO) { textItemDao.updateEmbedding(id, embedding) }
    }

    suspend fun updateImageItemEmbedding(id: Long, embedding: String) {
        withContext(Dispatchers.IO) { imageItemDao.updateEmbedding(id, embedding) }
    }

    suspend fun updateLinkItemEmbedding(id: Long, embedding: String) {
        withContext(Dispatchers.IO) { linkItemDao.updateEmbedding(id, embedding) }
    }

    suspend fun getAllTextEmbeddings(): List<ItemEmbeddingProjection> =
        withContext(Dispatchers.IO) { textItemDao.getAllEmbeddings() }

    suspend fun getAllImageEmbeddings(): List<ItemEmbeddingProjection> =
        withContext(Dispatchers.IO) { imageItemDao.getAllEmbeddings() }

    suspend fun getAllLinkEmbeddings(): List<ItemEmbeddingProjection> =
        withContext(Dispatchers.IO) { linkItemDao.getAllEmbeddings() }

    suspend fun getTextItemsWithoutEmbedding(): List<ItemWithoutEmbedding> =
        withContext(Dispatchers.IO) { textItemDao.getItemsWithoutEmbedding() }

    suspend fun getImageItemsWithoutEmbedding(): List<ItemWithoutEmbedding> =
        withContext(Dispatchers.IO) { imageItemDao.getItemsWithoutEmbedding() }

    suspend fun getLinkItemsWithoutEmbedding(): List<ItemWithoutEmbedding> =
        withContext(Dispatchers.IO) { linkItemDao.getItemsWithoutEmbedding() }

    // Chat messages (per-conversation)
    suspend fun insertChatMessage(message: ChatMessageEntity) {
        withContext(Dispatchers.IO) { chatMessageDao.insert(message) }
    }

    fun getChatMessages(conversationId: Int): Flow<List<ChatMessageEntity>> =
        chatMessageDao.getMessages(conversationId)

    suspend fun getRecentChatMessages(conversationId: Int, limit: Int): List<ChatMessageEntity> =
        withContext(Dispatchers.IO) { chatMessageDao.getRecentMessages(conversationId, limit) }

    // Legacy — kept for backward compat
    fun getAllChatMessages(): Flow<List<ChatMessageEntity>> = chatMessageDao.getAllMessages()

    suspend fun deleteAllChatMessages() {
        withContext(Dispatchers.IO) { chatMessageDao.deleteAll() }
    }

    // Conversations
    suspend fun createConversation(title: String): Int {
        val id = withContext(Dispatchers.IO) {
            conversationDao.insert(ConversationEntity(title = title, createdAt = System.currentTimeMillis()))
        }
        return id.toInt()
    }

    fun getAllConversations(): Flow<List<ConversationEntity>> = conversationDao.getAllConversations()

    suspend fun updateConversationTitle(conversationId: Int, title: String) {
        withContext(Dispatchers.IO) { conversationDao.updateTitle(conversationId, title) }
    }

    suspend fun deleteConversation(conversationId: Int) {
        withContext(Dispatchers.IO) {
            chatMessageDao.deleteByConversation(conversationId)
            conversationDao.deleteById(conversationId)
        }
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