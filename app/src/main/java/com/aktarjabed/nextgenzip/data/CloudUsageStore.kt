package com.aktarjabed.nextgenzip.data

import android.content.Context
import android.content.SharedPreferences
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey

/**
 * Tracks usage counters and last latency/error for cloud requests.
 * This avoids storing sensitive request data while giving visibility to the user.
 */
object CloudUsageStore {
    private const val PREF_NAME = "nextgenzip_cloud_usage"
    private const val KEY_COUNT = "cloud_count"
    private const val KEY_LAST_LATENCY_MS = "cloud_last_latency"
    private const val KEY_LAST_ERROR = "cloud_last_error"
    private const val KEY_LAST_TIMESTAMP = "cloud_last_ts"

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

    private fun checkInit() {
        if (!initialized) throw IllegalStateException("CloudUsageStore must be init(context)")
    }

    var callCount: Int
        get() { checkInit(); return prefs.getInt(KEY_COUNT, 0) }
        private set(value) { checkInit(); prefs.edit().putInt(KEY_COUNT, value).apply() }

    var lastLatencyMs: Long
        get() { checkInit(); return prefs.getLong(KEY_LAST_LATENCY_MS, 0L) }
        private set(value) { checkInit(); prefs.edit().putLong(KEY_LAST_LATENCY_MS, value).apply() }

    var lastError: String?
        get() { checkInit(); return prefs.getString(KEY_LAST_ERROR, null) }
        private set(value) { checkInit(); prefs.edit().putString(KEY_LAST_ERROR, value).apply() }

    var lastTimestamp: Long
        get() { checkInit(); return prefs.getLong(KEY_LAST_TIMESTAMP, 0L) }
        private set(value) { checkInit(); prefs.edit().putLong(KEY_LAST_TIMESTAMP, value).apply() }

    fun recordSuccess(latencyMs: Long) {
        checkInit()
        callCount = callCount + 1
        lastLatencyMs = latencyMs
        lastError = null
        lastTimestamp = System.currentTimeMillis()
    }

    fun recordError(errorMessage: String?) {
        checkInit()
        callCount = callCount + 1
        lastError = errorMessage
        lastTimestamp = System.currentTimeMillis()
    }

    fun reset() {
        checkInit()
        prefs.edit().clear().apply()
    }
}
