package com.aktarjabed.nextgenzip.data

import android.content.Context
import android.net.Uri
import com.aktarjabed.nextgenzip.data.model.*
import net.lingala.zip4j.ZipFile
import net.lingala.zip4j.model.ZipParameters
import net.lingala.zip4j.model.enums.CompressionLevel
import net.lingala.zip4j.model.enums.CompressionMethod
import net.lingala.zip4j.model.enums.EncryptionMethod
import org.apache.commons.compress.archivers.sevenz.SevenZFile
import org.apache.commons.compress.archivers.tar.TarArchiveEntry
import org.apache.commons.compress.archivers.tar.TarArchiveInputStream
import org.apache.commons.compress.archivers.tar.TarArchiveOutputStream
import org.apache.commons.compress.compressors.gzip.GzipCompressorInputStream
import org.apache.commons.compress.compressors.gzip.GzipCompressorOutputStream
import org.apache.commons.compress.compressors.xz.XZCompressorInputStream
import org.apache.commons.compress.compressors.xz.XZCompressorOutputStream
import com.github.junrar.Junrar
import org.apache.commons.io.IOUtils
import java.io.*

object ArchiveEngine {

    // This is the primary method the ViewModel will call for ZIP creation.
    suspend fun createZipWithSplitAndAesSuspend(
        context: Context,
        inputUris: List<Uri>,
        outZipFile: File,
        password: String?,
        splitSizeInBytes: Long,
        progressCallback: (Float) -> Unit
    ) {
        val zipFile = ZipFile(outZipFile)
        val tempFiles = mutableListOf<File>()

        try {
            // Configure encryption if needed
            if (!password.isNullOrEmpty()) {
                zipFile.setPassword(password.toCharArray())
            }

            val params = ZipParameters().apply {
                compressionMethod = CompressionMethod.DEFLATE
                compressionLevel = CompressionLevel.NORMAL // Default, can be customized
                if (!password.isNullOrEmpty()) {
                    isEncryptFiles = true
                    encryptionMethod = EncryptionMethod.AES
                }
            }

            // Convert URIs to temp files to be added to the archive
            uris.forEach { uri ->
                tempFiles.add(uriToTempFile(context, uri))
            }

            // Create split archive if necessary, otherwise a single file
            if (splitSizeInBytes > 0) {
                zipFile.createSplitZipFile(tempFiles, params, true, splitSizeInBytes)
            } else {
                zipFile.addFiles(tempFiles, params)
            }
        } finally {
            // Clean up temporary files
            tempFiles.forEach { it.delete() }
        }
    }

    // This is the primary method the ViewModel will call for extraction.
    fun extractArchive(
        context: Context,
        archiveUri: Uri,
        outDirPath: String,
        password: String?,
        progressCallback: (Float) -> Unit
    ) {
        val fileName = getFileName(context, archiveUri)?.lowercase() ?: "archive"

        when {
            fileName.endsWith(".zip") || fileName.endsWith(".cbz") ->
                extractZipArchive(context, archiveUri, outDirPath, password, progressCallback)
            fileName.endsWith(".7z") ->
                extract7zArchive(context, archiveUri, outDirPath, progressCallback)
            fileName.endsWith(".tar") ->
                extractTarArchive(context, archiveUri, outDirPath, progressCallback)
            fileName.endsWith(".gz") || fileName.endsWith(".tgz") ->
                extractGzipArchive(context, archiveUri, outDirPath, progressCallback)
            fileName.endsWith(".xz") || fileName.endsWith(".txz") ->
                extractXzArchive(context, archiveUri, outDirPath, progressCallback)
            fileName.endsWith(".rar") ->
                extractRarArchive(context, archiveUri, outDirPath, progressCallback)
            else -> throw IllegalArgumentException("Unsupported archive format: $fileName")
        }
    }

    private fun extractZipArchive(
        context: Context,
        archiveUri: Uri,
        outDirPath: String,
        password: String?,
        progressCallback: (Float) -> Unit
    ) {
        val tmp = uriToTempFile(context, archiveUri)
        try {
            val zipFile = ZipFile(tmp)
            if (!password.isNullOrEmpty()) {
                zipFile.setPassword(password.toCharArray())
            }

            val fileHeaders = zipFile.fileHeaders
            val totalSize = fileHeaders.sumOf { it.uncompressedSize }
            var processedSize = 0L

            fileHeaders.forEach { header ->
                progressCallback(processedSize.toFloat() / totalSize)
                zipFile.extractFile(header, outDirPath)
                processedSize += header.uncompressedSize
                progressCallback(processedSize.toFloat() / totalSize)
            }
            progressCallback(1f)
        } finally {
            tmp.delete()
        }
    }

    private fun extract7zArchive(
        context: Context,
        archiveUri: Uri,
        outDirPath: String,
        progressCallback: (Float) -> Unit
    ) {
        val tmp = uriToTempFile(context, archiveUri)
        try {
            var totalSize = 0L
            SevenZFile(tmp).use { szf ->
                szf.entries.forEach { entry -> totalSize += entry.size }
            }

            var processedSize = 0L
            SevenZFile(tmp).use { szf ->
                var entry = szf.nextEntry
                while (entry != null) {
                    val outFile = File(outDirPath, entry.name)
                    if (entry.isDirectory) {
                        outFile.mkdirs()
                    } else {
                        outFile.parentFile?.mkdirs()
                        FileOutputStream(outFile).use { fos ->
                            val buffer = ByteArray(8192)
                            var len: Int
                            while (szf.read(buffer).also { len = it } > 0) {
                                fos.write(buffer, 0, len)
                                processedSize += len
                                if (totalSize > 0) {
                                    progressCallback(processedSize.toFloat() / totalSize)
                                }
                            }
                        }
                    }
                    entry = szf.nextEntry
                }
            }
            progressCallback(1f)
        } finally {
            tmp.delete()
        }
    }

    private fun extractTarArchive(
        context: Context,
        archiveUri: Uri,
        outDirPath: String,
        progressCallback: (Float) -> Unit
    ) {
        context.contentResolver.openInputStream(archiveUri)?.use { fis ->
            BufferedInputStream(fis).use { bis ->
                TarArchiveInputStream(bis).use { tarIn ->
                    var entry: TarArchiveEntry?
                    while (tarIn.nextTarEntry.also { entry = it } != null) {
                        entry?.let {
                            val outFile = File(outDirPath, it.name)
                            if (it.isDirectory) {
                                outFile.mkdirs()
                            } else {
                                outFile.parentFile?.mkdirs()
                                FileOutputStream(outFile).use { fos ->
                                    IOUtils.copy(tarIn, fos)
                                }
                            }
                        }
                        // TAR progress is difficult to track accurately without reading twice
                        progressCallback(0.5f) // Indicate activity
                    }
                }
            }
        }
        progressCallback(1f)
    }

    private fun extractGzipArchive(
        context: Context,
        archiveUri: Uri,
        outDirPath: String,
        progressCallback: (Float) -> Unit
    ) {
        val outFileName = getFileName(context, archiveUri)?.removeSuffix(".gz") ?: "extracted_file"
        context.contentResolver.openInputStream(archiveUri)?.use { fis ->
            BufferedInputStream(fis).use { bis ->
                GzipCompressorInputStream(bis).use { gzipIn ->
                    FileOutputStream(File(outDirPath, outFileName)).use { fos ->
                        IOUtils.copy(gzipIn, fos)
                    }
                }
            }
        }
        progressCallback(1f)
    }

    private fun extractXzArchive(
        context: Context,
        archiveUri: Uri,
        outDirPath: String,
        progressCallback: (Float) -> Unit
    ) {
        val outFileName = getFileName(context, archiveUri)?.removeSuffix(".xz") ?: "extracted_file"
        context.contentResolver.openInputStream(archiveUri)?.use { fis ->
            BufferedInputStream(fis).use { bis ->
                XZCompressorInputStream(bis).use { xzIn ->
                    FileOutputStream(File(outDirPath, outFileName)).use { fos ->
                        IOUtils.copy(xzIn, fos)
                    }
                }
            }
        }
        progressCallback(1f)
    }

    private fun extractRarArchive(
        context: Context,
        archiveUri: Uri,
        outDirPath: String,
        progressCallback: (Float) -> Unit
    ) {
        val tmp = uriToTempFile(context, archiveUri)
        try {
            progressCallback(0.5f)
            Junrar.extract(tmp.absolutePath, outDirPath)
            progressCallback(1f)
        } finally {
            tmp.delete()
        }
    }

    fun repairZipArchive(filePath: String): Boolean {
        return try {
            val zipFile = ZipFile(filePath)
            zipFile.isValidZipFile
        } catch (e: Exception) {
            false
        }
    }

    // Helper Methods
    private fun uriToTempFile(context: Context, uri: Uri): File {
        val tmp = File.createTempFile("ngz_", null, context.cacheDir)
        context.contentResolver.openInputStream(uri)?.use { input ->
            FileOutputStream(tmp).use { output ->
                input.copyTo(output)
            }
        } ?: throw IOException("Cannot open input stream for URI: $uri")
        return tmp
    }

    private fun getFileName(context: Context, uri: Uri): String? {
        context.contentResolver.query(uri, null, null, null, null)?.use { cursor ->
            val nameIndex = cursor.getColumnIndex(android.provider.OpenableColumns.DISPLAY_NAME)
            if (nameIndex >= 0 && cursor.moveToFirst()) {
                return cursor.getString(nameIndex)
            }
        }
        return uri.lastPathSegment
    }
}
