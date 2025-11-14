# NextGenZip — Stage C Documentation

**Complete Material 3 UI Implementation**

## What's New in Stage C

- ✅ Material 3 Design System with dynamic theming
- ✅ Navigation with Jetpack Navigation Compose
- ✅ Archive creation wizard with file picker
- ✅ Extract screen with password support
- ✅ Settings screen with preferences
- ✅ Custom components (dialogs, progress cards, file list items)
- ✅ ViewModels with state management
- ✅ Edge-to-edge display support

## Screen Overview

### Home Screen
- Main navigation hub
- Quick access to create/extract operations
- Material 3 elevated cards

### Archive Screen
- Multi-file selection with SAF
- Password protection dialog
- Split archive settings dialog
- Real-time progress tracking
- File list with remove capability

### Extract Screen
- Single archive selection
- Password input for encrypted archives
- Format auto-detection
- Progress indicators

### Settings Screen
- Compression level adjustment
- Security preferences
- Cache management
- App information

## Navigation Flow

```
HomeScreen
├── ArchiveScreen (Create)
├── ExtractScreen (Extract)
└── SettingsScreen (Preferences)
```

## New Dependencies Required

Add to `app/build.gradle.kts`:

```
dependencies {
    // Existing dependencies...

    // Navigation
    implementation("androidx.navigation:navigation-compose:2.8.0")

    // ViewModel
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.9.0")

    // Icons Extended (for additional Material icons)
    implementation("androidx.compose.material:material-icons-extended:1.7.0")
}
```

## Usage

### Running the App

1. Build and run on device/emulator
2. Grant storage permissions when prompted
3. Navigate using the home screen cards

### Creating an Archive

1. Tap "Create Archive" on home screen
2. Tap "Add Files" to select files
3. (Optional) Tap lock icon to set password
4. (Optional) Tap split icon to configure volume splitting
5. Tap "Create ZIP" FAB
6. Wait for completion

### Extracting an Archive

1. Tap "Extract Archive" on home screen
2. Tap "Browse Files" to select archive
3. (Optional) Set password if archive is encrypted
4. Tap "Extract Archive"
5. Wait for completion

## Customization

### Theme Colors

Edit `Theme.kt` to customize color schemes:

```
private val LightColorScheme = lightColorScheme(
    primary = Color(0xFF0061A4),
    // ... customize other colors
)
```

### Default Settings

Modify `SettingsUiState` in `SettingsViewModel.kt`:

```
data class SettingsUiState(
    val compressionLevel: Int = 9, // Maximum compression
    val rememberPasswords: Boolean = true,
    // ... other defaults
)
```

## Known Limitations

- Settings are not persisted (Stage D will add DataStore)
- No file preview functionality
- Cache management button is placeholder
- No history/favorites tracking

## Performance Notes

- File list uses LazyColumn for efficient rendering
- ViewModels use StateFlow for reactive state updates
- All I/O operations run on Dispatchers.IO
- Progress updates don't block UI thread

## Next Stage

**Stage D** will add:
- DataStore for persistent settings
- Room database for history/favorites
- File preview support
- Real llama.cpp AI integration
- Advanced repair tools
- Cloud storage integration

## Build & Test

```
# Clean build
./gradlew clean

# Build APK
./gradlew assembleDebug

# Install on device
adb install app/build/outputs/apk/debug/app-debug.apk
```

Reply with **"Proceed Stage D"** when ready for AI integration and persistence layer.
