package com.aktarjabed.nextgenzip

import android.app.Application
import android.util.Log
import com.aktarjabed.nextgenzip.data.DataStoreManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class App : Application() {

    override fun onCreate() {
        super.onCreate()
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
