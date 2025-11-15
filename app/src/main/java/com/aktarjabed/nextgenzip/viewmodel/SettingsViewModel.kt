package com.aktarjabed.nextgenzip.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.aktarjabed.nextgenzip.data.DataStoreManager
import kotlinx.coroutines.launch
import java.io.IOException

class SettingsViewModel(application: Application) : AndroidViewModel(application) {

    // Example function to save password securely
    fun saveZipPassword(password: String, onResult: (success: Boolean, message: String?) -> Unit) {
        viewModelScope.launch {
            try {
                DataStoreManager.setPassword(getApplication(), password)
                onResult(true, null)
            } catch (e: Exception) {
                // Log and surface user-friendly message
                val msg = when (e) {
                    is IOException -> "Failed to save password securely: ${e.message}"
                    else -> "Unexpected error saving password"
                }
                onResult(false, msg)
            }
        }
    }

    // Example function to read password (only for UI purposes if needed)
    fun loadSavedPassword(onLoaded: (password: String?) -> Unit) {
        viewModelScope.launch {
            try {
                val pwd = DataStoreManager.getPassword(getApplication())
                onLoaded(pwd)
            } catch (e: Exception) {
                // If reading secure prefs fails, return null and log
                onLoaded(null)
            }
        }
    }

    // Call at app startup or on settings screen open to migrate any legacy plaintext
    fun ensureMigrationOfPlaintextPassword(onComplete: (success: Boolean, message: String?) -> Unit) {
        viewModelScope.launch {
            try {
                DataStoreManager.migratePlaintextPasswordIfAny(getApplication())
                onComplete(true, null)
            } catch (e: Exception) {
                onComplete(false, "Migration failed: ${e.message}")
            }
        }
    }

    fun clearStoredPassword(onComplete: (Boolean) -> Unit) {
        viewModelScope.launch {
            try {
                DataStoreManager.clearPassword(getApplication())
                onComplete(true)
            } catch (_: Exception) {
                onComplete(false)
            }
        }
    }
}
