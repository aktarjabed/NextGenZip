package com.aktarjabed.nextgenzip.ai

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow

/**
 * LlamaAIManager - Kotlin coroutine wrapper for llama native bindings.
 */
object LlamaAIManager {
    /**
     * Initialize llama model and generate text response.
     *
     * @param modelPath Device path to llama model files
     * @param prompt Input prompt string
     * @param maxTokens Maximum tokens to generate
     */
    fun respond(modelPath: String, prompt: String, maxTokens: Int = 128): Flow<String> = flow {
        val handle = LlamaNativeBridge.nativeInit(modelPath, 2048)
        try {
            val response = LlamaNativeBridge.nativeInfer(handle, prompt, maxTokens)
            emit(response)
        } finally {
            LlamaNativeBridge.nativeClose(handle)
        }
    }
}
