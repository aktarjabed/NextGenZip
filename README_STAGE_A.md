# NextGenZip — Stage A Documentation

**Version**: 0.1.0
**Stack**: AGP 8.13.0 | Gradle 8.9 | Kotlin 2.2.20 | Compose BOM 2025.08.00

## What's Included

This stage provides a complete, runnable Android Studio project:

- ✅ Compose-only UI (Material 3)
- ✅ Archive creation/extraction (ZIP, 7Z, RAR)
- ✅ SAF-based file picking (no legacy storage)
- ✅ JNI stub for AI (llama.cpp integration point)
- ✅ TensorFlow Lite dependencies (for Stage D/E)
- ✅ Streaming I/O (prevents OOM on large files)
- ✅ ProGuard rules for release builds
- ✅ Multi-ABI native library support

## How to Build

1. Create a new folder `NextGenZip/`
2. Copy all files maintaining the exact directory structure
3. Open in Android Studio Otter (2025.2.1 or later)
4. Wait for Gradle sync to complete
5. Build -> Make Project
6. Run on a physical device (arm64-v8a recommended)

## Testing the Demo

1. **Pick Files**: Tap "Pick Files (SAF)" and select multiple files
2. **Create ZIP**: Tap "Create ZIP" to compress selected files
3. **Extract Archive**: Select a single archive file and tap "Extract Archive"
4. **AI Analyze**: Tap "AI Analyze" to test the JNI stub (returns mock response)

## Performance Notes

- Minimum Android API: 32 (Android 12L)
- Target API: 35 (Android 15)
- Supported ABIs: arm64-v8a, armeabi-v7a, x86, x86_64
- Memory: Streams files in 8KB chunks to handle large archives

## Adding llama.cpp (Stage D)

**Option A - Source Integration**:
1. Place llama.cpp source tree in `app/src/main/cpp/llama_src/`
2. Update `CMakeLists.txt`:
```
add_subdirectory(llama_src)
target_link_libraries(llamainfer llama ${log-lib})
```

**Option B - Prebuilt Library**:
1. Build llama.cpp externally for each ABI
2. Place `.so` files in `app/src/main/jniLibs/`
3. Update `CMakeLists.txt` to link against prebuilt library

## Known Limitations (Stage A)

- AI integration is stubbed (mock responses only)
- No split archive support yet (Stage B)
- No encryption beyond basic ZIP password (Stage B adds AES-256)
- No repair heuristics (Stage B)
- No WorkManager background processing (Stage B)

## License Compliance

- **junrar**: LGPL - include license notice in your About screen
- **zip4j**: Apache 2.0
- **commons-compress**: Apache 2.0
- **TensorFlow Lite**: Apache 2.0

## Next Stage

**Stage B** adds:
- Split archive support (ZIP/7Z multi-volume)
- AES-256 encryption
- Archive repair heuristics
- WorkManager foreground service integration
- Progress notifications
- TAR/GZIP/XZ creation

Reply with **"Proceed Stage B"** when ready.
