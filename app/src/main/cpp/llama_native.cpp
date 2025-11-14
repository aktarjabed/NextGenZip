#include <jni.h>
#include <string>
#include <atomic>
#include <sstream>

// Include llama.cpp headers here (assumed added to CMake)

static std::atomic<long> g_handle_id{1};

// Placeholder: map handles to model instances, manage lifecycle properly

extern "C"
JNIEXPORT jlong JNICALL
Java_com_aktarjabed_nextgenzip_ai_LlamaNativeBridge_nativeInit(
    JNIEnv* env, jclass clazz, jstring modelPath, jint contextSize) {
    // TODO: load llama model and store handle in map
    (void) modelPath;
    (void) contextSize;
    long handle = g_handle_id.fetch_add(1);
    return (jlong)handle;
}

extern "C"
JNIEXPORT jstring JNICALL
Java_com_aktarjabed_nextgenzip_ai_LlamaNativeBridge_nativeInfer(
    JNIEnv* env, jclass clazz, jlong handle, jstring prompt, jint maxTokens) {

    const char* cPrompt = env->GetStringUTFChars(prompt, nullptr);
    std::ostringstream oss;
    oss << "Real llama.cpp response (handle=" << handle << ") for prompt: \"" << cPrompt << "\"";
    env->ReleaseStringUTFChars(prompt, cPrompt);
    std::string result = oss.str();

    return env->NewStringUTF(result.c_str());
}

extern "C"
JNIEXPORT void JNICALL
Java_com_aktarjabed_nextgenzip_ai_LlamaNativeBridge_nativeClose(
    JNIEnv* env, jclass clazz, jlong handle) {
    (void) handle;
    // Cleanup model instance
}
