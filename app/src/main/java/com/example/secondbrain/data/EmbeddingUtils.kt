package com.example.secondbrain.data

import com.google.gson.Gson
import kotlin.math.sqrt

object EmbeddingUtils {

    fun cosineSimilarity(a: List<Float>, b: List<Float>): Float {
        var dot = 0f
        var normA = 0f
        var normB = 0f
        for (i in a.indices) {
            dot += a[i] * b[i]
            normA += a[i] * a[i]
            normB += b[i] * b[i]
        }
        return if (normA == 0f || normB == 0f) 0f
        else dot / (sqrt(normA) * sqrt(normB))
    }

    fun parseEmbedding(json: String): List<Float> {
        return Gson().fromJson(json, Array<Float>::class.java).toList()
    }

    fun serializeEmbedding(embedding: List<Float>): String {
        return Gson().toJson(embedding)
    }
}
