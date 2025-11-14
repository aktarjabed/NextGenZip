package com.aktarjabed.nextgenzip.service

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.aktarjabed.nextgenzip.data.CloudSyncManager

class SyncWorker(appContext: Context, params: WorkerParameters) : CoroutineWorker(appContext, params) {

    override suspend fun doWork(): Result {
        val syncManager = CloudSyncManager(applicationContext)
        return try {
            syncManager.syncHistory()
            syncManager.syncSettings()
            Result.success()
        } catch (e: Exception) {
            Result.retry()
        }
    }
}
