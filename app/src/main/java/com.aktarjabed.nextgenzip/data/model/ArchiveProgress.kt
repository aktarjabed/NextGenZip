package com.aktarjabed.nextgenzip.data.model

data class ArchiveProgress(
    val operationId: String,
    val progress: Float,
    val currentFile: String? = null,
    val bytesProcessed: Long = 0,
    val totalBytes: Long = 0,
    val status: OperationStatus
)
