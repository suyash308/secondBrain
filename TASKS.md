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

## Feature 3: Claude API Summarization

### Task 3.1 -- Entities, DAOs, and Migration
**Scope:** Add summary column to all three entity classes. Add updateSummary DAO
method to all three DAOs. Add MIGRATION_2_3 to AppDatabase and bump version to 3.
**Files:** `TextItemEntity.kt`, `ImageItemEntity.kt`, `LinkItemEntity.kt`,
`TextItemDao.kt`, `ImageItemDao.kt`, `LinkItemDao.kt`, `AppDatabase.kt`
**Done when:** All three entities have a nullable summary field. All three DAOs have
updateSummary. Database version is 3. App does not crash on a device with version 2.
- [ ] Done

---

### Task 3.2 -- SettingsManager
**Scope:** Create SettingsManager with saveApiKey, getApiKey, clearApiKey using
EncryptedSharedPreferences. security-crypto dependency is already in build.gradle.
**Files:** Create `data/SettingsManager.kt`
**Done when:** File exists, compiles, and correctly stores and retrieves a test value
using EncryptedSharedPreferences.
- [ ] Done

---

### Task 3.3 -- ClaudeApiService
**Scope:** Create ClaudeApiService with summarize() using HttpURLConnection and Gson.
No new network dependency.
**Files:** Create `data/ClaudeApiService.kt`
**Done when:** File exists, compiles, sends correct request body per TECHNICAL_DESIGN.md,
parses content[0].text from response, returns Result.success or Result.failure.
- [ ] Done

---

### Task 3.4 -- DatabaseManager Summary Methods
**Scope:** Add updateTextItemSummary, updateImageItemSummary, updateLinkItemSummary
to DatabaseManager.
**Files:** `DatabaseManager.kt`
**Done when:** All three methods exist, run on IO dispatcher, and delegate to the
correct DAO updateSummary method. App compiles.
- [ ] Done

---

### Task 3.5 -- Settings Screen and Gear Icon
**Scope:** Add SettingsScreen composable and gear icon to the top app bar.
**Files:** `MainActivity.kt`
**Done when:** Gear icon visible in top bar. Tapping opens SettingsScreen. User can
enter, save, and clear API key. Confirmation Snackbar shown on save and clear.
Back navigation works via back button and BackHandler.
- [ ] Done

---

### Task 3.6 -- Summarize Button, Summary Card, and Full Flow
**Scope:** Add SummarizeButton and SummaryCard composables. Wire up the complete
summarization flow including all error states as specified in TECHNICAL_DESIGN.md.
**Files:** `MainActivity.kt`
**Done when:** All acceptance criteria for Feature 3 in REQUIREMENTS.md are met.
Verify each criterion one by one before marking done.
- [ ] Done

---

## Progress Summary

| Feature | Tasks | Done |
|---|---|---|
| Feature 1: Delete and Edit | 6 | 6 |
| Feature 2: Tags | 7 | 7 |
| Feature 3: Summarization | 6 | 0 |
| **Total** | **19** | **13** |

Update this table as tasks are completed.
