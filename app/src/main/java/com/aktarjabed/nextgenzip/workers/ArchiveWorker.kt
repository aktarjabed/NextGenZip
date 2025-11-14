package com.aktarjabed.nextgenzip.workers

import android.app.Notification
import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.ForegroundInfo
import androidx.work.WorkerParameters
import androidx.work.workDataOf
import com.aktarjabed.nextgenzip.data.ArchiveEngine
import com.aktarjabed.nextgenzip.data.model.ArchiveOperation
import com.aktarjabed.nextgenzip.data.model.OperationType
import com.aktarjabed.nextgenzip.notifications.NotificationHelper
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import java.util.*

class ArchiveWorker(
    appContext: Context,
    params: WorkerParameters
) : CoroutineWorker(appContext, params) {

    override suspend fun doWork(): Result = withContext(Dispatchers.IO) {
        val operationJson = inputData.getString(KEY_OPERATION)
            ?: return@withContext Result.failure()

        // Proper serialization would be needed here for production
        val operation = Json.decodeFromString<ArchiveOperation>(operationJson)
        val notificationId = id.hashCode()

        try {
            setForeground(createForegroundInfo(0, "Starting..."))

            when (operation.type) {
                OperationType.CREATE -> {
                    // Create logic would need progress updates to leverage this
                    ArchiveEngine.createZipWithSplitAndAes(
                        context = applicationContext,
                        uris = operation.inputUris,
                        outZipPath = operation.outputPath,
                        password = operation.password,
                        splitSizeInBytes = operation.splitSize
                    )
                }
                OperationType.EXTRACT -> {
                    ArchiveEngine.extractArchive(
                        context = applicationContext,
                        archiveUri = operation.inputUris.first(),
                        outDirPath = operation.outputPath,
                        password = operation.password
                    ) { progress ->
                        if (isStopped) return@extractArchive // Cancellation check

                        val progressPercent = (progress * 100).toInt()
                        setProgress(workDataOf(KEY_PROGRESS to progress))
                        setForegroundAsync(createForegroundInfo(progressPercent, "Extracting..."))
                    }
                }
                else -> return@withContext Result.failure()
            }
            Result.success()
        } catch (e: Exception) {
            if (isStopped) {
                Result.failure()
            } else {
                Result.failure()
            }
        }
    }

    private fun createForegroundInfo(progress: Int, message: String): ForegroundInfo {
        val notification = NotificationHelper.createProgressNotification(
            applicationContext,
            "Archive Operation",
            message,
            progress
        )
        return ForegroundInfo(id.hashCode(), notification)
    }

    companion object {
        const val KEY_OPERATION = "archive_operation"
        const val KEY_PROGRESS = "progress"
    }
}
