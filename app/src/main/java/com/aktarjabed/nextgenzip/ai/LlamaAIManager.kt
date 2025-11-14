package com.aktarjabed.nextgenzip.ai

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow

/**
 * LlamaAIManager Kotlin wrapper using native llama.cpp bindings.
 */
object LlamaAIManager {
    fun respond(
        modelPath: String,
        prompt: String,
        maxTokens: Int = 128,
    ): Flow<String> = flow {
        val handle = LlamaNativeBridge.nativeInit(modelPath, 2048)
        if (handle == 0L) {
            emit("Error: Failed to initialize model")
            return@flow
        }
        try {
            val response = LlamaNativeBridge.nativeInfer(handle, prompt, maxTokens)
            emit(response)
        } finally {
            LlamaNativeBridge.nativeClose(handle)
        }
    }
}
