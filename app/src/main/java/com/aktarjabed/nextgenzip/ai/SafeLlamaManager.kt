package com.aktarjabed.nextgenzip.ai

import android.util.Log
import java.io.File
import java.io.FileInputStream
import java.security.MessageDigest
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * SafeLlamaManager
 *
 * Defensive wrapper around the JNI Llama bridge. Provides safeInit() that:
 *  - verifies model file exists & readable
 *  - attempts to load native library via LlamaNativeBridge.ensureLoaded()
 *  - calls nativeInit() in try/catch and returns a result object (Success/Failure)
 *
 * Do not call safeInit() on the main thread if you enable checksum hashing — use Dispatchers.IO.
 */
object SafeLlamaManager {
    private const val TAG = "SafeLlamaManager"

    sealed class InitResult {
        data class Success(val handle: Long) : InitResult()
        data class Failure(val reason: String, val recoverable: Boolean = true) : InitResult()
    }

    /**
     * Initialize the native Llama engine in a safe manner.
     *
     * Should be called from a background thread (Dispatchers.IO) because it may perform file I/O.
     */
    suspend fun safeInit(modelPath: String, contextSize: Int = 2048): InitResult {
        return withContext(Dispatchers.IO) {
            // 1) Model file checks
            val modelFile = File(modelPath)
            if (!modelFile.exists()) {
                return@withContext InitResult.Failure("Model file not found at: $modelPath. Please install the model.", recoverable = true)
            }
            if (!modelFile.canRead()) {
                return@withContext InitResult.Failure("Model file exists but is not readable. Check file permissions.", recoverable = true)
            }

            try {
                // Optionally verify the file isn't trivially small (quick sanity check)
                if (modelFile.length() < 1024L) {
                    Log.w(TAG, "Model file appears unexpectedly small (${modelFile.length()} bytes).")
                }
            } catch (t: Throwable) {
                Log.w(TAG, "Model size check failed: ${t.message}")
            }

            // 2) Attempt to load native library lazily
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

            // 3) Call native init in try/catch
            try {
                val handle = LlamaNativeBridge.nativeInit(modelPath, contextSize)
                if (handle == 0L) {
                    return@withContext InitResult.Failure("Native engine returned invalid handle (0).", recoverable = false)
                }
                return@withContext InitResult.Success(handle)
            } catch (ule: UnsatisfiedLinkError) {
                Log.e(TAG, "Native init UnsatisfiedLinkError: ${ule.message}")
                return@withContext InitResult.Failure("Native library appears incompatible or missing. ${ule.message}", recoverable = true)
            } catch (t: Throwable) {
                Log.e(TAG, "Native init threw: ${t.message}")
                return@withContext InitResult.Failure("Native engine initialization failed: ${t.message}", recoverable = false)
            }
        }
    }

    /**
     * Close handle safely (wraps native close).
     */
    fun safeClose(handle: Long) {
        try {
            LlamaNativeBridge.nativeClose(handle)
        } catch (e: UnsatisfiedLinkError) {
            Log.w(TAG, "nativeClose not available: ${e.message}")
        } catch (t: Throwable) {
            Log.w(TAG, "Error while closing native engine: ${t.message}")
        }
    }

    // Optional helper: compute SHA-256 for a file (may be large - caller must run off main thread)
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
