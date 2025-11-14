package com.aktarjabed.nextgenzip.workers

import android.content.Context
import android.net.Uri
import android.app.ForegroundServiceStartNotAllowedException
import android.os.Build
import androidx.work.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import com.aktarjabed.nextgenzip.notifications.NotificationHelper
import com.aktarjabed.nextgenzip.data.ArchiveEngine
import java.io.File

/**
 * ArchiveWorker - Background archive operations with foreground service support
 *
 * Input keys:
 * - opType: "compress" | "extract" | "repair"
 * - uris: Array<String> (URI strings)
 * - outPath: String (output path)
 * - password: String? (optional)
 * - splitSize: Long (optional, for compress)
 * - damagedZip: String? (for repair operation)
 *
 * Output keys:
 * - resultPath: String (path to result)
 * - error: String (on failure)
 */
class ArchiveWorker(
    appContext: Context,
    params: WorkerParameters
) : CoroutineWorker(appContext, params) {

    companion object {
        const val NOTIF_ID = 1001

        // Input keys
        const val KEY_OP_TYPE = "opType"
        const val KEY_URIS = "uris"
        const val KEY_OUT_PATH = "outPath"
        const val KEY_PASSWORD = "password"
        const val KEY_SPLIT_SIZE = "splitSize"
        const val KEY_DAMAGED_ZIP = "damagedZip"

        // Operation types
        const val OP_COMPRESS = "compress"
        const val OP_EXTRACT = "extract"
        const val OP_REPAIR = "repair"

        // Output keys
        const val KEY_RESULT_PATH = "resultPath"
        const val KEY_ERROR = "error"
    }

    override suspend fun doWork(): Result = withContext(Dispatchers.IO) {
        val notif = NotificationHelper.buildForegroundNotification(
            applicationContext,
            "Preparing archive operation...",
            0
        )

        // Try to set foreground (may fail on Android 12+ if app in background)
        try {
            setForeground(ForegroundInfo(NOTIF_ID, notif))
        } catch (e: Exception) {
            // Android 12+ background restrictions
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S &&
                e is ForegroundServiceStartNotAllowedException) {
                android.util.Log.w("ArchiveWorker", "Cannot start foreground service: ${e.message}")
                // Continue without foreground service
            } else {
                throw e
            }
        }

        try {
            val opType = inputData.getString(KEY_OP_TYPE)
                ?: return@withContext Result.failure(
                    workDataOf(KEY_ERROR to "Missing operation type")
                )

            when (opType) {
                OP_COMPRESS -> handleCompress()
                OP_EXTRACT -> handleExtract()
                OP_REPAIR -> handleRepair()
                else -> Result.failure(workDataOf(KEY_ERROR to "Unknown operation: $opType"))
            }
        } catch (e: Exception) {
            Result.failure(workDataOf(KEY_ERROR to (e.localizedMessage ?: "Unknown error")))
        }
    }

    private suspend fun handleCompress(): Result {
        val uriStrings = inputData.getStringArray(KEY_URIS)
            ?: return Result.failure(workDataOf(KEY_ERROR to "Missing URIs"))
        val uris = uriStrings.map { Uri.parse(it) }
        val outPath = inputData.getString(KEY_OUT_PATH)
            ?: return Result.failure(workDataOf(KEY_ERROR to "Missing output path"))
        val password = inputData.getString(KEY_PASSWORD)
        val splitSize = inputData.getLong(KEY_SPLIT_SIZE, 0L)

        updateProgress("Compressing files...", 10)

        ArchiveEngine.createZipWithSplitAndAes(
            applicationContext,
            uris,
            outPath,
            password,
            splitSize
        )

        updateProgress("Compression complete", 100)
        return Result.success(workDataOf(KEY_RESULT_PATH to outPath))
    }

    private suspend fun handleExtract(): Result {
        val uriStrings = inputData.getStringArray(KEY_URIS)
            ?: return Result.failure(workDataOf(KEY_ERROR to "Missing URIs"))
        if (uriStrings.isEmpty()) {
            return Result.failure(workDataOf(KEY_ERROR to "No archive specified"))
        }
        val uri = Uri.parse(uriStrings.first())
        val outDir = inputData.getString(KEY_OUT_PATH)
            ?: return Result.failure(workDataOf(KEY_ERROR to "Missing output path"))
        val password = inputData.getString(KEY_PASSWORD)

        updateProgress("Extracting archive...", 5)

        ArchiveEngine.extractArchive(
            applicationContext,
            uri,
            outDir,
            password
        ) { progress ->
            val percent = (progress * 100).toInt()
            updateProgress("Extracting... $percent%", percent)
        }

        updateProgress("Extraction complete", 100)
        return Result.success(workDataOf(KEY_RESULT_PATH to outDir))
    }

    private suspend fun handleRepair(): Result {
        val damagedPath = inputData.getString(KEY_DAMAGED_ZIP)
            ?: return Result.failure(workDataOf(KEY_ERROR to "Missing damaged ZIP path"))
        val recoveredDir = File(applicationContext.cacheDir, "recovered_${System.currentTimeMillis()}")
        val recoveredZip = File(applicationContext.cacheDir, "recovered_${System.currentTimeMillis()}.zip")

        updateProgress("Repairing ZIP...", 20)

        ArchiveEngine.repairZipBestEffort(
            File(damagedPath),
            recoveredDir,
            recoveredZip
        )

        updateProgress("Repair complete", 100)
        return Result.success(workDataOf(KEY_RESULT_PATH to recoveredZip.absolutePath))
    }

    private suspend fun updateProgress(message: String, percent: Int) {
        val notif = NotificationHelper.buildForegroundNotification(
            applicationContext,
            message,
            percent
        )
        try {
            setForeground(ForegroundInfo(NOTIF_ID, notif))
        } catch (_: Exception) {
            // Ignore if foreground service unavailable
        }
    }
}
