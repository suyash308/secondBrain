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

## Feature 1: Delete and Edit

### Task 1.1 -- DAO Delete and Update Methods
**Scope:** Add delete and update methods to TextItemDao, ImageItemDao, LinkItemDao.
**Files:** `TextItemDao.kt`, `ImageItemDao.kt`, `LinkItemDao.kt`
**Done when:** All three DAOs have delete methods. TextItemDao and LinkItemDao also
have update methods. App compiles.
- [ ] Done

---

### Task 1.2 -- Repository Delete and Update Methods
**Scope:** Add delete and update suspend functions to all three repositories.
ImageItemRepository.delete must also delete the local file at localPath.
**Files:** `TextItemRepository.kt`, `ImageItemRepository.kt`, `LinkItemRepository.kt`
**Done when:** All three repositories have delete methods. Text and Link repositories
have update methods. File deletion logic is in ImageItemRepository.
- [ ] Done

---

### Task 1.3 -- DatabaseManager Delete and Update Methods
**Scope:** Expose delete and update methods in DatabaseManager for all content types.
**Files:** `DatabaseManager.kt`
**Done when:** DatabaseManager has deleteTextItem, updateTextItem, deleteImageItem,
deleteLinkItem, updateLinkItem methods. App compiles.
- [ ] Done

---

### Task 1.4 -- Delete Confirmation Dialog and Bottom Sheet
**Scope:** Add long press handler to item cards. Add ItemOptionsBottomSheet composable.
Add DeleteConfirmationDialog composable. Wire up delete flow end to end.
**Files:** `MainActivity.kt`
**Done when:** Long pressing any item shows the bottom sheet. Tapping Delete shows
the confirmation dialog. Confirming deletes the item. List updates immediately.
- [ ] Done

---

### Task 1.5 -- Edit Text Item Screen
**Scope:** Add EditTextItemScreen composable. Wire up from bottom sheet Edit button
for TextItem only. Validation: block save on empty content.
**Files:** `MainActivity.kt`
**Done when:** Edit opens a full-screen text editor pre-filled with existing content.
Save updates the database. Cancel makes no changes. Empty content shows error.
- [ ] Done

---

### Task 1.6 -- Edit Link Item Screen
**Scope:** Add EditLinkItemScreen composable. Wire up from bottom sheet Edit button
for LinkItem only. Validation: block save on blank or invalid URL. Trigger metadata
re-fetch after save.
**Files:** `MainActivity.kt`
**Done when:** Edit opens a full-screen URL editor. Save updates URL and triggers
background metadata re-fetch. Cancel makes no changes. Invalid URL shows error.
- [ ] Done

---

## Feature 2: Tags

### Task 2.1 -- Tag Entities and CrossRef Classes
**Scope:** Create TagEntity, TextItemTagCrossRef, ImageItemTagCrossRef,
LinkItemTagCrossRef entity classes.
**Files:** Create `data/entities/TagEntity.kt`, `data/entities/TextItemTagCrossRef.kt`,
`data/entities/ImageItemTagCrossRef.kt`, `data/entities/LinkItemTagCrossRef.kt`
**Done when:** All four files exist with correct Room annotations. App compiles.
- [ ] Done

---

### Task 2.2 -- TagDao
**Scope:** Create TagDao with all methods specified in TECHNICAL_DESIGN.md.
**Files:** Create `data/dao/TagDao.kt`
**Done when:** TagDao has insert, query, cross-ref insert/delete, tags-for-item,
item-ids-by-tag, and delete-tags-for-item methods. App compiles.
- [ ] Done

---

### Task 2.3 -- Database Migration for Tags
**Scope:** Add MIGRATION_1_2 to AppDatabase. Register TagEntity, CrossRef tables,
and TagDao. Bump database version.
**Files:** `AppDatabase.kt`
**Done when:** Database version is 2. Migration creates all four new tables.
App installs and launches without crashing on a device that had version 1 installed.
- [ ] Done

---

### Task 2.4 -- DatabaseManager Tag Methods
**Scope:** Add tag operation methods to DatabaseManager. Add ContentType enum.
Update delete methods to call deleteTagsForXxxItem before deleting the item.
**Files:** `DatabaseManager.kt`
**Done when:** All tag methods are exposed. Delete methods clean up cross-ref
entries. App compiles.
- [ ] Done

---

### Task 2.5 -- Tag Chips on Item Cards
**Scope:** Add TagChipRow composable. Display tags on each item card by collecting
tags flow per item. Show maximum 3 chips with "+N more" if needed.
**Files:** `MainActivity.kt`
**Done when:** Tags assigned to items appear as chips on item cards. "+N more"
shown when item has more than 3 tags.
- [ ] Done

---

### Task 2.6 -- Add Tag Bottom Sheet
**Scope:** Add AddTagBottomSheet composable. Wire up from item options bottom sheet.
Implement tag input with suggestions, add button, and remove existing tag option.
Input auto-lowercased, spaces replaced with hyphens.
**Files:** `MainActivity.kt`
**Done when:** User can open the tag sheet, type a tag name, see suggestions, add
a tag, and remove an existing tag from the item. Changes persist after restart.
- [ ] Done

---

### Task 2.7 -- Tag Filter Bar
**Scope:** Add TagFilterBar composable above content list. Wire up activeTagFilter
state. Filter displayed items by active tag combined with any active search query.
**Files:** `MainActivity.kt`
**Done when:** Tag filter bar appears when tags exist. Tapping a tag filters the
list. "All" chip clears the filter. Search works on top of the active filter.
- [ ] Done

---




## Progress Summary

| Feature | Tasks | Done |
|---|---|---|
| Feature 1: Delete and Edit | 6 | 0 |
| Feature 2: Tags | 7 | 0 |
| **Total** | **20** | **0** |

Update this table as tasks are completed.
