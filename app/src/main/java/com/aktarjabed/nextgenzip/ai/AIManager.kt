package com.aktarjabed.nextgenzip.ai

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow

/**
* High-level AI wrapper.
* modelPath == null -> uses native stub (mocked response).
* Stage D/E will implement real llama.cpp + TFLite fallback.
*/
object AIManager {
fun respond(modelPath: String?, prompt: String, maxTokens: Int = 128): Flow<String> = flow {
val handle = NativeBridge.nativeInit(modelPath, 2048)
try {
val response = NativeBridge.nativeInfer(handle, prompt, maxTokens)
emit(response)
} finally {
NativeBridge.nativeClose(handle)
}
}
}
