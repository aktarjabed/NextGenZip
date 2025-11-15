package com.aktarjabed.nextgenzip.data

import android.content.Context
import android.content.SharedPreferences
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.withContext
import java.io.IOException

// Provide a property delegate for DataStore on Context
private val Context.appDataStore: DataStore<Preferences> by preferencesDataStore(name = "nextgenzip_prefs")

object DataStoreManager {

    // Non-sensitive keys (keep in DataStore)
    private val KEY_COMPRESSION_LEVEL = stringPreferencesKey("compression_level")
    private val KEY_SPLIT_SIZE = stringPreferencesKey("split_size")
    // Old plaintext password key (if present) - used for migration only
    private val KEY_PLAINTEXT_PASSWORD = stringPreferencesKey("password_plaintext")

    // EncryptedSharedPreferences filename and key
    private const val ENC_PREFS_FILENAME = "nextgenzip_secure_prefs"
    private const val ENC_KEY_PASSWORD = "secure_zip_password"

    /**
     * Get non-sensitive preference values (example).
     * Add other getters/setters for your DataStore-backed preferences as needed.
     */
    suspend fun setCompressionLevel(context: Context, level: String) {
        withContext(Dispatchers.IO) {
            context.appDataStore.edit { prefs ->
                prefs[KEY_COMPRESSION_LEVEL] = level
            }
        }
    }

    suspend fun getCompressionLevel(context: Context): String? {
        return withContext(Dispatchers.IO) {
            val snapshot = context.appDataStore.data.firstOrNull()
            snapshot?.get(KEY_COMPRESSION_LEVEL)
        }
    }

    // -------------------------
    // Encrypted password APIs
    // -------------------------
    /**
     * Store the password securely in EncryptedSharedPreferences.
     * This will create a MasterKey stored in Android Keystore and use it to encrypt values.
     *
     * Throws IOException on failure to persist.
     */
    suspend fun setPassword(context: Context, password: String) {
        withContext(Dispatchers.IO) {
            val prefs = encryptedPrefs(context)
            val ok = prefs.edit().putString(ENC_KEY_PASSWORD, password).commit()
            if (!ok) throw IOException("Failed to write encrypted password to secure storage.")
        }
    }

    /**
     * Read the password from EncryptedSharedPreferences.
     * Returns null if no password saved.
     */
    suspend fun getPassword(context: Context): String? {
        return withContext(Dispatchers.IO) {
            try {
                val prefs = encryptedPrefs(context)
                prefs.getString(ENC_KEY_PASSWORD, null)
            } catch (e: Exception) {
                // Any exception reading secure prefs should be surfaced by caller
                throw IOException("Failed to read encrypted password: ${e.message}", e)
            }
        }
    }

    /**
     * Remove the saved password from secure storage.
     */
    suspend fun clearPassword(context: Context) {
        withContext(Dispatchers.IO) {
            val prefs = encryptedPrefs(context)
            prefs.edit().remove(ENC_KEY_PASSWORD).apply()
        }
    }

    // -------------------------
    // Migration helpers
    // -------------------------
    /**
     * If a plaintext password still exists in DataStore under the legacy key,
     * migrate it into EncryptedSharedPreferences and remove the plaintext copy.
     *
     * Call this at app startup (once) to migrate existing users.
     */
    suspend fun migratePlaintextPasswordIfAny(context: Context) {
        withContext(Dispatchers.IO) {
            try {
                // Read DataStore snapshot
                val snapshot = context.appDataStore.data.firstOrNull()
                val legacy = snapshot?.get(KEY_PLAINTEXT_PASSWORD)
                if (!legacy.isNullOrEmpty()) {
                    // Write to encrypted prefs
                    val prefs = encryptedPrefs(context)
                    val ok = prefs.edit().putString(ENC_KEY_PASSWORD, legacy).commit()
                    if (!ok) {
                        throw IOException("Failed to migrate plaintext password to secure storage.")
                    }
                    // Remove plaintext password from DataStore
                    context.appDataStore.edit { prefsMap ->
                        prefsMap.remove(KEY_PLAINTEXT_PASSWORD)
                    }
                }
            } catch (e: Exception) {
                // We fail loudly — caller can decide how to notify user or retry
                throw IOException("Password migration failed: ${e.message}", e)
            }
        }
    }

    // -------------------------
    // Internal helpers
    // -------------------------
    /**
     * Build or open the EncryptedSharedPreferences instance.
     * Uses MasterKey in Android Keystore (AES256_GCM).
     */
    private fun encryptedPrefs(context: Context): SharedPreferences {
        // MasterKey may throw on very old devices; wrap and rethrow as runtime exception
        val masterKey = try {
            MasterKey.Builder(context)
                .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
                .build()
        } catch (e: Exception) {
            throw RuntimeException("Failed to create MasterKey for EncryptedSharedPreferences: ${e.message}", e)
        }

        return EncryptedSharedPreferences.create(
            context,
            ENC_PREFS_FILENAME,
            masterKey,
            EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
            EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
        )
    }
}
