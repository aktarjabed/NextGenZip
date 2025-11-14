package com.aktarjabed.nextgenzip.viewmodel

import android.content.Context
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.aktarjabed.nextgenzip.ai.LlamaAIManager
import com.aktarjabed.nextgenzip.data.ArchiveEngine
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

data class ArchiveUiState(
    val selectedFiles: List<Uri> = emptyList(),
    val password: String? = null,
    val splitSizeBytes: Long = 0L,
    val isProcessing: Boolean = false,
    val progress: Float = 0f,
    val statusMessage: String = "",
    val resultMessage: String? = null,
    val isSuccess: Boolean = false,
    val aiResponse: String = ""
)

class ArchiveViewModel : ViewModel() {

    private val _uiState = MutableStateFlow(ArchiveUiState())
    val uiState: StateFlow<ArchiveUiState> = _uiState.asStateFlow()

    fun addFiles(uris: List<Uri>) {
        _uiState.value = _uiState.value.copy(selectedFiles = _uiState.value.selectedFiles + uris)
    }

    fun removeFile(uri: Uri) {
        _uiState.value = _uiState.value.copy(selectedFiles = _uiState.value.selectedFiles - uri)
    }

    fun setPassword(password: String?) {
        _uiState.value = _uiState.value.copy(password = password)
    }

    fun setSplitSize(bytes: Long) {
        _uiState.value = _uiState.value.copy(splitSizeBytes = bytes)
    }

    fun createArchive(context: Context) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                _uiState.value = _uiState.value.copy(isProcessing = true, progress = 0f, statusMessage = "Creating archive...")

                val outPath = "${context.cacheDir.absolutePath}/archive_${System.currentTimeMillis()}.zip"

                ArchiveEngine.createZipWithSplitAndAes(
                    context,
                    _uiState.value.selectedFiles,
                    outPath,
                    _uiState.value.password,
                    _uiState.value.splitSizeBytes
                )

                _uiState.value = _uiState.value.copy(isProcessing = false, progress = 1f, resultMessage = "Archive created at:\n$outPath", isSuccess = true, selectedFiles = emptyList())
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(isProcessing = false, resultMessage = "Error: ${e.localizedMessage}", isSuccess = false)
            }
        }
    }

    fun analyzeWithAI(modelPath: String?, prompt: String, maxTokens: Int = 128) {
        viewModelScope.launch(Dispatchers.IO) {
            LlamaAIManager.respond(modelPath ?: "", prompt, maxTokens)
                .collect { chunk ->
                    _uiState.update { it.copy(aiResponse = it.aiResponse + chunk) }
                }
        }
    }

    fun clearResult() {
        _uiState.value = _uiState.value.copy(resultMessage = null, isSuccess = false, aiResponse = "")
    }
}
