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
- Delete and Edit (Feature 1) is COMPLETE
- Tags (Feature 2) is COMPLETE
- Room database is currently at version 2

---

## Feature 1: Delete and Edit -- COMPLETE

All acceptance criteria met. No further work required.

---

## Feature 2: Tags -- COMPLETE

All acceptance criteria met. No further work required.

---

## Feature 3: Claude API Summarization

### Goal
Allow users to generate a short AI summary of any saved item using the Claude API,
turning the app from a storage tool into a thinking tool.

### Scope (What Is Included)
- User can request a summary for any single item (text, image, link)
- Summary is generated once and cached in the database
- User can manually regenerate a summary
- API key is entered once by the user in a Settings screen and stored securely
- Summarization works for all three content types using available text

### Scope (What Is NOT Included)
- Batch summarization of multiple items
- Summarization of image content visually (only OCR-extracted text is used)
- Automatic summarization without user action
- Summarization history or versioning

### Content Sent to API per Content Type
- TextItem: full `content` field, truncated to 2000 characters if longer
- LinkItem: `title` + `description` concatenated, truncated to 2000 characters
- ImageItem: `extractedText` field only, truncated to 2000 characters
  - If `extractedText` is null or empty, show message: "No text extracted from
    this image. Summarization is not available." Do not call the API.

### API Configuration
- Model: `claude-haiku-4-5`
- Max output tokens: 150
- System prompt: "You are a concise summarizer. Respond with 2-3 sentences only."
- User prompt: "Summarize the following:\n\n{content}"
- API endpoint: `https://api.anthropic.com/v1/messages`
- API key header: `x-api-key`
- Anthropic version header: `anthropic-version: 2023-06-01`

### Data Model Changes

**Existing entities get one new nullable column each**
```
TextItemEntity:  summary TEXT (nullable, default null)
ImageItemEntity: summary TEXT (nullable, default null)
LinkItemEntity:  summary TEXT (nullable, default null)
```

**Room Migration: version 2 to version 3**
- ALTER TABLE statements to add the summary column to each entity table

### Settings Screen
- Accessible via a gear icon in the top app bar of MainActivity
- Single field: "Claude API Key" (masked input, show/hide toggle)
- "Save" button stores key in EncryptedSharedPreferences
- "Clear" button removes the stored key
- No other settings in this screen for this iteration

### UI States

**Summarize Button**
- Shown on the detail view of each item (not on the list card)
- Label: "Summarize" if no summary exists
- Label: "Regenerate Summary" if summary already exists

**Loading State**
- Button replaced with a circular progress indicator while API call is in progress
- User cannot trigger another summarization while one is in progress

**Summary Display**
- Shown in a distinct card below the main content
- Header: "AI Summary"
- Body: summary text
- Footer: small "Regenerate" text button

**Error State**
- If API call fails, show a Snackbar: "Summarization failed. Tap to retry."
- Tapping the Snackbar triggers a retry

**No API Key State**
- If no API key is stored, tapping "Summarize" shows a dialog:
  "API key not set. Go to Settings to add your Claude API key."
- Dialog has two buttons: "Go to Settings" and "Cancel"

### Edge Cases
- If device is offline when summarize is tapped, show Snackbar: "No internet
  connection. Summarization requires an internet connection."
- If the API returns an error status code, log the error and show the error Snackbar
- Regenerating a summary overwrites the previous summary in the database
- If the user clears the API key, existing summaries in the database are not deleted

### Acceptance Criteria
- [ ] Settings screen accessible from main screen via gear icon
- [ ] API key saved and retrieved securely via EncryptedSharedPreferences
- [ ] Summarize button appears on item detail view
- [ ] Loading indicator shown during API call
- [ ] Summary displayed and persisted after generation
- [ ] Regenerate overwrites previous summary
- [ ] Error Snackbar shown on API failure with retry option
- [ ] No API key dialog shown when key is not set, with Go to Settings button
- [ ] Offline Snackbar shown when network is unavailable
- [ ] ImageItem with no extracted text shows unavailable message, no API call made
