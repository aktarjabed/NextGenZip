# NextGenZip - Stage D Documentation

**AI Integration and Persistent Settings**

## New Features in Stage D

### 🧠 On-Device AI
- **Real `llama.cpp` Integration**: The app now includes a JNI bridge to a native `llama.cpp` library.
- **AI-Powered Analysis**: A new `analyzeWithAI` function in the `ArchiveViewModel` allows the app to send prompts to the local AI model.
- **Kotlin Coroutine Wrapper**: The `LlamaAIManager` provides a safe and easy-to-use Kotlin interface for interacting with the native AI code.

### 💾 Persistent Settings
- **Jetpack DataStore**: The app now uses Jetpack DataStore to persist user settings across app launches.
- **Saved Preferences**: The `SettingsScreen` now saves and loads the user's preferred compression level, split size, and password.
- **Archive History**: The foundation for storing a history of archive operations has been laid with the `ArchiveHistoryEntry` data model.

## Usage Examples

### Running AI Analysis
```kotlin
// In the UI, you can now call the new function in the ArchiveViewModel:
viewModel.analyzeWithAI(
    modelPath = "/path/to/your/model.gguf",
    prompt = "Suggest the best compression format for these files."
)
```

### Persisting Settings
```kotlin
// The SettingsViewModel now automatically loads and saves settings.
// For example, when you change the compression level:
viewModel.setCompressionLevel(9) // This will be saved to DataStore.
```

## Setup and Configuration

### `llama.cpp` Model
- To use the AI features, you must place a `llama.cpp`-compatible model file (in GGUF format) on the device's storage.
- The path to this model file must be provided to the `analyzeWithAI` function.

### Native Build
- The `CMakeLists.txt` file has been updated to build both the original `llamainfer` and the new `llama_native` libraries.
- Ensure you have the NDK installed and configured in your Android Studio environment.

## Next Stage (Stage E)

**Planned Enhancements:**
- A rich UI for viewing and managing the archive history.
- Cloud synchronization for settings and history.
- Advanced AI features, such as smart suggestions for archive names.
- Further performance optimizations for the AI and archive engines.
