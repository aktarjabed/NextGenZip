package com.aktarjabed.nextgenzip.security

import android.content.Context
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

object QuarantineManager {
    private const val TAG = "QuarantineManager"
    private const val QUARANTINE_DIR = "quarantine"

    data class QuarantineEntry(
        val originalName: String,
        val quarantinePath: String,
        val dateQuarantined: Long,
        val threatLevel: MalwareDetectionService.Severity,
        val threatDescription: String
    )

    suspend fun quarantineFile(
        context: Context,
        file: File,
        severity: MalwareDetectionService.Severity,
        reason: String
    ): Result<QuarantineEntry> {
        return withContext(Dispatchers.IO) {
            try {
                val quarantineDir = File(context.cacheDir, QUARANTINE_DIR).apply {
                    mkdirs()
                }

                val timestamp = System.currentTimeMillis()
                val quarantinedFile = File(quarantineDir, "${file.nameWithoutExtension}_${timestamp}.quarantine")

                file.copyTo(quarantinedFile, overwrite = true)
                file.delete()

                val entry = QuarantineEntry(
                    originalName = file.name,
                    quarantinePath = quarantinedFile.absolutePath,
                    dateQuarantined = timestamp,
                    threatLevel = severity,
                    threatDescription = reason
                )

                Log.i(TAG, "File quarantined: ${file.name}")
                Result.success(entry)
            } catch (e: Exception) {
                Log.e(TAG, "Quarantine failed: ${e.message}")
                Result.failure(e)
            }
        }
    }

    suspend fun getQuarantinedFiles(context: Context): List<QuarantineEntry> {
        return withContext(Dispatchers.IO) {
            try {
                val quarantineDir = File(context.cacheDir, QUARANTINE_DIR)
                if (!quarantineDir.exists()) return@withContext emptyList()

                quarantineDir.listFiles()?.map { file ->
                    QuarantineEntry(
                        originalName = file.name.split("_")[0],
                        quarantinePath = file.absolutePath,
                        dateQuarantined = file.lastModified(),
                        threatLevel = MalwareDetectionService.Severity.HIGH,
                        threatDescription = "Quarantined file"
                    )
                } ?: emptyList()
            } catch (e: Exception) {
                Log.e(TAG, "Error listing quarantine: ${e.message}")
                emptyList()
            }
        }
    }

    suspend fun deleteFromQuarantine(quarantinePath: String): Result<Unit> {
        return withContext(Dispatchers.IO) {
            try {
                val file = File(quarantinePath)
                if (file.exists()) {
                    file.delete()
                    Log.i(TAG, "Quarantined file permanently deleted")
                    Result.success(Unit)
                } else {
                    Result.failure(Exception("File not found"))
                }
            } catch (e: Exception) {
                Result.failure(e)
            }
        }
    }

    suspend fun restoreFromQuarantine(quarantinePath: String, restorePath: String): Result<Unit> {
        return withContext(Dispatchers.IO) {
            try {
                val quarantinedFile = File(quarantinePath)
                val restoreFile = File(restorePath)

                quarantinedFile.copyTo(restoreFile, overwrite = true)
                quarantinedFile.delete()

                Log.i(TAG, "File restored from quarantine")
                Result.success(Unit)
            } catch (e: Exception) {
                Log.e(TAG, "Restore failed: ${e.message}")
                Result.failure(e)
            }
        }
    }

    suspend fun clearQuarantine(context: Context): Result<Int> {
        return withContext(Dispatchers.IO) {
            try {
                val quarantineDir = File(context.cacheDir, QUARANTINE_DIR)
                var deleted = 0

                quarantineDir.listFiles()?.forEach { file ->
                    if (file.delete()) deleted++
                }

                Log.i(TAG, "Quarantine cleared: $deleted files deleted")
                Result.success(deleted)
            } catch (e: Exception) {
                Result.failure(e)
            }
        }
    }
}
