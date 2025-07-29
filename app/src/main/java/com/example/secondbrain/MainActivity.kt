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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.example.secondbrain.ui.theme.SecondBrainTheme
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import androidx.lifecycle.lifecycleScope
import java.io.File
import java.io.FileOutputStream
import java.io.InputStream

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
        val originalUri: String,
        val localPath: String = "",
        val extractedText: String = "",
        val timestamp: Long = System.currentTimeMillis()
    )

    data class TextItem(
        val content: String,
        val timestamp: Long = System.currentTimeMillis()
    )

    data class LinkItem(
        val url: String,
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
                            val imageUri = when (imageInput) {
                                is File -> imageInput.absolutePath
                                is Uri -> imageInput.toString()
                                else -> return@addOnSuccessListener
                            }
                            println("DEBUG: Updating image with URI: $imageUri")
                            updateImageWithExtractedText(imageUri, extractedText)
                            
                            // Trigger UI update
                            lifecycleScope.launch(Dispatchers.Main) {
                                println("DEBUG: Triggering UI update on main thread")
                                // This will trigger a recomposition
                            }
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
        val updatedItems = items.map { item ->
            if (item.originalUri == imageUri || item.localPath == imageUri) {
                println("DEBUG: Updating image item with OCR text: '$extractedText'")
                item.copy(extractedText = extractedText)
            } else {
                item
            }
        }
        
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

        // Function to refresh data
        fun refreshData() {
            textCount = prefs.getInt(TEXT_COUNT_KEY, 0)
            imageCount = prefs.getInt(IMAGE_COUNT_KEY, 0)
            linkCount = prefs.getInt(LINK_COUNT_KEY, 0)
            refreshTrigger++ // Trigger recomposition
        }

        // Update counts when app becomes active
        LaunchedEffect(Unit) {
            refreshData()
        }

        // Refresh data when returning to main screen
        LaunchedEffect(currentScreen) {
            if (currentScreen == "main") {
                refreshData()
            }
        }

        // Refresh data when refreshTrigger changes
        LaunchedEffect(refreshTrigger) {
            refreshData()
        }

        Scaffold(
            modifier = Modifier.fillMaxSize()
        ) { innerPadding ->
            when (currentScreen) {
                "main" -> {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(innerPadding)
                            .padding(16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        // Header
                        Text(
                            text = "Second Brain",
                            fontSize = 28.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.padding(bottom = 24.dp)
                        )

                        // Search Bar
                        OutlinedTextField(
                            value = searchQuery,
                            onValueChange = { 
                                searchQuery = it
                                isSearching = it.isNotEmpty()
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(bottom = 16.dp),
                            placeholder = { Text("Search in text items...") },
                            leadingIcon = { Icon(Icons.Default.Search, contentDescription = "Search") },
                            singleLine = true,
                            shape = RoundedCornerShape(12.dp)
                        )

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
                                    onClick = { currentScreen = "image" }
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
                            category = "Image"
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

    private fun getItems(prefs: android.content.SharedPreferences, key: String): List<Any> {
        val itemsJson = prefs.getString(key, "[]")
        val type = when (key) {
            TEXT_ITEMS_KEY -> object : TypeToken<List<TextItem>>() {}.type
            IMAGE_ITEMS_KEY -> object : TypeToken<List<ImageItem>>() {}.type
            LINK_ITEMS_KEY -> object : TypeToken<List<LinkItem>>() {}.type
            else -> object : TypeToken<List<String>>() {}.type
        }
        return gson.fromJson(itemsJson, type) ?: listOf()
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
        modifier: Modifier = Modifier
    ) {
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
                        shape = RoundedCornerShape(8.dp)
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
                                    
                                    Spacer(modifier = Modifier.height(8.dp))
                                    
                                    // Show OCR text if available
                                    if (!item.extractedText.isNullOrEmpty()) {
                                        Card(
                                            modifier = Modifier.fillMaxWidth(),
                                            colors = CardDefaults.cardColors(
                                                containerColor = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.3f)
                                            )
                                        ) {
                                            Column(
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .padding(12.dp)
                                            ) {
                                                Text(
                                                    text = "📝 Extracted Text:",
                                                    fontSize = 12.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    color = MaterialTheme.colorScheme.secondary
                                                )
                                                Spacer(modifier = Modifier.height(4.dp))
                                                Text(
                                                    text = item.extractedText,
                                                    fontSize = 14.sp,
                                                    color = MaterialTheme.colorScheme.onSurface,
                                                    modifier = Modifier.fillMaxWidth()
                                                )
                                            }
                                        }
                                    } else {
                                        // Show placeholder if no OCR text
                                        Card(
                                            modifier = Modifier.fillMaxWidth(),
                                            colors = CardDefaults.cardColors(
                                                containerColor = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.3f)
                                            )
                                        ) {
                                            Column(
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .padding(12.dp)
                                            ) {
                                                Text(
                                                    text = "🔍 No text extracted from image",
                                                    fontSize = 12.sp,
                                                    color = MaterialTheme.colorScheme.error,
                                                    modifier = Modifier.fillMaxWidth()
                                                )
                                            }
                                        }
                                    }
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
                                        text = item.content,
                                        fontSize = 14.sp,
                                        color = MaterialTheme.colorScheme.onSurface,
                                        modifier = Modifier.fillMaxWidth()
                                    )
                                }
                            }
                        }
                    }
                    
                    // Image items with OCR text
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
                                    text = "🖼️ Image with OCR Text",
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
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    text = "Extracted Text:",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = item.extractedText,
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