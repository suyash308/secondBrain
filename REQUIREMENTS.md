# REQUIREMENTS.md - Product Requirements Document

This file defines what to build. Every feature scope, UI state, edge case, and
acceptance criterion is documented here. Do not build anything not described in
this file.

---

## Current App State (Baseline)

- Captures text, images, and links via Android share sheet and gallery upload
- Stores all content locally in a Room database
- Searches across all content types in real time
- OCR extracts text from images offline using Google ML Kit
- Link metadata (title, description, preview image) fetched via Jsoup
- No delete, no edit, no tagging, no AI features currently exist

---

## Feature 1: Delete and Edit

### Goal
Allow users to remove unwanted content and correct mistakes in saved items.

### Scope (What Is Included)
- User can delete any single item of any content type (text, image, link)
- User can edit the text content of a TextItem
- User can edit the URL of a LinkItem; after saving the new URL, metadata is
  re-fetched automatically
- Image items support delete only; the image itself cannot be edited

### Scope (What Is NOT Included)
- Bulk delete is not in this iteration
- Editing image files or replacing images is not in this iteration
- Undo after delete is not in this iteration

### Trigger
- Long press on any item card opens a bottom sheet with available actions

### UI States

**Bottom Sheet (appears on long press)**
- Title: "Options"
- Button: "Edit" (shown for TextItem and LinkItem only)
- Button: "Delete" (shown for all content types)
- Button: "Cancel"

**Delete Confirmation Dialog**
- Title: "Delete this item?"
- Body: "This action cannot be undone."
- Buttons: "Cancel" and "Delete"
- On confirm: item is removed from database; list updates automatically via Flow

**Edit Screen for TextItem**
- Full-screen composable (not a dialog)
- Multiline text field pre-filled with existing content
- Top bar with "Cancel" and "Save" buttons
- On Save: content updated in database; navigate back to list
- On Cancel: no changes saved; navigate back to list

**Edit Screen for LinkItem**
- Full-screen composable
- Single-line text field pre-filled with existing URL
- Top bar with "Cancel" and "Save" buttons
- On Save: URL updated in database; metadata re-fetch triggered in background;
  navigate back to list immediately without waiting for metadata
- On Cancel: no changes saved; navigate back to list

### Edge Cases
- Deleting an image item must also delete the local image file from internal storage
- If local image file is missing at delete time, proceed with database deletion anyway
- Empty text content on Save shows a validation error; do not save
- Invalid URL format on Save shows a validation error; do not save
- If metadata re-fetch fails for an edited link, keep the new URL and show empty
  title and description

### Acceptance Criteria
- [ ] Long press on any item shows the bottom sheet
- [ ] Delete confirmation dialog appears before deletion
- [ ] Deleted item disappears from list immediately
- [ ] Deleted image file is removed from internal storage
- [ ] Edited text is reflected in the list immediately after save
- [ ] Edited link URL triggers metadata re-fetch in background
- [ ] Empty content or invalid URL shows a validation error and blocks save
- [ ] Cancel on any edit screen makes no changes

---

## Feature 2: Tags

### Goal
Allow users to label saved items with custom tags and filter content by tag.

### Scope (What Is Included)
- User can add one or more tags to any item (text, image, link)
- User can remove a tag from an item
- Tags are free-form, lowercase strings with no spaces (hyphens allowed)
- User can filter the content list by selecting a tag
- Tags are included in search results (searching a tag name surfaces tagged items)
- Existing tags appear as suggestions when adding a tag to an item

### Scope (What Is NOT Included)
- No global tag management screen (no rename tag, no delete tag globally)
- No tag color customization
- No limit on number of tags per item (reasonable use assumed)
- No tag import or export

### Data Model Changes

**New Table: TagEntity**
```
id        INTEGER PRIMARY KEY AUTOINCREMENT
name      TEXT NOT NULL UNIQUE
```

**New Junction Tables (one per content type)**
```
TextItemTagCrossRef:  textItemId INTEGER, tagId INTEGER, PRIMARY KEY (textItemId, tagId)
ImageItemTagCrossRef: imageItemId INTEGER, tagId INTEGER, PRIMARY KEY (imageItemId, tagId)
LinkItemTagCrossRef:  linkItemId INTEGER, tagId INTEGER, PRIMARY KEY (linkItemId, tagId)
```

**Room Migration**
- Version increment required
- Add all four new tables in the migration

### UI States

**Tag Chips on Item Cards**
- Tags displayed as small chips below the item content preview
- If no tags, no chip row is shown
- Maximum 3 tags shown inline; if more, show "+N more" chip

**Add Tag Bottom Sheet (triggered from item options or a dedicated tag icon)**
- Title: "Add Tag"
- Text input field: placeholder "Enter tag name"
- Input is auto-lowercased and spaces replaced with hyphens on submission
- Suggestion list below input showing all existing tags filtered by input text
- Tap a suggestion to add it instantly
- "Add" button to add the typed tag
- Existing tags on the item shown at top with an X to remove them

**Tag Filter Bar**
- Horizontal scrollable row of tag chips shown above the content list
- Only shown when at least one tag exists in the database
- Tap a tag chip to activate filter; chip style changes to filled/highlighted
- Tap the active tag chip again to deactivate filter
- Only one tag filter active at a time
- "All" chip at the start; always visible; active by default

**Filtered List Behavior**
- When a tag filter is active, show only items that have that tag
- All content types are shown (text, image, link) if they have the tag
- Search bar works on top of the active tag filter (AND logic: tag filter AND search query)

### Edge Cases
- Tag name with only spaces or hyphens is rejected with a validation error
- Adding a duplicate tag to the same item is silently ignored
- Removing the last tag from an item shows no chip row (not an empty row)
- Deleting an item must also delete its junction table entries
- A tag that is no longer assigned to any item remains in the TagEntity table
  (orphan cleanup is not required in this iteration)

### Acceptance Criteria
- [ ] Tags persist after app restart
- [ ] Tag chips appear on item cards
- [ ] Add tag bottom sheet shows existing tag suggestions
- [ ] Duplicate tag on same item is ignored silently
- [ ] Tag filter bar appears when tags exist
- [ ] Filtering by tag shows correct items across all content types
- [ ] Search works together with active tag filter
- [ ] Removing a tag from an item works correctly
- [ ] Deleting an item removes its junction table entries

---
