package com.aktarjabed.nextgenzip.data

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.IBinder
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import kotlinx.coroutines.*
import java.io.File
import java.lang.Exception

/**
 * Foreground service that performs archive operations (CREATE).
 *
 * Expected intent extras for CREATE:
 * - "input_uris": ArrayList<String>  (list of content/file URI strings)
 * - "output_path": String           (absolute path for resulting zip file)
 * - "password": String?             (optional)
 * - "split_size": Long?             (optional, bytes)
 *
 * Cancellation:
 * Send an Intent to this service with action ACTION_CANCEL to cancel the running operation.
 *
 * Result broadcast:
 * Local broadcast action ACTION_ARCHIVE_RESULT with extras:
 *  - "result_status": "success" | "failure" | "cancelled"
 *  - "result_message": String
 *  - "output_path": String? (on success)
 */
class ArchiveService : Service() {

    companion object {
        private const val TAG = "ArchiveService"

        // Actions
        const val ACTION_CREATE = "com.aktarjabed.nextgenzip.ACTION_CREATE"
        const val ACTION_CANCEL = "com.aktarjabed.nextgenzip.ACTION_CANCEL"

        // Intent extras keys
        const val EXTRA_INPUT_URIS = "input_uris"
        const val EXTRA_OUTPUT_PATH = "output_path"
        const val EXTRA_PASSWORD = "password"
        const val EXTRA_SPLIT_SIZE = "split_size"

        // Broadcast result action & extras
        const val ACTION_ARCHIVE_RESULT = "com.aktarjabed.nextgenzip.ACTION_ARCHIVE_RESULT"
        const val EXTRA_RESULT_STATUS = "result_status"     // "success" | "failure" | "cancelled"
        const val EXTRA_RESULT_MESSAGE = "result_message"
        const val EXTRA_RESULT_OUTPUT_PATH = "output_path"

        // Notification
        const val NOTIFICATION_CHANNEL_ID = "ngz_archive_channel"
        const val NOTIFICATION_CHANNEL_NAME = "Archive Operations"
        const val NOTIFICATION_ID = 0xA1  // arbitrary id
    }

    // Service coroutine scope tied to lifecycle
    private val serviceJob = SupervisorJob()
    private val serviceScope = CoroutineScope(serviceJob + Dispatchers.Main.immediate)

    // Track current job for cancellation
    @Volatile
    private var currentJob: Job? = null

    private lateinit var notificationManager: NotificationManager

    override fun onCreate() {
        super.onCreate()
        notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        createNotificationChannelIfNeeded()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent == null) {
            stopSelf()
            return START_NOT_STICKY
        }

        when (intent.action) {
            ACTION_CREATE, null -> {
                // If action is null, treat as CREATE for backward compatibility
                if (currentJob?.isActive == true) {
                    // Already running - ignore or optionally queue. We'll reject additional concurrent creates.
                    logWarn("Create operation requested while another operation is running; ignoring request.")
                    return START_NOT_STICKY
                }
                handleCreate(intent)
            }

            ACTION_CANCEL -> {
                logInfo("Cancellation requested via intent.")
                cancelCurrentOperation("Cancelled by user")
            }

            else -> {
                logWarn("Unknown action: ${intent.action}")
            }
        }

        // We don't want the system to recreate the service if it's killed while not running.
        return START_NOT_STICKY
    }

    private fun handleCreate(intent: Intent) {
        val inputUris = intent.getStringArrayListExtra(EXTRA_INPUT_URIS) ?: arrayListOf()
        val outputPath = intent.getStringExtra(EXTRA_OUTPUT_PATH)
        val password = intent.getStringExtra(EXTRA_PASSWORD)
        val splitSize = intent.getLongExtra(EXTRA_SPLIT_SIZE, 0L)

        if (outputPath.isNullOrBlank()) {
            sendResultBroadcast("failure", "Missing output path", null)
            stopSelf()
            return
        }

        if (inputUris.isEmpty()) {
            sendResultBroadcast("failure", "No input files provided", null)
            stopSelf()
            return
        }

        // Prepare notification and start foreground
        val initialNotification = buildNotification("Creating archive...", 0)
        startForeground(NOTIFICATION_ID, initialNotification)

        // Launch creation job
        currentJob = serviceScope.launch {
            try {
                // Convert strings to Uri objects and call ArchiveEngine
                val uriList = inputUris.map { android.net.Uri.parse(it) }

                // Ensure work runs on IO dispatcher inside the engine as well
                withContext(Dispatchers.IO) {
                    ArchiveEngine.createZipWithSplitAndAesSuspend(
                        context = this@ArchiveService,
                        inputUris = uriList,
                        outZipFile = File(outputPath),
                        password = password,
                        splitSizeInBytes = splitSize
                    ) { progress ->
                        // Progress callback (called from IO thread context)
                        // Update notification on Main dispatcher
                        try {
                            val percent = (progress * 100).toInt().coerceIn(0, 100)
                            val notif = buildNotification("Creating archive... $percent%", percent)
                            // Update existing foreground notification
                            notificationManager.notify(NOTIFICATION_ID, notif)
                        } catch (e: Exception) {
                            // Notification updates should not crash the service
                            logWarn("Failed to update notification: ${e.message}")
                        }
                    }
                }

                // Success
                val successMessage = "Archive created successfully"
                notificationManager.notify(NOTIFICATION_ID, buildFinalNotification(successMessage, success = true))
                sendResultBroadcast("success", successMessage, outputPath)
            } catch (ce: CancellationException) {
                logInfo("Archive creation cancelled: ${ce.message}")
                notificationManager.notify(NOTIFICATION_ID, buildFinalNotification("Archive creation cancelled", success = false))
                sendResultBroadcast("cancelled", "Operation cancelled", null)
            } catch (io: java.io.IOException) {
                val msg = "Failed to create archive: ${io.message ?: "I/O error"}"
                logWarn(msg)
                notificationManager.notify(NOTIFICATION_ID, buildFinalNotification(msg, success = false))
                sendResultBroadcast("failure", msg, null)
            } catch (t: Throwable) {
                val msg = "Unexpected error: ${t.message ?: t.javaClass.simpleName}"
                logWarn(msg)
                notificationManager.notify(NOTIFICATION_ID, buildFinalNotification(msg, success = false))
                sendResultBroadcast("failure", msg, null)
            } finally {
                // Operation finished — stop foreground (remove notification) and stop service
                try {
                    stopForeground(false) // keep notification shown but not as foreground, then cancel after short delay
                } catch (ignore: Exception) {}
                // schedule stopSelf. Allow a brief time for the final notification to be observed by user.
                delay(800)
                try { stopForeground(true) } catch (_: Exception) {}
                stopSelf()
            }
        }
    }

    private fun cancelCurrentOperation(reason: String) {
        currentJob?.let { job ->
            if (job.isActive) {
                job.cancel(CancellationException(reason))
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        // Cancel any running job and the service scope
        try {
            serviceJob.cancel(CancellationException("Service destroyed"))
        } catch (_: Exception) {}
    }

    // -------------------------
    // Notification helpers
    // -------------------------
    private fun createNotificationChannelIfNeeded() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                NOTIFICATION_CHANNEL_ID,
                NOTIFICATION_CHANNEL_NAME,
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Notifications for archive creation and extraction operations"
                setShowBadge(false)
            }
            notificationManager.createNotificationChannel(channel)
        }
    }

    private fun buildNotification(contentText: String, progressPercent: Int): Notification {
        val builder = NotificationCompat.Builder(this, NOTIFICATION_CHANNEL_ID)
            .setContentTitle("NextGenZip")
            .setContentText(contentText)
            .setSmallIcon(resolveSmallIcon())
            .setOnlyAlertOnce(true)
            .setOngoing(true)
            .setProgress(100, progressPercent, false)

        // On API 31+ set foreground service type if desired (not required for basic foreground)
        return builder.build()
    }

    private fun buildFinalNotification(message: String, success: Boolean): Notification {
        val builder = NotificationCompat.Builder(this, NOTIFICATION_CHANNEL_ID)
            .setContentTitle("NextGenZip")
            .setContentText(message)
            .setSmallIcon(resolveSmallIcon())
            .setOnlyAlertOnce(false)
            .setAutoCancel(true)
            .setOngoing(false)
            .setProgress(0, 0, false)

        // Optionally change priority for errors
        if (!success) {
            builder.priority = NotificationCompat.PRIORITY_HIGH
        } else {
            builder.priority = NotificationCompat.PRIORITY_DEFAULT
        }

        return builder.build()
    }

    /**
     * Resolve small icon resource. Tries to use ic_archive_notification if present; falls back to application icon.
     */
    private fun resolveSmallIcon(): Int {
        val pkg = packageName
        val res = resources
        val candidate = res.getIdentifier("ic_archive_notification", "drawable", pkg)
        return if (candidate != 0) candidate else applicationInfo.icon
    }

    // -------------------------
    // Result broadcasting
    // -------------------------
    private fun sendResultBroadcast(status: String, message: String, outputPath: String?) {
        try {
            val intent = Intent(ACTION_ARCHIVE_RESULT).apply {
                putExtra(EXTRA_RESULT_STATUS, status)
                putExtra(EXTRA_RESULT_MESSAGE, message)
                if (!outputPath.isNullOrBlank()) putExtra(EXTRA_RESULT_OUTPUT_PATH, outputPath)
            }
            // Local broadcast for in-app listeners
            LocalBroadcastManager.getInstance(this).sendBroadcast(intent)
        } catch (e: Exception) {
            logWarn("Failed to send local broadcast result: ${e.message}")
        }
    }

    // -------------------------
    // Logging helpers
    // -------------------------
    private fun logInfo(msg: String) {
        try { Log.i(TAG, msg) } catch (_: Exception) {}
    }

    private fun logWarn(msg: String) {
        try { Log.w(TAG, msg) } catch (_: Exception) {}
    }
}
