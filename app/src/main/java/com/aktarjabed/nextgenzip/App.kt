package com.aktarjabed.nextgenzip

import android.app.Application
import android.util.Log
import com.aktarjabed.nextgenzip.data.ApiKeyStore
import com.aktarjabed.nextgenzip.data.CloudConfigStore
import com.aktarjabed.nextgenzip.data.CloudUsageStore
import com.aktarjabed.nextgenzip.data.DataStoreManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class App : Application() {

    override fun onCreate() {
        super.onCreate()

        // Initialize secure stores (safe and idempotent)
        try {
            ApiKeyStore.init(applicationContext)
            CloudConfigStore.init(applicationContext)
            CloudUsageStore.init(applicationContext)
            Log.i("NextGenZip", "Cloud stores initialized")
        } catch (e: Exception) {
            Log.e("NextGenZip", "Cloud store init failed: ${e.message}")
        }

        // Run migration in IO
        runPasswordMigration()
    }

    private fun runPasswordMigration() {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                DataStoreManager.migratePlaintextPasswordIfAny(applicationContext)
                Log.i("NextGenZip", "Password migration complete or not needed.")
            } catch (e: Exception) {
                Log.e("NextGenZip", "Password migration failed: ${e.message}")
            }
        }
    }
}
