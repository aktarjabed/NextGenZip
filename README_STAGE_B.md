# NextGenZip - Stage B Documentation

**Enhanced Archive Engine with Background Processing**

## New Features in Stage B

### 🎯 Advanced Archive Support
- **Multiple Formats**: ZIP, 7Z, TAR, GZIP, XZ creation & extraction
- **AES-256 Encryption**: Military-grade encryption for sensitive files
- **Compression Levels**: 0 (Store) to 9 (Maximum)
- **Split Archives**: Multi-volume archive support (foundation)

### 🔄 Background Processing
- **WorkManager Integration**: Reliable background operations
- **Foreground Services**: User-visible progress with notifications
- **Operation Queuing**: Manage multiple archive jobs
- **Progress Tracking**: Real-time updates in Compose UI

### 🛡️ Enterprise Features
- **ZIP Repair**: Basic corruption detection and recovery
- **Error Handling**: Graceful failure recovery
- **Operation History**: Track completed and failed jobs
- **Cancel/Resume**: User control over long-running operations

## Usage Examples

### Create Encrypted Archive
```kotlin
val operation = ArchiveOperation(
type = OperationType.CREATE,
inputUris = selectedUris,
outputPath = outputFile.absolutePath,
format = ArchiveFormat.ZIP,
password = "securepassword",
compressionLevel = 9,
encrypt = true
)

ArchiveManager.createArchive(context, operation) { progress ->
// Update UI with progress
}
```

### Extract with Progress
```kotlin
val operation = ArchiveOperation(
type = OperationType.EXTRACT,
inputUris = listOf(archiveUri),
outputPath = outputDir.absolutePath,
format = ArchiveFormat.SEVEN_ZIP
)

ArchiveManager.extractArchiveWithProgress(context, operation) { progress ->
// Update progress bar and current file
}
```

## Performance Characteristics

- **Memory Usage**: Streams files in 8KB chunks (OOM prevention)
- **CPU Usage**: Background-optimized compression algorithms
- **Battery**: WorkManager ensures efficient scheduling
- **Storage**: Temporary files automatically cleaned up

## Next Stage (Stage C)

**Planned Enhancements:**
- Cloud storage integration (Google Drive, Dropbox)
- Advanced repair algorithms
- Archive preview and content browsing
- Batch operations and templates
- Performance optimizations for large archives

## License Compliance

All dependencies updated with proper licensing:
- **zip4j**: Apache 2.0
- **commons-compress**: Apache 2.0
- **junrar**: LGPL (include notice)
- **WorkManager**: Apache 2.0
