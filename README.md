# Second Brain - Android App

A powerful Android application that serves as your personal digital second brain, helping you capture, organize, and search through text, images, and links — and chat with your saved content using AI.

## 🧠 Features

### 📱 Core Functionality
- **Share Sheet Integration**: Appears in Android's share menu for easy content capture
- **Content Categorization**: Automatically categorizes shared content into Text, Images, and Links
- **Real-time Search**: Search across all content types including tag names
- **Persistent Storage**: All data stored locally using Room database

### 📄 Text Management
- Capture and store text content from any app
- Full-text search capabilities
- Edit or delete saved text items
- Clean, organized display with timestamps

### 🖼️ Image Management
- **OCR Integration**: Extract text from images using Google ML Kit (offline, no internet required)
- **Image Persistence**: Store images locally for offline access
- **Searchable Images**: Find images by their extracted text content
- **Full-screen Viewer**: Tap images to view in full-screen with zoom and swipe-to-close support
- **Gallery Upload**: Add images directly from device gallery
- Delete saved images (also removes the local file)

### 🔗 Link Management
- **Metadata Extraction**: Automatically fetch title, description, and preview images
- **Rich Link Previews**: Display link metadata in organized cards
- **Browser Integration**: Tap links to open in default browser
- **Searchable Content**: Search through link titles, descriptions, and URLs
- Edit URL and re-fetch metadata; delete saved links

### 🏷️ Tags
- Add one or more tags to any item (text, image, or link)
- Tags are lowercase with hyphens (spaces auto-converted)
- Filter content lists by tag
- Search by tag name — items appear in results even if the tag doesn't match the content
- Tag chips shown on each item card (up to 3, then "+N more")

### 🤖 RAG Chat Agent
- Ask questions in plain English about your saved content
- Answers are grounded in your own notes, images, and links — not general knowledge
- **Vector embeddings** generated automatically when any item is saved
- Retrieves the top 4 most relevant items using cosine similarity
- Multi-turn conversation with history sent to the model
- Multiple named conversation threads — each auto-titled from the first message
- Conversation history persists across app restarts
- Source attribution: every answer lists which saved items were used

### 🔍 Advanced Search
- **Cross-content Search**: Search across text, images, and links simultaneously
- **Tag Search**: Typing a tag name surfaces items tagged with it
- **Real-time Results**: Instant search results as you type
- **Tag + text AND logic**: active tag filter combines with search query

## 🛠️ Technical Stack

### Core Technologies
- **Kotlin**: Primary programming language
- **Jetpack Compose**: Modern UI framework
- **Room Database**: Local data persistence (version 4)
- **Flow**: Reactive data streams
- **Coroutines**: Asynchronous programming

### Key Libraries
- **Google ML Kit** `16.0.1`: Offline OCR text recognition (16KB page-size compatible)
- **Coil**: Image loading and caching
- **Jsoup**: Web scraping for link metadata
- **Gson**: JSON serialization and embedding storage
- **Material 3**: Modern design system
- **AndroidX Security Crypto**: EncryptedSharedPreferences for API key storage

### Architecture
- **Single-Activity**: All UI in `MainActivity` using Jetpack Compose
- **Repository Pattern**: Data access abstraction via `DatabaseManager`
- **Flow-based UI**: Reactive UI updates driven by Room Flows
- **RAG Pipeline**: Embed → Store → Retrieve by cosine similarity → Chat

### AI / API
- **Provider**: OpenRouter (`https://openrouter.ai/api/v1`)
- **Embedding model**: `openai/text-embedding-3-small` (1536 dimensions)
- **Chat model**: `anthropic/claude-haiku-4-5`
- **Transport**: `HttpURLConnection` only — no OkHttp or Retrofit

## 📋 Prerequisites

- Android Studio Hedgehog or later
- Android SDK 28+ (API level 28)
- Kotlin 1.9+
- Minimum Android version: API 28 (Android 9.0)
- An [OpenRouter](https://openrouter.ai) API key (free tier available) to use the Chat feature

## 🚀 Installation

1. **Clone the repository**
   ```bash
   git clone <repository-url>
   cd secondBrain
   ```

2. **Open in Android Studio**
   - Launch Android Studio
   - Open the project folder
   - Wait for Gradle sync to complete

3. **Build and Run**
   - Connect an Android device or start an emulator
   - Click "Run" in Android Studio
   - The app will install and launch automatically

## 📱 Usage Guide

### Adding Content

#### Via Share Sheet
1. Open any app with content you want to save
2. Tap the share button
3. Select "Second Brain" from the share menu
4. Content is automatically categorized and saved

#### Via App Interface
1. Open the Second Brain app
2. Use the **Upload Image** button to add images from gallery
3. Content is processed and made searchable via OCR

### Editing and Deleting
- **Long press** any item card → bottom sheet appears with **Edit**, **Tags**, and **Delete** options
- **Edit** is available for text and link items; images support delete only
- Deleting an image also removes the file from internal storage

### Tags
1. Long press any item → **Tags**
2. Type a tag name and tap **Add** (or tap a suggestion)
3. Existing tags shown at the top with **×** to remove
4. Use the **tag filter bar** above each content list to filter by tag
5. Tags are also searchable from the main search bar

### Searching Content
1. Use the search bar at the top of the main screen
2. Type any query — matches content text, link metadata, OCR text, and tag names
3. Results appear instantly across all content types

> **Note:** Images are only searchable if OCR successfully extracted text from them.

### Viewing Content
- **Text Items**: Tap the "Text" card to view all text content
- **Image Items**: Tap the "Image" card to view all images
- **Link Items**: Tap the "Link" card to view all links

### Image Interaction
- Tap any image thumbnail to open full-screen viewer
- Use pinch gestures to zoom in/out
- Swipe down to close the viewer
- Double-tap to toggle 2× zoom

### Chat (RAG Agent)
1. Tap the **chat icon** (top-right of main screen) to open the conversation list
2. Tap **+** to start a new conversation
3. Type a question about your saved content and tap **Send**
4. The app embeds your question, finds the most relevant saved items, and asks Claude to answer using only your content
5. Each answer shows a **Sources used** section listing which items were referenced
6. Tap any conversation in the list to continue it; long press to delete it

#### First-time Setup
1. Tap the **gear icon** → Settings
2. Paste your [OpenRouter API key](https://openrouter.ai/keys)
3. Tap **Save**

#### How retrieval works
| Step | What happens |
|---|---|
| Item saved | Embedding generated via OpenRouter and stored in DB |
| Question sent | Question is embedded; cosine similarity run against all stored embeddings |
| Retrieval | Top 4 items scoring ≥ 0.3 similarity passed as context to Claude |
| Response | Claude answers using only those items; sources listed below the reply |

## 🏗️ Project Structure

```
app/src/main/java/com/example/secondbrain/
├── MainActivity.kt                  # Single activity — all UI logic
├── data/
│   ├── AppDatabase.kt              # Room database (version 4)
│   ├── DatabaseManager.kt          # Central access point for all DB ops
│   ├── SettingsManager.kt          # Encrypted API key storage
│   ├── OpenRouterService.kt        # Embeddings + chat API calls
│   ├── EmbeddingUtils.kt           # Cosine similarity, serialize/parse
│   ├── entities/
│   │   ├── TextItemEntity.kt       # + embedding column
│   │   ├── ImageItemEntity.kt      # + embedding column
│   │   ├── LinkItemEntity.kt       # + embedding column
│   │   ├── TagEntity.kt
│   │   ├── TextItemTagCrossRef.kt
│   │   ├── ImageItemTagCrossRef.kt
│   │   ├── LinkItemTagCrossRef.kt
│   │   ├── ChatMessageEntity.kt    # + conversationId column
│   │   └── ConversationEntity.kt
│   ├── dao/
│   │   ├── TextItemDao.kt
│   │   ├── ImageItemDao.kt
│   │   ├── LinkItemDao.kt
│   │   ├── TagDao.kt
│   │   ├── ChatMessageDao.kt
│   │   └── ConversationDao.kt
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

## 🔧 Configuration

### Permissions
- `INTERNET`: For link metadata, embeddings, and chat API calls
- `READ_MEDIA_IMAGES`: Gallery access on Android 13+ (API 33+)
- `READ_EXTERNAL_STORAGE`: Gallery access on Android 9–12 (API 28–32, max SDK 32)

### Build Configuration
- **Target SDK**: 36 (Android 15)
- **Minimum SDK**: 28 (Android 9.0)
- **Compile SDK**: 36

### 16KB Page Size
Uses ML Kit `16.0.1`, which ships native libraries aligned at 16KB boundaries. Required for Google Play submissions targeting Android 15+.

### API Key Security
The OpenRouter API key is stored in `EncryptedSharedPreferences` using AES-256-GCM with an Android Keystore-backed master key. It is never logged, never hardcoded, and never transmitted except as an HTTPS header to OpenRouter.

## 🗄️ Database Schema

### Version History
| Version | Changes |
|---|---|
| 1 | Baseline: TextItemEntity, ImageItemEntity, LinkItemEntity |
| 2 | Tags: TagEntity, three CrossRef tables |
| 3 | RAG: embedding column on all item entities, ChatMessageEntity |
| 4 | Multi-thread chat: ConversationEntity, conversationId on ChatMessageEntity |

### Key Entities
- **TextItemEntity**: id, content, timestamp, embedding, summary (nullable)
- **ImageItemEntity**: id, originalUri, localPath, extractedText, timestamp, embedding
- **LinkItemEntity**: id, url, title, description, imageUrl, timestamp, embedding
- **ChatMessageEntity**: id, role, content, timestamp, sourceIds, sourceTypes, conversationId
- **ConversationEntity**: id, title, createdAt

## 🔄 Data Flow

1. **Content Addition**
   ```
   User shares/uploads → Room DB insert (returns real ID) → embedding generated → stored
   ```

2. **OCR Processing**
   ```
   Image saved → Background OCR (offline) → extractedText saved → embedding generated
   ```

3. **Link Metadata**
   ```
   URL saved → Background Jsoup fetch → title/description/imageUrl saved in DB
   ```

4. **RAG Chat**
   ```
   Question typed → embed question → cosine similarity vs all stored embeddings
   → top 4 items retrieved → sent as context to Claude → answer + sources saved
   ```

5. **Background Embedding Job**
   ```
   First launch after install → embed all items with null embedding → set done flag
   ```

## 🚀 Performance
- **Reactive UI**: Room Flow streams; no manual refresh
- **Bitmap downsampling**: Full-screen image viewer caps at 1920×1080 to prevent OOM
- **Background processing**: OCR, link fetch, and embedding on IO dispatcher
- **Silent embedding failures**: Item is always saved; embedding retried by background job

## 🐛 Troubleshooting

1. **App not appearing in share sheet** — Launch the app at least once; only `text/plain` and `image/*` are supported
2. **Images not loading** — Check storage permissions in device Settings
3. **OCR not extracting text** — Image must contain clear Latin-script text; runs offline
4. **Link preview missing** — Some sites block scraping; raw URL shown as fallback
5. **Chat says "could not find"** — Save content about the topic first; embeddings need ~10s after saving
6. **Chat not responding** — Check API key in Settings; verify internet connection
7. **Embeddings not generating for new items** — Ensure API key is saved before adding content

### Debug Logging
Filter logcat by `DEBUG`:
- `DEBUG: OCR completed successfully!`
- `DEBUG: Image embedding stored after OCR`
- `DEBUG: Image metadata updated successfully in database`

## 🤝 Contributing

1. Fork the repository
2. Create a feature branch
3. Make your changes
4. Submit a pull request

## 📄 License

This project is licensed under the MIT License - see the LICENSE file for details.

## 🙏 Acknowledgments

- Google ML Kit for offline OCR
- OpenRouter for unified AI API access
- Jetpack Compose for the UI framework
- Room for local data persistence
- Material Design for the design system

---

**Second Brain** - Your digital memory, organized and searchable. 🧠✨
