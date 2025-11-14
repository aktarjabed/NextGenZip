package com.aktarjabed.nextgenzip.ai;

public class LlamaNativeBridge {
    static {
        System.loadLibrary("llama_native");
    }

    public static native long nativeInit(String modelPath, int contextSize);
    public static native String nativeInfer(long handle, String prompt, int maxTokens);
    public static native void nativeClose(long handle);
}
