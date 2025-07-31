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
- **OCR Integration**: Extract text from images using Google ML Kit
- **Image Persistence**: Store images locally for offline access
- **Searchable Images**: Find images by their extracted text content
- **Full-screen Viewer**: Tap images to view in full-screen with zoom support
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
- **Google ML Kit**: OCR text recognition
- **Coil**: Image loading and caching
- **Jsoup**: Web scraping for link metadata
- **Gson**: JSON serialization
- **Material 3**: Modern design system

### Architecture
- **MVVM Pattern**: Clean architecture with ViewModels
- **Repository Pattern**: Data access abstraction
- **Flow-based UI**: Reactive UI updates
- **Room Database**: SQLite with type safety

## 📋 Prerequisites

- Android Studio Arctic Fox or later
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
3. Content is processed and made searchable

### Searching Content
1. Open the app
2. Use the search bar at the top
3. Type your search query
4. Results appear instantly across all content types

### Viewing Content
- **Text Items**: Tap the "Text" card to view all text content
- **Image Items**: Tap the "Image" card to view all images
- **Link Items**: Tap the "Link" card to view all links

### Image Interaction
- Tap any image thumbnail to open full-screen viewer
- Use pinch gestures to zoom in/out
- Swipe down to close the viewer

## 🏗️ Project Structure

```
app/src/main/java/com/example/secondbrain/
├── MainActivity.kt                 # Main activity with UI logic
├── data/
│   ├── AppDatabase.kt             # Room database configuration
│   ├── DatabaseManager.kt         # Database operations manager
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
- `READ_EXTERNAL_STORAGE`: For accessing gallery images

### Build Configuration
- **Target SDK**: 36 (Android 14)
- **Minimum SDK**: 28 (Android 9.0)
- **Compile SDK**: 36

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

// Image search (OCR text)
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
- `extractedText`: OCR extracted text
- `timestamp`: When added

### LinkItemEntity
- `id`: Primary key (auto-generated)
- `url`: Link URL
- `title`: Page title
- `description`: Page description
- `imageUrl`: Preview image URL
- `timestamp`: When added

## 🔄 Data Flow

1. **Content Addition**
   ```
   User shares content → Intent handling → Content categorization → Database storage
   ```

2. **OCR Processing**
   ```
   Image added → OCR extraction → Text storage → Search indexing
   ```

3. **Search Flow**
   ```
   User types query → Database query → Filter results → UI update
   ```

4. **UI Updates**
   ```
   Database change → Flow emission → UI recomposition → Visual update
   ```

## 🚀 Performance Features

- **Reactive UI**: Flow-based data streams for real-time updates
- **Efficient Search**: Database-level search queries
- **Image Optimization**: Coil for efficient image loading
- **Background Processing**: OCR runs in background threads
- **Memory Management**: Proper lifecycle management

## 🐛 Troubleshooting

### Common Issues

1. **App not appearing in share sheet**
   - Ensure app is installed and launched at least once
   - Check that content type is supported (text, image, link)

2. **Images not loading**
   - Verify storage permissions are granted
   - Check if image file exists in internal storage

3. **Search not working**
   - Ensure database is properly initialized
   - Check that content has been added successfully

4. **OCR not extracting text**
   - Verify image contains readable text
   - Check internet connection for ML Kit models

### Debug Logging
The app includes comprehensive debug logging. Check logcat for:
- `DEBUG: OCR completed successfully!`
- `DEBUG: Image metadata updated successfully`
- `DEBUG: Search results found`

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