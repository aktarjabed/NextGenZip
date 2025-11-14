package com.aktarjabed.nextgenzip.data

import android.content.Context
import android.database.Cursor
import android.net.Uri
import android.provider.OpenableColumns
import net.lingala.zip4j.ZipFile
import net.lingala.zip4j.model.ZipParameters
import net.lingala.zip4j.model.enums.AesKeyStrength
import net.lingala.zip4j.model.enums.CompressionMethod
import net.lingala.zip4j.model.enums.EncryptionMethod
import org.apache.commons.compress.archivers.tar.TarArchiveEntry
import org.apache.commons.compress.archivers.tar.TarArchiveInputStream
import org.apache.commons.compress.archivers.tar.TarArchiveOutputStream
import org.apache.commons.compress.compressors.gzip.GzipCompressorInputStream
import org.apache.commons.compress.compressors.gzip.GzipCompressorOutputStream
import org.apache.commons.compress.compressors.xz.XZCompressorInputStream
import org.apache.commons.compress.compressors.xz.XZCompressorOutputStream
import org.apache.commons.compress.archivers.sevenz.SevenZFile
import com.github.junrar.Junrar
import java.io.*
import kotlin.math.min

/**
 * ArchiveEngine - Complete archive operations for NextGenZip
 *
 * All methods are synchronous and should be called from IO dispatcher/background thread.
 *
 * Features:
 * - ZIP creation with AES-256 encryption and split volumes
 * - TAR.GZ and TAR.XZ creation
 * - Multi-format extraction (ZIP, 7Z, RAR, TAR.GZ, TAR.XZ)
 * - ZIP repair heuristics
 */
object ArchiveEngine {

    private const val BUFFER_SIZE = 8192
    private const val MIN_SPLIT_SIZE = 65536L // 64KB minimum per ZIP spec

    /**
     * Create ZIP with optional AES-256 encryption and split volumes.
     *
     * @param context Context for SAF URI resolution
     * @param uris List of content URIs to compress
     * @param outZipPath Full path to output ZIP file
     * @param password Optional password (enables AES-256 if provided)
     * @param splitSizeInBytes Split size in bytes (0 = no split, must be >= 64KB if used)
     * @throws IOException on I/O failure
     * @throws IllegalArgumentException if split size is invalid
     */
    fun createZipWithSplitAndAes(
        context: Context,
        uris: List<Uri>,
        outZipPath: String,
        password: String? = null,
        splitSizeInBytes: Long = 0L
    ) {
        // Validate split size
        if (splitSizeInBytes > 0L && splitSizeInBytes < MIN_SPLIT_SIZE) {
            throw IllegalArgumentException("Split size must be at least $MIN_SPLIT_SIZE bytes (64KB)")
        }

        val tmpFiles = mutableListOf<File>()
        try {
            // Stream URIs to temp files
            for (uri in uris) {
                tmpFiles.add(streamUriToTempFile(context, uri))
            }

            val zipFile = ZipFile(outZipPath)
            val params = ZipParameters().apply {
                compressionMethod = CompressionMethod.DEFLATE
                if (!password.isNullOrEmpty()) {
                    isEncryptFiles = true
                    encryptionMethod = EncryptionMethod.AES
                    aesKeyStrength = AesKeyStrength.KEY_STRENGTH_256
                }
            }

            if (!password.isNullOrEmpty()) {
                zipFile.setPassword(password.toCharArray())
            }

            if (splitSizeInBytes > 0L) {
                // Create split ZIP
                zipFile.createSplitZipFile(tmpFiles, params, true, splitSizeInBytes)
            } else {
                // Regular ZIP
                for (file in tmpFiles) {
                    zipFile.addFile(file, params)
                }
            }
        } finally {
            // Cleanup temp files
            for (f in tmpFiles) {
                try { f.delete() } catch (_: Exception) {}
            }
        }
    }

    /**
     * Create TAR.GZ archive from URIs (streaming).
     *
     * @param context Context for SAF
     * @param uris List of URIs to compress
     * @param outTarGzPath Full path to output .tar.gz file
     * @throws IOException on failure
     */
    fun createTarGz(context: Context, uris: List<Uri>, outTarGzPath: String) {
        val outFile = File(outTarGzPath)
        outFile.parentFile?.mkdirs()

        try {
            FileOutputStream(outFile).use { fos ->
                BufferedOutputStream(fos).use { buffered ->
                    GzipCompressorOutputStream(buffered).use { gzOut ->
                        TarArchiveOutputStream(gzOut).use { tarOut ->
                            tarOut.setLongFileMode(TarArchiveOutputStream.LONGFILE_GNU)

                            for (uri in uris) {
                                val name = uriToSafeName(context, uri)
                                val tmp = streamUriToTempFile(context, uri)
                                try {
                                    val entry = TarArchiveEntry(tmp, name)
                                    tarOut.putArchiveEntry(entry)
                                    FileInputStream(tmp).use { fis ->
                                        val buffer = ByteArray(BUFFER_SIZE)
                                        var read: Int
                                        while (fis.read(buffer).also { read = it } != -1) {
                                            tarOut.write(buffer, 0, read)
                                        }
                                    }
                                    tarOut.closeArchiveEntry()
                                } finally {
                                    try { tmp.delete() } catch (_: Exception) {}
                                }
                            }
                            tarOut.finish()
                        }
                    }
                }
            }
        } catch (e: Exception) {
            outFile.delete() // Clean up on failure
            throw e
        }
    }

    /**
     * Create TAR.XZ archive from URIs (streaming).
     * WARNING: XZ compression is CPU-intensive on mobile devices.
     *
     * @param context Context for SAF
     * @param uris List of URIs to compress
     * @param outTarXzPath Full path to output .tar.xz file
     * @throws IOException on failure
     */
    fun createTarXz(context: Context, uris: List<Uri>, outTarXzPath: String) {
        val outFile = File(outTarXzPath)
        outFile.parentFile?.mkdirs()

        try {
            FileOutputStream(outFile).use { fos ->
                BufferedOutputStream(fos).use { buffered ->
                    XZCompressorOutputStream(buffered).use { xzOut ->
                        TarArchiveOutputStream(xzOut).use { tarOut ->
                            tarOut.setLongFileMode(TarArchiveOutputStream.LONGFILE_GNU)

                            for (uri in uris) {
                                val name = uriToSafeName(context, uri)
                                val tmp = streamUriToTempFile(context, uri)
                                try {
                                    val entry = TarArchiveEntry(tmp, name)
                                    tarOut.putArchiveEntry(entry)
                                    FileInputStream(tmp).use { fis ->
                                        val buffer = ByteArray(BUFFER_SIZE)
                                        var read: Int
                                        while (fis.read(buffer).also { read = it } != -1) {
                                            tarOut.write(buffer, 0, read)
                                        }
                                    }
                                    tarOut.closeArchiveEntry()
                                } finally {
                                    try { tmp.delete() } catch (_: Exception) {}
                                }
                            }
                            tarOut.finish()
                        }
                    }
                }
            }
        } catch (e: Exception) {
            outFile.delete() // Clean up on failure
            throw e
        }
    }

    /**
     * Extract archive (auto-detects format from filename).
     *
     * Supported formats: ZIP, 7Z, RAR, TAR.GZ, TAR.XZ
     *
     * @param context Context for SAF
     * @param archiveUri URI of archive file
     * @param outDirPath Destination directory path
     * @param password Optional password (ZIP only)
     * @param progressCallback Callback receiving progress 0.0-1.0
     * @throws IOException on extraction failure
     * @throws IllegalArgumentException if format unsupported
     */
    fun extractArchive(
        context: Context,
        archiveUri: Uri,
        outDirPath: String,
        password: String? = null,
        progressCallback: (Float) -> Unit = {}
    ) {
        val name = ArchiveEngineUtils.uriToName(context, archiveUri) ?: "archive"
        val lower = name.lowercase()
        val outDir = File(outDirPath)
        outDir.mkdirs()

        when {
            lower.endsWith(".zip") || lower.endsWith(".cbz") -> {
                val tmp = streamUriToTempFile(context, archiveUri)
                try {
                    val zip = ZipFile(tmp)
                    if (!password.isNullOrEmpty()) {
                        zip.setPassword(password.toCharArray())
                    }
                    zip.extractAll(outDirPath)
                    progressCallback(1f)
                } finally {
                    try { tmp.delete() } catch (_: Exception) {}
                }
            }

            lower.endsWith(".7z") -> {
                val tmp = streamUriToTempFile(context, archiveUri)
                try {
                    var total = 0L
                    SevenZFile(tmp).use { szf ->
                        var entry = szf.nextEntry
                        while (entry != null) {
                            total += entry.size
                            entry = szf.nextEntry
                        }
                    }

                    var processed = 0L
                    SevenZFile(tmp).use { szf ->
                        var entry = szf.nextEntry
                        while (entry != null) {
                            val outFile = File(outDir, entry.name)
                            if (entry.isDirectory) {
                                outFile.mkdirs()
                            } else {
                                outFile.parentFile?.mkdirs()
                                FileOutputStream(outFile).use { fos ->
                                    val buffer = ByteArray(BUFFER_SIZE)
                                    var remaining = entry.size
                                    while (remaining > 0) {
                                        val toRead = min(BUFFER_SIZE.toLong(), remaining).toInt()
                                        val read = szf.read(buffer, 0, toRead)
                                        if (read <= 0) break
                                        fos.write(buffer, 0, read)
                                        processed += read
                                        remaining -= read
                                        if (total > 0) {
                                            progressCallback((processed.toFloat() / total).coerceAtMost(1f))
                                        }
                                    }
                                }
                            }
                            entry = szf.nextEntry
                        }
                    }
                    progressCallback(1f)
                } finally {
                    try { tmp.delete() } catch (_: Exception) {}
                }
            }

            lower.endsWith(".rar") -> {
                val tmp = streamUriToTempFile(context, archiveUri)
                try {
                    progressCallback(0.0f)
                    Junrar.extract(tmp.absolutePath, outDirPath)
                    progressCallback(1f)
                } finally {
                    try { tmp.delete() } catch (_: Exception) {}
                }
            }

            lower.endsWith(".tar.gz") || lower.endsWith(".tgz") -> {
                val tmp = streamUriToTempFile(context, archiveUri)
                try {
                    ArchiveEngineUtils.extractTarCompressed(tmp, outDirPath, true, progressCallback)
                } finally {
                    try { tmp.delete() } catch (_: Exception) {}
                }
            }

            lower.endsWith(".tar.xz") || lower.endsWith(".txz") -> {
                val tmp = streamUriToTempFile(context, archiveUri)
                try {
                    ArchiveEngineUtils.extractTarCompressed(tmp, outDirPath, false, progressCallback)
                } finally {
                    try { tmp.delete() } catch (_: Exception) {}
                }
            }

            else -> {
                throw IllegalArgumentException("Unsupported archive format: $name")
            }
        }
    }

    /**
     * Best-effort ZIP repair.
     * Attempts to recover readable entries from corrupted ZIP.
     *
     * @param damagedZipFile Path to damaged ZIP
     * @param recoveredOutDir Directory to extract recovered files
     * @param createRecoveredZipAt Optional path to create clean ZIP from recovered files
     * @throws IOException on failure
     */
    fun repairZipBestEffort(
        damagedZipFile: File,
        recoveredOutDir: File,
        createRecoveredZipAt: File? = null
    ) {
        recoveredOutDir.mkdirs()

        try {
            // Try zip4j first (fast path)
            val zip = ZipFile(damagedZipFile)
            zip.extractAll(recoveredOutDir.absolutePath)
        } catch (e: Exception) {
            // Fallback: salvage with ZipInputStream
            FileInputStream(damagedZipFile).use { fis ->
                java.util.zip.ZipInputStream(BufferedInputStream(fis)).use { zis ->
                    var entry = zis.nextEntry
                    while (entry != null) {
                        try {
                            val outFile = File(recoveredOutDir, entry.name)
                            if (!entry.isDirectory) {
                                outFile.parentFile?.mkdirs()
                                FileOutputStream(outFile).use { fos ->
                                    val buffer = ByteArray(BUFFER_SIZE)
                                    var read: Int
                                    while (zis.read(buffer).also { read = it } > 0) {
                                        fos.write(buffer, 0, read)
                                    }
                                }
                            } else {
                                outFile.mkdirs()
                            }
                        } catch (_: Exception) {
                            // Skip corrupted entry
                        } finally {
                            try { zis.closeEntry() } catch (_: Exception) {}
                        }
                        entry = zis.nextEntry
                    }
                }
            }
        }

        // Create clean ZIP if requested
        createRecoveredZipAt?.let { cleanZipFile ->
            val zip = ZipFile(cleanZipFile)
            val params = ZipParameters().apply {
                compressionMethod = CompressionMethod.DEFLATE
            }
            val files = recoveredOutDir.walkTopDown().filter { it.isFile }.toList()
            if (files.isNotEmpty()) {
                zip.addFiles(files, params)
            }
        }
    }

    // Private helpers

    private fun streamUriToTempFile(context: Context, uri: Uri): File {
        val tmp = File.createTempFile("ngz_", null, context.cacheDir)
        context.contentResolver.openInputStream(uri)?.use { input ->
            FileOutputStream(tmp).use { output ->
                val buffer = ByteArray(BUFFER_SIZE)
                var read: Int
                while (input.read(buffer).also { read = it } != -1) {
                    output.write(buffer, 0, read)
                }
            }
        } ?: throw IOException("Cannot open input stream for URI: $uri")
        return tmp
    }

    private fun uriToSafeName(context: Context, uri: Uri): String {
        return ArchiveEngineUtils.uriToName(context, uri)?.replace(Regex("[^a-zA-Z0-9._-]"), "_")
            ?: "file_${System.currentTimeMillis()}"
    }
}

/**
 * Internal utilities for ArchiveEngine
 */
internal object ArchiveEngineUtils {

    fun uriToName(context: Context, uri: Uri): String? {
        val cursor: Cursor? = context.contentResolver.query(uri, null, null, null, null)
        cursor?.use {
            val nameIndex = it.getColumnIndex(OpenableColumns.DISPLAY_NAME)
            if (nameIndex >= 0 && it.moveToFirst()) {
                return it.getString(nameIndex)
            }
        }
        return uri.lastPathSegment
    }

    fun extractTarCompressed(
        tmpFile: File,
        outDirPath: String,
        isGzip: Boolean,
        progressCallback: (Float) -> Unit = {}
    ) {
        val outDir = File(outDirPath)
        outDir.mkdirs()

        val inputStream = if (isGzip) {
            GzipCompressorInputStream(BufferedInputStream(FileInputStream(tmpFile)))
        } else {
            XZCompressorInputStream(BufferedInputStream(FileInputStream(tmpFile)))
        }

        TarArchiveInputStream(inputStream).use { tarIn ->
            var entry = tarIn.nextTarEntry
            var processed = 0
            while (entry != null) {
                val outFile = File(outDir, entry.name)
                if (entry.isDirectory) {
                    outFile.mkdirs()
                } else {
                    outFile.parentFile?.mkdirs()
                    FileOutputStream(outFile).use { fos ->
                        val buffer = ByteArray(8192)
                        var read: Int
                        while (tarIn.read(buffer).also { read = it } > 0) {
                            fos.write(buffer, 0, read)
                        }
                    }
                }
                processed++
                progressCallback(0.5f) // Simplified progress
                entry = tarIn.nextTarEntry
            }
            progressCallback(1f)
        }
    }
}
