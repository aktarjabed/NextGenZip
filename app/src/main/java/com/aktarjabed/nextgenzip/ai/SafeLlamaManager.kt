package com.aktarjabed.nextgenzip.ai

import android.util.Log
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import java.io.File

/**
 * SafeLlamaManager - Production-safe wrapper around LlamaNativeBridge.
 *
 * Handles:
 * - Native library load failures (UnsatisfiedLinkError)
 * - Missing model files
 * - Model initialization failures
 * - Runtime errors during inference
 *
 * Returns Flow<Result<String>> instead of Flow<String> so callers can distinguish
 * success from failure without exceptions.
 *
 * Usage:
 *   SafeLlamaManager.respond(modelPath, prompt).collect { result ->
 *       result.onSuccess { text -> /* show to user */ }
 *       result.onFailure { error -> /* show error message */ }
 *   }
 */
object SafeLlamaManager {

    private const val TAG = "SafeLlamaManager"

    // Track native library availability (lazy-initialized)
    private val isNativeAvailable: Boolean by lazy {
        checkNativeLibraryAvailability()
    }

    /**
     * Check if native AI engine is available on this device/build.
     * Returns true if libllama_native.so loaded successfully.
     */
    fun isAvailable(): Boolean = isNativeAvailable

    /**
     * Generate AI response with full error handling.
     *
     * @param modelPath Absolute path to GGUF/GGML model file on device storage.
     * @param prompt User prompt/query text.
     * @param maxTokens Maximum tokens to generate (default 128).
     * @return Flow emitting Result<String> - success contains response text, failure contains exception.
     */
    fun respond(
        modelPath: String,
        prompt: String,
        maxTokens: Int = 128
    ): Flow<Result<String>> = flow {
        // 1) Check native library availability first
        if (!isNativeAvailable) {
            emit(Result.failure(NativeLibraryUnavailableException(
                "Native AI engine not available. Please install required components or use a build with native library support."
            )))
            return@flow
        }

        // 2) Validate model file exists
        if (modelPath.isBlank()) {
            emit(Result.failure(ModelNotFoundException("Model path is empty")))
            return@flow
        }

        val modelFile = File(modelPath)
        if (!modelFile.exists() || !modelFile.isFile) {
            emit(Result.failure(ModelNotFoundException(
                "Model file not found at: $modelPath"
            )))
            return@flow
        }

        if (!modelFile.canRead()) {
            emit(Result.failure(ModelNotFoundException(
                "Model file exists but cannot be read: $modelPath (check permissions)"
            )))
            return@flow
        }

        // 3) Validate prompt
        if (prompt.isBlank()) {
            emit(Result.failure(IllegalArgumentException("Prompt cannot be empty")))
            return@flow
        }

        // 4) Attempt model initialization and inference
        var handle: Long = 0L
        try {
            // Initialize native model
            handle = LlamaNativeBridge.nativeInit(modelPath, 2048)

            if (handle == 0L) {
                emit(Result.failure(ModelInitializationException(
                    "Failed to initialize model. The model file may be corrupted or incompatible."
                )))
                return@flow
            }

            // Run inference
            val response = try {
                LlamaNativeBridge.nativeInfer(handle, prompt, maxTokens)
            } catch (e: Exception) {
                throw InferenceException("AI inference failed: ${e.message}", e)
            }

            // Validate response
            if (response.isNullOrBlank()) {
                emit(Result.failure(InferenceException(
                    "Model returned empty response. This may indicate model compatibility issues."
                )))
                return@flow
            }

            // Success
            emit(Result.success(response))

        } catch (e: UnsatisfiedLinkError) {
            // Should not happen if isNativeAvailable check passed, but guard anyway
            logError("Native method call failed despite library being available", e)
            emit(Result.failure(NativeLibraryUnavailableException(
                "Native method invocation failed: ${e.message}"
            )))
        } catch (e: ModelInitializationException) {
            emit(Result.failure(e))
        } catch (e: InferenceException) {
            emit(Result.failure(e))
        } catch (e: Exception) {
            logError("Unexpected error during AI inference", e)
            emit(Result.failure(InferenceException(
                "Unexpected error: ${e.message ?: e.javaClass.simpleName}", e
            )))
        } finally {
            // 5) Always clean up native resources
            if (handle != 0L) {
                try {
                    LlamaNativeBridge.nativeClose(handle)
                } catch (e: Exception) {
                    logError("Failed to close native model handle", e)
                }
            }
        }
    }

    /**
     * Check if native library can be loaded.
     * This is called once during lazy initialization.
     */
    private fun checkNativeLibraryAvailability(): Boolean {
        return try {
            // Attempt to trigger static library load in LlamaNativeBridge
            Class.forName("com.aktarjabed.nextgenzip.ai.LlamaNativeBridge")
            logInfo("Native AI library loaded successfully")
            true
        } catch (e: UnsatisfiedLinkError) {
            logWarn("Native AI library not available: ${e.message}")
            false
        } catch (e: Exception) {
            logWarn("Failed to check native library availability: ${e.message}")
            false
        }
    }

    // -------------------------
    // Custom exception types for clear error handling
    // -------------------------

    /**
     * Thrown when native library (.so file) is not present or cannot be loaded.
     */
    class NativeLibraryUnavailableException(message: String) : Exception(message)

    /**
     * Thrown when model file is missing, unreadable, or invalid path.
     */
    class ModelNotFoundException(message: String) : Exception(message)

    /**
     * Thrown when native model initialization fails (returns 0 handle or throws).
     */
    class ModelInitializationException(message: String) : Exception(message)

    /**
     * Thrown when inference call fails or returns invalid data.
     */
    class InferenceException(message: String, cause: Throwable? = null) : Exception(message, cause)

    // -------------------------
    // Logging helpers
    // -------------------------
    private fun logInfo(msg: String) {
        try { Log.i(TAG, msg) } catch (_: Exception) {}
    }

    private fun logWarn(msg: String) {
        try { Log.w(TAG, msg) } catch (_: Exception) {}
    }

    private fun logError(msg: String, e: Throwable) {
        try { Log.e(TAG, msg, e) } catch (_: Exception) {}
    }
}
