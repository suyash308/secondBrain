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

class MainActivity : ComponentActivity() {
    private val PREFS_NAME = "SecondBrainPrefs"
    private val TEXT_COUNT_KEY = "text_count"
    private val IMAGE_COUNT_KEY = "image_count"
    private val LINK_COUNT_KEY = "link_count"
    private val TEXT_ITEMS_KEY = "text_items"
    private val IMAGE_ITEMS_KEY = "image_items"
    private val LINK_ITEMS_KEY = "link_items"
    private val gson = Gson()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        
        // Handle shared content
        handleSharedContent(intent)
        
        setContent {
            SecondBrainTheme {
                CategoryCounter()
            }
        }
    }

    override fun onNewIntent(intent: Intent?) {
        super.onNewIntent(intent)
        handleSharedContent(intent)
    }

    private fun handleSharedContent(intent: Intent?) {
        if (intent?.action == Intent.ACTION_SEND) {
            when (intent.type) {
                "text/plain" -> {
                    val sharedText = intent.getStringExtra(Intent.EXTRA_TEXT)
                    if (!sharedText.isNullOrEmpty()) {
                        if (isUrl(sharedText)) {
                            addToCategory(LINK_COUNT_KEY, LINK_ITEMS_KEY, sharedText)
                        } else {
                            addToCategory(TEXT_COUNT_KEY, TEXT_ITEMS_KEY, sharedText)
                        }
                    }
                }
                "image/*", "image/jpeg", "image/png", "image/gif", "image/webp" -> {
                    val imageUri = intent.getParcelableExtra<Uri>(Intent.EXTRA_STREAM)
                    if (imageUri != null) {
                        addToCategory(IMAGE_COUNT_KEY, IMAGE_ITEMS_KEY, imageUri.toString())
                    }
                }
            }
        }
    }

    private fun isUrl(text: String): Boolean {
        return text.startsWith("http://") || text.startsWith("https://") || 
               text.startsWith("www.") || text.contains("://")
    }

    private fun addToCategory(countKey: String, itemsKey: String, content: String) {
        val prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE)
        
        // Increment count
        val currentCount = prefs.getInt(countKey, 0)
        prefs.edit().putInt(countKey, currentCount + 1).apply()
        
        // Add to items list
        val itemsJson = prefs.getString(itemsKey, "[]")
        val type = object : TypeToken<List<String>>() {}.type
        val items: MutableList<String> = gson.fromJson(itemsJson, type) ?: mutableListOf()
        items.add(content)
        prefs.edit().putString(itemsKey, gson.toJson(items)).apply()
    }

    @Composable
    fun CategoryCounter() {
        val prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE)
        var textCount by remember { mutableStateOf(prefs.getInt(TEXT_COUNT_KEY, 0)) }
        var imageCount by remember { mutableStateOf(prefs.getInt(IMAGE_COUNT_KEY, 0)) }
        var linkCount by remember { mutableStateOf(prefs.getInt(LINK_COUNT_KEY, 0)) }
        var currentScreen by remember { mutableStateOf("main") }

        // Update counts when app becomes active
        LaunchedEffect(Unit) {
            textCount = prefs.getInt(TEXT_COUNT_KEY, 0)
            imageCount = prefs.getInt(IMAGE_COUNT_KEY, 0)
            linkCount = prefs.getInt(LINK_COUNT_KEY, 0)
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

    private fun getItems(prefs: android.content.SharedPreferences, key: String): List<String> {
        val itemsJson = prefs.getString(key, "[]")
        val type = object : TypeToken<List<String>>() {}.type
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
        items: List<String>,
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
                            if (category == "Image") {
                                // Display image
                                AsyncImage(
                                    model = ImageRequest.Builder(LocalContext.current)
                                        .data(item)
                                        .crossfade(true)
                                        .build(),
                                    contentDescription = "Shared image",
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(200.dp),
                                    contentScale = ContentScale.Crop
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                                // Show URI as caption
                                Text(
                                    text = "Image URI: $item",
                                    fontSize = 12.sp,
                                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                                    modifier = Modifier.fillMaxWidth()
                                )
                            } else {
                                // Display text/link content
                                Text(
                                    text = item,
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