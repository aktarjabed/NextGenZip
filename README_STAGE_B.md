# NextGenZip — Stage B Documentation

**Full Archive Engine Implementation**

## What's New in Stage B

- ✅ ZIP creation with AES-256 encryption
- ✅ ZIP split volume support (multi-part archives)
- ✅ TAR.GZ and TAR.XZ creation
- ✅ Multi-format extraction (ZIP, 7Z, RAR, TAR.GZ, TAR.XZ)
- ✅ ZIP repair heuristics (best-effort recovery)
- ✅ WorkManager background processing with foreground service
- ✅ Progress notifications with cancellation support

## Usage Examples

### Direct API Usage (from coroutine/background thread)

```
import com.aktarjabed.nextgenzip.data.ArchiveEngine
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

// Create encrypted split ZIP
withContext(Dispatchers.IO) {
    ArchiveEngine.createZipWithSplitAndAes(
        context = context,
        uris = listOf(uri1, uri2, uri3),
        outZipPath = "/storage/emulated/0/Download/archive.zip",
        password = "MySecretPassword123",
        splitSizeInBytes = 100 * 1024 * 1024 // 100MB parts
    )
}

// Extract archive with progress
withContext(Dispatchers.IO) {
    ArchiveEngine.extractArchive(
        context = context,
        archiveUri = archiveUri,
        outDirPath = "/storage/emulated/0/Download/extracted",
        password = "MySecretPassword123"
    ) { progress ->
        println("Progress: ${(progress * 100).toInt()}%")
    }
}

// Create TAR.GZ
withContext(Dispatchers.IO) {
    ArchiveEngine.createTarGz(
        context = context,
        uris = listOf(uri1, uri2),
        outTarGzPath = "/storage/emulated/0/Download/archive.tar.gz"
    )
}

// Repair damaged ZIP
withContext(Dispatchers.IO) {
    ArchiveEngine.repairZipBestEffort(
        damagedZipFile = File("/path/to/damaged.zip"),
        recoveredOutDir = File("/path/to/recovery_dir"),
        createRecoveredZipAt = File("/path/to/repaired.zip")
    )
}
```

### WorkManager Background Processing

```
import androidx.work.*
import com.aktarjabed.nextgenzip.workers.ArchiveWorker

// Enqueue compression job
val uriStrings = uris.map { it.toString() }.toTypedArray()
val inputData = workDataOf(
    ArchiveWorker.KEY_OP_TYPE to ArchiveWorker.OP_COMPRESS,
    ArchiveWorker.KEY_URIS to uriStrings,
    ArchiveWorker.KEY_OUT_PATH to "/storage/emulated/0/Download/output.zip",
    ArchiveWorker.KEY_PASSWORD to "SecurePass",
    ArchiveWorker.KEY_SPLIT_SIZE to (100L * 1024L * 1024L) // 100MB
)

val request = OneTimeWorkRequestBuilder<ArchiveWorker>()
    .setInputData(inputData)
    .build()

WorkManager.getInstance(context).enqueue(request)

// Observe progress
WorkManager.getInstance(context)
    .getWorkInfoByIdLiveData(request.id)
    .observe(lifecycleOwner) { workInfo ->
        when (workInfo?.state) {
            WorkInfo.State.SUCCEEDED -> {
                val resultPath = workInfo.outputData.getString(ArchiveWorker.KEY_RESULT_PATH)
                println("Complete: $resultPath")
            }
            WorkInfo.State.FAILED -> {
                val error = workInfo.outputData.getString(ArchiveWorker.KEY_ERROR)
                println("Error: $error")
            }
            else -> { /* Running... */ }
        }
    }
```

## Supported Archive Formats

| Format | Create | Extract | Encryption | Split |
|--------|--------|---------|------------|-------|
| ZIP | ✅ | ✅ | AES-256 | ✅ |
| 7Z | ❌ | ✅ | ❌ | ❌ |
| RAR | ❌ | ✅ | ❌ | ❌ |
| TAR.GZ | ✅ | ✅ | ❌ | ❌ |
| TAR.XZ | ✅ | ✅ | ❌ | ❌ |

## Performance Notes

- **Split archives**: Minimum split size is 64KB (ZIP spec requirement)
- **TAR.XZ**: Very CPU-intensive; avoid on low-end devices
- **Large files**: All operations use 8KB buffered streaming to prevent OOM
- **7Z creation**: Not implemented (requires native binding - see below)

## Adding 7Z Creation Support

To enable 7Z creation, integrate sevenzipjbinding or build native 7z libs:

1. Add dependency to `app/build.gradle.kts`:
   ```
   implementation("net.sf.sevenzipjbinding:sevenzipjbinding:16.02-2.01")
   implementation("net.sf.sevenzipjbinding:sevenzipjbinding-all-platforms:16.02-2.01")
   ```

2. Implement 7Z creation in `ArchiveEngine.kt`

## Permissions

Already configured in manifest:
- `FOREGROUND_SERVICE` - Required for WorkManager foreground service
- `FOREGROUND_SERVICE_DATA_SYNC` - Required for Android 14+
- `POST_NOTIFICATIONS` - Required for Android 13+

## Troubleshooting

**Problem**: `ForegroundServiceStartNotAllowedException` on Android 12+
**Solution**: Already handled - worker continues without foreground service if app in background

**Problem**: Split ZIP won't create
**Solution**: Ensure split size >= 64KB and output directory is writable

**Problem**: TAR.XZ extraction fails
**Solution**: XZ decompression requires more memory - increase heap or use TAR.GZ

## Next Stage

**Stage C** will add:
- Complete Material 3 UI redesign
- Archive creation wizard
- Split/encryption settings dialogs
- File browser with preview
- History and favorites

Reply with **"Proceed Stage C"** when ready.
