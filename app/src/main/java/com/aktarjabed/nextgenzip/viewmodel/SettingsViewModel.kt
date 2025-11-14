package com.aktarjabed.nextgenzip.viewmodel

import androidx.lifecycle.ViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

data class SettingsUiState(
    val compressionLevel: Int = 5,
    val rememberPasswords: Boolean = false,
    val useEncryptionByDefault: Boolean = false
)

class SettingsViewModel : ViewModel() {

    private val _uiState = MutableStateFlow(SettingsUiState())
    val uiState: StateFlow<SettingsUiState> = _uiState.asStateFlow()

    fun setCompressionLevel(level: Int) {
        _uiState.value = _uiState.value.copy(compressionLevel = level)
    }

    fun setRememberPasswords(remember: Boolean) {
        _uiState.value = _uiState.value.copy(rememberPasswords = remember)
    }

    fun setUseEncryptionByDefault(use: Boolean) {
        _uiState.value = _uiState.value.copy(useEncryptionByDefault = use)
    }

    fun clearCache() {
        // Implementation for clearing cache
        // Could use WorkManager to clean cache directory
    }
}
