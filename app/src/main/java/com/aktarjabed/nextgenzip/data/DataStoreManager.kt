package com.aktarjabed.nextgenzip.data

import android.content.Context
import androidx.datastore.preferences.core.*
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

val Context.dataStore by preferencesDataStore(name = "nextgenzip_prefs")

object DataStoreManager {

    private val KEY_PASSWORD = stringPreferencesKey("password")
    private val KEY_SPLIT_SIZE = longPreferencesKey("split_size_bytes")
    private val KEY_COMPRESSION_LEVEL = intPreferencesKey("compression_level")

    fun getPassword(context: Context): Flow<String?> =
        context.dataStore.data.map { it[KEY_PASSWORD] }

    suspend fun setPassword(context: Context, value: String?) {
        context.dataStore.edit { prefs ->
            if (value == null) prefs.remove(KEY_PASSWORD) else prefs[KEY_PASSWORD] = value
        }
    }

    fun getSplitSize(context: Context): Flow<Long> =
        context.dataStore.data.map { it[KEY_SPLIT_SIZE] ?: 0L }

    suspend fun setSplitSize(context: Context, value: Long) {
        context.dataStore.edit { prefs -> prefs[KEY_SPLIT_SIZE] = value }
    }

    fun getCompressionLevel(context: Context): Flow<Int> =
        context.dataStore.data.map { it[KEY_COMPRESSION_LEVEL] ?: 5 }

    suspend fun setCompressionLevel(context: Context, value: Int) {
        context.dataStore.edit { prefs -> prefs[KEY_COMPRESSION_LEVEL] = value }
    }

    // Additional settings and history support can be added similarly
}
