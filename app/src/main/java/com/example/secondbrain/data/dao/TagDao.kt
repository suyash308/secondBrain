package com.example.secondbrain.data.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.secondbrain.data.entities.ImageItemTagCrossRef
import com.example.secondbrain.data.entities.LinkItemTagCrossRef
import com.example.secondbrain.data.entities.TagEntity
import com.example.secondbrain.data.entities.TextItemTagCrossRef
import kotlinx.coroutines.flow.Flow

@Dao
interface TagDao {

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertTag(tag: TagEntity): Long

    @Query("SELECT * FROM TagEntity ORDER BY name ASC")
    fun getAllTags(): Flow<List<TagEntity>>

    @Query("SELECT * FROM TagEntity WHERE name LIKE '%' || :query || '%' ORDER BY name ASC")
    suspend fun searchTags(query: String): List<TagEntity>

    // --- TextItem cross-ref ---

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun addTagToTextItem(crossRef: TextItemTagCrossRef)

    @Delete
    suspend fun removeTagFromTextItem(crossRef: TextItemTagCrossRef)

    @Query("SELECT t.* FROM TagEntity t INNER JOIN TextItemTagCrossRef x ON t.id = x.tagId WHERE x.textItemId = :itemId")
    fun getTagsForTextItem(itemId: Long): Flow<List<TagEntity>>

    @Query("SELECT textItemId FROM TextItemTagCrossRef WHERE tagId = :tagId")
    fun getTextItemIdsByTag(tagId: Int): Flow<List<Long>>

    @Query("DELETE FROM TextItemTagCrossRef WHERE textItemId = :itemId")
    suspend fun deleteTagsForTextItem(itemId: Long)

    // --- ImageItem cross-ref ---

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun addTagToImageItem(crossRef: ImageItemTagCrossRef)

    @Delete
    suspend fun removeTagFromImageItem(crossRef: ImageItemTagCrossRef)

    @Query("SELECT t.* FROM TagEntity t INNER JOIN ImageItemTagCrossRef x ON t.id = x.tagId WHERE x.imageItemId = :itemId")
    fun getTagsForImageItem(itemId: Long): Flow<List<TagEntity>>

    @Query("SELECT imageItemId FROM ImageItemTagCrossRef WHERE tagId = :tagId")
    fun getImageItemIdsByTag(tagId: Int): Flow<List<Long>>

    @Query("DELETE FROM ImageItemTagCrossRef WHERE imageItemId = :itemId")
    suspend fun deleteTagsForImageItem(itemId: Long)

    // --- LinkItem cross-ref ---

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun addTagToLinkItem(crossRef: LinkItemTagCrossRef)

    @Delete
    suspend fun removeTagFromLinkItem(crossRef: LinkItemTagCrossRef)

    @Query("SELECT t.* FROM TagEntity t INNER JOIN LinkItemTagCrossRef x ON t.id = x.tagId WHERE x.linkItemId = :itemId")
    fun getTagsForLinkItem(itemId: Long): Flow<List<TagEntity>>

    @Query("SELECT linkItemId FROM LinkItemTagCrossRef WHERE tagId = :tagId")
    fun getLinkItemIdsByTag(tagId: Int): Flow<List<Long>>

    @Query("DELETE FROM LinkItemTagCrossRef WHERE linkItemId = :itemId")
    suspend fun deleteTagsForLinkItem(itemId: Long)
}
