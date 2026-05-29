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

## Feature 3: RAG Chat Agent

### Goal
Allow users to ask questions in plain English and get answers grounded in their
own saved content. The app retrieves the most relevant saved items using vector
similarity and passes them as context to Claude, which generates a grounded answer.

Example: User saved a photographed travel itinerary, a hotel booking link, and a
text note about flights. They ask "when is my flight to Amsterdam?" and the app
answers from their own saved content.

---

### How It Works (Plain Terms)

1. When an item is saved, the app calls the OpenRouter embeddings API and stores
   a vector representation of the item's text in the database.
2. When the user asks a question in the chat screen, the app embeds the question
   and finds the saved items most similar to it using cosine similarity.
3. The top 4 most relevant items are passed to Claude as context along with the
   conversation history and the question.
4. Claude answers using only the provided context.
5. The conversation is stored in the database so it persists across sessions.

---

### Scope (What Is Included)

- A dedicated Chat screen accessible from the main screen
- Multi-turn conversation (user can ask follow-up questions)
- Retrieval from all three content types (text, image, link)
- Embeddings generated automatically when any item is saved
- One-time background embedding job for all existing items on first launch after update
- OpenRouter API key stored securely in EncryptedSharedPreferences
- Settings screen with API key entry (same screen used for future settings)
- Conversation history persisted in the database
- New conversation button to start fresh
- Source attribution: Claude's answer shows which saved items were used

---

### Scope (What Is NOT Included)

- Multiple named conversations or conversation management
- Exporting conversation history
- Voice input
- Image understanding (only OCR-extracted text from images is used, not visual content)
- Automatic re-embedding when an item is edited (re-embedding is triggered manually
  or on next save; edited items will get a stale embedding until the app is restarted
  and the background job runs again -- acceptable for this iteration)
- Semantic search on the main list (RAG is chat-only)

---

### Content Used for Embeddings per Item Type

| Content Type | Text Sent for Embedding |
|---|---|
| TextItem | Full `content` field, truncated to 2000 chars |
| LinkItem | `title` + " " + `description`, truncated to 2000 chars |
| ImageItem | `extractedText` field, truncated to 2000 chars |

Items with no embeddable text (ImageItem with null extractedText, LinkItem with
null title and null description) are skipped silently. They will not appear in
RAG retrieval results.

---

### API Configuration

**Embeddings**
- Provider: OpenRouter
- Endpoint: `https://openrouter.ai/api/v1/embeddings`
- Model: `openai/text-embedding-3-small`
- Output: 1536-dimensional float array
- Stored as: JSON string in database

**Chat Completions**
- Provider: OpenRouter
- Endpoint: `https://openrouter.ai/api/v1/chat/completions`
- Model: `anthropic/claude-haiku-4-5`
- Max output tokens: 500
- Required headers on every request:
  - `Authorization: Bearer {key}`
  - `Content-Type: application/json`
  - `HTTP-Referer: second-brain-android`

**System Prompt Sent with Every Chat Request**
```
You are a personal assistant with access to the user's saved notes, images, and links.
Answer questions using only the saved items provided below as your source of truth.
If the answer cannot be found in the saved items, say clearly: "I could not find
this in your saved content."
At the end of your answer, always list the saved items you used as sources,
prefixed with "Sources used:".
Keep answers concise and direct.
```

---

### Data Model Changes

**Existing entities get one new nullable column each**
```
TextItemEntity:  embedding TEXT (nullable, default null) -- JSON array of 1536 floats
ImageItemEntity: embedding TEXT (nullable, default null)
LinkItemEntity:  embedding TEXT (nullable, default null)
```

**New Table: ChatMessageEntity**
```
id          INTEGER PRIMARY KEY AUTOINCREMENT
role        TEXT NOT NULL  -- "user" or "assistant"
content     TEXT NOT NULL  -- message text
timestamp   INTEGER NOT NULL
sourceIds   TEXT           -- nullable, JSON array of item IDs used as sources
sourceTypes TEXT           -- nullable, JSON array of content types matching sourceIds
```

**Room Migration: version 2 to version 3**
- ALTER TABLE to add embedding column to all three item entities
- CREATE TABLE for ChatMessageEntity

---

### Settings Screen

- Accessible via a gear icon in the top app bar of MainActivity
- Single field: "OpenRouter API Key" (masked input, show/hide toggle)
- "Save" button: stores key, shows confirmation Snackbar "API key saved"
- "Clear" button: removes key, shows confirmation Snackbar "API key cleared"
- Back navigation via back button and BackHandler

---

### Chat Screen UI States

**Empty State (no messages yet)**
- Centered illustration or icon
- Text: "Ask anything about your saved content"
- Subtitle: "Your notes, images, and links are your knowledge base"

**Message List**
- User messages: right-aligned bubble, distinct background color
- Assistant messages: left-aligned bubble, different background color
- Each assistant message shows a "Sources" section below the bubble
  listing the titles or previews of items retrieved for that answer
- Timestamps shown below each message

**Input Area**
- Text field at the bottom: placeholder "Ask a question..."
- Send button (arrow icon) to the right
- Send button disabled while a response is loading
- Input field disabled while a response is loading

**Loading State**
- Three-dot typing indicator shown as a temporary assistant bubble
  while the API call is in progress

**New Conversation Button**
- Icon button in the top bar of the Chat screen
- Tapping shows a confirmation dialog: "Start a new conversation? This will
  clear the current chat history."
- On confirm: delete all ChatMessageEntity rows, reset UI to empty state

**No API Key State**
- If no API key is stored when user opens Chat screen, show a banner:
  "No API key set. Add your OpenRouter key in Settings to use chat."
- Banner has a "Go to Settings" button
- Chat input is disabled in this state

**Error State**
- If API call fails, show a Snackbar: "Failed to get a response. Tap to retry."
- Tapping Snackbar re-sends the last user message
- The failed user message remains in the UI; no duplicate is created on retry

**Offline State**
- If device is offline when Send is tapped, show Snackbar:
  "No internet connection. Chat requires an internet connection."
- Do not add the user message to the database if offline check fails

---

### Retrieval Logic

1. Embed the user's question (1 API call)
2. Load all stored embeddings from the database (all three content types)
3. Compute cosine similarity between the question embedding and each item embedding
4. Sort by similarity score descending
5. Take the top 4 items with similarity score above 0.3 (ignore weaker matches)
6. If fewer than 4 items score above 0.3, use however many do
7. If zero items score above 0.3, still send the question to Claude but with a
   note in the context: "No relevant saved items were found."
8. Pass the retrieved items as context in the system message

---

### Context Assembly for Claude

Build the context string from retrieved items as follows:

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

Append this context block to the system prompt before sending.

---

### Conversation History Sent to Claude

Send the last 10 message pairs (20 messages total) to stay within token limits.
Older messages are stored in the database but not sent to the API.

---

### Background Embedding Job

On first app launch after this feature is installed:
- Check which existing items have a null embedding column
- Generate embeddings for all such items in batches of 10
- Run entirely on IO dispatcher, no UI blocking
- Show no UI indicator; this is silent background work
- If any individual item embedding fails, skip it and continue
- Items that fail will be retried on the next app launch

---

### Edge Cases

- If embedding generation fails when saving a new item, save the item anyway
  without an embedding. It will be retried by the background job.
- If the API key is cleared while a chat request is in flight, let the in-flight
  request complete normally.
- If the user sends a very long message (over 500 chars), truncate it to 500
  chars before embedding and sending to Claude.
- Deleting a saved item does not delete chat messages that cited it as a source.
  The source label will still show but the item will no longer be retrievable.
- New conversation clears all ChatMessageEntity rows. This is permanent with
  a confirmation dialog. No undo.

---

### Acceptance Criteria

- [ ] Settings screen accessible from main screen via gear icon
- [ ] OpenRouter API key saved and retrieved securely
- [ ] Embedding generated and stored when a new item is saved
- [ ] Background embedding job runs silently on first launch after update
- [ ] Chat screen accessible from main screen
- [ ] User can type a question and receive a grounded answer
- [ ] Answer references content from saved items
- [ ] Sources section shown below each assistant message
- [ ] Multi-turn conversation works (follow-up questions use prior context)
- [ ] Conversation persists after app restart
- [ ] New conversation clears history after confirmation
- [ ] No API key banner shown with Go to Settings button when key is missing
- [ ] Loading indicator shown while response is generating
- [ ] Error Snackbar shown on API failure with retry
- [ ] Offline Snackbar shown when network unavailable
- [ ] Items with no embeddable text are skipped silently
