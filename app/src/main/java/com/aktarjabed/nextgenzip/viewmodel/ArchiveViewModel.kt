package com.aktarjabed.nextgenzip.viewmodel

import android.content.Context
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.aktarjabed.nextgenzip.data.ArchiveEngine
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
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
    val isSuccess: Boolean = false
)

class ArchiveViewModel : ViewModel() {

    private val _uiState = MutableStateFlow(ArchiveUiState())
    val uiState: StateFlow<ArchiveUiState> = _uiState.asStateFlow()

    fun addFiles(uris: List<Uri>) {
        _uiState.value = _uiState.value.copy(
            selectedFiles = _uiState.value.selectedFiles + uris
        )
    }

    fun removeFile(uri: Uri) {
        _uiState.value = _uiState.value.copy(
            selectedFiles = _uiState.value.selectedFiles.filter { it != uri }
        )
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
                _uiState.value = _uiState.value.copy(
                    isProcessing = true,
                    progress = 0f,
                    statusMessage = "Creating archive..."
                )

                val outputPath = "${context.cacheDir.absolutePath}/archive_${System.currentTimeMillis()}.zip"

                ArchiveEngine.createZipWithSplitAndAes(
                    context = context,
                    uris = _uiState.value.selectedFiles,
                    outZipPath = outputPath,
                    password = _uiState.value.password,
                    splitSizeInBytes = _uiState.value.splitSizeBytes
                )

                _uiState.value = _uiState.value.copy(
                    isProcessing = false,
                    progress = 1f,
                    resultMessage = "Archive created successfully at:\n$outputPath",
                    isSuccess = true,
                    selectedFiles = emptyList()
                )
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isProcessing = false,
                    resultMessage = "Error: ${e.localizedMessage}",
                    isSuccess = false
                )
            }
        }
    }

    fun extractArchive(context: Context, archiveUri: Uri) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                _uiState.value = _uiState.value.copy(
                    isProcessing = true,
                    progress = 0f,
                    statusMessage = "Extracting..."
                )

                val outputDir = "${context.cacheDir.absolutePath}/extracted_${System.currentTimeMillis()}"
                File(outputDir).mkdirs()

                ArchiveEngine.extractArchive(
                    context = context,
                    archiveUri = archiveUri,
                    outDirPath = outputDir,
                    password = _uiState.value.password
                ) { progress ->
                    _uiState.value = _uiState.value.copy(
                        progress = progress,
                        statusMessage = "Extracting... ${(progress * 100).toInt()}%"
                    )
                }

                _uiState.value = _uiState.value.copy(
                    isProcessing = false,
                    progress = 1f,
                    resultMessage = "Archive extracted successfully to:\n$outputDir",
                    isSuccess = true
                )
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isProcessing = false,
                    resultMessage = "Error: ${e.localizedMessage}",
                    isSuccess = false
                )
            }
        }
    }

    fun clearResult() {
        _uiState.value = _uiState.value.copy(
            resultMessage = null,
            isSuccess = false
        )
    }
}
