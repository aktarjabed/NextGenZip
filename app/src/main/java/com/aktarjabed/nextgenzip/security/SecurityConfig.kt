package com.aktarjabed.nextgenzip.security

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.withContext

private val Context.securityDataStore by preferencesDataStore(name = "security_prefs")

object SecurityConfig {
    private val VIRUSTOTAL_API_KEY = stringPreferencesKey("virustotal_api_key")
    private val ENABLE_MALWARE_SCAN = stringPreferencesKey("enable_malware_scan")
    private val AUTO_QUARANTINE = stringPreferencesKey("auto_quarantine_threats")

    suspend fun setVirusTotalApiKey(context: Context, apiKey: String) {
        withContext(Dispatchers.IO) {
            context.securityDataStore.edit { prefs ->
                prefs[VIRUSTOTAL_API_KEY] = apiKey
            }
            VirusTotalService.setApiKey(apiKey)
        }
    }

    suspend fun getVirusTotalApiKey(context: Context): String? {
        return withContext(Dispatchers.IO) {
            val prefs = context.securityDataStore.data.firstOrNull()
            prefs?.get(VIRUSTOTAL_API_KEY)
        }
    }

    suspend fun setMalwareScanEnabled(context: Context, enabled: Boolean) {
        withContext(Dispatchers.IO) {
            context.securityDataStore.edit { prefs ->
                prefs[ENABLE_MALWARE_SCAN] = enabled.toString()
            }
        }
    }

    suspend fun isMalwareScanEnabled(context: Context): Boolean {
        return withContext(Dispatchers.IO) {
            val prefs = context.securityDataStore.data.firstOrNull()
            prefs?.get(ENABLE_MALWARE_SCAN)?.toBoolean() ?: true
        }
    }

    suspend fun setAutoQuarantine(context: Context, enabled: Boolean) {
        withContext(Dispatchers.IO) {
            context.securityDataStore.edit { prefs ->
                prefs[AUTO_QUARANTINE] = enabled.toString()
            }
        }
    }

    suspend fun isAutoQuarantineEnabled(context: Context): Boolean {
        return withContext(Dispatchers.IO) {
            val prefs = context.securityDataStore.data.firstOrNull()
            prefs?.get(AUTO_QUARANTINE)?.toBoolean() ?: true
        }
    }
}
