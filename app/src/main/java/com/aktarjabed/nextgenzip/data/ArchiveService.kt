package com.aktarjabed.nextgenzip.data

import android.app.Service
import android.content.Intent
import android.os.IBinder
import com.aktarjabed.nextgenzip.data.model.ArchiveOperation
import com.aktarjabed.nextgenzip.data.model.OperationType
import com.aktarjabed.nextgenzip.notifications.NotificationHelper
import kotlinx.coroutines.*

class ArchiveService : Service() {
    private val serviceScope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private var currentOperation: ArchiveOperation? = null

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        NotificationHelper.createNotificationChannel(this)

        when (intent?.action) {
            ACTION_START_OPERATION -> {
                val operation = intent.getParcelableExtra<ArchiveOperation>(EXTRA_OPERATION)
                operation?.let { startArchiveOperation(it) }
            }
            ACTION_CANCEL_OPERATION -> {
                cancelCurrentOperation()
            }
        }
        return START_NOT_STICKY
    }

    private fun startArchiveOperation(operation: ArchiveOperation) {
        currentOperation = operation

        val notification = NotificationHelper.createProgressNotification(
            this,
            "Archive Operation",
            "Starting...",
            0
        )
        startForeground(NOTIFICATION_ID, notification)

        serviceScope.launch {
            try {
                when (operation.type) {
                    OperationType.CREATE -> {
                        // For simplicity, we'll use the ViewModel's direct call path.
                        // A full implementation would require more complex progress handling here.
                    }
                    OperationType.EXTRACT -> {
                        ArchiveEngine.extractArchive(this@ArchiveService, operation.inputUris.first(), operation.outputPath, operation.password) { progress ->
                            updateProgressNotification(operation, progress)
                        }
                    }
                    else -> {
                        // Handle other operation types
                    }
                }
                updateCompletionNotification(operation, "Operation Successful", "Your files are ready.")
            } catch (e: Exception) {
                if (e !is CancellationException) {
                    updateCompletionNotification(operation, "Operation Failed", e.message ?: "Unknown error")
                }
            } finally {
                stopForeground(STOP_FOREGROUND_DETACH)
                stopSelf()
            }
        }
    }

    private fun updateProgressNotification(operation: ArchiveOperation, progress: Float) {
        val notification = NotificationHelper.createProgressNotification(
            this,
            "Processing: ${getFileNameFromUris(operation.inputUris)}",
            "${(progress * 100).toInt()}% complete",
            (progress * 100).toInt()
        )
        startForeground(NOTIFICATION_ID, notification)
    }

    private fun updateCompletionNotification(operation: ArchiveOperation, title: String, message: String) {
        val notification = NotificationHelper.createProgressNotification(
            this,
            title,
            message,
            100
        )
        startForeground(NOTIFICATION_ID, notification)
    }

    private fun cancelCurrentOperation() {
        serviceScope.coroutineContext.cancelChildren()
        stopForeground(true)
        stopSelf()
    }

    private fun getFileNameFromUris(uris: List<Uri>): String {
        if (uris.isEmpty()) return "Archive"
        return uris.first().lastPathSegment ?: "Archive"
    }

    override fun onDestroy() {
        super.onDestroy()
        serviceScope.cancel()
    }

    companion object {
        const val ACTION_START_OPERATION = "START_OPERATION"
        const val ACTION_CANCEL_OPERATION = "CANCEL_OPERATION"
        const val EXTRA_OPERATION = "EXTRA_OPERATION"
        const val NOTIFICATION_ID = 1001
    }
}
