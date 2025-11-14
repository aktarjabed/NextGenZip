package com.aktarjabed.nextgenzip.workers

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import androidx.work.workDataOf
import com.aktarjabed.nextgenzip.data.ArchiveEngine
import com.aktarjabed.nextgenzip.data.model.ArchiveOperation
import kotlinx.serialization.json.Json
import android.net.Uri
import com.aktarjabed.nextgenzip.data.model.OperationType

class ArchiveWorker(
    appContext: Context,
    params: WorkerParameters
) : CoroutineWorker(appContext, params) {

    override suspend fun doWork(): Result {
        return try {
            val operationJson = inputData.getString(KEY_OPERATION)
                ?: return Result.failure()

            // Note: Direct serialization of Uri is not recommended for production.
            // A more robust solution would convert Uris to strings and back.
            val operation = Json.decodeFromString<ArchiveOperation>(operationJson)

            when (operation.type) {
                OperationType.CREATE -> {
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
                        // This requires a more complex setup to pass progress back to UI
                        // For now, we rely on the foreground service notification for progress
                        setProgress(workDataOf(KEY_PROGRESS to progress))
                    }
                }
                else -> {
                    // Handle other types or return failure
                    return Result.failure()
                }
            }
            Result.success()
        } catch (e: Exception) {
            Result.failure()
        }
    }

    companion object {
        const val KEY_OPERATION = "archive_operation"
        const val KEY_PROGRESS = "progress"
    }
}
