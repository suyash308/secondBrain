# TASKS.md - Atomic Task List

Each task is small enough for one Claude Code session.
Tasks within a feature must be completed in order.
Do not start a task until all prior tasks in that feature are marked done.
Mark tasks done by changing `[ ]` to `[x]`.

---

## How to Run a Session

1. Open Claude Code in the project root.
2. Say: "Read CLAUDE.md, REQUIREMENTS.md, and TECHNICAL_DESIGN.md before starting."
3. Say: "Implement Task X.Y only. Do not touch anything outside that task's scope."
4. Review the diff. Verify against the acceptance criteria in REQUIREMENTS.md.
5. Run the app on a device or emulator. Confirm the behavior manually.
6. Mark the task done in this file.

---

## Feature 1: Delete and Edit -- COMPLETE

- [x] Task 1.1 -- DAO Delete and Update Methods
- [x] Task 1.2 -- Repository Delete and Update Methods
- [x] Task 1.3 -- DatabaseManager Delete and Update Methods
- [x] Task 1.4 -- Delete Confirmation Dialog and Bottom Sheet
- [x] Task 1.5 -- Edit Text Item Screen
- [x] Task 1.6 -- Edit Link Item Screen

---

## Feature 2: Tags -- COMPLETE

- [x] Task 2.1 -- Tag Entities and CrossRef Classes
- [x] Task 2.2 -- TagDao
- [x] Task 2.3 -- Database Migration for Tags
- [x] Task 2.4 -- DatabaseManager Tag Methods
- [x] Task 2.5 -- Tag Chips on Item Cards
- [x] Task 2.6 -- Add Tag Bottom Sheet
- [x] Task 2.7 -- Tag Filter Bar

---

## Feature 3: RAG Chat Agent

### Task 3.1 -- Entities, DAO, and Migration
**Scope:** Add embedding column to all three item entities. Create
ChatMessageEntity. Create ChatMessageDao. Add MIGRATION_2_3. Register new
entity and DAO in AppDatabase. Bump version to 3.
**Files:** `TextItemEntity.kt`, `ImageItemEntity.kt`, `LinkItemEntity.kt`,
`AppDatabase.kt`. Create `data/entities/ChatMessageEntity.kt`,
`data/dao/ChatMessageDao.kt`.
**Done when:** All three item entities have nullable embedding column.
ChatMessageEntity and ChatMessageDao exist. Database version is 3. App does not
crash on a device with version 2 installed.
- [ ] Done

---

### Task 3.2 -- DAO Embedding Methods
**Scope:** Add updateEmbedding, getAllEmbeddings, and getItemsWithoutEmbedding
methods to TextItemDao, ImageItemDao, and LinkItemDao.
**Files:** `TextItemDao.kt`, `ImageItemDao.kt`, `LinkItemDao.kt`
**Done when:** All three DAOs have the three new methods. App compiles.
- [ ] Done

---

### Task 3.3 -- SettingsManager and EmbeddingUtils
**Scope:** Create SettingsManager for secure API key storage. Create
EmbeddingUtils with cosineSimilarity, parseEmbedding, and serializeEmbedding.
**Files:** Create `data/SettingsManager.kt`, create `data/EmbeddingUtils.kt`
**Done when:** Both files exist and compile. SettingsManager correctly stores
and retrieves a value using EncryptedSharedPreferences.
- [ ] Done

---

### Task 3.4 -- OpenRouterService
**Scope:** Create OpenRouterService with generateEmbedding and chat methods.
HttpURLConnection and Gson only. No new dependencies.
**Files:** Create `data/OpenRouterService.kt`
**Done when:** File exists and compiles. generateEmbedding sends correct request
to /embeddings endpoint and parses data[0].embedding. chat sends correct request
to /chat/completions with system prompt, context block, history, and question,
and parses choices[0].message.content. Both return Result.success or
Result.failure. API key is never logged.
- [ ] Done

---

### Task 3.5 -- DatabaseManager Chat and Embedding Methods
**Scope:** Add all embedding update, embedding query, and chat message methods
to DatabaseManager as specified in TECHNICAL_DESIGN.md Step 7.
**Files:** `DatabaseManager.kt`
**Done when:** All methods exist, run on IO dispatcher, and delegate to correct
DAOs. App compiles.
- [ ] Done

---

### Task 3.6 -- Settings Screen and Gear Icon
**Scope:** Add SettingsScreen composable. Add gear icon to top app bar.
Wire up navigation via showSettingsScreen state variable.
**Files:** `MainActivity.kt`
**Done when:** Gear icon visible in top bar. Tapping opens SettingsScreen. User
can enter, save, and clear API key. Confirmation Snackbar shown on both actions.
Back navigation works.
- [ ] Done

---

### Task 3.7 -- Chat Screen UI (No API Calls Yet)
**Scope:** Build the full Chat screen UI with static/mock data. No real API calls
in this task. Wire up navigation from main screen via chat icon.
**Files:** `MainActivity.kt`
**Done when:** Chat icon visible in top bar. Tapping opens ChatScreen. Empty
state shown when no messages. Message bubbles render correctly for both user and
assistant roles. TypingIndicator composable exists. ChatInputBar renders with
send button. NoApiKeyBanner renders. NewConversationDialog renders. Back
navigation works. All UI driven by state variables, no API calls yet.
- [ ] Done

---

### Task 3.8 -- Embedding on Item Save and Background Job
**Scope:** After any new item is saved in MainActivity, trigger embedding
generation via OpenRouterService and store result via DatabaseManager.
Implement background embedding job that runs once on first launch after update
for all items with null embedding.
**Files:** `MainActivity.kt`
**Done when:** Saving a new text, image, or link item triggers an embedding API
call in the background. The embedding is stored in the database. Background job
runs silently on first launch, generates embeddings for all items that have none,
and sets the done flag in SharedPreferences so it does not run again.
- [ ] Done

---

### Task 3.9 -- Full Chat Send Flow and Retrieval
**Scope:** Wire up the complete send flow: key check, offline check, embed
question, retrieve relevant items via cosine similarity, call chat API, save
response with source metadata, display in UI. Implement all error states.
**Files:** `MainActivity.kt`
**Done when:** All acceptance criteria for Feature 3 in REQUIREMENTS.md are met.
Go through each criterion one by one and confirm before marking done.
- [ ] Done

---

## Progress Summary

| Feature | Tasks | Done |
|---|---|---|
| Feature 1: Delete and Edit | 6 | 6 |
| Feature 2: Tags | 7 | 7 |
| Feature 3: RAG Chat Agent | 9 | 0 |
| **Total** | **22** | **13** |

Update this table as tasks are completed.
