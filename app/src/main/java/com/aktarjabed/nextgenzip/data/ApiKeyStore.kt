package com.aktarjabed.nextgenzip.data

import android.content.Context
import android.content.SharedPreferences
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey

/**
 * ApiKeyStore - securely stores the user's cloud AI API key using EncryptedSharedPreferences.
 *
 * Call ApiKeyStore.init(context) once (e.g., Application.onCreate()) before using save/get/clear.
 */
object ApiKeyStore {
    private const val PREF_NAME = "nextgenzip_cloud_prefs"
    private const val KEY_API = "cloud_api_key"

    private lateinit var prefs: SharedPreferences
    private var initialized = false

    fun init(context: Context) {
        if (initialized) return
        val masterKey = MasterKey.Builder(context)
            .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
            .build()
        prefs = EncryptedSharedPreferences.create(
            context,
            PREF_NAME,
            masterKey,
            EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
            EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
        )
        initialized = true
    }

    fun saveKey(key: String) {
        checkInit()
        prefs.edit().putString(KEY_API, key).apply()
    }

    fun getKey(): String? {
        checkInit()
        return prefs.getString(KEY_API, null)
    }

    fun clearKey() {
        checkInit()
        prefs.edit().remove(KEY_API).apply()
    }

    private fun checkInit() {
        if (!initialized) {
            throw IllegalStateException("ApiKeyStore must be initialized via ApiKeyStore.init(context)")
        }
    }
}
