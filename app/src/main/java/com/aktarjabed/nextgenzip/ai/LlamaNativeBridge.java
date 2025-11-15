package com.aktarjabed.nextgenzip.ai;

import android.util.Log;

/**
 * LlamaNativeBridge — lazy-load variant.
 *
 * Replaces the eager static System.loadLibrary(...) block with a safe, synchronized ensureLoaded()
 * method that callers (SafeLlamaManager) can call before invoking native methods.
 *
 * This prevents classloader-time UnsatisfiedLinkError crashes when the native .so is missing.
 */
public class LlamaNativeBridge {
    private static final String TAG = "LlamaNativeBridge";
    private static volatile boolean sLoaded = false;

    /**
     * Attempt to load the native library if not already loaded.
     *
     * @return true if library successfully loaded or already loaded, false otherwise.
     */
    public static synchronized boolean ensureLoaded() {
        if (sLoaded) return true;
        try {
            System.loadLibrary("llama_native");
            sLoaded = true;
            Log.i(TAG, "Native library llama_native loaded successfully.");
            return true;
        } catch (UnsatisfiedLinkError e) {
            Log.w(TAG, "Native library llama_native not available: " + e.getMessage());
            sLoaded = false;
            return false;
        } catch (Throwable t) {
            Log.w(TAG, "Unexpected error loading native library: " + t.getMessage());
            sLoaded = false;
            return false;
        }
    }

    // Native entrypoints — keep declarations unchanged
    public static native long nativeInit(String modelPath, int contextSize);
    public static native String nativeInfer(long handle, String prompt, int maxTokens);
    public static native void nativeClose(long handle);
}
