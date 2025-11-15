package com.aktarjabed.nextgenzip.viewmodel

import android.content.Context
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.aktarjabed.nextgenzip.ai.LlamaNativeBridge
import com.aktarjabed.nextgenzip.ai.SafeLlamaManager
import com.aktarjabed.nextgenzip.data.ArchiveEngine
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.io.File

data class ArchiveUiState(
    val selectedFiles: List<Uri> = emptyList(),
    val password: String? = null,
    val splitSizeBytes: Long = 0L,
    val isProcessing: Boolean = false,
    val progress: Float = 0f,
    val statusMessage: String = "",
    val resultMessage: String? = null,
    val isSuccess: Boolean = false,
    val aiResponse: String = "",
    val aiError: String? = null
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

                ArchiveEngine.createZipWithSplitAndAesSuspend(
                    context,
                    _uiState.value.selectedFiles,
                    File(outPath),
                    _uiState.value.password,
                    _uiState.value.splitSizeBytes
                ) { progress ->
                    _uiState.update { it.copy(progress = progress) }
                }

                _uiState.value = _uiState.value.copy(isProcessing = false, progress = 1f, resultMessage = "Archive created at:\n$outPath", isSuccess = true, selectedFiles = emptyList())
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(isProcessing = false, resultMessage = "Error: ${e.localizedMessage}", isSuccess = false)
            }
        }
    }

    fun analyzeWithAI(modelPath: String, prompt: String) {
        viewModelScope.launch {
            when (val res = SafeLlamaManager.safeInit(modelPath, 2048)) {
                is SafeLlamaManager.InitResult.Success -> {
                    val handle = res.handle
                    try {
                        val response = LlamaNativeBridge.nativeInfer(handle, prompt, 256)
                        _uiState.update { it.copy(aiResponse = response, aiError = null) }
                    } catch (e: Exception) {
                        _uiState.update { it.copy(aiResponse = "", aiError = "AI inference failed: ${e.message}") }
                    } finally {
                        SafeLlamaManager.safeClose(handle)
                    }
                }
                is SafeLlamaManager.InitResult.Failure -> {
                    _uiState.update { it.copy(aiResponse = "", aiError = res.reason) }
                }
            }
        }
    }

    fun clearResult() {
        _uiState.value = _uiState.value.copy(resultMessage = null, isSuccess = false, aiResponse = "", aiError = null)
    }
}
