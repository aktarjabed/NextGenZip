#include <jni.h>
#include <string>
#include <atomic>
#include "llama.h"  // Include llama.cpp headers (assumed in llama_src/)

static std::atomic<long> g_handle_id{1};

// Map to hold pointers to llama contexts indexed by handle
#include <unordered_map>
#include <mutex>

std::unordered_map<long, llama_context*> g_contexts;
std::mutex g_mutex;

extern "C"
JNIEXPORT jlong JNICALL
Java_com_aktarjabed_nextgenzip_ai_LlamaNativeBridge_nativeInit(JNIEnv* env, jclass clazz, jstring modelPath, jint contextSize) {
    const char* c_modelPath = env->GetStringUTFChars(modelPath, nullptr);

    // Placeholder for actual llama.cpp initialization
    // In a real implementation, you would call llama_init_from_file()
    // For this example, we'll simulate a context pointer.
    llama_context* ctx = reinterpret_cast<llama_context*>(new int(1)); // SIMULATED

    env->ReleaseStringUTFChars(modelPath, c_modelPath);

    if (!ctx) {
        return 0; // error indicator
    }

    std::lock_guard<std::mutex> lock(g_mutex);
    long handle = g_handle_id.fetch_add(1);
    g_contexts[handle] = ctx;
    return handle;
}

extern "C"
JNIEXPORT jstring JNICALL
Java_com_aktarjabed_nextgenzip_ai_LlamaNativeBridge_nativeInfer(JNIEnv* env, jclass clazz, jlong handle, jstring prompt, jint maxTokens) {
    std::lock_guard<std::mutex> lock(g_mutex);
    if (g_contexts.find(handle) == g_contexts.end()) return env->NewStringUTF("Invalid handle");

    llama_context* ctx = g_contexts[handle];
    const char* cPrompt = env->GetStringUTFChars(prompt, nullptr);

    // Placeholder for actual llama.cpp inference
    // In a real implementation, you would tokenize the prompt and evaluate the model.
    std::string result = "Simulated llama.cpp response for prompt: " + std::string(cPrompt);

    env->ReleaseStringUTFChars(prompt, cPrompt);
    return env->NewStringUTF(result.c_str());
}

extern "C"
JNIEXPORT void JNICALL
Java_com_aktarjabed_nextgenzip_ai_LlamaNativeBridge_nativeClose(JNIEnv* env, jclass clazz, jlong handle) {
    std::lock_guard<std::mutex> lock(g_mutex);
    if (g_contexts.find(handle) != g_contexts.end()) {
        // In a real implementation, you would call llama_free()
        delete reinterpret_cast<int*>(g_contexts[handle]); // SIMULATED
        g_contexts.erase(handle);
    }
}
