package com.aktarjabed.nextgenzip.data

import android.content.Context
import android.content.SharedPreferences
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey

/**
 * CloudConfigStore - simple secure storage for cloud AI configuration flags.
 *
 * We store provider id and "enable cloud" flag here. The values are not highly sensitive,
 * but using EncryptedSharedPreferences keeps everything in a single secure place.
 *
 * Keys:
 *  - cloud_enabled (boolean)
 *  - cloud_provider (string): "openai", "custom", "gemini", "deepseek"
 *  - cloud_custom_endpoint (string)
 */
object CloudConfigStore {
    private const val PREF_NAME = "nextgenzip_cloud_config"
    private const val KEY_ENABLED = "cloud_enabled"
    private const val KEY_PROVIDER = "cloud_provider"
    private const val KEY_CUSTOM_ENDPOINT = "cloud_custom_endpoint"

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

    var enabled: Boolean
        get() { checkInit(); return prefs.getBoolean(KEY_ENABLED, false) }
        set(value) { checkInit(); prefs.edit().putBoolean(KEY_ENABLED, value).apply() }

    var provider: String?
        get() { checkInit(); return prefs.getString(KEY_PROVIDER, "openai") }
        set(value) { checkInit(); prefs.edit().putString(KEY_PROVIDER, value).apply() }

    var customEndpoint: String?
        get() { checkInit(); return prefs.getString(KEY_CUSTOM_ENDPOINT, null) }
        set(value) { checkInit(); prefs.edit().putString(KEY_CUSTOM_ENDPOINT, value).apply() }

    private fun checkInit() {
        if (!initialized) throw IllegalStateException("CloudConfigStore must be init(context)")
    }
}
