package com.aktarjabed.nextgenzip

import android.app.Application
import android.util.Log
import com.aktarjabed.nextgenzip.data.DataStoreManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.io.IOException

class App : Application() {

    override fun onCreate() {
        super.onCreate()
        runPasswordMigration()
    }

    /**
     * Runs the plaintext → encrypted password migration one time.
     * Non-blocking, safe, idempotent.
     */
    private fun runPasswordMigration() {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                DataStoreManager.migratePlaintextPasswordIfAny(applicationContext)
                Log.i("NextGenZip", "Password migration complete or not needed.")
            } catch (e: IOException) {
                Log.e("NextGenZip", "Password migration FAILED: ${e.message}")

                // If you want to notify the user, send a broadcast or set a flag here.
                // DO NOT crash the app — just fall back to requiring password re-entry.
            } catch (e: Exception) {
                Log.e("NextGenZip", "Unexpected migration error: ${e.message}")
            }
        }
    }
}
