package com.example.secondbrain

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.background
import androidx.compose.foundation.Image
import androidx.compose.foundation.gestures.rememberTransformableState
import androidx.compose.foundation.gestures.transformable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.graphics.graphicsLayer
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.example.secondbrain.ui.theme.SecondBrainTheme
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlin.math.roundToInt
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import androidx.lifecycle.lifecycleScope
import java.io.File
import java.io.FileOutputStream
import java.io.InputStream
import android.graphics.BitmapFactory
import android.graphics.Bitmap

class MainActivity : ComponentActivity() {
    private val PREFS_NAME = "SecondBrainPrefs"
    private val TEXT_COUNT_KEY = "text_count"
    private val IMAGE_COUNT_KEY = "image_count"
    private val LINK_COUNT_KEY = "link_count"
    private val TEXT_ITEMS_KEY = "text_items"
    private val IMAGE_ITEMS_KEY = "image_items"
    private val LINK_ITEMS_KEY = "link_items"
    private val IMAGE_METADATA_KEY = "image_metadata"
    private val gson = Gson()
    private val textRecognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)

    data class ImageItem(
        val originalUri: String? = null,
        val localPath: String? = null,
        val extractedText: String? = null,
        val timestamp: Long = System.currentTimeMillis()
    )

    data class TextItem(
        val content: String? = null,
        val timestamp: Long = System.currentTimeMillis()
    )

    data class LinkItem(
        val url: String? = null,
        val timestamp: Long = System.currentTimeMillis()
    )

    private fun copyImageToInternalStorage(uri: Uri): String? {
        return try {
            val inputStream: InputStream? = contentResolver.openInputStream(uri)
            val fileName = "image_${System.currentTimeMillis()}.jpg"
            val file = File(filesDir, fileName)
            
            inputStream?.use { input ->
                FileOutputStream(file).use { output ->
                    input.copyTo(output)
                }
            }
            
            // Verify file was created and has content
            if (file.exists() && file.length() > 0) {
                val fileUri = "file://${file.absolutePath}"
                println("DEBUG: Image copied successfully to $fileUri (${file.length()} bytes)")
                fileUri
            } else {
                println("DEBUG: Failed to copy image - file doesn't exist or is empty")
                null
            }
        } catch (e: Exception) {
            println("DEBUG: Error copying image: ${e.message}")
            null
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        
        // Handle shared content only if it's a share intent
        if (intent?.action == Intent.ACTION_SEND) {
            handleSharedContent(intent)
        }
        
        setContent {
            SecondBrainTheme {
                CategoryCounter()
            }
        }
    }

    override fun onNewIntent(intent: Intent?) {
        super.onNewIntent(intent)
        // Handle shared content when app is already running
        if (intent?.action == Intent.ACTION_SEND) {
            handleSharedContent(intent)
        }
    }

    private fun handleSharedContent(intent: Intent?) {
        if (intent?.action == Intent.ACTION_SEND) {
            when (intent.type) {
                "text/plain" -> {
                    val sharedText = intent.getStringExtra(Intent.EXTRA_TEXT)
                    if (!sharedText.isNullOrEmpty()) {
                        if (isUrl(sharedText)) {
                            addToCategory(LINK_COUNT_KEY, LINK_ITEMS_KEY, LinkItem(sharedText))
                        } else {
                            addToCategory(TEXT_COUNT_KEY, TEXT_ITEMS_KEY, TextItem(sharedText))
                        }
                    }
                }
                "image/*", "image/jpeg", "image/png", "image/gif", "image/webp" -> {
                    val imageUri = intent.getParcelableExtra<Uri>(Intent.EXTRA_STREAM)
                    if (imageUri != null) {
                        // Copy image to internal storage
                        val localPath = copyImageToInternalStorage(imageUri)
                        val imageItem = ImageItem(
                            originalUri = imageUri.toString(),
                            localPath = localPath ?: ""
                        )
                        addToCategory(IMAGE_COUNT_KEY, IMAGE_ITEMS_KEY, imageItem)
                        
                        // Start OCR processing with local path if available
                        if (localPath != null) {
                            // Extract the actual file path from the file:// URI
                            val filePath = localPath.removePrefix("file://")
                            val file = File(filePath)
                            println("DEBUG: Starting OCR with local file: ${file.absolutePath}")
                            processImageWithOCR(file)
                        } else {
                            println("DEBUG: Starting OCR with original URI: $imageUri")
                            processImageWithOCR(imageUri)
                        }
                    }
                }
            }
        }
    }

    private fun isUrl(text: String): Boolean {
        return text.startsWith("http://") || text.startsWith("https://") || 
               text.startsWith("www.") || text.contains("://")
    }

    private fun addToCategory(countKey: String, itemsKey: String, item: Any) {
        val prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE)
        
        // Increment count
        val currentCount = prefs.getInt(countKey, 0)
        prefs.edit().putInt(countKey, currentCount + 1).apply()
        
        // Add to items list
        val itemsJson = prefs.getString(itemsKey, "[]")
        val type = when (item) {
            is TextItem -> object : TypeToken<List<TextItem>>() {}.type
            is ImageItem -> object : TypeToken<List<ImageItem>>() {}.type
            is LinkItem -> object : TypeToken<List<LinkItem>>() {}.type
            else -> object : TypeToken<List<String>>() {}.type
        }
        val items: MutableList<Any> = gson.fromJson(itemsJson, type) ?: mutableListOf()
        items.add(item)
        prefs.edit().putString(itemsKey, gson.toJson(items)).apply()
    }

    private fun processImageWithOCR(imageInput: Any) {
        println("DEBUG: processImageWithOCR called with: ${imageInput::class.simpleName} = $imageInput")
        lifecycleScope.launch(Dispatchers.IO) {
            try {
                val image = when (imageInput) {
                    is File -> {
                        println("DEBUG: Processing file: ${imageInput.absolutePath}")
                        println("DEBUG: File exists: ${imageInput.exists()}")
                        println("DEBUG: File size: ${imageInput.length()} bytes")
                        InputImage.fromFilePath(this@MainActivity, Uri.fromFile(imageInput))
                    }
                    is Uri -> {
                        println("DEBUG: Processing URI: $imageInput")
                        InputImage.fromFilePath(this@MainActivity, imageInput)
                    }
                    else -> {
                        println("DEBUG: Unknown image input type: ${imageInput::class.simpleName}")
                        return@launch
                    }
                }
                
                println("DEBUG: InputImage created successfully")
                println("DEBUG: Starting OCR processing...")
                
                textRecognizer.process(image)
                    .addOnSuccessListener { result ->
                        val extractedText = result.text
                        println("DEBUG: OCR completed successfully!")
                        println("DEBUG: Extracted text length: ${extractedText.length}")
                        println("DEBUG: Extracted text: '$extractedText'")
                        
                        if (extractedText.isNotEmpty()) {
                            // Update the image item with extracted text
                            // We need to match against the original URI that was saved
                            val imageUri = when (imageInput) {
                                is File -> {
                                    // For File, we need to find the matching ImageItem by localPath
                                    val fileUri = "file://${imageInput.absolutePath}"
                                    println("DEBUG: File input, looking for localPath: $fileUri")
                                    fileUri
                                }
                                is Uri -> {
                                    // For Uri, use the original URI
                                    println("DEBUG: URI input, looking for originalUri: $imageInput")
                                    imageInput.toString()
                                }
                                else -> return@addOnSuccessListener
                            }
                            println("DEBUG: Updating image with URI: $imageUri")
                            updateImageWithExtractedText(imageUri, extractedText)
                            
                            println("DEBUG: OCR completed and data saved successfully")
                        } else {
                            println("DEBUG: No text extracted from image - text is empty")
                        }
                    }
                    .addOnFailureListener { e ->
                        println("DEBUG: OCR failed with error: ${e.message}")
                        println("DEBUG: Error type: ${e::class.simpleName}")
                        e.printStackTrace()
                    }
            } catch (e: Exception) {
                println("DEBUG: Error in OCR processing: ${e.message}")
                e.printStackTrace()
            }
        }
    }

    private fun updateImageWithExtractedText(imageUri: String, extractedText: String) {
        val prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE)
        val itemsJson = prefs.getString(IMAGE_ITEMS_KEY, "[]")
        val type = object : TypeToken<List<ImageItem>>() {}.type
        val items: MutableList<ImageItem> = gson.fromJson(itemsJson, type) ?: mutableListOf()
        
        // Find and update the image item
        println("DEBUG: Looking for image with URI: $imageUri")
        println("DEBUG: Current items count: ${items.size}")
        items.forEachIndexed { index, item ->
            println("DEBUG: Item $index - originalUri: '${item.originalUri}', localPath: '${item.localPath}'")
        }
        
        val updatedItems = items.map { item ->
            if (item.originalUri == imageUri || item.localPath == imageUri) {
                println("DEBUG: Found matching image item, updating with OCR text: '$extractedText'")
                item.copy(extractedText = extractedText)
            } else {
                item
            }
        }
        
        // Check if any item was updated
        val wasUpdated = updatedItems.any { it.extractedText == extractedText }
        println("DEBUG: Was any item updated? $wasUpdated")
        
        prefs.edit().putString(IMAGE_ITEMS_KEY, gson.toJson(updatedItems)).apply()
        println("DEBUG: Image metadata updated successfully")
    }

    @Composable
    fun CategoryCounter() {
        val prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE)
        var textCount by remember { mutableStateOf(prefs.getInt(TEXT_COUNT_KEY, 0)) }
        var imageCount by remember { mutableStateOf(prefs.getInt(IMAGE_COUNT_KEY, 0)) }
        var linkCount by remember { mutableStateOf(prefs.getInt(LINK_COUNT_KEY, 0)) }
        var currentScreen by remember { mutableStateOf("main") }
        var searchQuery by remember { mutableStateOf("") }
        var isSearching by remember { mutableStateOf(false) }
        var refreshTrigger by remember { mutableStateOf(0) }
        var ocrUpdateTrigger by remember { mutableStateOf(0L) }
        var forceRefresh by remember { mutableStateOf(0) }
        var selectedImageItem by remember { mutableStateOf<ImageItem?>(null) }

        // Function to refresh data
        fun refreshData() {
            println("DEBUG: refreshData() called")
            println("DEBUG: Reading from SharedPreferences - TEXT_COUNT_KEY: ${prefs.getInt(TEXT_COUNT_KEY, 0)}")
            println("DEBUG: Reading from SharedPreferences - IMAGE_COUNT_KEY: ${prefs.getInt(IMAGE_COUNT_KEY, 0)}")
            println("DEBUG: Reading from SharedPreferences - LINK_COUNT_KEY: ${prefs.getInt(LINK_COUNT_KEY, 0)}")
            
            textCount = prefs.getInt(TEXT_COUNT_KEY, 0)
            imageCount = prefs.getInt(IMAGE_COUNT_KEY, 0)
            linkCount = prefs.getInt(LINK_COUNT_KEY, 0)
            refreshTrigger++ // Trigger recomposition
            println("DEBUG: refreshData() completed - textCount: $textCount, imageCount: $imageCount, linkCount: $linkCount, refreshTrigger: $refreshTrigger")
            
            // Check if there's actual data in the items
            val textItems = getItems(prefs, TEXT_ITEMS_KEY)
            val imageItems = getItems(prefs, IMAGE_ITEMS_KEY)
            val linkItems = getItems(prefs, LINK_ITEMS_KEY)
            println("DEBUG: Actual items count - Text: ${textItems.size}, Image: ${imageItems.size}, Link: ${linkItems.size}")
        }

        // Update counts when app becomes active
        LaunchedEffect(Unit) {
            println("DEBUG: LaunchedEffect(Unit) triggered - loading data from SharedPreferences")
            refreshData()
        }

        // Refresh data when returning to main screen
        LaunchedEffect(currentScreen) {
            if (currentScreen == "main") {
                refreshData()
            }
        }



        Scaffold(
            modifier = Modifier.fillMaxSize()
        ) { innerPadding ->
            if (selectedImageItem != null) {
                FullScreenImageViewer(
                    imageItem = selectedImageItem!!,
                    onClose = { selectedImageItem = null },
                    modifier = Modifier.padding(innerPadding)
                )
            } else {
                when (currentScreen) {
                "main" -> {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(innerPadding)
                            .padding(16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        // Header with refresh button
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(bottom = 24.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Second Brain",
                                fontSize = 28.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )
                            Button(
                                onClick = { 
                                    println("DEBUG: Manual refresh triggered")
                                    refreshData() 
                                },
                                modifier = Modifier.padding(start = 8.dp)
                            ) {
                                Text("Refresh")
                            }
                        }

                        if (isSearching) {
                            // Search Results
                            SearchResults(
                                query = searchQuery,
                                textItems = getItems(prefs, TEXT_ITEMS_KEY),
                                modifier = Modifier.weight(1f)
                            )
                        } else {
                            Text(
                                text = "Your categorized content",
                                fontSize = 16.sp,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
                                modifier = Modifier.padding(bottom = 32.dp)
                            )

                            // Category Cards
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceEvenly
                            ) {
                                CategoryCard(
                                    title = "Text",
                                    count = textCount,
                                    color = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.weight(1f),
                                    onClick = { currentScreen = "text" }
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                CategoryCard(
                                    title = "Image",
                                    count = imageCount,
                                    color = MaterialTheme.colorScheme.secondary,
                                    modifier = Modifier.weight(1f),
                                    onClick = { 
                                        println("DEBUG: Image folder clicked, currentScreen set to 'image'")
                                        currentScreen = "image" 
                                    }
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                CategoryCard(
                                    title = "Link",
                                    count = linkCount,
                                    color = MaterialTheme.colorScheme.tertiary,
                                    modifier = Modifier.weight(1f),
                                    onClick = { currentScreen = "link" }
                                )
                            }

                            Spacer(modifier = Modifier.height(32.dp))

                            Text(
                                text = "Tap on folders to view your shared content!",
                                fontSize = 14.sp,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                                modifier = Modifier.padding(horizontal = 16.dp),
                                textAlign = TextAlign.Center
                            )

                            Spacer(modifier = Modifier.height(24.dp))

                            Button(
                                onClick = {
                                    prefs.edit()
                                        .putInt(TEXT_COUNT_KEY, 0)
                                        .putInt(IMAGE_COUNT_KEY, 0)
                                        .putInt(LINK_COUNT_KEY, 0)
                                        .putString(TEXT_ITEMS_KEY, "[]")
                                        .putString(IMAGE_ITEMS_KEY, "[]")
                                        .putString(LINK_ITEMS_KEY, "[]")
                                        .apply()
                                    textCount = 0
                                    imageCount = 0
                                    linkCount = 0
                                },
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = MaterialTheme.colorScheme.error
                                )
                            ) {
                                Text("Reset All")
                            }
                        }

                        // Search Bar at the bottom
                        OutlinedTextField(
                            value = searchQuery,
                            onValueChange = { 
                                searchQuery = it
                                isSearching = it.isNotEmpty()
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 16.dp),
                            placeholder = { Text("Search in text and images...") },
                            leadingIcon = { Icon(Icons.Default.Search, contentDescription = "Search") },
                            singleLine = true,
                            shape = RoundedCornerShape(12.dp)
                        )
                    }
                }
                "text" -> {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(innerPadding)
                            .padding(16.dp)
                    ) {
                        // Header with back button
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            IconButton(onClick = { currentScreen = "main" }) {
                                Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                            }
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Text Items",
                                fontSize = 20.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        Spacer(modifier = Modifier.height(16.dp))
                        ContentList(
                            items = getItems(prefs, TEXT_ITEMS_KEY),
                            category = "Text"
                        )
                    }
                }
                "image" -> {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(innerPadding)
                            .padding(16.dp)
                    ) {
                        // Header with back button
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            IconButton(onClick = { currentScreen = "main" }) {
                                Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                            }
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Image Items",
                                fontSize = 20.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        Spacer(modifier = Modifier.height(16.dp))
                        ContentList(
                            items = getItems(prefs, IMAGE_ITEMS_KEY),
                            category = "Image",
                            onImageClick = { imageItem ->
                                selectedImageItem = imageItem
                            }
                        )
                    }
                }
                "link" -> {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(innerPadding)
                            .padding(16.dp)
                    ) {
                        // Header with back button
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            IconButton(onClick = { currentScreen = "main" }) {
                                Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                            }
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Link Items",
                                fontSize = 20.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        Spacer(modifier = Modifier.height(16.dp))
                        ContentList(
                            items = getItems(prefs, LINK_ITEMS_KEY),
                            category = "Link"
                        )
                    }
                }
            }
        }
    }
    }

    private fun getItems(prefs: android.content.SharedPreferences, key: String): List<Any> {
        val itemsJson = prefs.getString(key, "[]")
        val type = when (key) {
            TEXT_ITEMS_KEY -> object : TypeToken<List<TextItem>>() {}.type
            IMAGE_ITEMS_KEY -> object : TypeToken<List<ImageItem>>() {}.type
            LINK_ITEMS_KEY -> object : TypeToken<List<LinkItem>>() {}.type
            else -> object : TypeToken<List<String>>() {}.type
        }
        val items: List<Any> = gson.fromJson(itemsJson, type) ?: listOf()
        
        if (key == IMAGE_ITEMS_KEY) {
            println("DEBUG: Loading ${items.size} image items from SharedPreferences")
            items.forEach { item ->
                if (item is ImageItem) {
                    println("DEBUG: Image item - URI: ${item.originalUri}, LocalPath: ${item.localPath}, HasText: ${!item.extractedText.isNullOrEmpty()}, TextLength: ${item.extractedText?.length ?: 0}")
                }
            }
        }
        
        return items
    }

    @Composable
    fun CategoryCard(
        title: String,
        count: Int,
        color: androidx.compose.ui.graphics.Color,
        modifier: Modifier = Modifier,
        onClick: () -> Unit,
        icon: androidx.compose.ui.graphics.vector.ImageVector? = null
    ) {
        Card(
            modifier = modifier
                .height(120.dp),
            shape = RoundedCornerShape(12.dp),
            colors = CardDefaults.cardColors(
                containerColor = color.copy(alpha = 0.1f)
            ),
            onClick = onClick
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(12.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                if (icon != null) {
                    Icon(
                        imageVector = icon,
                        contentDescription = title,
                        tint = color,
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                }
                Text(
                    text = count.toString(),
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold,
                    color = color
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = title,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium,
                    color = color.copy(alpha = 0.8f)
                )
        }
    }
}

@Composable
    fun ContentList(
        items: List<Any>,
        category: String,
        modifier: Modifier = Modifier,
        onImageClick: ((ImageItem) -> Unit)? = null
    ) {
        println("DEBUG: ContentList called with ${items.size} items for category: $category")
        items.forEachIndexed { index, item ->
            println("DEBUG: Item $index is ${item::class.simpleName}")
            if (item is ImageItem) {
                println("DEBUG: ImageItem $index - URI: ${item.originalUri}, LocalPath: ${item.localPath}, HasText: ${!item.extractedText.isNullOrEmpty()}, TextLength: ${item.extractedText?.length ?: 0}")
                if (!item.extractedText.isNullOrEmpty()) {
                    println("DEBUG: ImageItem $index - Extracted text preview: '${item.extractedText?.take(100)}...'")
                }
            }
        }
        
        if (items.isEmpty()) {
            Column(
                modifier = modifier
                    .fillMaxSize()
                    .padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Text(
                    text = "📁",
                    fontSize = 48.sp,
                    modifier = Modifier.padding(bottom = 16.dp)
                )
                Text(
                    text = "No $category items yet",
                    fontSize = 18.sp,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                )
                Spacer(modifier = Modifier.height(8.dp))
    Text(
                    text = "Share some content to see it here!",
                    fontSize = 14.sp,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f),
                    textAlign = TextAlign.Center
                )
            }
        } else {
            LazyColumn(
        modifier = modifier
                    .fillMaxSize()
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(items.reversed()) { item ->
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(8.dp),
                        onClick = {
                            if (item is ImageItem && onImageClick != null) {
                                println("DEBUG: Image card clicked, opening full-screen viewer")
                                onImageClick(item)
                            }
                        }
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp)
                        ) {
                            when (item) {
                                is ImageItem -> {
                                    // Display image with fallback
                                    val imageData = when {
                                        !item.localPath.isNullOrEmpty() -> item.localPath
                                        !item.originalUri.isNullOrEmpty() -> item.originalUri
                                        else -> ""
                                    }
                                    
                                    if (imageData.isNotEmpty()) {
                                        AsyncImage(
                                            model = ImageRequest.Builder(LocalContext.current)
                                                .data(imageData)
                                                .crossfade(true)
                                                .build(),
                                            contentDescription = "Shared image",
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .height(200.dp),
                                            contentScale = ContentScale.Crop
                                        )
                                    } else {
                                        // Show placeholder if no image data
                                        Box(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .height(200.dp),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Text(
                                                text = "🖼️ No Image Data",
                                                fontSize = 24.sp,
                                                color = MaterialTheme.colorScheme.error
                                            )
                                        }
                                    }
                                    
                                    // OCR text is hidden from UI but still searchable
                                    // The extractedText field remains in the ImageItem model for search functionality
                                }
                                is TextItem -> {
                                    // Display text content with null safety
                                    Text(
                                        text = item.content ?: "No content",
                                        fontSize = 14.sp,
                                        color = MaterialTheme.colorScheme.onSurface,
                                        modifier = Modifier.fillMaxWidth()
                                    )
                                }
                                is LinkItem -> {
                                    // Display link content with null safety
                                    Text(
                                        text = item.url ?: "No URL",
                                        fontSize = 14.sp,
                                        color = MaterialTheme.colorScheme.onSurface,
                                        modifier = Modifier.fillMaxWidth()
                                    )
                                }
                                else -> {
                                    // Fallback for old string format
                                    Text(
                                        text = item.toString(),
                                        fontSize = 14.sp,
                                        color = MaterialTheme.colorScheme.onSurface,
                                        modifier = Modifier.fillMaxWidth()
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    @Composable
    fun FullScreenImageViewer(
        imageItem: ImageItem,
        onClose: () -> Unit,
        modifier: Modifier = Modifier
    ) {
        val imageData = when {
            !imageItem.localPath.isNullOrEmpty() -> {
                if (imageItem.localPath.startsWith("file://")) {
                    imageItem.localPath.removePrefix("file://")
                } else {
                    imageItem.localPath
                }
            }
            !imageItem.originalUri.isNullOrEmpty() -> imageItem.originalUri
            else -> ""
        }
        
        // Use state to hold the bitmap
        var bitmap by remember { mutableStateOf<Bitmap?>(null) }
        var isLoading by remember { mutableStateOf(true) }
        var error by remember { mutableStateOf<String?>(null) }
        
        // Swipe to close state
        var dragOffset by remember { mutableStateOf(Offset.Zero) }
        var isDragging by remember { mutableStateOf(false) }
        
        // Load bitmap in LaunchedEffect
        LaunchedEffect(imageData) {
            if (imageData.startsWith("/")) {
                val file = File(imageData)
                if (file.exists()) {
                    try {
                        val loadedBitmap = BitmapFactory.decodeFile(imageData)
                        if (loadedBitmap != null) {
                            bitmap = loadedBitmap
                            isLoading = false
                        } else {
                            error = "Failed to decode image"
                            isLoading = false
                        }
                    } catch (e: Exception) {
                        error = "Error loading image: ${e.message}"
                        isLoading = false
                    }
                } else {
                    error = "File not found"
                    isLoading = false
                }
            } else {
                error = "Invalid file path"
                isLoading = false
            }
        }
        
        Dialog(
            onDismissRequest = onClose,
            properties = DialogProperties(
                dismissOnBackPress = true,
                dismissOnClickOutside = true,
                usePlatformDefaultWidth = false
            )
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.9f))
                    .offset { IntOffset(dragOffset.x.roundToInt(), dragOffset.y.roundToInt()) }
                    .pointerInput(Unit) {
                        detectDragGestures(
                            onDragStart = { isDragging = true },
                            onDragEnd = {
                                isDragging = false
                                // Close if dragged far enough
                                if (kotlin.math.abs(dragOffset.y) > 200f) {
                                    onClose()
                                } else {
                                    // Reset position
                                    dragOffset = Offset.Zero
                                }
                            },
                            onDrag = { change, dragAmount ->
                                change.consume()
                                dragOffset += Offset(dragAmount.x, dragAmount.y)
                            }
                        )
                    }
            ) {
                // Image content
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(16.dp)
                ) {
                    when {
                        isLoading -> {
                            CircularProgressIndicator(
                                modifier = Modifier.align(Alignment.Center),
                                color = Color.White
                            )
                        }
                        error != null -> {
                            Column(
                                modifier = Modifier.align(Alignment.Center),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Warning,
                                    contentDescription = "Error",
                                    tint = Color.White,
                                    modifier = Modifier.size(48.dp)
                                )
                                Spacer(modifier = Modifier.height(16.dp))
                                Text(
                                    text = error ?: "Unknown error",
                                    color = Color.White,
                                    textAlign = TextAlign.Center
                                )
                            }
                        }
                        bitmap != null -> {
                            // Zoom state
                            var scale by remember { mutableStateOf(1f) }
                            var offset by remember { mutableStateOf(Offset.Zero) }
                            
                            // Transformable state for zoom and pan
                            val transformableState = rememberTransformableState { zoomChange, offsetChange, _ ->
                                if (!isDragging) {
                                    scale *= zoomChange
                                    offset += offsetChange
                                    
                                    // Limit zoom range
                                    if (scale < 0.5f) scale = 0.5f
                                    if (scale > 3f) scale = 3f
                                }
                            }
                            
                            // Double tap to zoom
                            var doubleTapScale by remember { mutableStateOf(1f) }
                            
                            Image(
                                bitmap = bitmap!!.asImageBitmap(),
                                contentDescription = "Full-screen image",
                                modifier = Modifier
                                    .fillMaxSize()
                                    .graphicsLayer(
                                        scaleX = scale * doubleTapScale,
                                        scaleY = scale * doubleTapScale,
                                        translationX = offset.x,
                                        translationY = offset.y
                                    )
                                    .transformable(
                                        state = transformableState,
                                        lockRotationOnZoomPan = true
                                    )
                                    .pointerInput(Unit) {
                                        detectTapGestures(
                                            onDoubleTap = {
                                                // Toggle between 1x and 2x zoom
                                                if (doubleTapScale == 1f) {
                                                    doubleTapScale = 2f
                                                    offset = Offset.Zero
                                                } else {
                                                    doubleTapScale = 1f
                                                    offset = Offset.Zero
                                                }
                                            }
                                        )
                                    },
                                contentScale = ContentScale.Fit
                            )
                        }
                        else -> {
                            Text(
                                text = "No image data",
                                modifier = Modifier.align(Alignment.Center),
                                color = Color.White
                            )
                        }
                    }
                }
                
                // Swipe indicator
                if (isDragging) {
                    Text(
                        text = if (kotlin.math.abs(dragOffset.y) > 100f) "Release to close" else "Swipe down to close",
                        color = Color.White.copy(alpha = 0.8f),
                        modifier = Modifier
                            .align(Alignment.TopCenter)
                            .padding(top = 32.dp)
                            .background(
                                color = Color.Black.copy(alpha = 0.5f),
                                shape = RoundedCornerShape(8.dp)
                            )
                            .padding(horizontal = 16.dp, vertical = 8.dp)
                    )
                }
            }
        }
    }

@Composable
    fun SearchResults(
        query: String,
        textItems: List<Any>,
        modifier: Modifier = Modifier
    ) {
        val prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE)
        val imageItems = getItems(prefs, IMAGE_ITEMS_KEY) as List<ImageItem>
        
        val filteredTextItems = textItems.filter { item ->
            if (item is TextItem) {
                !item.content.isNullOrEmpty() && item.content.contains(query, ignoreCase = true)
            } else {
                false
            }
        }
        
        val filteredImageItems = imageItems.filter { item ->
            !item.extractedText.isNullOrEmpty() && item.extractedText.contains(query, ignoreCase = true)
        }
        
        val totalResults = filteredTextItems.size + filteredImageItems.size

        Column(modifier = modifier) {
            Text(
                text = "Search Results (${totalResults} found)",
                fontSize = 16.sp,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(bottom = 16.dp)
            )

            if (totalResults == 0) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(32.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Text(
                        text = "🔍",
                        fontSize = 48.sp,
                        modifier = Modifier.padding(bottom = 16.dp)
                    )
                    Text(
                        text = "No items found",
                        fontSize = 18.sp,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Try a different search term",
                        fontSize = 14.sp,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f),
                        textAlign = TextAlign.Center
                    )
                }
            } else {
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // Text items
                    items(filteredTextItems.reversed()) { item ->
                        if (item is TextItem) {
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(8.dp),
                                colors = CardDefaults.cardColors(
                                    containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f)
                                )
                            ) {
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(16.dp)
                                ) {
                                    Text(
                                        text = "📝 Text Item",
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        text = item.content ?: "No content",
                                        fontSize = 14.sp,
                                        color = MaterialTheme.colorScheme.onSurface,
                                        modifier = Modifier.fillMaxWidth()
                                    )
                                }
                            }
                        }
                    }
                    
                    // Image items (OCR text hidden but searchable)
                    items(filteredImageItems.reversed()) { item ->
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(8.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.3f)
                            )
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(16.dp)
                            ) {
                                Text(
                                    text = "🖼️ Image (contains matching text)",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.secondary
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                                
                                val imageData = when {
                                    !item.localPath.isNullOrEmpty() -> item.localPath
                                    !item.originalUri.isNullOrEmpty() -> item.originalUri
                                    else -> ""
                                }
                                
                                if (imageData.isNotEmpty()) {
                                    AsyncImage(
                                        model = ImageRequest.Builder(LocalContext.current)
                                            .data(imageData)
                                            .crossfade(true)
                                            .build(),
                                        contentDescription = "Shared image",
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .height(120.dp),
                                        contentScale = ContentScale.Crop
                                    )
                                } else {
                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .height(120.dp),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            text = "🖼️",
                                            fontSize = 24.sp,
                                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                                        )
                                    }
                                }
                                // OCR text is hidden from UI but still searchable
                            }
                        }
                    }
                }
            }
        }
    }
}