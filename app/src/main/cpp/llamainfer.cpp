#include <jni.h>
#include <string>
#include <sstream>
#include <atomic>

static std::atomic<long> g_counter(1);

extern "C"
JNIEXPORT jlong JNICALL
Java_com_aktarjabed_nextgenzip_ai_NativeBridge_nativeInit(
JNIEnv *env,
jclass clazz,
jstring modelPath,
jint contextSize
) {
// Stub: In Stage D, initialize llama.cpp model here
(void)modelPath;
(void)contextSize;
long handle = g_counter.fetch_add(1);
return (jlong)handle;
}

extern "C"
JNIEXPORT jstring JNICALL
Java_com_aktarjabed_nextgenzip_ai_NativeBridge_nativeInfer(
JNIEnv *env,
jclass clazz,
jlong handle,
jstring prompt,
jint maxTokens
) {
const char* cPrompt = env->GetStringUTFChars(prompt, nullptr);
std::ostringstream oss;
oss << "Mock LLM response (handle=" << handle << ") for: \""
<< cPrompt << "\" [llama.cpp stub]";
env->ReleaseStringUTFChars(prompt, cPrompt);

std::string result = oss.str();
return env->NewStringUTF(result.c_str());
}

extern "C"
JNIEXPORT void JNICALL
Java_com_aktarjabed_nextgenzip_ai_NativeBridge_nativeClose(
JNIEnv *env,
jclass clazz,
jlong handle
) {
(void)handle;
// Release resources when implemented
}
