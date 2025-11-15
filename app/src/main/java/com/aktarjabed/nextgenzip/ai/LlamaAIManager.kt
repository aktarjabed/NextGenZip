package com.aktarjabed.nextgenzip.ai

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

/**
 * LlamaAIManager - Backward-compatible wrapper.
 * For new code, prefer SafeLlamaManager directly for better error handling.
 */
object LlamaAIManager {

    /**
     * Legacy API - returns Flow<String> (throws on error).
     * Use SafeLlamaManager.respond() for Result-based error handling.
     */
    @Deprecated(
        message = "Use SafeLlamaManager.respond() for better error handling",
        replaceWith = ReplaceWith("SafeLlamaManager.respond(modelPath, prompt, maxTokens)")
    )
    fun respond(modelPath: String, prompt: String, maxTokens: Int = 128): Flow<String> {
        return SafeLlamaManager.respond(modelPath, prompt, maxTokens).map { result ->
            result.getOrThrow() // throws exception on failure
        }
    }
}
