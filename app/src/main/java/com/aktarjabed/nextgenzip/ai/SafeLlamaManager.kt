package com.aktarjabed.nextgenzip.ai

import android.util.Log
import java.io.File
import java.io.FileInputStream
import java.security.MessageDigest
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

object SafeLlamaManager {
    private const val TAG = "SafeLlamaManager"

    sealed class InitResult {
        data class Success(val handle: Long) : InitResult()
        data class Failure(val reason: String, val recoverable: Boolean = true) : InitResult()
    }

    suspend fun safeInit(modelPath: String, contextSize: Int = 2048): InitResult {
        return withContext(Dispatchers.IO) {
            val modelFile = File(modelPath)
            if (!modelFile.exists()) {
                return@withContext InitResult.Failure(
                    "Model file not found at: $modelPath. Please install the model.",
                    recoverable = true
                )
            }
            if (!modelFile.canRead()) {
                return@withContext InitResult.Failure(
                    "Model file exists but is not readable. Check file permissions.",
                    recoverable = true
                )
            }

            val loaded = try {
                LlamaNativeBridge.ensureLoaded()
            } catch (t: Throwable) {
                Log.e(TAG, "ensureLoaded threw: ${t.message}")
                false
            }
            if (!loaded) {
                return@withContext InitResult.Failure(
                    "Native library not available on this device. Install the native bundle or use the cloud fallback.",
                    recoverable = true
                )
            }

            try {
                val handle = LlamaNativeBridge.nativeInit(modelPath, contextSize)
                if (handle == 0L) {
                    return@withContext InitResult.Failure(
                        "Native engine failed to initialize (returned handle=0).",
                        recoverable = false
                    )
                }
                return@withContext InitResult.Success(handle)
            } catch (ule: UnsatisfiedLinkError) {
                Log.e(TAG, "Native library not found or failed to load: ${ule.message}")
                return@withContext InitResult.Failure(
                    "Native library not available on this device. Install the native bundle or use the cloud fallback.",
                    recoverable = true
                )
            } catch (t: Throwable) {
                Log.e(TAG, "Native init threw: ${t.message}")
                return@withContext InitResult.Failure(
                    "Native engine initialization failed: ${t.message}",
                    recoverable = false
                )
            }
        }
    }

    fun safeClose(handle: Long) {
        try {
            LlamaNativeBridge.nativeClose(handle)
        } catch (e: UnsatisfiedLinkError) {
            Log.w(TAG, "nativeClose not available: ${e.message}")
        } catch (t: Throwable) {
            Log.w(TAG, "Error while closing native engine: ${t.message}")
        }
    }

    @Throws(Exception::class)
    fun sha256Checksum(file: File): String {
        val digest = MessageDigest.getInstance("SHA-256")
        FileInputStream(file).use { fis ->
            val buffer = ByteArray(32 * 1024)
            var read: Int
            while (true) {
                read = fis.read(buffer)
                if (read <= 0) break
                digest.update(buffer, 0, read)
            }
        }
        return digest.digest().joinToString("") { "%02x".format(it) }
    }
}
