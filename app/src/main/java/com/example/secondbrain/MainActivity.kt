package com.example.secondbrain

import android.content.Context
import android.content.Intent
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.draw.clip
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
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
import androidx.compose.material.icons.filled.Create
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Send
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Star
import androidx.activity.compose.BackHandler
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.material3.SnackbarResult
import androidx.compose.animation.core.EaseOutBack
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.animateIntAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
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
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.graphics.graphicsLayer
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.example.secondbrain.ui.theme.SBBackground
import com.example.secondbrain.ui.theme.SBFaint
import com.example.secondbrain.ui.theme.SBLinkBlue
import com.example.secondbrain.ui.theme.SBMuted
import com.example.secondbrain.ui.theme.SBOk
import com.example.secondbrain.ui.theme.SBOnPrimary
import com.example.secondbrain.ui.theme.SBOnPrimaryContainer
import com.example.secondbrain.ui.theme.SBOnSurfaceVariant
import com.example.secondbrain.ui.theme.SBOutlineVariant
import com.example.secondbrain.ui.theme.SBPrimary
import com.example.secondbrain.ui.theme.SBPrimaryContainer
import com.example.secondbrain.ui.theme.SBPrimaryTint
import com.example.secondbrain.ui.theme.SBSurface
import com.example.secondbrain.ui.theme.SBSurfaceHigh
import com.example.secondbrain.ui.theme.SBOutline
import com.example.secondbrain.ui.theme.SBSurfaceLow
import com.example.secondbrain.ui.theme.SBSurfaceLowest
import com.example.secondbrain.ui.theme.SecondBrainTheme
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
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
import com.example.secondbrain.data.ContentType
import com.example.secondbrain.data.DatabaseManager
import com.example.secondbrain.data.EmbeddingUtils
import com.example.secondbrain.data.OpenRouterService
import com.example.secondbrain.data.RetrievedItem
import com.example.secondbrain.data.SettingsManager
import com.example.secondbrain.data.entities.ChatMessageEntity
import com.example.secondbrain.data.entities.ConversationEntity
import com.example.secondbrain.data.entities.TagEntity
import com.example.secondbrain.data.entities.TextItemEntity
import com.example.secondbrain.data.entities.ImageItemEntity
import com.example.secondbrain.data.entities.LinkItemEntity
import com.example.secondbrain.data.mapper.DataMapper

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
    private lateinit var settingsManager: SettingsManager
    private val openRouterService = OpenRouterService()
    
    // Gallery picker launcher
    private val galleryLauncher = registerForActivityResult(
        ActivityResultContracts.GetContent()
    ) { uri ->
        uri?.let { selectedUri ->
            handleGalleryImageSelection(selectedUri)
        }
    }
    


    data class ImageItem(
        val id: Long = 0,
        val originalUri: String? = null,
        val localPath: String? = null,
        val extractedText: String? = null,
        val timestamp: Long = System.currentTimeMillis()
    )

    data class TextItem(
        val id: Long = 0,
        val content: String? = null,
        val timestamp: Long = System.currentTimeMillis()
    )

    data class LinkItem(
        val id: Long = 0,
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
        
        // Initialize database manager and settings manager
        databaseManager = DatabaseManager(this)
        settingsManager = SettingsManager(this)

        // Background embedding job for existing items (runs once after v3 install)
        runBackgroundEmbeddingJobIfNeeded()

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
                            addToCategory(LINK_COUNT_KEY, LINK_ITEMS_KEY, LinkItem(url = sharedText))
                            
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
                            addToCategory(TEXT_COUNT_KEY, TEXT_ITEMS_KEY, TextItem(content = sharedText))
                            
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

            // Generate embedding now that we have title/description to embed
            val entity = databaseManager.getLinkItemByUrl(originalUrl) ?: return@launch
            val text = listOfNotNull(updatedLinkItem.title, updatedLinkItem.description)
                .joinToString(" ").take(2000).ifBlank { null } ?: return@launch
            val apiKey = settingsManager.getApiKey() ?: return@launch
            val result = openRouterService.generateEmbedding(text, apiKey)
            result.onSuccess { embedding ->
                databaseManager.updateLinkItemEmbedding(entity.id, EmbeddingUtils.serializeEmbedding(embedding))
                println("DEBUG: Link embedding generated after metadata fetch for id=${entity.id}")
            }
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
                    val insertedId = databaseManager.insertTextItem(item)
                    getSharedPreferences(PREFS_NAME, MODE_PRIVATE).edit()
                        .putLong(TEXT_ADDED_TRIGGER_KEY, System.currentTimeMillis())
                        .apply()
                    generateEmbeddingForItem(item, insertedId)
                }
                is ImageItem -> {
                    val insertedId = databaseManager.insertImageItem(item)
                    getSharedPreferences(PREFS_NAME, MODE_PRIVATE).edit()
                        .putLong(IMAGE_ADDED_TRIGGER_KEY, System.currentTimeMillis())
                        .apply()
                    generateEmbeddingForItem(item, insertedId)
                }
                is LinkItem -> {
                    val insertedId = databaseManager.insertLinkItem(item)
                    getSharedPreferences(PREFS_NAME, MODE_PRIVATE).edit()
                        .putLong(LINK_ADDED_TRIGGER_KEY, System.currentTimeMillis())
                        .apply()
                    generateEmbeddingForItem(item, insertedId)
                }
            }
        }
    }

    /** Extracts embeddable text for an item, or null if none is available. */
    private fun extractTextForEmbedding(item: Any): String? = when (item) {
        is TextItem  -> item.content?.take(2000)
        is ImageItem -> item.extractedText?.take(2000)
        is LinkItem  -> {
            val combined = listOfNotNull(item.title, item.description)
                .joinToString(" ")
                .take(2000)
            combined.ifBlank { null }
        }
        else -> null
    }

    /** Generates and stores an embedding for a newly-saved item. Silently skips on failure. */
    private suspend fun generateEmbeddingForItem(item: Any, insertedId: Long) {
        val apiKey = settingsManager.getApiKey() ?: return
        val text = extractTextForEmbedding(item)?.takeIf { it.isNotBlank() } ?: return
        val result = openRouterService.generateEmbedding(text, apiKey)
        result.onSuccess { embedding ->
            val json = EmbeddingUtils.serializeEmbedding(embedding)
            when (item) {
                is TextItem  -> databaseManager.updateTextItemEmbedding(insertedId, json)
                is ImageItem -> databaseManager.updateImageItemEmbedding(insertedId, json)
                is LinkItem  -> databaseManager.updateLinkItemEmbedding(insertedId, json)
            }
        }
        // On failure: silently skip; background job will retry
    }

    /** One-time background job: embed all items that have no embedding yet. */
    private fun runBackgroundEmbeddingJobIfNeeded() {
        val prefs = getSharedPreferences("second_brain_prefs", MODE_PRIVATE)
        if (prefs.getBoolean("embedding_job_v3_done", false)) return
        val apiKey = settingsManager.getApiKey() ?: return  // no key — skip; retry next launch

        lifecycleScope.launch(Dispatchers.IO) {
            val allItems: List<Pair<Long, ContentType>> = buildList {
                databaseManager.getTextItemsWithoutEmbedding().forEach { add(it.id to ContentType.TEXT) }
                databaseManager.getImageItemsWithoutEmbedding().forEach { add(it.id to ContentType.IMAGE) }
                databaseManager.getLinkItemsWithoutEmbedding().forEach { add(it.id to ContentType.LINK) }
            }

            allItems.chunked(10).forEach { batch ->
                batch.forEach { (id, type) ->
                    try {
                        // Load the full item to get its text
                        val text: String? = when (type) {
                            ContentType.TEXT  -> databaseManager.getAllTextItems()
                                .first().find { it.id == id }?.content?.take(2000)
                            ContentType.IMAGE -> databaseManager.getAllImageItems()
                                .first().find { it.id == id }?.extractedText?.take(2000)
                            ContentType.LINK  -> {
                                val link = databaseManager.getAllLinkItems()
                                    .first().find { it.id == id }
                                listOfNotNull(link?.title, link?.description)
                                    .joinToString(" ").take(2000).ifBlank { null }
                            }
                        }
                        if (!text.isNullOrBlank()) {
                            val result = openRouterService.generateEmbedding(text, apiKey)
                            result.onSuccess { embedding ->
                                val json = EmbeddingUtils.serializeEmbedding(embedding)
                                when (type) {
                                    ContentType.TEXT  -> databaseManager.updateTextItemEmbedding(id, json)
                                    ContentType.IMAGE -> databaseManager.updateImageItemEmbedding(id, json)
                                    ContentType.LINK  -> databaseManager.updateLinkItemEmbedding(id, json)
                                }
                            }
                            // On failure: skip this item, continue
                        }
                    } catch (e: Exception) {
                        // Skip this item and continue with the rest
                    }
                }
            }

            prefs.edit().putBoolean("embedding_job_v3_done", true).apply()
        }
    }

    private fun isNetworkAvailable(): Boolean {
        val cm = getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val network = cm.activeNetwork ?: return false
        val caps = cm.getNetworkCapabilities(network) ?: return false
        return caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
    }

    private suspend fun retrieveRelevantItems(questionEmbedding: List<Float>): List<RetrievedItem> {
        val similarityThreshold = 0.3f
        val topK = 4
        val results = mutableListOf<Pair<RetrievedItem, Float>>()
        val gson = com.google.gson.Gson()

        // Text items
        databaseManager.getAllTextEmbeddings().forEach { proj ->
            val score = EmbeddingUtils.cosineSimilarity(
                questionEmbedding, EmbeddingUtils.parseEmbedding(proj.embedding)
            )
            if (score >= similarityThreshold) {
                val fullItem = databaseManager.getAllTextItems().first().find { it.id == proj.id }
                if (fullItem != null) {
                    results += RetrievedItem(
                        id = proj.id,
                        contentType = ContentType.TEXT,
                        label = "Text Note",
                        contextText = "[Saved Item - Text Note]\n${fullItem.content ?: ""}",
                        similarityScore = score
                    ) to score
                }
            }
        }

        // Image items
        databaseManager.getAllImageEmbeddings().forEach { proj ->
            val score = EmbeddingUtils.cosineSimilarity(
                questionEmbedding, EmbeddingUtils.parseEmbedding(proj.embedding)
            )
            if (score >= similarityThreshold) {
                val fullItem = databaseManager.getAllImageItems().first().find { it.id == proj.id }
                if (fullItem != null) {
                    results += RetrievedItem(
                        id = proj.id,
                        contentType = ContentType.IMAGE,
                        label = "Image Note",
                        contextText = "[Saved Item - Image Note]\nExtracted text: ${fullItem.extractedText ?: ""}",
                        similarityScore = score
                    ) to score
                }
            }
        }

        // Link items
        databaseManager.getAllLinkEmbeddings().forEach { proj ->
            val score = EmbeddingUtils.cosineSimilarity(
                questionEmbedding, EmbeddingUtils.parseEmbedding(proj.embedding)
            )
            if (score >= similarityThreshold) {
                val fullItem = databaseManager.getAllLinkItems().first().find { it.id == proj.id }
                if (fullItem != null) {
                    results += RetrievedItem(
                        id = proj.id,
                        contentType = ContentType.LINK,
                        label = fullItem.title ?: fullItem.url ?: "Link",
                        contextText = buildString {
                            append("[Saved Item - Link]\n")
                            fullItem.title?.let { append("Title: $it\n") }
                            fullItem.description?.let { append("Description: $it\n") }
                            fullItem.url?.let { append("URL: $it") }
                        },
                        similarityScore = score
                    ) to score
                }
            }
        }

        return results.sortedByDescending { it.second }.take(topK).map { it.first }
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

                // Now that OCR text is available, generate embedding for this image.
                // (At save time the text was blank, so embedding was skipped then.)
                val apiKey = settingsManager.getApiKey() ?: return@launch
                if (extractedText.isBlank()) return@launch
                val imageItem = databaseManager.getAllImageItems().first()
                    .find { it.originalUri == imageUri || it.localPath == imageUri }
                    ?: return@launch
                val result = openRouterService.generateEmbedding(extractedText.take(2000), apiKey)
                result.onSuccess { embedding ->
                    databaseManager.updateImageItemEmbedding(
                        imageItem.id,
                        EmbeddingUtils.serializeEmbedding(embedding)
                    )
                    println("DEBUG: Image embedding stored after OCR for id=${imageItem.id}")
                }
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
        var showOptionsSheet by remember { mutableStateOf(false) }
        var selectedItemForAction by remember { mutableStateOf<Any?>(null) }
        var showDeleteConfirmation by remember { mutableStateOf(false) }
        var selectedTextItemForEdit by remember { mutableStateOf<TextItem?>(null) }
        var selectedLinkItemForEdit by remember { mutableStateOf<LinkItem?>(null) }
        var currentTab by remember { mutableStateOf("home") }
        var activeConversationId by remember { mutableStateOf<Int?>(null) }
        var chatInput by remember { mutableStateOf("") }
        var isChatLoading by remember { mutableStateOf(false) }
        var chatError by remember { mutableStateOf<String?>(null) }
        var lastUserMessage by remember { mutableStateOf<String?>(null) }
        val chatSnackbarHostState = remember { SnackbarHostState() }
        val categoryCounterScope = rememberCoroutineScope()
        var activeTagFilter by remember { mutableStateOf<TagEntity?>(null) }
        var showAddTagSheet by remember { mutableStateOf(false) }
        var tagSheetTargetItemId by remember { mutableStateOf<Long?>(null) }
        var tagSheetTargetContentType by remember { mutableStateOf<ContentType?>(null) }
        val allTags by databaseManager.getAllTags().collectAsState(initial = emptyList())

        // Item IDs whose tag names match the current search query
        var tagSearchTextIds by remember { mutableStateOf<Set<Long>>(emptySet()) }
        var tagSearchImageIds by remember { mutableStateOf<Set<Long>>(emptySet()) }
        var tagSearchLinkIds by remember { mutableStateOf<Set<Long>>(emptySet()) }

        LaunchedEffect(searchQuery, allTags) {
            if (searchQuery.isBlank()) {
                tagSearchTextIds = emptySet()
                tagSearchImageIds = emptySet()
                tagSearchLinkIds = emptySet()
                return@LaunchedEffect
            }
            val matchingTags = allTags.filter { it.name.contains(searchQuery, ignoreCase = true) }
            val tText = mutableSetOf<Long>()
            val tImage = mutableSetOf<Long>()
            val tLink = mutableSetOf<Long>()
            matchingTags.forEach { tag ->
                databaseManager.getItemIdsByTag(tag.id, ContentType.TEXT).first().forEach { tText += it }
                databaseManager.getItemIdsByTag(tag.id, ContentType.IMAGE).first().forEach { tImage += it }
                databaseManager.getItemIdsByTag(tag.id, ContentType.LINK).first().forEach { tLink += it }
            }
            tagSearchTextIds = tText
            tagSearchImageIds = tImage
            tagSearchLinkIds = tLink
        }

        // Filtered item ID sets for tag filter — empty sets mean "show all"
        val tagFilteredTextIds by remember(activeTagFilter) {
            activeTagFilter?.let { tag ->
                databaseManager.getItemIdsByTag(tag.id, ContentType.TEXT)
            } ?: flowOf(emptyList())
        }.collectAsState(initial = emptyList())
        val tagFilteredImageIds by remember(activeTagFilter) {
            activeTagFilter?.let { tag ->
                databaseManager.getItemIdsByTag(tag.id, ContentType.IMAGE)
            } ?: flowOf(emptyList())
        }.collectAsState(initial = emptyList())
        val tagFilteredLinkIds by remember(activeTagFilter) {
            activeTagFilter?.let { tag ->
                databaseManager.getItemIdsByTag(tag.id, ContentType.LINK)
            } ?: flowOf(emptyList())
        }.collectAsState(initial = emptyList())

        fun deleteSelectedItem() {
            val item = selectedItemForAction ?: return
            lifecycleScope.launch(Dispatchers.IO) {
                when (item) {
                    is TextItem  -> databaseManager.deleteTextItem(DataMapper.toTextItemEntity(item))
                    is ImageItem -> databaseManager.deleteImageItem(DataMapper.toImageItemEntity(item))
                    is LinkItem  -> databaseManager.deleteLinkItem(DataMapper.toLinkItemEntity(item))
                }
            }
            selectedItemForAction = null
        }

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
                currentTab == "settings" -> { currentTab = "home" }
                currentTab == "chat" && activeConversationId != null -> {
                    activeConversationId = null
                    chatInput = ""
                    isChatLoading = false
                }
                currentTab == "chat" -> { currentTab = "home" }
                currentScreen == "editText" -> {
                    currentScreen = "text"
                    selectedTextItemForEdit = null
                }
                currentScreen == "editLink" -> {
                    currentScreen = "link"
                    selectedLinkItemForEdit = null
                }
                isSearching -> {
                    isSearching = false
                    searchQuery = ""
                }
                currentScreen != "main" -> {
                    currentScreen = "main"
                }
                else -> {
                    // On main screen — let the system handle back (close app)
                }
            }
        }
        
        // Trigger counter animations whenever counts change
        LaunchedEffect(imageCount) { if (imageCount > 0) triggerImageAddedFeedback() }
        LaunchedEffect(textCount) { if (textCount > 0) triggerTextAddedFeedback() }
        LaunchedEffect(linkCount) { if (linkCount > 0) triggerLinkAddedFeedback() }



        // Hide bottom nav inside full-screen sub-screens
        val showBottomNav = selectedImageItem == null &&
            activeConversationId == null &&
            currentScreen !in listOf("editText", "editLink")

        Scaffold(
            modifier = Modifier.fillMaxSize(),
            containerColor = SBBackground,
            bottomBar = {
                if (showBottomNav) {
                    SecondBrainBottomNav(
                        currentTab = currentTab,
                        onTabSelected = { tab ->
                            currentTab = tab
                            if (tab != "chat") activeConversationId = null
                        }
                    )
                }
            }
        ) { innerPadding ->
            when {
                currentTab == "settings" -> {
                    SettingsScreen(
                        onBack = { currentTab = "home" },
                        modifier = Modifier.padding(innerPadding)
                    )
                }
                currentTab == "chat" && activeConversationId == null -> {
                    ConversationListScreen(
                        onOpenConversation = { convId ->
                            activeConversationId = convId
                            chatInput = ""
                        },
                        onNewConversation = {
                            categoryCounterScope.launch(Dispatchers.IO) {
                                val id = databaseManager.createConversation("New Conversation")
                                withContext(Dispatchers.Main) {
                                    activeConversationId = id
                                    chatInput = ""
                                }
                            }
                        },
                        onBack = { currentTab = "home" },
                        modifier = Modifier.padding(innerPadding)
                    )
                }
                currentTab == "chat" && activeConversationId != null -> {
                    ChatScreen(
                        conversationId = activeConversationId!!,
                        hasApiKey = settingsManager.getApiKey() != null,
                        chatInput = chatInput,
                        isChatLoading = isChatLoading,
                        snackbarHostState = chatSnackbarHostState,
                        onInputChange = { chatInput = it },
                        onSend = {
                            val apiKey = settingsManager.getApiKey()
                            if (apiKey == null) {
                                // Banner already shown; do nothing
                                return@ChatScreen
                            }
                            if (!isNetworkAvailable()) {
                                categoryCounterScope.launch {
                                    chatSnackbarHostState.showSnackbar(
                                        "No internet connection. Chat requires an internet connection."
                                    )
                                }
                                return@ChatScreen
                            }
                            val question = chatInput.take(500).trim()
                            if (question.isBlank()) return@ChatScreen

                            val convId = activeConversationId ?: return@ChatScreen
                            val userMessage = ChatMessageEntity(
                                role = "user",
                                content = question,
                                timestamp = System.currentTimeMillis(),
                                conversationId = convId
                            )
                            lastUserMessage = question
                            chatInput = ""
                            isChatLoading = true

                            lifecycleScope.launch(Dispatchers.IO) {
                                databaseManager.insertChatMessage(userMessage)
                                // Auto-title: set conversation title to first message if still default
                                val isFirstMessage = databaseManager
                                    .getChatMessages(convId).first().size <= 1
                                if (isFirstMessage) {
                                    databaseManager.updateConversationTitle(
                                        convId, question.take(50)
                                    )
                                }

                                // a. Embed the question
                                val embeddingResult = openRouterService.generateEmbedding(question, apiKey)
                                if (embeddingResult.isFailure) {
                                    withContext(Dispatchers.Main) {
                                        isChatLoading = false
                                        chatError = question
                                        categoryCounterScope.launch {
                                            val action = chatSnackbarHostState.showSnackbar(
                                                message = "Failed to get a response. Tap to retry.",
                                                actionLabel = "Retry"
                                            )
                                            if (action == SnackbarResult.ActionPerformed) {
                                                chatError?.let { retry ->
                                                    // Re-trigger send with last message
                                                    chatInput = retry
                                                }
                                            }
                                        }
                                    }
                                    return@launch
                                }

                                // b. Retrieve relevant items
                                val questionEmbedding = embeddingResult.getOrThrow()
                                val retrievedItems = retrieveRelevantItems(questionEmbedding)

                                // c. Get conversation history
                                val history = databaseManager.getRecentChatMessages(convId, limit = 20)

                                // d. Call chat API
                                val chatResult = openRouterService.chat(question, history, retrievedItems, apiKey)
                                if (chatResult.isFailure) {
                                    withContext(Dispatchers.Main) {
                                        isChatLoading = false
                                        chatError = question
                                        categoryCounterScope.launch {
                                            val action = chatSnackbarHostState.showSnackbar(
                                                message = "Failed to get a response. Tap to retry.",
                                                actionLabel = "Retry"
                                            )
                                            if (action == SnackbarResult.ActionPerformed) {
                                                chatError?.let { retry -> chatInput = retry }
                                            }
                                        }
                                    }
                                    return@launch
                                }

                                // e. Save assistant message with source metadata
                                val gson = com.google.gson.Gson()
                                val assistantMessage = ChatMessageEntity(
                                    role = "assistant",
                                    content = chatResult.getOrThrow(),
                                    timestamp = System.currentTimeMillis(),
                                    sourceIds = if (retrievedItems.isEmpty()) null
                                                else gson.toJson(retrievedItems.map { it.id }),
                                    sourceTypes = if (retrievedItems.isEmpty()) null
                                                  else gson.toJson(retrievedItems.map { it.contentType.name }),
                                    conversationId = convId
                                )
                                databaseManager.insertChatMessage(assistantMessage)

                                withContext(Dispatchers.Main) {
                                    isChatLoading = false
                                    chatError = null
                                }
                            }
                        },
                        onGoToSettings = { currentTab = "settings" },
                        onBack = {
                            activeConversationId = null
                            chatInput = ""
                            isChatLoading = false
                        },
                        modifier = Modifier.padding(innerPadding)
                    )
                }
                selectedImageItem != null -> {
                    FullScreenImageViewer(
                        imageItem = selectedImageItem!!,
                        onClose = { selectedImageItem = null },
                        modifier = Modifier.padding(innerPadding)
                    )
                }
                else -> {
                when (currentScreen) {
                "main" -> {
                    // Home A — Mixed feed (time-sorted across all content types)
                    val feedItems = remember(textItems, imageItems, linkItems) {
                        (textItems + imageItems + linkItems).sortedByDescending { item ->
                            when (item) {
                                is TextItem  -> item.timestamp
                                is ImageItem -> item.timestamp
                                is LinkItem  -> item.timestamp
                                else -> 0L
                            }
                        }
                    }
                    Box(modifier = Modifier.fillMaxSize().padding(innerPadding)) {
                        Column(modifier = Modifier.fillMaxSize()) {
                            // Top bar
                            Row(
                                modifier = Modifier.fillMaxWidth()
                                    .padding(start = 22.dp, end = 8.dp, top = 10.dp, bottom = 4.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text("Second Brain", fontSize = 22.sp, fontWeight = FontWeight.Bold,
                                    letterSpacing = (-0.2).sp, color = MaterialTheme.colorScheme.onSurface)
                            }
                            // Search pill
                            val searchFocusRequester = remember { FocusRequester() }
                            LaunchedEffect(isSearching) {
                                if (isSearching) searchFocusRequester.requestFocus()
                            }
                            Box(
                                modifier = Modifier.fillMaxWidth()
                                    .padding(horizontal = 18.dp, vertical = 6.dp)
                                    .height(52.dp)
                                    .background(SBSurface, RoundedCornerShape(26.dp))
                                    .clickable { isSearching = true }
                                    .padding(horizontal = 18.dp),
                                contentAlignment = Alignment.CenterStart
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                                    Icon(Icons.Default.Search, contentDescription = null, tint = SBOnSurfaceVariant, modifier = Modifier.size(22.dp))
                                    if (isSearching) {
                                        BasicTextField(
                                            value = searchQuery, onValueChange = { searchQuery = it },
                                            modifier = Modifier.weight(1f).focusRequester(searchFocusRequester),
                                            textStyle = androidx.compose.ui.text.TextStyle(color = MaterialTheme.colorScheme.onSurface, fontSize = 15.5.sp),
                                            cursorBrush = androidx.compose.ui.graphics.SolidColor(SBPrimary),
                                            singleLine = true,
                                            decorationBox = { inner ->
                                                if (searchQuery.isEmpty()) Text("Search your brain", color = SBMuted, fontSize = 15.5.sp)
                                                inner()
                                            }
                                        )
                                        IconButton(onClick = { searchQuery = ""; isSearching = false }, modifier = Modifier.size(36.dp)) {
                                            Icon(Icons.Default.Close, contentDescription = "Clear", tint = SBMuted, modifier = Modifier.size(18.dp))
                                        }
                                    } else {
                                        Text("Search your brain", color = SBMuted, fontSize = 15.5.sp, modifier = Modifier.weight(1f))
                                    }
                                }
                            }
                            if (isSearching) {
                                val sTxt = if (activeTagFilter == null) textItems else textItems.filter { (it as? TextItem)?.id in tagFilteredTextIds }
                                val sImg = if (activeTagFilter == null) imageItems else imageItems.filter { it.id in tagFilteredImageIds }
                                val sLnk = if (activeTagFilter == null) linkItems else linkItems.filter { it.id in tagFilteredLinkIds }
                                SearchResults(
                                    query = searchQuery, textItems = sTxt, imageItems = sImg, linkItems = sLnk,
                                    tagMatchedTextIds = tagSearchTextIds, tagMatchedImageIds = tagSearchImageIds, tagMatchedLinkIds = tagSearchLinkIds,
                                    modifier = Modifier.weight(1f), onImageClick = { selectedImageItem = it }
                                )
                            } else {
                                // Feed
                                LazyColumn(
                                    modifier = Modifier.weight(1f),
                                    contentPadding = PaddingValues(horizontal = 18.dp, vertical = 8.dp),
                                    verticalArrangement = Arrangement.spacedBy(12.dp)
                                ) {
                                    // ── Stat cards ───────────────────────────
                                    item {
                                        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                                            listOf(
                                                Triple("Notes",  textCount,  { currentScreen = "text" }),
                                                Triple("Images", imageCount, { currentScreen = "image" }),
                                                Triple("Links",  linkCount,  { currentScreen = "link" })
                                            ).forEach { (label, count, onClick) ->
                                                Box(modifier = Modifier.weight(1f)) {
                                                    DesignCategoryCard(label = label, count = count, onClick = onClick)
                                                }
                                            }
                                        }
                                    }
                                    if (feedItems.isEmpty()) {
                                        item {
                                            Column(modifier = Modifier.fillMaxWidth().padding(vertical = 40.dp),
                                                horizontalAlignment = Alignment.CenterHorizontally) {
                                                Text("Nothing saved yet", fontSize = 17.sp, fontWeight = FontWeight.Medium, color = SBMuted)
                                                Spacer(modifier = Modifier.height(6.dp))
                                                Text("Share content to get started", fontSize = 14.sp, color = SBFaint)
                                            }
                                        }
                                    } else {
                                        items(feedItems) { item ->
                                            HomeFeedCard(
                                                item = item,
                                                onImageClick = { selectedImageItem = it },
                                                onLongPress = { selectedItemForAction = it; showOptionsSheet = true }
                                            )
                                        }
                                    }
                                }
                            }
                        }
                        // FAB
                        FloatingActionButton(
                            onClick = { onUploadImage() },
                            modifier = Modifier.align(Alignment.BottomEnd).padding(bottom = 18.dp, end = 18.dp),
                            shape = RoundedCornerShape(18.dp),
                            containerColor = SBPrimary,
                            contentColor = SBOnPrimary
                        ) {
                            Icon(Icons.Default.Add, contentDescription = "Add", modifier = Modifier.size(24.dp))
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
                        TagFilterBar(
                            allTags = allTags,
                            activeTag = activeTagFilter,
                            onTagSelected = { tag -> activeTagFilter = if (activeTagFilter?.id == tag?.id) null else tag }
                        )
                        val filteredText = if (activeTagFilter == null) textItems
                            else textItems.filter { (it as? TextItem)?.id in tagFilteredTextIds }
                        ContentList(
                            items = filteredText,
                            category = "Text",
                            onLongPress = { item ->
                                selectedItemForAction = item
                                showOptionsSheet = true
                            }
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
                        TagFilterBar(
                            allTags = allTags,
                            activeTag = activeTagFilter,
                            onTagSelected = { tag -> activeTagFilter = if (activeTagFilter?.id == tag?.id) null else tag }
                        )
                        val filteredImage = if (activeTagFilter == null) imageItems
                            else imageItems.filter { (it as? ImageItem)?.id in tagFilteredImageIds }
                        ContentList(
                            items = filteredImage,
                            category = "Image",
                            onImageClick = { imageItem ->
                                selectedImageItem = imageItem
                            },
                            onLongPress = { item ->
                                selectedItemForAction = item
                                showOptionsSheet = true
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
                        TagFilterBar(
                            allTags = allTags,
                            activeTag = activeTagFilter,
                            onTagSelected = { tag -> activeTagFilter = if (activeTagFilter?.id == tag?.id) null else tag }
                        )
                        val filteredLink = if (activeTagFilter == null) linkItems
                            else linkItems.filter { (it as? LinkItem)?.id in tagFilteredLinkIds }
                        ContentList(
                            items = filteredLink,
                            category = "Link",
                            onLongPress = { item ->
                                selectedItemForAction = item
                                showOptionsSheet = true
                            }
                        )
                    }
                }
                "editText" -> {
                    if (selectedTextItemForEdit != null) {
                        EditTextItemScreen(
                            item = selectedTextItemForEdit!!,
                            onCancel = {
                                currentScreen = "text"
                                selectedTextItemForEdit = null
                            },
                            onSave = { updatedContent ->
                                val updated = DataMapper.toTextItemEntity(
                                    selectedTextItemForEdit!!.copy(content = updatedContent)
                                )
                                lifecycleScope.launch(Dispatchers.IO) {
                                    databaseManager.updateTextItem(updated)
                                }
                                currentScreen = "text"
                                selectedTextItemForEdit = null
                            },
                            modifier = Modifier.padding(innerPadding)
                        )
                    }
                }
                "editLink" -> {
                    if (selectedLinkItemForEdit != null) {
                        EditLinkItemScreen(
                            item = selectedLinkItemForEdit!!,
                            onCancel = {
                                currentScreen = "link"
                                selectedLinkItemForEdit = null
                            },
                            onSave = { updatedUrl ->
                                val oldUrl = selectedLinkItemForEdit!!.url ?: ""
                                val updatedEntity = DataMapper.toLinkItemEntity(
                                    selectedLinkItemForEdit!!.copy(
                                        url = updatedUrl,
                                        title = null,
                                        description = null,
                                        imageUrl = null
                                    )
                                )
                                lifecycleScope.launch(Dispatchers.IO) {
                                    databaseManager.updateLinkItem(updatedEntity)
                                    // Re-fetch metadata in background; ignore failures per spec
                                    val withMetadata = fetchLinkMetadata(updatedUrl)
                                    updateLinkItem(updatedUrl, withMetadata)
                                }
                                currentScreen = "link"
                                selectedLinkItemForEdit = null
                            },
                            modifier = Modifier.padding(innerPadding)
                        )
                    }
                }
            }
        } // end else (non-settings, non-chat)
        } // end Scaffold content

        if (showOptionsSheet && selectedItemForAction != null) {
            ItemOptionsBottomSheet(
                item = selectedItemForAction!!,
                onEdit = {
                    showOptionsSheet = false
                    when (val item = selectedItemForAction) {
                        is TextItem -> {
                            selectedTextItemForEdit = item
                            currentScreen = "editText"
                        }
                        is LinkItem -> {
                            selectedLinkItemForEdit = item
                            currentScreen = "editLink"
                        }
                    }
                },
                onDelete = {
                    showOptionsSheet = false
                    showDeleteConfirmation = true
                },
                onTags = {
                    showOptionsSheet = false
                    val item = selectedItemForAction
                    tagSheetTargetItemId = when (item) {
                        is TextItem  -> item.id
                        is ImageItem -> item.id
                        is LinkItem  -> item.id
                        else -> null
                    }
                    tagSheetTargetContentType = when (item) {
                        is TextItem  -> ContentType.TEXT
                        is ImageItem -> ContentType.IMAGE
                        is LinkItem  -> ContentType.LINK
                        else -> null
                    }
                    if (tagSheetTargetItemId != null) showAddTagSheet = true
                },
                onDismiss = { showOptionsSheet = false }
            )
        }

        if (showAddTagSheet && tagSheetTargetItemId != null && tagSheetTargetContentType != null) {
            AddTagBottomSheet(
                itemId = tagSheetTargetItemId!!,
                contentType = tagSheetTargetContentType!!,
                allTags = allTags,
                onDismiss = {
                    showAddTagSheet = false
                    tagSheetTargetItemId = null
                    tagSheetTargetContentType = null
                }
            )
        }

        if (showDeleteConfirmation) {
            DeleteConfirmationDialog(
                onConfirm = {
                    deleteSelectedItem()
                    showDeleteConfirmation = false
                },
                onDismiss = {
                    showDeleteConfirmation = false
                    selectedItemForAction = null
                }
            )
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

@OptIn(ExperimentalFoundationApi::class)
@Composable
    private fun ContentList(
        items: List<Any>,
        category: String,
        modifier: Modifier = Modifier,
        onImageClick: ((ImageItem) -> Unit)? = null,
        onLongPress: (Any) -> Unit = {}
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
                        modifier = Modifier
                            .fillMaxWidth()
                            .combinedClickable(
                                onClick = {
                                    if (item is ImageItem && onImageClick != null) {
                                        println("DEBUG: Image card clicked, opening full-screen viewer")
                                        onImageClick(item)
                                    }
                                },
                                onLongClick = { onLongPress(item) }
                            ),
                        shape = RoundedCornerShape(8.dp),
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
                                    TagChipRow(itemId = item.id, contentType = ContentType.IMAGE)
                                }
                                is TextItem -> {
                                    // Display text content with null safety
                                    Text(
                                        text = item.content ?: "No content",
                                        fontSize = 14.sp,
                                        color = MaterialTheme.colorScheme.onSurface,
                                        modifier = Modifier.fillMaxWidth()
                                    )
                                    TagChipRow(itemId = item.id, contentType = ContentType.TEXT)
                                }
                                is LinkItem -> {
                                    // Display link with metadata
                                    Column(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .combinedClickable(
                                                onClick = {
                                                    item.url?.let { url -> openUrlInBrowser(url) }
                                                },
                                                onLongClick = { onLongPress(item) }
                                            ),
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
                                        TagChipRow(itemId = item.id, contentType = ContentType.LINK)
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
    private fun TagChipRow(itemId: Long, contentType: ContentType) {
        val tags by databaseManager.getTagsForItem(itemId, contentType)
            .collectAsState(initial = emptyList())
        if (tags.isEmpty()) return
        val displayed = tags.take(3)
        val extra = tags.size - displayed.size
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            displayed.forEach { tag ->
                SuggestionChip(
                    onClick = {},
                    label = { Text(tag.name, fontSize = 11.sp) }
                )
            }
            if (extra > 0) {
                SuggestionChip(
                    onClick = {},
                    label = { Text("+$extra more", fontSize = 11.sp) }
                )
            }
        }
    }

    @OptIn(ExperimentalMaterial3Api::class)
    @Composable
    private fun EditTextItemScreen(
        item: TextItem,
        onCancel: () -> Unit,
        onSave: (String) -> Unit,
        modifier: Modifier = Modifier
    ) {
        var content by remember { mutableStateOf(item.content ?: "") }
        val isBlank = content.isBlank()

        Scaffold(
            modifier = modifier.fillMaxSize(),
            topBar = {
                TopAppBar(
                    title = { Text("Edit Text") },
                    navigationIcon = {
                        IconButton(onClick = onCancel) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Cancel")
                        }
                    },
                    actions = {
                        TextButton(
                            onClick = { if (!isBlank) onSave(content.trim()) },
                            enabled = !isBlank
                        ) {
                            Text("Save")
                        }
                    }
                )
            }
        ) { innerPadding ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .padding(16.dp)
            ) {
                OutlinedTextField(
                    value = content,
                    onValueChange = { content = it },
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    placeholder = { Text("Enter text content") },
                    isError = isBlank,
                    supportingText = if (isBlank) {
                        { Text("Content cannot be empty") }
                    } else null
                )
            }
        }
    }

    @OptIn(ExperimentalMaterial3Api::class)
    @Composable
    private fun EditLinkItemScreen(
        item: LinkItem,
        onCancel: () -> Unit,
        onSave: (String) -> Unit,
        modifier: Modifier = Modifier
    ) {
        var url by remember { mutableStateOf(item.url ?: "") }
        val isInvalid = url.isBlank() || (!url.startsWith("http://") && !url.startsWith("https://"))

        Scaffold(
            modifier = modifier.fillMaxSize(),
            topBar = {
                TopAppBar(
                    title = { Text("Edit Link") },
                    navigationIcon = {
                        IconButton(onClick = onCancel) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Cancel")
                        }
                    },
                    actions = {
                        TextButton(
                            onClick = { if (!isInvalid) onSave(url.trim()) },
                            enabled = !isInvalid
                        ) {
                            Text("Save")
                        }
                    }
                )
            }
        ) { innerPadding ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .padding(16.dp)
            ) {
                OutlinedTextField(
                    value = url,
                    onValueChange = { url = it },
                    modifier = Modifier.fillMaxWidth(),
                    placeholder = { Text("https://example.com") },
                    singleLine = true,
                    isError = isInvalid,
                    supportingText = when {
                        url.isBlank() -> { { Text("URL cannot be empty") } }
                        isInvalid     -> { { Text("URL must start with http:// or https://") } }
                        else          -> null
                    }
                )
            }
        }
    }

    @OptIn(ExperimentalMaterial3Api::class)
    @Composable
    private fun ChatScreen(
        conversationId: Int,
        hasApiKey: Boolean,
        chatInput: String,
        isChatLoading: Boolean,
        snackbarHostState: SnackbarHostState = remember { SnackbarHostState() },
        onInputChange: (String) -> Unit,
        onSend: () -> Unit,
        onGoToSettings: () -> Unit,
        onBack: () -> Unit,
        modifier: Modifier = Modifier
    ) {
        val messages by databaseManager.getChatMessages(conversationId).collectAsState(initial = emptyList())
        val listState = rememberLazyListState()
        var showNewConversationDialog by remember { mutableStateOf(false) }

        // Auto-scroll to bottom when new messages arrive
        LaunchedEffect(messages.size) {
            if (messages.isNotEmpty()) {
                listState.animateScrollToItem(messages.size - 1)
            }
        }

        BackHandler { onBack() }

        Scaffold(
            modifier = modifier.fillMaxSize(),
            snackbarHost = { SnackbarHost(snackbarHostState) },
            topBar = {
                TopAppBar(
                    title = { Text("Chat") },
                    navigationIcon = {
                        IconButton(onClick = onBack) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                        }
                    },
                    actions = {
                        IconButton(onClick = { showNewConversationDialog = true }) {
                            Icon(Icons.Default.Add, contentDescription = "New conversation")
                        }
                    }
                )
            },
            bottomBar = {
                ChatInputBar(
                    input = chatInput,
                    isLoading = isChatLoading,
                    enabled = hasApiKey,
                    onInputChange = onInputChange,
                    onSend = onSend
                )
            }
        ) { innerPadding ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
            ) {
                if (!hasApiKey) {
                    NoApiKeyBanner(onGoToSettings = onGoToSettings)
                }

                if (messages.isEmpty() && !isChatLoading) {
                    // Empty state
                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth()
                            .padding(32.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Text("💬", fontSize = 48.sp, modifier = Modifier.padding(bottom = 16.dp))
                        Text(
                            "Ask anything about your saved content",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Medium,
                            textAlign = TextAlign.Center
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            "Your notes, images, and links are your knowledge base",
                            fontSize = 14.sp,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                            textAlign = TextAlign.Center
                        )
                    }
                } else {
                    LazyColumn(
                        state = listState,
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth()
                            .padding(horizontal = 12.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                        contentPadding = androidx.compose.foundation.layout.PaddingValues(vertical = 12.dp)
                    ) {
                        items(messages) { message ->
                            if (message.role == "user") {
                                UserMessageBubble(message)
                            } else {
                                AssistantMessageBubble(message)
                            }
                        }
                        if (isChatLoading) {
                            item { TypingIndicator() }
                        }
                    }
                }
            }
        }

        if (showNewConversationDialog) {
            NewConversationDialog(
                onConfirm = {
                    showNewConversationDialog = false
                    onBack() // go back to conversation list
                },
                onDismiss = { showNewConversationDialog = false }
            )
        }
    }

    @Composable
    private fun UserMessageBubble(message: ChatMessageEntity) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.End
        ) {
            Column(horizontalAlignment = Alignment.End) {
                Box(
                    modifier = Modifier
                        .background(
                            MaterialTheme.colorScheme.primary,
                            RoundedCornerShape(16.dp, 4.dp, 16.dp, 16.dp)
                        )
                        .padding(horizontal = 14.dp, vertical = 10.dp)
                        .widthIn(max = 280.dp)
                ) {
                    Text(
                        text = message.content,
                        color = MaterialTheme.colorScheme.onPrimary,
                        fontSize = 14.sp
                    )
                }
                Text(
                    text = java.text.SimpleDateFormat("HH:mm", java.util.Locale.getDefault())
                        .format(java.util.Date(message.timestamp)),
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f),
                    modifier = Modifier.padding(top = 2.dp, end = 4.dp)
                )
            }
        }
    }

    @Composable
    private fun AssistantMessageBubble(message: ChatMessageEntity) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.Start
        ) {
            Column {
                Box(
                    modifier = Modifier
                        .background(
                            MaterialTheme.colorScheme.surfaceVariant,
                            RoundedCornerShape(4.dp, 16.dp, 16.dp, 16.dp)
                        )
                        .padding(horizontal = 14.dp, vertical = 10.dp)
                        .widthIn(max = 280.dp)
                ) {
                    Text(
                        text = message.content,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontSize = 14.sp
                    )
                }
                SourcesSection(sourceIds = message.sourceIds, sourceTypes = message.sourceTypes)
                Text(
                    text = java.text.SimpleDateFormat("HH:mm", java.util.Locale.getDefault())
                        .format(java.util.Date(message.timestamp)),
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f),
                    modifier = Modifier.padding(top = 2.dp, start = 4.dp)
                )
            }
        }
    }

    @Composable
    private fun SourcesSection(sourceIds: String?, sourceTypes: String?) {
        if (sourceIds.isNullOrEmpty() || sourceTypes.isNullOrEmpty()) return
        val gson = com.google.gson.Gson()
        val ids = runCatching {
            gson.fromJson(sourceIds, Array<Long>::class.java).toList()
        }.getOrElse { emptyList() }
        val types = runCatching {
            gson.fromJson(sourceTypes, Array<String>::class.java).toList()
        }.getOrElse { emptyList() }
        if (ids.isEmpty()) return
        Column(modifier = Modifier.padding(start = 4.dp, top = 4.dp)) {
            Text(
                "Sources used:",
                fontSize = 11.sp,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
            )
            ids.zip(types).forEach { (id, type) ->
                Text(
                    "• ${type.lowercase().replaceFirstChar { it.uppercase() }} #$id",
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                )
            }
        }
    }

    @Composable
    private fun TypingIndicator() {
        val transition = rememberInfiniteTransition(label = "typing")
        val alpha by transition.animateFloat(
            initialValue = 0.3f,
            targetValue = 1f,
            animationSpec = infiniteRepeatable(tween(700), RepeatMode.Reverse),
            label = "typingAlpha"
        )
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.Start
        ) {
            Box(
                modifier = Modifier
                    .background(
                        MaterialTheme.colorScheme.surfaceVariant,
                        RoundedCornerShape(4.dp, 16.dp, 16.dp, 16.dp)
                    )
                    .padding(horizontal = 16.dp, vertical = 12.dp)
            ) {
                Text(
                    "● ● ●",
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = alpha),
                    fontSize = 14.sp,
                    letterSpacing = 4.sp
                )
            }
        }
    }

    @Composable
    private fun ChatInputBar(
        input: String,
        isLoading: Boolean,
        enabled: Boolean,
        onInputChange: (String) -> Unit,
        onSend: () -> Unit
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(MaterialTheme.colorScheme.surface)
                .padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            OutlinedTextField(
                value = input,
                onValueChange = onInputChange,
                modifier = Modifier.weight(1f),
                placeholder = { Text("Ask a question…") },
                enabled = enabled && !isLoading,
                singleLine = false,
                maxLines = 4,
                shape = RoundedCornerShape(24.dp)
            )
            IconButton(
                onClick = onSend,
                enabled = enabled && !isLoading && input.isNotBlank(),
                modifier = Modifier
                    .size(48.dp)
                    .background(
                        if (enabled && !isLoading && input.isNotBlank())
                            MaterialTheme.colorScheme.primary
                        else
                            MaterialTheme.colorScheme.onSurface.copy(alpha = 0.12f),
                        shape = CircleShape
                    )
            ) {
                if (isLoading) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(20.dp),
                        strokeWidth = 2.dp,
                        color = MaterialTheme.colorScheme.onPrimary
                    )
                } else {
                    Icon(
                        Icons.Default.Send,
                        contentDescription = "Send",
                        tint = if (enabled && input.isNotBlank())
                            MaterialTheme.colorScheme.onPrimary
                        else
                            MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f)
                    )
                }
            }
        }
    }

    @Composable
    private fun NoApiKeyBanner(onGoToSettings: () -> Unit) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(MaterialTheme.colorScheme.errorContainer)
                .padding(horizontal = 16.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                "No API key set. Add your OpenRouter key in Settings to use chat.",
                fontSize = 13.sp,
                color = MaterialTheme.colorScheme.onErrorContainer,
                modifier = Modifier.weight(1f)
            )
            TextButton(onClick = onGoToSettings) {
                Text("Go to Settings", fontSize = 13.sp)
            }
        }
    }

    @Composable
    private fun NewConversationDialog(onConfirm: () -> Unit, onDismiss: () -> Unit) {
        AlertDialog(
            onDismissRequest = onDismiss,
            title = { Text("Start a new conversation?") },
            text = { Text("This will clear the current chat history.") },
            confirmButton = {
                TextButton(onClick = onConfirm) { Text("Clear") }
            },
            dismissButton = {
                TextButton(onClick = onDismiss) { Text("Cancel") }
            }
        )
    }

    @OptIn(ExperimentalFoundationApi::class, ExperimentalMaterial3Api::class)
    @Composable
    private fun ConversationListScreen(
        onOpenConversation: (Int) -> Unit,
        onNewConversation: () -> Unit,
        onBack: () -> Unit,
        modifier: Modifier = Modifier
    ) {
        val conversations by databaseManager.getAllConversations().collectAsState(initial = emptyList())
        var conversationToDelete by remember { mutableStateOf<ConversationEntity?>(null) }

        BackHandler { onBack() }

        Column(
            modifier = modifier
                .fillMaxSize()
                .background(SBBackground)
        ) {
            // Top bar
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 22.dp, end = 8.dp, top = 10.dp, bottom = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text("Chat", fontSize = 22.sp, fontWeight = FontWeight.Bold, letterSpacing = (-0.2).sp)
                IconButton(onClick = onNewConversation) {
                    Icon(Icons.Default.Add, contentDescription = "New conversation", tint = SBOnSurfaceVariant)
                }
            }

            // Chat C — grouped by date
            val grouped = remember(conversations) {
                val now = java.util.Calendar.getInstance()
                val yesterday = java.util.Calendar.getInstance().apply { add(java.util.Calendar.DAY_OF_YEAR, -1) }
                conversations.groupBy { conv ->
                    val cal = java.util.Calendar.getInstance().apply { timeInMillis = conv.createdAt }
                    when {
                        cal.get(java.util.Calendar.DAY_OF_YEAR) == now.get(java.util.Calendar.DAY_OF_YEAR) &&
                            cal.get(java.util.Calendar.YEAR) == now.get(java.util.Calendar.YEAR) -> "Today"
                        cal.get(java.util.Calendar.DAY_OF_YEAR) == yesterday.get(java.util.Calendar.DAY_OF_YEAR) &&
                            cal.get(java.util.Calendar.YEAR) == yesterday.get(java.util.Calendar.YEAR) -> "Yesterday"
                        else -> "Earlier"
                    }
                }
            }

            if (conversations.isEmpty()) {
                Column(modifier = Modifier.fillMaxSize().padding(32.dp),
                    horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
                    Text("No conversations yet", fontSize = 17.sp, fontWeight = FontWeight.Medium, color = SBMuted)
                    Spacer(modifier = Modifier.height(6.dp))
                    Text("Tap + to start one", fontSize = 14.sp, color = SBFaint)
                }
            } else {
                LazyColumn(modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(horizontal = 18.dp, vertical = 4.dp)) {
                    listOf("Today", "Yesterday", "Earlier").forEach { group ->
                        val convs = grouped[group] ?: return@forEach
                        item {
                            Text(group.uppercase(), fontSize = 12.sp, fontWeight = FontWeight.Bold,
                                letterSpacing = 1.sp, color = SBMuted,
                                modifier = Modifier.padding(start = 4.dp, top = 14.dp, bottom = 8.dp))
                        }
                        items(convs, key = { it.id }) { conversation ->
                            Box(modifier = Modifier.padding(bottom = 8.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth()
                                        .background(SBSurfaceLow, RoundedCornerShape(14.dp))
                                        .combinedClickable(
                                            onClick = { onOpenConversation(conversation.id) },
                                            onLongClick = { conversationToDelete = conversation }
                                        )
                                        .padding(horizontal = 14.dp, vertical = 13.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(conversation.title, fontSize = 15.5.sp, fontWeight = FontWeight.Bold,
                                            letterSpacing = (-0.1).sp, color = MaterialTheme.colorScheme.onSurface,
                                            maxLines = 1, overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis)
                                        Text("Tap to open", fontSize = 13.sp, color = SBMuted, modifier = Modifier.padding(top = 1.dp),
                                            maxLines = 1, overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis)
                                    }
                                    Icon(Icons.AutoMirrored.Filled.ArrowBack,
                                        contentDescription = null, tint = SBFaint,
                                        modifier = Modifier.size(20.dp).graphicsLayer(rotationZ = 180f))
                                }
                            }
                        }
                    }
                }
            }
        }

        conversationToDelete?.let { conv ->
            AlertDialog(
                onDismissRequest = { conversationToDelete = null },
                title = { Text("Delete conversation?") },
                text = { Text("\"${conv.title}\" and all its messages will be deleted.") },
                confirmButton = {
                    TextButton(onClick = {
                        lifecycleScope.launch(Dispatchers.IO) { databaseManager.deleteConversation(conv.id) }
                        conversationToDelete = null
                    }) { Text("Delete", color = MaterialTheme.colorScheme.error) }
                },
                dismissButton = {
                    TextButton(onClick = { conversationToDelete = null }) { Text("Cancel") }
                }
            )
        }
    }

    @OptIn(ExperimentalMaterial3Api::class)
    @Composable
    private fun SettingsScreen(
        onBack: () -> Unit,
        modifier: Modifier = Modifier
    ) {
        var apiKeyInput by remember { mutableStateOf(settingsManager.getApiKey() ?: "") }
        var showKey by remember { mutableStateOf(false) }
        var isVerifyingKey by remember { mutableStateOf(false) }
        val snackbarHostState = remember { SnackbarHostState() }
        val scope = rememberCoroutineScope()
        val focusManager = androidx.compose.ui.platform.LocalFocusManager.current
        val keyboardController = androidx.compose.ui.platform.LocalSoftwareKeyboardController.current
        val hasKey = settingsManager.getApiKey() != null
        val settingsTextItems by databaseManager.getAllTextItems().collectAsState(initial = emptyList())
        val settingsImageItems by databaseManager.getAllImageItems().collectAsState(initial = emptyList())
        val settingsLinkItems by databaseManager.getAllLinkItems().collectAsState(initial = emptyList())
        val textCount = settingsTextItems.size
        val imageCount = settingsImageItems.size
        val linkCount = settingsLinkItems.size

        BackHandler { onBack() }

        Scaffold(
            modifier = modifier.fillMaxSize(),
            containerColor = SBBackground,
            snackbarHost = { SnackbarHost(snackbarHostState) }
        ) { innerPadding ->
            LazyColumn(
                modifier = Modifier.fillMaxSize().padding(innerPadding),
                contentPadding = PaddingValues(bottom = 32.dp)
            ) {
                item {
                    Row(modifier = Modifier.fillMaxWidth().padding(start = 22.dp, end = 8.dp, top = 10.dp, bottom = 8.dp),
                        verticalAlignment = Alignment.CenterVertically) {
                        Text("Settings", fontSize = 22.sp, fontWeight = FontWeight.Bold, letterSpacing = (-0.2).sp)
                    }
                }

                // API key hero card
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 18.dp, vertical = 8.dp)
                            .background(SBPrimaryTint, RoundedCornerShape(28.dp))
                            .border(1.dp, SBOutlineVariant, RoundedCornerShape(28.dp))
                            .padding(20.dp)
                    ) {
                        Column {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(
                                    modifier = Modifier.size(46.dp)
                                        .background(SBPrimaryContainer, RoundedCornerShape(14.dp)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(Icons.Default.Lock, contentDescription = null, tint = SBOnPrimaryContainer, modifier = Modifier.size(22.dp))
                                }
                                if (hasKey) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(5.dp)
                                    ) {
                                        Icon(Icons.Default.Settings, contentDescription = null, tint = SBOk, modifier = Modifier.size(15.dp))
                                        Text("Connected", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = SBOk)
                                    }
                                }
                            }
                            Spacer(modifier = Modifier.height(14.dp))
                            Text("OpenRouter API Key", fontSize = 18.sp, fontWeight = FontWeight.Bold, letterSpacing = (-0.2).sp)
                            Spacer(modifier = Modifier.height(10.dp))
                            // Key field
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(48.dp)
                                    .background(SBBackground, RoundedCornerShape(14.dp))
                                    .border(1.dp, SBOutline, RoundedCornerShape(14.dp))
                                    .padding(horizontal = 14.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                BasicTextField(
                                    value = apiKeyInput,
                                    onValueChange = { apiKeyInput = it },
                                    modifier = Modifier.weight(1f),
                                    textStyle = androidx.compose.ui.text.TextStyle(
                                        color = SBOnSurfaceVariant,
                                        fontSize = 13.sp,
                                        fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace,
                                        letterSpacing = 1.sp
                                    ),
                                    cursorBrush = androidx.compose.ui.graphics.SolidColor(SBPrimary),
                                    singleLine = true,
                                    visualTransformation = if (showKey) VisualTransformation.None else PasswordVisualTransformation(),
                                    decorationBox = { inner ->
                                        if (apiKeyInput.isEmpty()) Text("sk-or-v1-••••••••", color = SBFaint, fontSize = 13.sp,
                                            fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace)
                                        inner()
                                    }
                                )
                                TextButton(
                                    onClick = { showKey = !showKey },
                                    contentPadding = PaddingValues(horizontal = 8.dp)
                                ) {
                                    Text(if (showKey) "Hide" else "Show", fontSize = 12.sp, color = SBPrimary)
                                }
                            }
                            Spacer(modifier = Modifier.height(10.dp))
                            Text("Used for embeddings and chat. Stored encrypted on device.", fontSize = 12.sp, color = SBMuted, lineHeight = 17.sp)
                            Spacer(modifier = Modifier.height(14.dp))
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                Button(
                                    onClick = {
                                        val trimmedKey = apiKeyInput.trim()
                                        keyboardController?.hide()
                                        focusManager.clearFocus()
                                        isVerifyingKey = true
                                        scope.launch(Dispatchers.IO) {
                                            val result = openRouterService.generateEmbedding("test", trimmedKey)
                                            withContext(Dispatchers.Main) {
                                                isVerifyingKey = false
                                                if (result.isSuccess) {
                                                    settingsManager.saveApiKey(trimmedKey)
                                                    snackbarHostState.showSnackbar("API key verified and saved")
                                                } else {
                                                    snackbarHostState.showSnackbar("Invalid API key — not saved")
                                                }
                                            }
                                        }
                                    },
                                    enabled = apiKeyInput.isNotBlank() && !isVerifyingKey,
                                    shape = RoundedCornerShape(12.dp)
                                ) { Text(if (isVerifyingKey) "Verifying…" else "Save") }
                                OutlinedButton(
                                    onClick = {
                                        settingsManager.clearApiKey()
                                        apiKeyInput = ""
                                        scope.launch { snackbarHostState.showSnackbar("API key cleared") }
                                    },
                                    shape = RoundedCornerShape(12.dp),
                                    border = androidx.compose.foundation.BorderStroke(1.dp, SBOutline)
                                ) { Text("Clear", color = SBOnSurfaceVariant) }
                            }
                        }
                    }
                }

                // Models section label
                item {
                    Text("MODELS", fontSize = 12.sp, fontWeight = FontWeight.Bold, letterSpacing = 0.8.sp,
                        color = SBPrimary, modifier = Modifier.padding(start = 32.dp, top = 14.dp, bottom = 4.dp))
                }
                item {
                    SettingsRow(icon = Icons.Default.Email, title = "Chat model", subtitle = "anthropic/claude-haiku-4-5")
                }
                item {
                    SettingsRow(icon = Icons.Default.Search, title = "Embedding model", subtitle = "openai/text-embedding-3-small")
                }

                // App section label
                item {
                    Text("APP", fontSize = 12.sp, fontWeight = FontWeight.Bold, letterSpacing = 0.8.sp,
                        color = SBPrimary, modifier = Modifier.padding(start = 32.dp, top = 14.dp, bottom = 4.dp))
                }
                item {
                    SettingsRow(icon = Icons.Default.Settings, title = "Theme", subtitle = "Dark — always on")
                }
                item {
                    SettingsRow(icon = Icons.Default.Lock, title = "Storage", subtitle = "Encrypted on device")
                }
            }
        }
    }

    @Composable
    private fun SettingsRow(
        icon: androidx.compose.ui.graphics.vector.ImageVector,
        title: String,
        subtitle: String? = null,
        trailingOk: Boolean = false,
        trailingToggle: Boolean = false,
        trailingSwatch: Boolean = false
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 4.dp, vertical = 2.dp)
                .clip(RoundedCornerShape(16.dp)).background(Color.Transparent)
                .padding(horizontal = 14.dp, vertical = 13.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Box(modifier = Modifier.size(40.dp).background(SBSurface, RoundedCornerShape(11.dp)), contentAlignment = Alignment.Center) {
                Icon(icon, contentDescription = null, tint = SBOnSurfaceVariant, modifier = Modifier.size(20.dp))
            }
            Column(modifier = Modifier.weight(1f)) {
                Text(title, fontSize = 15.sp, fontWeight = FontWeight.Medium, color = MaterialTheme.colorScheme.onSurface)
                if (subtitle != null) Text(subtitle, fontSize = 12.sp, color = SBMuted, modifier = Modifier.padding(top = 1.dp))
            }
            // Trailing elements
            when {
                trailingOk -> Icon(Icons.Default.Settings, contentDescription = null, tint = SBOk, modifier = Modifier.size(20.dp))
                trailingToggle -> Box(modifier = Modifier.width(48.dp).height(28.dp).background(SBPrimary, RoundedCornerShape(16.dp))) {
                    Box(modifier = Modifier.size(22.dp).padding(3.dp).background(SBOnPrimary, CircleShape).align(Alignment.CenterEnd))
                }
                trailingSwatch -> Box(modifier = Modifier.size(24.dp).background(SBPrimary, CircleShape))
            }
        }
    }

    // ── Bottom Navigation Bar ──────────────────────────────────────────────────
    @Composable
    private fun SecondBrainBottomNav(
        currentTab: String,
        onTabSelected: (String) -> Unit
    ) {
        val items = listOf(
            Triple("home",     Icons.Default.Home,     "Home"),
            Triple("chat",     Icons.Default.Email,    "Chat"),
            Triple("settings", Icons.Default.Settings, "Settings")
        )
        NavigationBar(
            containerColor = SBSurfaceLowest,
            tonalElevation = 0.dp
        ) {
            items.forEach { (id, icon, label) ->
                val selected = currentTab == id
                NavigationBarItem(
                    selected = selected,
                    onClick = { onTabSelected(id) },
                    icon = { Icon(icon, contentDescription = label, modifier = Modifier.size(24.dp)) },
                    label = { Text(label, fontSize = 12.sp, fontWeight = FontWeight.SemiBold) },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = SBOnPrimaryContainer,
                        selectedTextColor = MaterialTheme.colorScheme.onSurface,
                        indicatorColor = SBPrimaryContainer,
                        unselectedIconColor = SBMuted,
                        unselectedTextColor = SBMuted
                    )
                )
            }
        }
    }

    // ── Home Feed card (Home A) ────────────────────────────────────────────────
    private fun timeAgo(timestamp: Long): String {
        val diff = System.currentTimeMillis() - timestamp
        val mins = diff / 60_000
        val hours = diff / 3_600_000
        val days = diff / 86_400_000
        return when {
            mins < 60  -> "${mins}m"
            hours < 24 -> "${hours}h"
            else       -> "${days}d"
        }
    }

    @OptIn(ExperimentalFoundationApi::class)
    @Composable
    private fun HomeFeedCard(item: Any, onImageClick: (ImageItem) -> Unit, onLongPress: (Any) -> Unit) {
        val (typeLabel, typeTint, typeBg) = when (item) {
            is TextItem  -> Triple("Note",  SBPrimary, SBPrimaryTint)
            is ImageItem -> Triple("Image", SBOk,      Color(0x238FD19A))
            is LinkItem  -> Triple("Link",  SBLinkBlue,Color(0x23AAC6F5))
            else         -> Triple("Item",  SBMuted,   SBSurfaceHigh)
        }
        val typeIcon = when (item) {
            is TextItem  -> Icons.Default.Create
            is ImageItem -> Icons.Default.Star
            is LinkItem  -> Icons.Default.Share
            else         -> Icons.Default.Add
        }
        val timestamp = when (item) {
            is TextItem  -> item.timestamp
            is ImageItem -> item.timestamp
            is LinkItem  -> item.timestamp
            else -> 0L
        }

        Card(
            modifier = Modifier.fillMaxWidth().combinedClickable(
                onClick = { if (item is ImageItem) onImageClick(item) },
                onLongClick = { onLongPress(item) }
            ),
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = SBSurfaceLow)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                // Header row: type badge + label + dot + time
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.padding(bottom = 10.dp)
                ) {
                    Box(modifier = Modifier.size(30.dp).background(typeBg, RoundedCornerShape(9.dp)), contentAlignment = Alignment.Center) {
                        Icon(typeIcon, contentDescription = null, tint = typeTint, modifier = Modifier.size(16.dp))
                    }
                    Text(typeLabel.uppercase(), fontSize = 12.sp, fontWeight = FontWeight.Bold,
                        letterSpacing = 0.5.sp, color = SBOnSurfaceVariant)
                    Text("·", color = SBFaint, fontSize = 13.sp)
                    Text(timeAgo(timestamp), fontSize = 13.sp, color = SBMuted)
                }
                // Title
                val title = when (item) {
                    is TextItem  -> item.content?.lines()?.firstOrNull { it.isNotBlank() }?.take(80) ?: item.content?.take(80) ?: ""
                    is ImageItem -> "Image" // never repeat OCR text as title
                    is LinkItem  -> item.title?.take(80) ?: item.url ?: ""
                    else -> ""
                }
                if (title.isNotBlank()) {
                    Text(title, fontSize = 17.sp, fontWeight = FontWeight.Bold,
                        letterSpacing = (-0.2).sp, color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 2, overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis)
                }
                // Body — one level of supporting text only, no repeat
                when (item) {
                    is TextItem -> {
                        val rest = item.content?.let { c ->
                            val firstEnd = c.indexOf('\n').takeIf { it >= 0 } ?: c.length.coerceAtMost(80)
                            c.drop(firstEnd).trim().take(120)
                        }
                        if (!rest.isNullOrBlank()) {
                            Text(rest, fontSize = 14.sp, color = SBOnSurfaceVariant,
                                modifier = Modifier.padding(top = 5.dp), maxLines = 2,
                                overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis)
                        }
                    }
                    is LinkItem -> item.description?.takeIf { it.isNotBlank() }?.let {
                        Text(it.take(120), fontSize = 14.sp, color = SBOnSurfaceVariant,
                            modifier = Modifier.padding(top = 5.dp), maxLines = 2,
                            overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis)
                    }
                    is ImageItem -> {
                        // Show first non-blank line of OCR text as a single subtitle — no repeat
                        val firstLine = item.extractedText?.lines()?.firstOrNull { it.isNotBlank() }?.take(80)
                        if (!firstLine.isNullOrBlank()) {
                            Text(firstLine, fontSize = 13.sp, color = SBMuted,
                                modifier = Modifier.padding(top = 4.dp), maxLines = 1,
                                overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis)
                        }
                    }
                }
                // Media thumbnail for image items
                if (item is ImageItem && !item.localPath.isNullOrEmpty()) {
                    val path = item.localPath.removePrefix("file://")
                    AsyncImage(
                        model = coil.request.ImageRequest.Builder(LocalContext.current).data(path).crossfade(true).build(),
                        contentDescription = null,
                        modifier = Modifier.fillMaxWidth().height(130.dp)
                            .padding(top = 12.dp).clip(RoundedCornerShape(12.dp)),
                        contentScale = ContentScale.Crop
                    )
                }
                // Link image preview
                if (item is LinkItem && !item.imageUrl.isNullOrEmpty()) {
                    AsyncImage(
                        model = coil.request.ImageRequest.Builder(LocalContext.current).data(item.imageUrl).crossfade(true).build(),
                        contentDescription = null,
                        modifier = Modifier.fillMaxWidth().height(100.dp)
                            .padding(top = 12.dp).clip(RoundedCornerShape(12.dp)),
                        contentScale = ContentScale.Crop
                    )
                }
                // Tags
                val tags by databaseManager.getTagsForItem(
                    when (item) { is TextItem -> item.id; is ImageItem -> item.id; is LinkItem -> item.id; else -> 0L },
                    when (item) { is TextItem -> ContentType.TEXT; is ImageItem -> ContentType.IMAGE; else -> ContentType.LINK }
                ).collectAsState(initial = emptyList())
                if (tags.isNotEmpty()) {
                    Row(modifier = Modifier.padding(top = 12.dp), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        tags.take(3).forEach { tag ->
                            Box(modifier = Modifier.background(SBSurfaceHigh, RoundedCornerShape(8.dp)).padding(horizontal = 9.dp, vertical = 3.dp)) {
                                Text("#${tag.name}", fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = SBOnSurfaceVariant, letterSpacing = 0.2.sp)
                            }
                        }
                    }
                }
            }
        }
    }

    // ── Redesigned category card (design system B variant) ─────────────────────
    @Composable
    private fun DesignCategoryCard(label: String, count: Int, onClick: () -> Unit) {
        val (icon, tint, bg) = when (label) {
            "Notes"  -> Triple(Icons.Default.Create, SBPrimary, SBPrimaryTint)
            "Images" -> Triple(Icons.Default.Star,   SBOk,      Color(0x238FD19A))
            "Links"  -> Triple(Icons.Default.Share,  SBLinkBlue,Color(0x23AAC6F5))
            else     -> Triple(Icons.Default.Add,    SBMuted,   SBSurfaceHigh)
        }
        Card(
            onClick = onClick,
            modifier = Modifier
                .fillMaxWidth()
                .height(104.dp),
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = SBSurfaceLowest)
        ) {
            Box(modifier = Modifier.fillMaxSize().padding(16.dp)) {
                // Icon badge top-left
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .background(bg, RoundedCornerShape(12.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(icon, contentDescription = null, tint = tint, modifier = Modifier.size(22.dp))
                }
                // Count top-right
                Text(
                    text = count.toString(),
                    fontSize = 26.sp,
                    fontWeight = FontWeight.ExtraBold,
                    letterSpacing = (-1).sp,
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.align(Alignment.TopEnd)
                )
                // Label bottom-left
                Text(
                    text = label,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = SBOnSurfaceVariant,
                    modifier = Modifier.align(Alignment.BottomStart)
                )
            }
        }
    }

    @Composable
    private fun TagFilterBar(
        allTags: List<TagEntity>,
        activeTag: TagEntity?,
        onTagSelected: (TagEntity?) -> Unit
    ) {
        if (allTags.isEmpty()) return
        LazyRow(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            item {
                FilterChip(
                    selected = activeTag == null,
                    onClick = { onTagSelected(null) },
                    label = { Text("All") }
                )
            }
            items(allTags) { tag ->
                FilterChip(
                    selected = activeTag?.id == tag.id,
                    onClick = { onTagSelected(tag) },
                    label = { Text(tag.name) }
                )
            }
        }
    }

    @OptIn(ExperimentalMaterial3Api::class)
    @Composable
    private fun AddTagBottomSheet(
        itemId: Long,
        contentType: ContentType,
        allTags: List<TagEntity>,
        onDismiss: () -> Unit
    ) {
        val existingTags by databaseManager.getTagsForItem(itemId, contentType)
            .collectAsState(initial = emptyList())
        var inputText by remember { mutableStateOf("") }
        var validationError by remember { mutableStateOf<String?>(null) }

        // Normalise: lowercase, spaces → hyphens
        fun normalise(raw: String) = raw.lowercase().replace(' ', '-')

        val normalisedInput = normalise(inputText)
        val suggestions = allTags.filter { tag ->
            normalisedInput.isNotEmpty() &&
            tag.name.contains(normalisedInput, ignoreCase = true) &&
            existingTags.none { it.id == tag.id }
        }

        fun tryAddTag(name: String) {
            val cleaned = normalise(name).trim('-')
            if (cleaned.isEmpty() || cleaned.all { it == '-' }) {
                validationError = "Tag name cannot be blank or hyphens only"
                return
            }
            // Silently ignore duplicate
            if (existingTags.any { it.name == cleaned }) {
                inputText = ""
                validationError = null
                return
            }
            lifecycleScope.launch(Dispatchers.IO) {
                databaseManager.addTagToItem(cleaned, itemId, contentType)
            }
            inputText = ""
            validationError = null
        }

        ModalBottomSheet(onDismissRequest = onDismiss) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 16.dp, end = 16.dp, bottom = 32.dp)
            ) {
                Text(
                    text = "Add Tag",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier
                        .padding(bottom = 12.dp)
                        .align(Alignment.CenterHorizontally)
                )

                // Existing tags with X to remove
                if (existingTags.isNotEmpty()) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 12.dp),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        existingTags.forEach { tag ->
                            InputChip(
                                selected = false,
                                onClick = {},
                                label = { Text(tag.name) },
                                trailingIcon = {
                                    IconButton(
                                        onClick = {
                                            lifecycleScope.launch(Dispatchers.IO) {
                                                databaseManager.removeTagFromItem(tag.id, itemId, contentType)
                                            }
                                        },
                                        modifier = Modifier.size(18.dp)
                                    ) {
                                        Icon(
                                            Icons.Default.Close,
                                            contentDescription = "Remove ${tag.name}",
                                            modifier = Modifier.size(14.dp)
                                        )
                                    }
                                }
                            )
                        }
                    }
                }

                // Tag input
                OutlinedTextField(
                    value = inputText,
                    onValueChange = {
                        inputText = it
                        validationError = null
                    },
                    modifier = Modifier.fillMaxWidth(),
                    placeholder = { Text("Enter tag name") },
                    singleLine = true,
                    isError = validationError != null,
                    supportingText = validationError?.let { msg -> { Text(msg) } },
                    trailingIcon = {
                        TextButton(onClick = { tryAddTag(inputText) }) {
                            Text("Add")
                        }
                    }
                )

                // Suggestions
                if (suggestions.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(8.dp))
                    suggestions.forEach { tag ->
                        TextButton(
                            onClick = { tryAddTag(tag.name) },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(tag.name, modifier = Modifier.fillMaxWidth())
                        }
                    }
                }
            }
        }
    }

    @OptIn(ExperimentalMaterial3Api::class)
    @Composable
    private fun ItemOptionsBottomSheet(
        item: Any,
        onEdit: () -> Unit,
        onDelete: () -> Unit,
        onTags: () -> Unit,
        onDismiss: () -> Unit
    ) {
        ModalBottomSheet(onDismissRequest = onDismiss) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 16.dp, end = 16.dp, bottom = 32.dp)
            ) {
                Text(
                    text = "Options",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier
                        .padding(bottom = 16.dp)
                        .align(Alignment.CenterHorizontally)
                )
                if (item is TextItem || item is LinkItem) {
                    TextButton(
                        onClick = { onEdit(); onDismiss() },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Edit", fontSize = 16.sp)
                    }
                }
                TextButton(
                    onClick = onTags,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Tags", fontSize = 16.sp)
                }
                TextButton(
                    onClick = onDelete,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Delete", fontSize = 16.sp, color = MaterialTheme.colorScheme.error)
                }
                TextButton(
                    onClick = onDismiss,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Cancel", fontSize = 16.sp)
                }
            }
        }
    }

    @Composable
    private fun DeleteConfirmationDialog(
        onConfirm: () -> Unit,
        onDismiss: () -> Unit
    ) {
        AlertDialog(
            onDismissRequest = onDismiss,
            title = { Text("Delete this item?") },
            text = { Text("This action cannot be undone.") },
            confirmButton = {
                TextButton(onClick = onConfirm) {
                    Text("Delete", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = onDismiss) {
                    Text("Cancel")
                }
            }
        )
    }

    @Composable
    private fun SearchResults(
        query: String,
        textItems: List<Any>,
        imageItems: List<ImageItem>,
        linkItems: List<LinkItem>,
        tagMatchedTextIds: Set<Long> = emptySet(),
        tagMatchedImageIds: Set<Long> = emptySet(),
        tagMatchedLinkIds: Set<Long> = emptySet(),
        modifier: Modifier = Modifier,
        onImageClick: ((ImageItem) -> Unit)? = null
    ) {

        val filteredTextItems = textItems.filter { item ->
            if (item is TextItem) {
                item.id in tagMatchedTextIds ||
                (!item.content.isNullOrEmpty() && item.content.contains(query, ignoreCase = true))
            } else false
        }

        val filteredImageItems = imageItems.filter { item ->
            item.id in tagMatchedImageIds ||
            (!item.extractedText.isNullOrEmpty() && item.extractedText.contains(query, ignoreCase = true))
        }

        val filteredLinkItems = linkItems.filter { item ->
            item.id in tagMatchedLinkIds ||
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