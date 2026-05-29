# TECHNICAL_DESIGN.md - Technical Design Document

This file defines how to build each feature. Architecture decisions, file changes,
database migrations, and implementation notes are all documented here.
Claude Code must follow this design. Do not invent alternative approaches.

---

## Existing Architecture Reference

```
app/src/main/java/com/example/secondbrain/
├── MainActivity.kt                 # Single activity, all UI
├── data/
│   ├── AppDatabase.kt             # Room database, current version: 1
│   ├── DatabaseManager.kt         # Single access point for all DB operations
│   ├── entities/
│   │   ├── TextItemEntity.kt
│   │   ├── ImageItemEntity.kt
│   │   └── LinkItemEntity.kt
│   ├── dao/
│   │   ├── TextItemDao.kt
│   │   ├── ImageItemDao.kt
│   │   └── LinkItemDao.kt
│   ├── repository/
│   │   ├── TextItemRepository.kt
│   │   ├── ImageItemRepository.kt
│   │   └── LinkItemRepository.kt
│   └── mapper/
│       └── DataMapper.kt
└── ui/theme/
    ├── Color.kt
    ├── Theme.kt
    └── Type.kt
```

---

## Feature 1: Delete and Edit

### Files to Modify
- `TextItemDao.kt` -- add delete and update methods
- `ImageItemDao.kt` -- add delete method
- `LinkItemDao.kt` -- add delete and update methods
- `TextItemRepository.kt` -- add delete and update methods
- `ImageItemRepository.kt` -- add delete method
- `LinkItemRepository.kt` -- add delete and update methods
- `DatabaseManager.kt` -- expose delete and update methods for all types
- `MainActivity.kt` -- add long press handler, bottom sheet, edit screens

### Files to Create
- None. All changes are in existing files.

### No Database Migration Required
- No schema changes for this feature.
- Delete uses existing Room `@Delete` annotation.
- Update uses existing Room `@Update` annotation.

### DAO Changes

**TextItemDao.kt**
```kotlin
@Delete
suspend fun delete(item: TextItemEntity)

@Update
suspend fun update(item: TextItemEntity)
```

**ImageItemDao.kt**
```kotlin
@Delete
suspend fun delete(item: ImageItemEntity)
```

**LinkItemDao.kt**
```kotlin
@Delete
suspend fun delete(item: LinkItemEntity)

@Update
suspend fun update(item: LinkItemEntity)
```

### Repository Changes

Each repository adds corresponding suspend functions that call the DAO methods.
`ImageItemRepository.deleteItem()` must also delete the local file at `localPath`
using `File(localPath).delete()` before calling the DAO delete. If the file does
not exist, proceed with DAO delete anyway.

### DatabaseManager Changes

Add public suspend functions:
```kotlin
suspend fun deleteTextItem(item: TextItemEntity)
suspend fun updateTextItem(item: TextItemEntity)
suspend fun deleteImageItem(item: ImageItemEntity)
suspend fun deleteLinkItem(item: LinkItemEntity)
suspend fun updateLinkItem(item: LinkItemEntity)
```

### UI Changes in MainActivity.kt

**State Variables to Add**
```kotlin
var showOptionsSheet by remember { mutableStateOf(false) }
var selectedTextItem by remember { mutableStateOf<TextItem?>(null) }
var selectedImageItem by remember { mutableStateOf<ImageItem?>(null) }
var selectedLinkItem by remember { mutableStateOf<LinkItem?>(null) }
var showDeleteConfirmation by remember { mutableStateOf(false) }
var showEditTextScreen by remember { mutableStateOf(false) }
var showEditLinkScreen by remember { mutableStateOf(false) }
```

**New Composables to Add Inside MainActivity.kt**
- `ItemOptionsBottomSheet()` -- shows Edit/Delete/Cancel options
- `DeleteConfirmationDialog()` -- confirms deletion
- `EditTextItemScreen()` -- full-screen text editor
- `EditLinkItemScreen()` -- full-screen link URL editor

**Long Press Handler**
Add `combinedClickable(onLongClick = { ... })` modifier to each item card composable.
On long press, set the appropriate selected item state variable and set
`showOptionsSheet = true`.

**Validation Logic**
- EditTextItemScreen: disable Save button and show error text if input is blank
- EditLinkItemScreen: disable Save button and show error text if input is blank
  or does not start with `http://` or `https://`

**Link Metadata Re-fetch After Edit**
After saving an updated URL in EditLinkItemScreen, launch a coroutine on IO
dispatcher to fetch new metadata using the existing Jsoup-based fetch logic already
present in the codebase. Update the link item in the database after fetch completes.

---

## Feature 2: Tags

### Files to Modify
- `AppDatabase.kt` -- add new entities, DAOs, and migration
- `DatabaseManager.kt` -- add tag operation methods
- `MainActivity.kt` -- add tag chips, tag filter bar, add-tag bottom sheet

### Files to Create
- `data/entities/TagEntity.kt`
- `data/entities/TextItemTagCrossRef.kt`
- `data/entities/ImageItemTagCrossRef.kt`
- `data/entities/LinkItemTagCrossRef.kt`
- `data/dao/TagDao.kt`

### Database Migration

**Migration from version 1 to version 2** (or current version + 1)

```kotlin
val MIGRATION_1_2 = object : Migration(1, 2) {
    override fun migrate(database: SupportSQLiteDatabase) {
        database.execSQL(
            "CREATE TABLE IF NOT EXISTS TagEntity (id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, name TEXT NOT NULL UNIQUE)"
        )
        database.execSQL(
            "CREATE TABLE IF NOT EXISTS TextItemTagCrossRef (textItemId INTEGER NOT NULL, tagId INTEGER NOT NULL, PRIMARY KEY (textItemId, tagId))"
        )
        database.execSQL(
            "CREATE TABLE IF NOT EXISTS ImageItemTagCrossRef (imageItemId INTEGER NOT NULL, tagId INTEGER NOT NULL, PRIMARY KEY (imageItemId, tagId))"
        )
        database.execSQL(
            "CREATE TABLE IF NOT EXISTS LinkItemTagCrossRef (linkItemId INTEGER NOT NULL, tagId INTEGER NOT NULL, PRIMARY KEY (linkItemId, tagId))"
        )
    }
}
```

Register in `AppDatabase.kt`:
```kotlin
.addMigrations(MIGRATION_1_2)
```

### New Entity Classes

**TagEntity.kt**
```kotlin
@Entity
data class TagEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val name: String
)
```

**CrossRef entities** follow the same pattern with composite primary keys using
`@Entity(primaryKeys = [...])`.

### TagDao.kt Methods
```kotlin
@Insert(onConflict = OnConflictStrategy.IGNORE)
suspend fun insertTag(tag: TagEntity): Long

@Query("SELECT * FROM TagEntity ORDER BY name ASC")
fun getAllTags(): Flow<List<TagEntity>>

@Query("SELECT * FROM TagEntity WHERE name LIKE :query ORDER BY name ASC")
suspend fun searchTags(query: String): List<TagEntity>

// Insert cross-ref methods for each content type
@Insert(onConflict = OnConflictStrategy.IGNORE)
suspend fun addTagToTextItem(crossRef: TextItemTagCrossRef)

@Delete
suspend fun removeTagFromTextItem(crossRef: TextItemTagCrossRef)

// Repeat for Image and Link cross-refs

// Query tags for a specific item
@Query("SELECT t.* FROM TagEntity t INNER JOIN TextItemTagCrossRef x ON t.id = x.tagId WHERE x.textItemId = :itemId")
fun getTagsForTextItem(itemId: Int): Flow<List<TagEntity>>

// Repeat for Image and Link

// Query items by tag
@Query("SELECT textItemId FROM TextItemTagCrossRef WHERE tagId = :tagId")
fun getTextItemIdsByTag(tagId: Int): Flow<List<Int>>

// Repeat for Image and Link

// Delete all cross-refs when an item is deleted
@Query("DELETE FROM TextItemTagCrossRef WHERE textItemId = :itemId")
suspend fun deleteTagsForTextItem(itemId: Int)

// Repeat for Image and Link
```

### DatabaseManager Changes

Add methods:
```kotlin
suspend fun addTagToItem(tagName: String, itemId: Int, contentType: ContentType)
suspend fun removeTagFromItem(tagId: Int, itemId: Int, contentType: ContentType)
fun getTagsForItem(itemId: Int, contentType: ContentType): Flow<List<TagEntity>>
fun getAllTags(): Flow<List<TagEntity>>
suspend fun searchTagSuggestions(query: String): List<TagEntity>
fun getItemIdsByTag(tagId: Int, contentType: ContentType): Flow<List<Int>>
```

Use a `ContentType` enum with values `TEXT`, `IMAGE`, `LINK`.

### Repository Changes

Delete methods in each repository must call the corresponding
`deleteTagsForXxxItem(itemId)` before deleting the item itself to maintain
referential integrity.

### UI Changes in MainActivity.kt

**State Variables to Add**
```kotlin
var activeTagFilter by remember { mutableStateOf<TagEntity?>(null) }
var showAddTagSheet by remember { mutableStateOf(false) }
var tagSheetTargetItem by remember { mutableStateOf<Pair<Int, ContentType>?>(null) }
var tagSearchQuery by remember { mutableStateOf("") }
```

**New Composables to Add**
- `TagFilterBar(tags, activeTag, onTagSelected)` -- horizontal scrollable chip row
- `AddTagBottomSheet(itemId, contentType, existingTags, onDismiss)` -- tag input
  with suggestions
- `TagChipRow(tags, onTagRemove)` -- chips displayed on item cards

**Tag Chip Row on Item Cards**
- Collect tags for each item using `getTagsForItem()` as a Flow
- Display maximum 3 chips; show "+N more" if more exist
- No remove button on card chips; remove is only available in AddTagBottomSheet

**Tag Filter Logic**
- When `activeTagFilter` is not null, collect item IDs for that tag and filter
  the displayed lists to only those IDs before applying any search query

---



## Database Version Summary

| Version | Changes |
|---|---|
| 1 | Baseline: TextItemEntity, ImageItemEntity, LinkItemEntity |
| 2 | Feature 2: TagEntity, three CrossRef tables |

If features are implemented together in one session, combine migrations accordingly
and use version 3 as the final target.
