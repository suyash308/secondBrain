# Second Brain - Android App

A powerful Android application that serves as your personal digital second brain, helping you capture, organize, and search through text, images, and links with intelligent OCR capabilities.

## 🧠 Features

### 📱 Core Functionality
- **Share Sheet Integration**: Appears in Android's share menu for easy content capture
- **Content Categorization**: Automatically categorizes shared content into Text, Images, and Links
- **Real-time Search**: Search across all content types with instant results
- **Persistent Storage**: All data stored locally using Room database

### 📄 Text Management
- Capture and store text content from any app
- Full-text search capabilities
- Clean, organized display with timestamps

### 🖼️ Image Management
- **OCR Integration**: Extract text from images using Google ML Kit (offline, no internet required)
- **Image Persistence**: Store images locally for offline access
- **Searchable Images**: Find images by their extracted text content (only images with readable text are searchable)
- **Full-screen Viewer**: Tap images to view in full-screen with zoom and swipe-to-close support
- **Gallery Upload**: Add images directly from device gallery

### 🔗 Link Management
- **Metadata Extraction**: Automatically fetch title, description, and preview images
- **Rich Link Previews**: Display link metadata in organized cards
- **Browser Integration**: Tap links to open in default browser
- **Searchable Content**: Search through link titles, descriptions, and URLs

### 🔍 Advanced Search
- **Cross-content Search**: Search across text, images, and links simultaneously
- **Real-time Results**: Instant search results as you type
- **Smart Filtering**: Intelligent matching with case-insensitive search
- **Rich Results Display**: Different UI for each content type

## 🛠️ Technical Stack

### Core Technologies
- **Kotlin**: Primary programming language
- **Jetpack Compose**: Modern UI framework
- **Room Database**: Local data persistence
- **Flow**: Reactive data streams
- **Coroutines**: Asynchronous programming

### Key Libraries
- **Google ML Kit** `16.0.1`: Offline OCR text recognition (16KB page-size compatible)
- **Coil**: Image loading and caching
- **Jsoup**: Web scraping for link metadata
- **Gson**: JSON serialization
- **Material 3**: Modern design system

### Architecture
- **Single-Activity**: All UI in `MainActivity` using Jetpack Compose
- **Repository Pattern**: Data access abstraction via `DatabaseManager`
- **Flow-based UI**: Reactive UI updates driven by Room Flows
- **Room Database**: SQLite with type safety

## 📋 Prerequisites

- Android Studio Hedgehog or later
- Android SDK 28+ (API level 28)
- Kotlin 1.9+
- Minimum Android version: API 28 (Android 9.0)

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
2. Use the "Upload Image" button to add images from gallery
3. Content is processed and made searchable via OCR

### Searching Content
1. Open the app
2. Use the search bar at the top
3. Type your search query
4. Results appear instantly across all content types

> **Note:** Images are only searchable if OCR successfully extracted text from them. Images without readable text will not appear in search results.

### Viewing Content
- **Text Items**: Tap the "Text" card to view all text content
- **Image Items**: Tap the "Image" card to view all images
- **Link Items**: Tap the "Link" card to view all links

### Image Interaction
- Tap any image thumbnail to open full-screen viewer
- Use pinch gestures to zoom in/out
- Swipe down to close the viewer
- Double-tap to toggle 2× zoom

## 🏗️ Project Structure

```
app/src/main/java/com/example/secondbrain/
├── MainActivity.kt                 # Main activity with all UI logic
├── data/
│   ├── AppDatabase.kt             # Room database configuration
│   ├── DatabaseManager.kt         # Central database access point
│   ├── entities/                  # Room entity classes
│   │   ├── TextItemEntity.kt
│   │   ├── ImageItemEntity.kt
│   │   └── LinkItemEntity.kt
│   ├── dao/                       # Data Access Objects
│   │   ├── TextItemDao.kt
│   │   ├── ImageItemDao.kt
│   │   └── LinkItemDao.kt
│   ├── repository/                # Repository classes
│   │   ├── TextItemRepository.kt
│   │   ├── ImageItemRepository.kt
│   │   └── LinkItemRepository.kt
│   └── mapper/                    # Data mapping utilities
│       └── DataMapper.kt
└── ui/theme/                      # UI theme and styling
    ├── Color.kt
    ├── Theme.kt
    └── Type.kt
```

## 🔧 Configuration

### Permissions
The app requires the following permissions:
- `INTERNET`: For fetching link metadata
- `READ_MEDIA_IMAGES`: For accessing gallery images on Android 13+ (API 33+)
- `READ_EXTERNAL_STORAGE`: For accessing gallery images on Android 9–12 (API 28–32)

### Build Configuration
- **Target SDK**: 36 (Android 15)
- **Minimum SDK**: 28 (Android 9.0)
- **Compile SDK**: 36

### 16KB Page Size
This app targets Android 15+ and uses ML Kit `16.0.1`, which ships native libraries aligned at 16KB boundaries. This is required for all apps submitted to Google Play targeting Android 15+.

## 🎨 UI Components

### Main Dashboard
- **Search Bar**: Full-width search with rounded corners
- **Category Cards**: Animated counters for each content type
- **Upload Button**: Direct image upload from gallery

### Search Results
- **Unified Results**: All content types in one view
- **Rich Previews**: Different layouts for each content type
- **Interactive Elements**: Clickable images and links

### Content Lists
- **Organized Display**: Clean, card-based layouts
- **Timestamps**: When content was added
- **Quick Actions**: Tap to view or open content

## 🔍 Search Implementation

### Search Logic
```kotlin
// Text search
val filteredTextItems = textItems.filter { item ->
    if (item is TextItem) {
        !item.content.isNullOrEmpty() &&
        item.content.contains(query, ignoreCase = true)
    } else false
}

// Image search (OCR text only)
val filteredImageItems = imageItems.filter { item ->
    !item.extractedText.isNullOrEmpty() &&
    item.extractedText.contains(query, ignoreCase = true)
}

// Link search
val filteredLinkItems = linkItems.filter { item ->
    (!item.title.isNullOrEmpty() && item.title.contains(query, ignoreCase = true)) ||
    (!item.description.isNullOrEmpty() && item.description.contains(query, ignoreCase = true)) ||
    (!item.url.isNullOrEmpty() && item.url.contains(query, ignoreCase = true))
}
```

## 🗄️ Database Schema

### TextItemEntity
- `id`: Primary key (auto-generated)
- `content`: Text content
- `timestamp`: When added

### ImageItemEntity
- `id`: Primary key (auto-generated)
- `originalUri`: Original image URI
- `localPath`: Local file path
- `extractedText`: OCR extracted text (nullable)
- `timestamp`: When added

### LinkItemEntity
- `id`: Primary key (auto-generated)
- `url`: Link URL
- `title`: Page title (nullable, fetched asynchronously)
- `description`: Page description (nullable)
- `imageUrl`: Preview image URL (nullable)
- `timestamp`: When added

## 🔄 Data Flow

1. **Content Addition**
   ```
   User shares content → Intent handling → Content categorization → Room DB storage
   ```

2. **OCR Processing**
   ```
   Image added → Background OCR (offline) → extractedText updated in DB → UI recomposes
   ```

3. **Link Metadata**
   ```
   URL saved → Background Jsoup fetch → title/description/imageUrl updated in DB → UI recomposes
   ```

4. **Search Flow**
   ```
   User types query → In-memory filter on Flow data → UI update
   ```

5. **UI Updates**
   ```
   Room DB change → Flow emission → collectAsState → Compose recomposition
   ```

## 🚀 Performance Features

- **Reactive UI**: Room Flow streams drive all UI state — no manual refresh needed
- **Efficient Search**: In-memory filtering on already-loaded Flow data
- **Image Optimization**: Coil for list thumbnails; bitmap downsampling (max 1920×1080) in full-screen viewer to prevent OOM
- **Background Processing**: OCR and link metadata fetched on IO dispatcher
- **Memory Management**: Proper lifecycle management via `lifecycleScope`

## 🐛 Troubleshooting

### Common Issues

1. **App not appearing in share sheet**
   - Ensure the app is installed and has been launched at least once
   - Only `text/plain` and `image/*` MIME types are supported

2. **Images not loading**
   - Verify storage permissions are granted in device Settings
   - Check that the image file exists in internal storage

3. **Search not working**
   - Ensure the database has been initialized (open the app at least once)
   - Check that content has been added successfully

4. **OCR not extracting text**
   - Verify the image contains clear, readable text
   - OCR runs fully offline — no internet connection required
   - Only Latin script is recognized (English and similar languages)

5. **Link preview not showing title/image**
   - Ensure an internet connection is available when saving the link
   - Some sites block metadata scraping; the raw URL will be shown as fallback

### Debug Logging
The app includes debug logging. Check logcat (filter by `DEBUG`) for:
- `DEBUG: OCR completed successfully!`
- `DEBUG: Image metadata updated successfully in database`
- `DEBUG: Link metadata updated successfully in database`

## 🤝 Contributing

1. Fork the repository
2. Create a feature branch
3. Make your changes
4. Add tests if applicable
5. Submit a pull request

## 📄 License

This project is licensed under the MIT License - see the LICENSE file for details.

## 🙏 Acknowledgments

- Google ML Kit for OCR capabilities
- Jetpack Compose team for the UI framework
- Room database team for persistence layer
- Material Design team for design system

## 📞 Support

For support and questions:
- Create an issue in the repository
- Check the troubleshooting section
- Review the debug logs for specific errors

---

**Second Brain** - Your digital memory, organized and searchable. 🧠✨
