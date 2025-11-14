package com.aktarjabed.nextgenzip.data.model

import android.net.Uri
import android.os.Parcelable
import kotlinx.parcelize.Parcelize
import java.util.UUID

@Parcelize
data class ArchiveOperation(
    val id: String = UUID.randomUUID().toString(),
    val type: OperationType,
    val inputUris: List<Uri>,
    val outputPath: String,
    val format: ArchiveFormat,
    val password: String? = null,
    val splitSize: Long = 0L, // 0 = no splitting
    val compressionLevel: Int = 6, // 0-9, where 0=store, 9=maximum
    val encrypt: Boolean = false,
    val createdAt: Long = System.currentTimeMillis(),
    var status: OperationStatus = OperationStatus.PENDING,
    var progress: Float = 0f,
    var currentFile: String? = null,
    var errorMessage: String? = null
) : Parcelable

enum class OperationType {
    CREATE, EXTRACT, REPAIR
}

enum class OperationStatus {
    PENDING, RUNNING, COMPLETED, FAILED, CANCELLED
}

enum class ArchiveFormat(val extension: String, val mimeType: String) {
    ZIP("zip", "application/zip"),
    SEVEN_ZIP("7z", "application/x-7z-compressed"),
    TAR("tar", "application/x-tar"),
    GZIP("gz", "application/gzip"),
    XZ("xz", "application/x-xz"),
    RAR("rar", "application/vnd.rar");

    companion object {
        fun fromExtension(ext: String): ArchiveFormat? {
            return values().find { it.extension.equals(ext, true) }
        }
    }
}
