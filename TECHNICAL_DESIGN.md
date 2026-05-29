# TECHNICAL_DESIGN.md - Technical Design Document

This file defines how to build each feature. Architecture decisions, file changes,
database migrations, and implementation notes are all documented here.
Claude Code must follow this design. Do not invent alternative approaches.

---

## Current Architecture State

Features 1 and 2 are complete. The codebase now looks like this:

```
app/src/main/java/com/example/secondbrain/
├── MainActivity.kt                 # Single activity, all UI
├── data/
│   ├── AppDatabase.kt             # Room database, current version: 2
│   ├── DatabaseManager.kt         # Single access point for all DB operations
│   ├── entities/
│   │   ├── TextItemEntity.kt
│   │   ├── ImageItemEntity.kt
│   │   ├── LinkItemEntity.kt
│   │   ├── TagEntity.kt           # Added in Feature 2
│   │   ├── TextItemTagCrossRef.kt # Added in Feature 2
│   │   ├── ImageItemTagCrossRef.kt# Added in Feature 2
│   │   └── LinkItemTagCrossRef.kt # Added in Feature 2
│   ├── dao/
│   │   ├── TextItemDao.kt
│   │   ├── ImageItemDao.kt
│   │   ├── LinkItemDao.kt
│   │   └── TagDao.kt              # Added in Feature 2
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

## Feature 1: Delete and Edit -- COMPLETE

No further changes required.

---

## Feature 2: Tags -- COMPLETE

No further changes required.

---

## Feature 3: Claude API Summarization

### Files to Modify
- `TextItemEntity.kt` -- add summary column
- `ImageItemEntity.kt` -- add summary column
- `LinkItemEntity.kt` -- add summary column
- `TextItemDao.kt` -- add updateSummary method
- `ImageItemDao.kt` -- add updateSummary method
- `LinkItemDao.kt` -- add updateSummary method
- `DatabaseManager.kt` -- add updateXxxItemSummary methods
- `AppDatabase.kt` -- add MIGRATION_2_3, bump version to 3
- `MainActivity.kt` -- add Settings screen, Summarize button, summary display

### Files to Create
- `data/ClaudeApiService.kt` -- handles HTTP call to Claude API
- `data/SettingsManager.kt` -- handles EncryptedSharedPreferences for API key

---

### Step 1: Entity Changes

Add to each of the three entity classes:
```kotlin
val summary: String? = null
```

No other changes to entity classes.

---

### Step 2: DAO Changes

Add to TextItemDao, ImageItemDao, and LinkItemDao:
```kotlin
@Query("UPDATE <TableName> SET summary = :summary WHERE id = :id")
suspend fun updateSummary(id: Int, summary: String)
```

Use the correct table name for each DAO:
- TextItemEntity table name: `TextItemEntity`
- ImageItemEntity table name: `ImageItemEntity`
- LinkItemEntity table name: `LinkItemEntity`

---

### Step 3: Database Migration

**Migration from version 2 to version 3**

```kotlin
val MIGRATION_2_3 = object : Migration(2, 3) {
    override fun migrate(database: SupportSQLiteDatabase) {
        database.execSQL("ALTER TABLE TextItemEntity ADD COLUMN summary TEXT")
        database.execSQL("ALTER TABLE ImageItemEntity ADD COLUMN summary TEXT")
        database.execSQL("ALTER TABLE LinkItemEntity ADD COLUMN summary TEXT")
    }
}
```

Register in AppDatabase.kt:
```kotlin
.addMigrations(MIGRATION_1_2, MIGRATION_2_3)
```

Bump database version from 2 to 3.

---

### Step 4: DatabaseManager Changes

Add three suspend functions:
```kotlin
suspend fun updateTextItemSummary(id: Int, summary: String) {
    withContext(Dispatchers.IO) {
        textItemDao.updateSummary(id, summary)
    }
}

suspend fun updateImageItemSummary(id: Int, summary: String) {
    withContext(Dispatchers.IO) {
        imageItemDao.updateSummary(id, summary)
    }
}

suspend fun updateLinkItemSummary(id: Int, summary: String) {
    withContext(Dispatchers.IO) {
        linkItemDao.updateSummary(id, summary)
    }
}
```

---

### Step 5: SettingsManager.kt

Create `data/SettingsManager.kt`:

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
        prefs.edit().putString("claude_api_key", key).apply()
    }

    fun getApiKey(): String? {
        return prefs.getString("claude_api_key", null)
    }

    fun clearApiKey() {
        prefs.edit().remove("claude_api_key").apply()
    }
}
```

---

### Step 6: ClaudeApiService.kt

Create `data/ClaudeApiService.kt`.

Use `java.net.HttpURLConnection` only. Do not add OkHttp or Retrofit.
Use Gson (already in project) for JSON serialization and deserialization.

**Request construction:**
```
POST https://api.anthropic.com/v1/messages
Headers:
  Content-Type: application/json
  x-api-key: {apiKey}
  anthropic-version: 2023-06-01

Body:
{
  "model": "claude-haiku-4-5",
  "max_tokens": 150,
  "system": "You are a concise summarizer. Respond with 2-3 sentences only.",
  "messages": [
    {
      "role": "user",
      "content": "Summarize the following:\n\n{content}"
    }
  ]
}
```

**Response parsing:**
Parse `response.content[0].text` from the JSON response.

**Return type:**
`suspend fun summarize(content: String, apiKey: String): Result<String>`
- Return `Result.success(text)` on HTTP 200 with valid response
- Return `Result.failure(exception)` on any error (network, non-200 status, parse failure)
- Never log the apiKey parameter

---

### Step 7: MainActivity.kt UI Changes

**New State Variables**
```kotlin
var showSettingsScreen by remember { mutableStateOf(false) }
var summarizingItemId by remember { mutableStateOf<Int?>(null) }
var summarizeError by remember { mutableStateOf<String?>(null) }
```

**New Composables**

`SettingsScreen(settingsManager, onBack)`:
- Masked text field for API key with show/hide toggle
- "Save" button: calls settingsManager.saveApiKey(), shows confirmation Snackbar
- "Clear" button: calls settingsManager.clearApiKey(), shows confirmation Snackbar
- Back navigation via onBack callback and BackHandler

`SummaryCard(summary, onRegenerate)`:
- Card with header "AI Summary"
- Body: summary text
- Footer: "Regenerate" TextButton that calls onRegenerate

`SummarizeButton(hasSummary, isLoading, onClick)`:
- If isLoading: show CircularProgressIndicator, disable interaction
- If hasSummary: label "Regenerate Summary"
- If no summary: label "Summarize"

**Gear Icon in Top App Bar**
Add an `IconButton` with `Icons.Default.Settings` to the existing top app bar.
On click: set `showSettingsScreen = true`.

**Summarization Flow**

Wire this up when SummarizeButton is clicked:

```
1. val apiKey = settingsManager.getApiKey()
   If null: show NoApiKeyDialog (two buttons: "Go to Settings", "Cancel")

2. Check connectivity via ConnectivityManager.activeNetworkInfo?.isConnected
   If false: show Snackbar "No internet connection. Summarization requires an internet connection."

3. Determine content string based on item type:
   - TextItem: item.content, truncated to 2000 chars
   - LinkItem: "${item.title} ${item.description}", truncated to 2000 chars
   - ImageItem: item.extractedText
     If null or blank: show message "No text extracted from this image.
     Summarization is not available." Return early, do not call API.

4. Set summarizingItemId = item.id

5. Launch coroutine on IO dispatcher:
   val result = ClaudeApiService().summarize(content, apiKey)

6. On Result.success:
   Call the appropriate DatabaseManager.updateXxxItemSummary(item.id, summaryText)
   Set summarizingItemId = null

7. On Result.failure:
   Set summarizingItemId = null
   Set summarizeError = "Summarization failed. Tap to retry."
   Show Snackbar with retry action that re-triggers step 1
```

---

## Database Version Summary

| Version | Changes |
|---|---|
| 1 | Baseline: TextItemEntity, ImageItemEntity, LinkItemEntity |
| 2 | Feature 2: TagEntity, three CrossRef tables |
| 3 | Feature 3: summary column on all three item entities |
