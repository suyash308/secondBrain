package com.example.secondbrain.data

import com.example.secondbrain.data.entities.ChatMessageEntity
import com.google.gson.Gson
import com.google.gson.annotations.SerializedName
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.OutputStreamWriter
import java.net.HttpURLConnection
import java.net.URL

data class RetrievedItem(
    val id: Long,
    val contentType: ContentType,
    val label: String,
    val contextText: String,
    val similarityScore: Float
)

class OpenRouterService {

    companion object {
        private const val BASE_URL = "https://openrouter.ai/api/v1"
        private const val REFERER = "second-brain-android"
        private const val EMBEDDING_MODEL = "openai/text-embedding-3-small"
        private const val CHAT_MODEL = "anthropic/claude-haiku-4-5"
        private const val MAX_CHAT_TOKENS = 500
    }

    private val gson = Gson()

    // ── Embeddings ─────────────────────────────────────────────────────────────

    suspend fun generateEmbedding(text: String, apiKey: String): Result<List<Float>> =
        withContext(Dispatchers.IO) {
            try {
                val url = URL("$BASE_URL/embeddings")
                val conn = (url.openConnection() as HttpURLConnection).apply {
                    requestMethod = "POST"
                    setRequestProperty("Content-Type", "application/json")
                    setRequestProperty("Authorization", "Bearer $apiKey")
                    setRequestProperty("HTTP-Referer", REFERER)
                    doOutput = true
                }

                val body = gson.toJson(mapOf("model" to EMBEDDING_MODEL, "input" to text))
                OutputStreamWriter(conn.outputStream).use { it.write(body) }

                val responseCode = conn.responseCode
                val responseText = if (responseCode == 200) {
                    conn.inputStream.bufferedReader().readText()
                } else {
                    conn.errorStream?.bufferedReader()?.readText() ?: "HTTP $responseCode"
                }

                if (responseCode != 200) {
                    return@withContext Result.failure(Exception("HTTP $responseCode: $responseText"))
                }

                val parsed = gson.fromJson(responseText, EmbeddingResponse::class.java)
                val floats = parsed.data.firstOrNull()?.embedding
                    ?: return@withContext Result.failure(Exception("Empty embedding data"))
                Result.success(floats)
            } catch (e: Exception) {
                Result.failure(e)
            }
        }

    // ── Chat ───────────────────────────────────────────────────────────────────

    suspend fun chat(
        question: String,
        conversationHistory: List<ChatMessageEntity>,
        retrievedItems: List<RetrievedItem>,
        apiKey: String
    ): Result<String> = withContext(Dispatchers.IO) {
        try {
            val systemPrompt = buildSystemPrompt(retrievedItems)
            val messages = buildMessages(systemPrompt, conversationHistory, question)

            val url = URL("$BASE_URL/chat/completions")
            val conn = (url.openConnection() as HttpURLConnection).apply {
                requestMethod = "POST"
                setRequestProperty("Content-Type", "application/json")
                setRequestProperty("Authorization", "Bearer $apiKey")
                setRequestProperty("HTTP-Referer", REFERER)
                doOutput = true
            }

            val body = gson.toJson(
                mapOf(
                    "model" to CHAT_MODEL,
                    "max_tokens" to MAX_CHAT_TOKENS,
                    "messages" to messages
                )
            )
            OutputStreamWriter(conn.outputStream).use { it.write(body) }

            val responseCode = conn.responseCode
            val responseText = if (responseCode == 200) {
                conn.inputStream.bufferedReader().readText()
            } else {
                conn.errorStream?.bufferedReader()?.readText() ?: "HTTP $responseCode"
            }

            if (responseCode != 200) {
                return@withContext Result.failure(Exception("HTTP $responseCode: $responseText"))
            }

            val parsed = gson.fromJson(responseText, ChatResponse::class.java)
            val content = parsed.choices.firstOrNull()?.message?.content
                ?: return@withContext Result.failure(Exception("Empty chat response"))
            Result.success(content)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // ── Private helpers ────────────────────────────────────────────────────────

    private fun buildSystemPrompt(retrievedItems: List<RetrievedItem>): String {
        val base = """
            You are a personal assistant with access to the user's saved notes, images, and links.
            Answer questions using only the saved items provided below as your source of truth.
            If the answer cannot be found in the saved items, say clearly: "I could not find this in your saved content."
            At the end of your answer, always list the saved items you used as sources, prefixed with "Sources used:".
            Keep answers concise and direct.
        """.trimIndent()

        if (retrievedItems.isEmpty()) {
            return "$base\n\nNo relevant saved items were found."
        }

        val contextBlock = retrievedItems.mapIndexed { index, item ->
            item.contextText.let { "[Saved Item ${index + 1}]\n$it" }
        }.joinToString("\n\n")

        return "$base\n\n$contextBlock"
    }

    private fun buildMessages(
        systemPrompt: String,
        history: List<ChatMessageEntity>,
        question: String
    ): List<Map<String, String>> {
        val messages = mutableListOf<Map<String, String>>()
        messages.add(mapOf("role" to "system", "content" to systemPrompt))
        // Last 10 pairs = 20 messages
        history.takeLast(20).forEach { msg ->
            messages.add(mapOf("role" to msg.role, "content" to msg.content))
        }
        messages.add(mapOf("role" to "user", "content" to question))
        return messages
    }

    // ── JSON response models ───────────────────────────────────────────────────

    private data class EmbeddingResponse(val data: List<EmbeddingData>)
    private data class EmbeddingData(val embedding: List<Float>)

    private data class ChatResponse(val choices: List<ChatChoice>)
    private data class ChatChoice(val message: ChatMessage)
    private data class ChatMessage(
        @SerializedName("role") val role: String,
        @SerializedName("content") val content: String
    )
}
