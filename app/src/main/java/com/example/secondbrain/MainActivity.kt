package com.example.secondbrain

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
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
import androidx.compose.material.icons.filled.Add
import androidx.activity.compose.BackHandler
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.animateIntAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.core.EaseOutBack
import androidx.compose.runtime.*
import androidx.compose.runtime.snapshotFlow
import androidx.compose.runtime.collectAsState
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
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import com.example.secondbrain.data.DatabaseManager

class MainActivity : ComponentActivity() {
    private val PREFS_NAME = "SecondBrainPrefs"
    private val TEXT_COUNT_KEY = "text_count"
    private val IMAGE_COUNT_KEY = "image_count"
    private val LINK_COUNT_KEY = "link_count"
    private val TEXT_ITEMS_KEY = "text_items"
    private val IMAGE_ITEMS_KEY = "image_items"
    private val LINK_ITEMS_KEY = "link_items"
    private val IMAGE_METADATA_KEY = "image_metadata"
    private val IMAGE_ADDED_TRIGGER_KEY = "image_added_trigger"
    private val TEXT_ADDED_TRIGGER_KEY = "text_added_trigger"
    private val LINK_ADDED_TRIGGER_KEY = "link_added_trigger"
    private val gson = Gson()
    private val textRecognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)
    private lateinit var databaseManager: DatabaseManager
    
    // Gallery picker launcher
    private val galleryLauncher = registerForActivityResult(
        ActivityResultContracts.GetContent()
    ) { uri ->
        uri?.let { selectedUri ->
            handleGalleryImageSelection(selectedUri)
        }
    }
    


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
        val title: String? = null,
        val description: String? = null,
        val imageUrl: String? = null,
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
        
        // Initialize database manager
        databaseManager = DatabaseManager(this)
        
        // Handle shared content only if it's a share intent
        if (intent?.action == Intent.ACTION_SEND) {
            handleSharedContent(intent)
        }
        
        setContent {
            SecondBrainTheme {
                CategoryCounter(
                    onUploadImage = { launchGalleryPicker() }
                )
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
                            // Add basic link item first for immediate UI update
                            addToCategory(LINK_COUNT_KEY, LINK_ITEMS_KEY, LinkItem(sharedText))
                            
                            // Set trigger flag for immediate feedback
                            getSharedPreferences(PREFS_NAME, MODE_PRIVATE).edit()
                                .putLong(LINK_ADDED_TRIGGER_KEY, System.currentTimeMillis())
                                .apply()
                            
                            // Fetch metadata in background and update
                            lifecycleScope.launch(Dispatchers.IO) {
                                val linkItemWithMetadata = fetchLinkMetadata(sharedText)
                                updateLinkItem(sharedText, linkItemWithMetadata)
                            }
                        } else {
                            addToCategory(TEXT_COUNT_KEY, TEXT_ITEMS_KEY, TextItem(sharedText))
                            
                            // Set trigger flag for immediate feedback
                            getSharedPreferences(PREFS_NAME, MODE_PRIVATE).edit()
                                .putLong(TEXT_ADDED_TRIGGER_KEY, System.currentTimeMillis())
                                .apply()
                        }
                    }
                }
                "image/*", "image/jpeg", "image/png", "image/gif", "image/webp" -> {
                    @Suppress("DEPRECATION")
                    val imageUri = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
                        intent.getParcelableExtra(Intent.EXTRA_STREAM, Uri::class.java)
                    } else {
                        intent.getParcelableExtra<Uri>(Intent.EXTRA_STREAM)
                    }
                    if (imageUri != null) {
                        // Copy image to internal storage
                        val localPath = copyImageToInternalStorage(imageUri)
                        val imageItem = ImageItem(
                            originalUri = imageUri.toString(),
                            localPath = localPath ?: ""
                        )
                        addToCategory(IMAGE_COUNT_KEY, IMAGE_ITEMS_KEY, imageItem)
                        
                        // Set trigger flag for immediate feedback
                        getSharedPreferences(PREFS_NAME, MODE_PRIVATE).edit()
                            .putLong(IMAGE_ADDED_TRIGGER_KEY, System.currentTimeMillis())
                            .apply()
                        
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
        val trimmed = text.trim()
        return trimmed.startsWith("http://") || trimmed.startsWith("https://") ||
               trimmed.startsWith("www.")
    }
    
    private fun fetchLinkMetadata(url: String): LinkItem {
        return try {
            println("DEBUG: Fetching metadata for URL: $url")
            
            val doc: Document = Jsoup.connect(url)
                .userAgent("Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36")
                .timeout(10000)
                .get()
            
            val title = doc.select("title").firstOrNull()?.text() ?: ""
            val description = doc.select("meta[name=description]").firstOrNull()?.attr("content") ?: ""
            val ogImage = doc.select("meta[property=og:image]").firstOrNull()?.attr("content") ?: ""
            val twitterImage = doc.select("meta[name=twitter:image]").firstOrNull()?.attr("content") ?: ""
            val imageUrl = if (ogImage.isNotEmpty()) ogImage else twitterImage
            
            println("DEBUG: Extracted metadata - Title: '$title', Description: '${description.take(100)}...', Image: '$imageUrl'")
            
            LinkItem(
                url = url,
                title = title,
                description = description,
                imageUrl = imageUrl
            )
        } catch (e: Exception) {
            println("DEBUG: Error fetching metadata for $url: ${e.message}")
            LinkItem(url = url)
        }
    }
    
    private fun updateLinkItem(originalUrl: String, updatedLinkItem: LinkItem) {
        lifecycleScope.launch(Dispatchers.IO) {
            databaseManager.updateLinkMetadata(
                url = originalUrl,
                title = updatedLinkItem.title,
                description = updatedLinkItem.description,
                imageUrl = updatedLinkItem.imageUrl
            )
            println("DEBUG: Link metadata updated successfully in database")
        }
    }
    
    private fun openUrlInBrowser(url: String) {
        try {
            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
            startActivity(intent)
            println("DEBUG: Opening URL in browser: $url")
        } catch (e: Exception) {
            println("DEBUG: Error opening URL: ${e.message}")
        }
    }
    
    private fun handleGalleryImageSelection(uri: Uri) {
        // Copy image to internal storage
        val localPath = copyImageToInternalStorage(uri)
        val imageItem = ImageItem(
            originalUri = uri.toString(),
            localPath = localPath ?: "",
            extractedText = "" // Will be updated after OCR
        )
        
        // Add to category (this will increment count and save to SharedPreferences)
        addToCategory(IMAGE_COUNT_KEY, IMAGE_ITEMS_KEY, imageItem)
        
        // Set trigger flag for immediate feedback
        getSharedPreferences(PREFS_NAME, MODE_PRIVATE).edit()
            .putLong(IMAGE_ADDED_TRIGGER_KEY, System.currentTimeMillis())
            .apply()
        
        // Start OCR processing
        if (localPath != null) {
            val filePath = localPath.removePrefix("file://")
            val file = File(filePath)
            println("DEBUG: Starting OCR with gallery image: ${file.absolutePath}")
            processImageWithOCR(file)
        } else {
            println("DEBUG: Starting OCR with gallery URI: $uri")
            processImageWithOCR(uri)
        }
        
        // Trigger immediate feedback for image addition
        // This will be handled by the CategoryCounter composable
    }
    
    private fun launchGalleryPicker() {
        galleryLauncher.launch("image/*")
    }
    
    private fun triggerImageAddedFeedback() {
        // This will be called from CategoryCounter when an image is added
        // The actual feedback is handled within the CategoryCounter composable
    }

    private fun addToCategory(countKey: String, itemsKey: String, item: Any) {
        lifecycleScope.launch(Dispatchers.IO) {
            when (item) {
                is TextItem -> {
                    databaseManager.insertTextItem(item)
                    // Set trigger for UI update
                    getSharedPreferences(PREFS_NAME, MODE_PRIVATE).edit()
                        .putLong(TEXT_ADDED_TRIGGER_KEY, System.currentTimeMillis())
                        .apply()
                }
                is ImageItem -> {
                    databaseManager.insertImageItem(item)
                    // Set trigger for UI update
                    getSharedPreferences(PREFS_NAME, MODE_PRIVATE).edit()
                        .putLong(IMAGE_ADDED_TRIGGER_KEY, System.currentTimeMillis())
                        .apply()
                }
                is LinkItem -> {
                    databaseManager.insertLinkItem(item)
                    // Set trigger for UI update
                    getSharedPreferences(PREFS_NAME, MODE_PRIVATE).edit()
                        .putLong(LINK_ADDED_TRIGGER_KEY, System.currentTimeMillis())
                        .apply()
                }
            }
        }
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
        lifecycleScope.launch(Dispatchers.IO) {
            try {
                println("DEBUG: Updating image with URI: $imageUri")
                databaseManager.updateImageExtractedTextByUri(imageUri, extractedText)
                println("DEBUG: Image metadata updated successfully in database")
            } catch (e: Exception) {
                println("DEBUG: Error updating image with extracted text: ${e.message}")
                e.printStackTrace()
            }
        }
    }

    @Composable
    private fun CategoryCounter(
        onUploadImage: () -> Unit = {},
        onImageAdded: () -> Unit = {}
    ) {
        // Use Flow-based data from DatabaseManager
        val textItems by databaseManager.getAllTextItems().collectAsState(initial = emptyList())
        val imageItems by databaseManager.getAllImageItems().collectAsState(initial = emptyList())
        val linkItems by databaseManager.getAllLinkItems().collectAsState(initial = emptyList())
        
        val textCount = textItems.size
        val imageCount = imageItems.size
        val linkCount = linkItems.size
        var currentScreen by remember { mutableStateOf("main") }
        var searchQuery by remember { mutableStateOf("") }
        var isSearching by remember { mutableStateOf(false) }
        var refreshTrigger by remember { mutableStateOf(0) }
        var ocrUpdateTrigger by remember { mutableStateOf(0L) }
        var forceRefresh by remember { mutableStateOf(0) }
        var selectedImageItem by remember { mutableStateOf<ImageItem?>(null) }
        
        // Animation state for counter highlight
        var imageCountAnimationTrigger by remember { mutableStateOf(0) }
        var textCountAnimationTrigger by remember { mutableStateOf(0) }
        var linkCountAnimationTrigger by remember { mutableStateOf(0) }
        
        val imageCountScale by animateFloatAsState(
            targetValue = if (imageCountAnimationTrigger > 0) 1.2f else 1f,
            animationSpec = tween(durationMillis = 300),
            label = "imageCountScale"
        )
        
        val textCountScale by animateFloatAsState(
            targetValue = if (textCountAnimationTrigger > 0) 1.2f else 1f,
            animationSpec = tween(durationMillis = 300),
            label = "textCountScale"
        )
        
        val linkCountScale by animateFloatAsState(
            targetValue = if (linkCountAnimationTrigger > 0) 1.2f else 1f,
            animationSpec = tween(durationMillis = 300),
            label = "linkCountScale"
        )
        


        // Function to refresh data (no longer needed with Flow-based data)
        fun refreshData() {
            println("DEBUG: refreshData() called - Flow-based data automatically updates")
            refreshTrigger++ // Trigger recomposition if needed
        }
        
        // Function to trigger visual feedback for image addition
        fun triggerImageAddedFeedback() {
            // Trigger counter animation
            imageCountAnimationTrigger++
            
            // Reset animation trigger after animation completes
            lifecycleScope.launch {
                delay(300)
                imageCountAnimationTrigger = 0
            }
        }
        
        // Function to trigger visual feedback for text addition
        fun triggerTextAddedFeedback() {
            // Trigger counter animation
            textCountAnimationTrigger++
            
            // Reset animation trigger after animation completes
            lifecycleScope.launch {
                delay(300)
                textCountAnimationTrigger = 0
            }
        }
        
        // Function to trigger visual feedback for link addition
        fun triggerLinkAddedFeedback() {
            // Trigger counter animation
            linkCountAnimationTrigger++
            
            // Reset animation trigger after animation completes
            lifecycleScope.launch {
                delay(300)
                linkCountAnimationTrigger = 0
            }
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
        
        // Handle system back button
        BackHandler {
            when {
                isSearching -> {
                    // If in search mode, go back to main screen
                    isSearching = false
                    searchQuery = ""
                }
                currentScreen != "main" -> {
                    // If in a category screen, go back to main
                    currentScreen = "main"
                }
                else -> {
                    // If on main screen, let the system handle back (close app)
                    // This will be handled by the system
                }
            }
        }
        
        // Trigger counter animations whenever counts change
        LaunchedEffect(imageCount) { if (imageCount > 0) triggerImageAddedFeedback() }
        LaunchedEffect(textCount) { if (textCount > 0) triggerTextAddedFeedback() }
        LaunchedEffect(linkCount) { if (linkCount > 0) triggerLinkAddedFeedback() }



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
                        // Header
                        Text(
                            text = "Second Brain",
                            fontSize = 28.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.padding(bottom = 16.dp)
                        )

                        // Search Bar at the top
                        OutlinedTextField(
                            value = searchQuery,
                            onValueChange = { 
                                searchQuery = it
                                isSearching = it.isNotEmpty()
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(bottom = 16.dp),
                            placeholder = { Text("Search across text, images, and links…") },
                            leadingIcon = { Icon(Icons.Default.Search, contentDescription = "Search") },
                            singleLine = true,
                            shape = RoundedCornerShape(16.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = MaterialTheme.colorScheme.primary,
                                unfocusedBorderColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f),
                                focusedContainerColor = MaterialTheme.colorScheme.surface,
                                unfocusedContainerColor = MaterialTheme.colorScheme.surface,
                                cursorColor = MaterialTheme.colorScheme.primary,
                                focusedTextColor = MaterialTheme.colorScheme.onSurface,
                                unfocusedTextColor = MaterialTheme.colorScheme.onSurface
                            )
                        )

                        if (isSearching) {
                            // Search Results with back button
                            Column(modifier = Modifier.weight(1f)) {
                                // Header with back button
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    IconButton(
                                        onClick = { 
                                            isSearching = false
                                            searchQuery = ""
                                        }
                                    ) {
                                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back to main")
                                    }
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = "Search Results",
                                        fontSize = 20.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                                Spacer(modifier = Modifier.height(16.dp))
                                
                                SearchResults(
                                    query = searchQuery,
                                    textItems = textItems,
                                    imageItems = imageItems,
                                    linkItems = linkItems,
                                    modifier = Modifier.weight(1f),
                                    onImageClick = { imageItem ->
                                        selectedImageItem = imageItem
                                    }
                                )
                            }
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
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                CategoryCard(
                                    title = "Text",
                                    count = textCount,
                                    color = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier
                                        .weight(1f)
                                        .graphicsLayer {
                                            scaleX = textCountScale
                                            scaleY = textCountScale
                                        },
                                    onClick = { currentScreen = "text" },
                                    icon = "📄"
                                )
                                CategoryCard(
                                    title = "Image",
                                    count = imageCount,
                                    color = MaterialTheme.colorScheme.secondary,
                                    modifier = Modifier
                                        .weight(1f)
                                        .graphicsLayer {
                                            scaleX = imageCountScale
                                            scaleY = imageCountScale
                                        },
                                    onClick = { 
                                        println("DEBUG: Image folder clicked, currentScreen set to 'image'")
                                        currentScreen = "image" 
                                    },
                                    icon = "🖼️"
                                )
                                CategoryCard(
                                    title = "Link",
                                    count = linkCount,
                                    color = MaterialTheme.colorScheme.tertiary,
                                    modifier = Modifier
                                        .weight(1f)
                                        .graphicsLayer {
                                            scaleX = linkCountScale
                                            scaleY = linkCountScale
                                        },
                                    onClick = { currentScreen = "link" },
                                    icon = "🔗"
                                )
                            }

                            Spacer(modifier = Modifier.height(32.dp))

                            // Upload Image Button
                            Button(
                                onClick = { onUploadImage() },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 32.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = MaterialTheme.colorScheme.primary
                                ),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Row(
                                    horizontalArrangement = Arrangement.Center,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Add,
                                        contentDescription = "Upload",
                                        modifier = Modifier.size(20.dp)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = "Upload Image",
                                        fontSize = 16.sp,
                                        fontWeight = FontWeight.Medium
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(16.dp))

                            Text(
                                text = "Tap on folders to view your shared content!",
                                fontSize = 14.sp,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                                modifier = Modifier.padding(horizontal = 16.dp),
                                textAlign = TextAlign.Center
                            )


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
                                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
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
                            items = textItems,
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
                                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
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
                            items = imageItems,
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
                                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
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
                            items = linkItems,
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
        icon: String = "📁"
    ) {
        // Animate count changes
        val animatedCount by animateIntAsState(
            targetValue = count,
            animationSpec = tween(durationMillis = 500, easing = EaseOutBack),
            label = "countAnimation"
        )
        
        Card(
            modifier = modifier
                .height(140.dp)
                .clickable { onClick() },
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(
                containerColor = color.copy(alpha = 0.08f)
            ),
            elevation = CardDefaults.cardElevation(
                defaultElevation = 4.dp,
                pressedElevation = 8.dp
            )
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                // Icon
    Text(
                    text = icon,
                    fontSize = 32.sp,
                    modifier = Modifier.padding(bottom = 8.dp)
                )
                
                // Animated Count
                Text(
                    text = animatedCount.toString(),
                    fontSize = 28.sp,
                    fontWeight = FontWeight.Bold,
                    color = color,
                    modifier = Modifier.padding(bottom = 4.dp)
                )
                
                // Label
                Text(
                    text = title,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Medium,
                    color = color.copy(alpha = 0.8f)
                )
        }
    }
}

@Composable
    private fun ContentList(
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
                items(items) { item ->
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
                                    // Display link with metadata
                                    Column(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clickable(
                                                interactionSource = remember { MutableInteractionSource() },
                                                indication = null
                                            ) { 
                                                item.url?.let { url -> openUrlInBrowser(url) }
                                            },
                                        verticalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        // Title or URL
                                        Text(
                                            text = item.title?.take(100) ?: item.url ?: "No URL",
                                            fontSize = 16.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.onSurface,
                                            modifier = Modifier.fillMaxWidth()
                                        )
                                        
                                        // Description
                                        if (!item.description.isNullOrEmpty()) {
                                            Text(
                                                text = item.description.take(150) + if (item.description.length > 150) "..." else "",
                                                fontSize = 14.sp,
                                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
                                                modifier = Modifier.fillMaxWidth()
                                            )
                                        }
                                        
                                        // URL
                                        Text(
                                            text = item.url ?: "No URL",
                                            fontSize = 12.sp,
                                            color = MaterialTheme.colorScheme.primary,
                                            modifier = Modifier.fillMaxWidth()
                                        )
                                        
                                        // Image preview if available
                                        if (!item.imageUrl.isNullOrEmpty()) {
                                            AsyncImage(
                                                model = ImageRequest.Builder(LocalContext.current)
                                                    .data(item.imageUrl)
                                                    .crossfade(true)
                                                    .build(),
                                                contentDescription = "Link preview image",
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .height(120.dp),
                                                contentScale = ContentScale.Crop
                                            )
                                        }
                                    }
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
    private fun FullScreenImageViewer(
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
                        val options = BitmapFactory.Options().apply {
                            inJustDecodeBounds = true
                            BitmapFactory.decodeFile(imageData, this)
                            inSampleSize = calculateInSampleSize(outWidth, outHeight, 1920, 1080)
                            inJustDecodeBounds = false
                        }
                        val loadedBitmap = BitmapFactory.decodeFile(imageData, options)
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
                    .offset { IntOffset(dragOffset.x.toInt(), dragOffset.y.toInt()) }
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
    private fun SearchResults(
        query: String,
        textItems: List<Any>,
        imageItems: List<ImageItem>,
        linkItems: List<LinkItem>,
        modifier: Modifier = Modifier,
        onImageClick: ((ImageItem) -> Unit)? = null
    ) {
        
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
        
        val filteredLinkItems = linkItems.filter { item ->
            (!item.title.isNullOrEmpty() && item.title.contains(query, ignoreCase = true)) ||
            (!item.description.isNullOrEmpty() && item.description.contains(query, ignoreCase = true)) ||
            (!item.url.isNullOrEmpty() && item.url.contains(query, ignoreCase = true))
        }
        
        val totalResults = filteredTextItems.size + filteredImageItems.size + filteredLinkItems.size

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
                    items(filteredTextItems) { item ->
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
                    items(filteredImageItems) { item ->
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable(
                                    interactionSource = remember { MutableInteractionSource() },
                                    indication = null
                                ) { onImageClick?.invoke(item) },
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
                    
                    // Link items
                    items(filteredLinkItems) { item ->
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable(
                                    interactionSource = remember { MutableInteractionSource() },
                                    indication = null
                                ) { 
                                    item.url?.let { url -> openUrlInBrowser(url) }
                                },
                            shape = RoundedCornerShape(8.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.3f)
                            )
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(16.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Text(
                                    text = "🔗 Link",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.tertiary
                                )
                                
                                // Title or URL
                                Text(
                                    text = item.title?.take(100) ?: item.url ?: "No URL",
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = MaterialTheme.colorScheme.onSurface,
                                    modifier = Modifier.fillMaxWidth()
                                )
                                
                                // Description
                                if (!item.description.isNullOrEmpty()) {
                                    Text(
                                        text = item.description.take(120) + if (item.description.length > 120) "..." else "",
                                        fontSize = 12.sp,
                                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
                                        modifier = Modifier.fillMaxWidth()
                                    )
                                }
                                
                                // URL
                                Text(
                                    text = item.url ?: "No URL",
                                    fontSize = 11.sp,
                                    color = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.fillMaxWidth()
                                )
                                
                                // Image preview if available
                                if (!item.imageUrl.isNullOrEmpty()) {
                                    AsyncImage(
                                        model = ImageRequest.Builder(LocalContext.current)
                                            .data(item.imageUrl)
                                            .crossfade(true)
                                            .build(),
                                        contentDescription = "Link preview image",
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .height(80.dp),
                                        contentScale = ContentScale.Crop
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    private fun calculateInSampleSize(width: Int, height: Int, reqWidth: Int, reqHeight: Int): Int {
        var inSampleSize = 1
        if (height > reqHeight || width > reqWidth) {
            val halfHeight = height / 2
            val halfWidth = width / 2
            while (halfHeight / inSampleSize >= reqHeight && halfWidth / inSampleSize >= reqWidth) {
                inSampleSize *= 2
            }
        }
        return inSampleSize
    }
}