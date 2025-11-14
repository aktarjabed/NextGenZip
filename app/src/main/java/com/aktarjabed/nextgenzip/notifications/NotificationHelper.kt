package com.aktarjabed.nextgenzip.notifications

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import androidx.core.app.NotificationCompat

object NotificationHelper {

    const val CHANNEL_ID = "nextgenzip_operations"
    private const val CHANNEL_NAME = "Archive Operations"

    /**
     * Ensure notification channel exists (required for Android 8+)
     */
    fun ensureChannel(context: Context) {
        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        if (notificationManager.getNotificationChannel(CHANNEL_ID) == null) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                CHANNEL_NAME,
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Background archive compress/extract operations"
                setShowBadge(false)
            }
            notificationManager.createNotificationChannel(channel)
        }
    }

    /**
     * Build foreground service notification
     *
     * @param context Application context
     * @param contentText Progress message
     * @param progress Progress percentage (0-100, or 0 for indeterminate)
     */
    fun buildForegroundNotification(
        context: Context,
        contentText: String,
        progress: Int
    ): Notification {
        ensureChannel(context)

        val builder = NotificationCompat.Builder(context, CHANNEL_ID)
            .setContentTitle("NextGenZip")
            .setContentText(contentText)
            .setSmallIcon(android.R.drawable.ic_menu_save) // Use system icon or replace with R.drawable.ic_notification
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .setPriority(NotificationCompat.PRIORITY_LOW)

        if (progress in 1..99) {
            builder.setProgress(100, progress, false)
        } else if (progress == 0) {
            builder.setProgress(100, 0, true) // Indeterminate
        }

        return builder.build()
    }
}
