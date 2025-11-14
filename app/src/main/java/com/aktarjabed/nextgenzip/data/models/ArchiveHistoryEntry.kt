package com.aktarjabed.nextgenzip.data.models

import kotlinx.serialization.Serializable

@Serializable
data class ArchiveHistoryEntry(
    val id: Long,
    val operation: String,   // "compress" | "extract" | "repair"
    val archivePath: String,
    val timestamp: Long
)
