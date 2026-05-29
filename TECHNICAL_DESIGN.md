# TECHNICAL_DESIGN.md - Technical Design Document

This file defines how to build each feature. Architecture decisions, file changes,
database migrations, and implementation notes are all documented here.
Claude Code must follow this design. Do not invent alternative approaches.

---

## Current Architecture State

Features 1 and 2 are complete. Current codebase:

```
app/src/main/java/com/example/secondbrain/
├── MainActivity.kt
├── data/
│   ├── AppDatabase.kt             # Room version: 2
│   ├── DatabaseManager.kt
│   ├── entities/
│   │   ├── TextItemEntity.kt
│   │   ├── ImageItemEntity.kt
│   │   ├── LinkItemEntity.kt
│   │   ├── TagEntity.kt
│   │   ├── TextItemTagCrossRef.kt
│   │   ├── ImageItemTagCrossRef.kt
│   │   └── LinkItemTagCrossRef.kt
│   ├── dao/
│   │   ├── TextItemDao.kt
│   │   ├── ImageItemDao.kt
│   │   ├── LinkItemDao.kt
│   │   └── TagDao.kt
│   ├── repository/
│   │   ├── TextItemRepository.kt
│   │   ├── ImageItemRepository.kt
│   │   └── LinkItemRepository.kt
│   └── mapper/
│       └── DataMapper.kt
└── ui/theme/
```

---

## Feature 1 and Feature 2 -- COMPLETE

No further changes required.

---

## Feature 3: RAG Chat Agent

### New Files to Create

```
data/
├── OpenRouterService.kt       # All API calls: embeddings + chat
├── SettingsManager.kt         # EncryptedSharedPreferences for API key
├── EmbeddingUtils.kt          # Cosine similarity computation
├── entities/
│   └── ChatMessageEntity.kt   # Room entity for chat history
└── dao/
    └── ChatMessageDao.kt      # DAO for chat messages
```

### Files to Modify

```
TextItemEntity.kt              # Add embedding column
ImageItemEntity.kt             # Add embedding column
LinkItemEntity.kt              # Add embedding column
TextItemDao.kt                 # Add updateEmbedding method
ImageItemDao.kt                # Add updateEmbedding method
LinkItemDao.kt                 # Add updateEmbedding method
AppDatabase.kt                 # Add migration, new entity, new DAO, bump to v3
DatabaseManager.kt             # Add embedding and chat methods
MainActivity.kt                # Add Settings screen, Chat screen, gear icon, chat icon
```

---

## Step 1: Entity Changes

### Existing Entities -- Add Embedding Column

Add to `TextItemEntity.kt`, `ImageItemEntity.kt`, `LinkItemEntity.kt`:
```kotlin
val embedding: String? = null  // JSON array of 1536 floats
```

### New Entity: ChatMessageEntity.kt

```kotlin
@Entity
data class ChatMessageEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val role: String,           // "user" or "assistant"
    val content: String,
    val timestamp: Long,
    val sourceIds: String? = null,    // JSON array of Int item IDs
    val sourceTypes: String? = null   // JSON array of String content types
)
```

---

## Step 2: DAO Changes

### Existing DAOs -- Add updateEmbedding

Add to `TextItemDao.kt`, `ImageItemDao.kt`, `LinkItemDao.kt`:
```kotlin
@Query("UPDATE <TableName> SET embedding = :embedding WHERE id = :id")
suspend fun updateEmbedding(id: Int, embedding: String)
```

Use correct table names:
- TextItemEntity, ImageItemEntity, LinkItemEntity

Also add to each DAO:
```kotlin
@Query("SELECT id, embedding FROM <TableName> WHERE embedding IS NOT NULL")
suspend fun getAllEmbeddings(): List<ItemEmbeddingProjection>
```

Create a data class `ItemEmbeddingProjection(val id: Int, val embedding: String)`
in a shared location or inline in each DAO file.

Also add to each DAO:
```kotlin
@Query("SELECT id, embedding FROM <TableName> WHERE embedding IS NULL")
suspend fun getItemsWithoutEmbedding(): List<ItemWithoutEmbedding>
```

Create `ItemWithoutEmbedding(val id: Int)` similarly.

### New DAO: ChatMessageDao.kt

```kotlin
@Dao
interface ChatMessageDao {

    @Insert
    suspend fun insert(message: ChatMessageEntity)

    @Query("SELECT * FROM ChatMessageEntity ORDER BY timestamp ASC")
    fun getAllMessages(): Flow<List<ChatMessageEntity>>

    @Query("SELECT * FROM ChatMessageEntity ORDER BY timestamp DESC LIMIT :limit")
    suspend fun getRecentMessages(limit: Int): List<ChatMessageEntity>

    @Query("DELETE FROM ChatMessageEntity")
    suspend fun deleteAll()
}
```

---

## Step 3: Database Migration

### MIGRATION_2_3

```kotlin
val MIGRATION_2_3 = object : Migration(2, 3) {
    override fun migrate(database: SupportSQLiteDatabase) {
        database.execSQL("ALTER TABLE TextItemEntity ADD COLUMN embedding TEXT")
        database.execSQL("ALTER TABLE ImageItemEntity ADD COLUMN embedding TEXT")
        database.execSQL("ALTER TABLE LinkItemEntity ADD COLUMN embedding TEXT")
        database.execSQL("""
            CREATE TABLE IF NOT EXISTS ChatMessageEntity (
                id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                role TEXT NOT NULL,
                content TEXT NOT NULL,
                timestamp INTEGER NOT NULL,
                sourceIds TEXT,
                sourceTypes TEXT
            )
        """.trimIndent())
    }
}
```

Register in `AppDatabase.kt`:
```kotlin
.addMigrations(MIGRATION_1_2, MIGRATION_2_3)
```

Bump version from 2 to 3.
Register `ChatMessageEntity` in the entities list.
Register `ChatMessageDao` and add `fun chatMessageDao(): ChatMessageDao`.

---

## Step 4: SettingsManager.kt

```kotlin
class SettingsManager(context: Context) {

    private val masterKey = MasterKey.Builder(context)
        .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
        .build()

    private val prefs = EncryptedSharedPreferences.create(
        context,
        "second_brain_secure_prefs",
        masterKey,
        EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
        EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
    )

    fun saveApiKey(key: String) {
        prefs.edit().putString("openrouter_api_key", key).apply()
    }

    fun getApiKey(): String? = prefs.getString("openrouter_api_key", null)

    fun clearApiKey() {
        prefs.edit().remove("openrouter_api_key").apply()
    }
}
```

---

## Step 5: EmbeddingUtils.kt

```kotlin
object EmbeddingUtils {

    fun cosineSimilarity(a: List<Float>, b: List<Float>): Float {
        var dot = 0f
        var normA = 0f
        var normB = 0f
        for (i in a.indices) {
            dot += a[i] * b[i]
            normA += a[i] * a[i]
            normB += b[i] * b[i]
        }
        return if (normA == 0f || normB == 0f) 0f
        else dot / (sqrt(normA) * sqrt(normB))
    }

    fun parseEmbedding(json: String): List<Float> {
        return Gson().fromJson(json, Array<Float>::class.java).toList()
    }

    fun serializeEmbedding(embedding: List<Float>): String {
        return Gson().toJson(embedding)
    }
}
```

---

## Step 6: OpenRouterService.kt

Use `HttpURLConnection` only. Use `Gson` for all JSON. No new dependencies.

```kotlin
class OpenRouterService {

    companion object {
        private const val BASE_URL = "https://openrouter.ai/api/v1"
        private const val REFERER = "second-brain-android"
        private const val EMBEDDING_MODEL = "openai/text-embedding-3-small"
        private const val CHAT_MODEL = "anthropic/claude-haiku-4-5"
        private const val SIMILARITY_THRESHOLD = 0.3f
        private const val TOP_K = 4
    }

    // --- Embeddings ---

    suspend fun generateEmbedding(text: String, apiKey: String): Result<List<Float>>
    // POST /embeddings
    // Body: { "model": "openai/text-embedding-3-small", "input": "{text}" }
    // Parse: response.data[0].embedding (array of floats)
    // Return Result.success(floatList) or Result.failure(exception)

    // --- Chat ---

    suspend fun chat(
        question: String,
        conversationHistory: List<ChatMessageEntity>,
        retrievedItems: List<RetrievedItem>,
        apiKey: String
    ): Result<String>
    // Build system prompt (see REQUIREMENTS.md) + context block from retrievedItems
    // Build messages array: system message + last 10 pairs from conversationHistory + new user message
    // POST /chat/completions
    // Body: { "model": "anthropic/claude-haiku-4-5", "max_tokens": 500, "messages": [...] }
    // Parse: response.choices[0].message.content
    // Return Result.success(text) or Result.failure(exception)
}
```

### RetrievedItem Data Class

```kotlin
data class RetrievedItem(
    val id: Int,
    val contentType: ContentType,  // reuse existing enum
    val label: String,             // short display label for sources UI
    val contextText: String,       // text block sent to Claude
    val similarityScore: Float
)
```

### Context Block Format Built in OpenRouterService

```
[Saved Item 1 - Text Note]
{content}

[Saved Item 2 - Link]
Title: {title}
Description: {description}
URL: {url}

[Saved Item 3 - Image Note]
Extracted text: {extractedText}
```

---

## Step 7: DatabaseManager Changes

Add the following methods to `DatabaseManager.kt`:

```kotlin
// Embeddings
suspend fun updateTextItemEmbedding(id: Int, embedding: String)
suspend fun updateImageItemEmbedding(id: Int, embedding: String)
suspend fun updateLinkItemEmbedding(id: Int, embedding: String)
suspend fun getAllTextEmbeddings(): List<ItemEmbeddingProjection>
suspend fun getAllImageEmbeddings(): List<ItemEmbeddingProjection>
suspend fun getAllLinkEmbeddings(): List<ItemEmbeddingProjection>
suspend fun getTextItemsWithoutEmbedding(): List<ItemWithoutEmbedding>
suspend fun getImageItemsWithoutEmbedding(): List<ItemWithoutEmbedding>
suspend fun getLinkItemsWithoutEmbedding(): List<ItemWithoutEmbedding>

// Chat
suspend fun insertChatMessage(message: ChatMessageEntity)
fun getAllChatMessages(): Flow<List<ChatMessageEntity>>
suspend fun getRecentChatMessages(limit: Int): List<ChatMessageEntity>
suspend fun deleteAllChatMessages()
```

Also update the existing item-save methods in `DatabaseManager` to trigger
embedding generation after saving. This is done by accepting an
`OpenRouterService` instance and `SettingsManager` instance as parameters on
the save methods, OR by calling the embedding generation from `MainActivity`
after the save completes. Use the second approach to avoid circular dependencies:
call save, then call embedding generation separately in `MainActivity`.

---

## Step 8: Retrieval Logic in MainActivity

This logic runs in `MainActivity` when the user sends a chat message.

```kotlin
suspend fun retrieveRelevantItems(
    questionEmbedding: List<Float>,
    databaseManager: DatabaseManager
): List<RetrievedItem> {

    val results = mutableListOf<Pair<RetrievedItem, Float>>()

    // Text items
    databaseManager.getAllTextEmbeddings().forEach { proj ->
        val itemEmbedding = EmbeddingUtils.parseEmbedding(proj.embedding)
        val score = EmbeddingUtils.cosineSimilarity(questionEmbedding, itemEmbedding)
        if (score >= SIMILARITY_THRESHOLD) {
            // fetch full item from DB to build contextText and label
            results.add(Pair(buildRetrievedItem(proj.id, ContentType.TEXT, score), score))
        }
    }

    // Repeat for image and link embeddings

    return results
        .sortedByDescending { it.second }
        .take(TOP_K)
        .map { it.first }
}
```

---

## Step 9: Background Embedding Job in MainActivity

Run this on first launch after update. Store a flag in regular SharedPreferences
(not encrypted) to track whether the job has run.

```kotlin
private fun runBackgroundEmbeddingJobIfNeeded() {
    val prefs = getSharedPreferences("second_brain_prefs", Context.MODE_PRIVATE)
    val jobDone = prefs.getBoolean("embedding_job_v3_done", false)
    if (jobDone) return

    val apiKey = settingsManager.getApiKey() ?: return  // skip if no key set

    lifecycleScope.launch(Dispatchers.IO) {
        val textItems = databaseManager.getTextItemsWithoutEmbedding()
        val imageItems = databaseManager.getImageItemsWithoutEmbedding()
        val linkItems = databaseManager.getLinkItemsWithoutEmbedding()

        // Process in batches of 10, generate embedding per item
        // On success: call updateXxxItemEmbedding
        // On failure: skip and continue
        // After all done: set embedding_job_v3_done = true in SharedPreferences
    }
}
```

Call `runBackgroundEmbeddingJobIfNeeded()` from `onCreate` in `MainActivity`.

---

## Step 10: MainActivity UI Changes

### New State Variables

```kotlin
var showSettingsScreen by remember { mutableStateOf(false) }
var showChatScreen by remember { mutableStateOf(false) }
var chatInput by remember { mutableStateOf("") }
var isChatLoading by remember { mutableStateOf(false) }
var chatError by remember { mutableStateOf<String?>(null) }
var lastUserMessage by remember { mutableStateOf<String?>(null) }
```

### Top App Bar Changes

Add two icon buttons to the existing top app bar:
- Gear icon (`Icons.Default.Settings`): opens Settings screen
- Chat icon (`Icons.Default.Chat`): opens Chat screen

### New Composables

**SettingsScreen(settingsManager, onBack)**
- Masked API key input with show/hide toggle
- Save button with Snackbar confirmation
- Clear button with Snackbar confirmation
- BackHandler for back press

**ChatScreen(messages, input, isLoading, onSend, onNewConversation, onBack)**
- LazyColumn for message list, auto-scrolls to bottom on new message
- UserMessageBubble(message): right-aligned
- AssistantMessageBubble(message): left-aligned with SourcesSection below
- SourcesSection(sourceIds, sourceTypes): small text showing source previews
- TypingIndicator(): three-dot animation, shown when isLoading = true
- ChatInputBar(input, isLoading, onSend): text field + send button
- NoApiKeyBanner(onGoToSettings): shown when key is missing
- NewConversationDialog: confirmation before clearing history

### Full Chat Send Flow

```
User taps Send
  1. Check API key via settingsManager.getApiKey()
     If null: show NoApiKeyBanner, stop

  2. Check connectivity via ConnectivityManager
     If offline: show Snackbar, stop

  3. Truncate input to 500 chars if needed

  4. Save user message to DB via databaseManager.insertChatMessage()
     Set lastUserMessage = input, clear input field

  5. Set isChatLoading = true

  6. Launch coroutine on IO dispatcher:

     a. Generate question embedding:
        openRouterService.generateEmbedding(question, apiKey)
        On failure: set error Snackbar, set isChatLoading = false, stop

     b. Retrieve relevant items:
        retrieveRelevantItems(questionEmbedding, databaseManager)

     c. Get recent conversation history:
        databaseManager.getRecentChatMessages(limit = 20)

     d. Call chat API:
        openRouterService.chat(question, history, retrievedItems, apiKey)
        On failure: set error Snackbar, set isChatLoading = false, stop

     e. On success:
        Save assistant message with sourceIds and sourceTypes
        databaseManager.insertChatMessage(assistantMessage)
        Set isChatLoading = false
```

### Embedding on Item Save

After saving any new item (text, image, link) in `MainActivity`, launch a
coroutine on IO dispatcher to generate and store the embedding:

```kotlin
lifecycleScope.launch(Dispatchers.IO) {
    val apiKey = settingsManager.getApiKey() ?: return@launch
    val text = extractTextForEmbedding(newItem)  // based on content type
    if (text.isNullOrBlank()) return@launch
    val result = openRouterService.generateEmbedding(text, apiKey)
    result.onSuccess { embedding ->
        val json = EmbeddingUtils.serializeEmbedding(embedding)
        databaseManager.updateXxxItemEmbedding(newItem.id, json)
    }
    // On failure: silently skip, background job will retry
}
```

---

## Database Version Summary

| Version | Changes |
|---|---|
| 1 | Baseline: TextItemEntity, ImageItemEntity, LinkItemEntity |
| 2 | Feature 2: TagEntity, three CrossRef tables |
| 3 | Feature 3: embedding column on item entities, ChatMessageEntity table |
