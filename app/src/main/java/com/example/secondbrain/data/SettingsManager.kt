package com.example.secondbrain.data

import android.content.Context
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey

class SettingsManager(context: Context) {

    private val masterKey = MasterKey.Builder(context)
        .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
        .build()

    private val prefs = EncryptedSharedPreferences.create(
        context,
        "second_brain_secure_prefs",
        masterKey,
        EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
        EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
    )

    fun saveApiKey(key: String) {
        prefs.edit().putString("openrouter_api_key", key).apply()
    }

    fun getApiKey(): String? = prefs.getString("openrouter_api_key", null)

    fun clearApiKey() {
        prefs.edit().remove("openrouter_api_key").apply()
    }
}
