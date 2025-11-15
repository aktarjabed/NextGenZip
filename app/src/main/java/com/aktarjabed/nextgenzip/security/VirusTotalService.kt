package com.aktarjabed.nextgenzip.security

import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.MultipartBody
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject
import java.io.File
import java.security.MessageDigest

object VirusTotalService {
    private const val TAG = "VirusTotal"
    private const val BASE_URL = "https://www.virustotal.com/api/v3"

    private var API_KEY = ""
    private val client = OkHttpClient()

    fun setApiKey(key: String) {
        API_KEY = key
    }

    sealed class VirusTotalResult {
        data class Threat(
            val fileName: String,
            val sha256: String,
            val detectionRatio: String,
            val detections: List<Detection>,
            val lastAnalysis: String,
            val virusTotalLink: String
        ) : VirusTotalResult()

        data class Clean(
            val fileName: String,
            val sha256: String,
            val scanDate: String
        ) : VirusTotalResult()

        data class Undetected(
            val fileName: String,
            val sha256: String
        ) : VirusTotalResult()

        data class Error(val message: String) : VirusTotalResult()
    }

    data class Detection(
        val engine: String,
        val threatName: String
    )

    suspend fun scanFile(file: File): VirusTotalResult {
        return withContext(Dispatchers.IO) {
            try {
                if (API_KEY.isEmpty()) {
                    return@withContext VirusTotalResult.Error("API key not configured")
                }

                if (!file.exists()) {
                    return@withContext VirusTotalResult.Error("File not found")
                }

                val sha256 = calculateSHA256(file)
                val hashLookup = lookupFileHash(sha256)

                if (hashLookup is VirusTotalResult.Threat || hashLookup is VirusTotalResult.Clean) {
                    Log.i(TAG, "Found in hash database: ${file.name}")
                    return@withContext hashLookup
                }

                Log.i(TAG, "Uploading file for analysis: ${file.name}")
                return@withContext uploadAndScanFile(file, sha256)
            } catch (e: Exception) {
                Log.e(TAG, "Scan error: ${e.message}")
                VirusTotalResult.Error(e.message ?: "Unknown error")
            }
        }
    }

    private suspend fun lookupFileHash(sha256: String): VirusTotalResult {
        return withContext(Dispatchers.IO) {
            try {
                val request = Request.Builder()
                    .url("$BASE_URL/files/$sha256")
                    .addHeader("x-apikey", API_KEY)
                    .build()

                val response = client.newCall(request).execute()

                if (!response.isSuccessful) {
                    return@withContext VirusTotalResult.Undetected("", sha256)
                }

                val responseBody = response.body?.string() ?: return@withContext VirusTotalResult.Error("Empty response")
                val jsonResponse = JSONObject(responseBody)
                val data = jsonResponse.getJSONObject("data")
                val attributes = data.getJSONObject("attributes")

                val stats = attributes.getJSONObject("last_analysis_stats")
                val maliciousCount = stats.optInt("malicious", 0)

                return@withContext if (maliciousCount > 0) {
                    parseDetections(attributes, "")
                } else {
                    VirusTotalResult.Clean(
                        fileName = "",
                        sha256 = sha256,
                        scanDate = attributes.optString("creation_date", "Unknown")
                    )
                }
            } catch (e: Exception) {
                Log.w(TAG, "Hash lookup failed: ${e.message}")
                VirusTotalResult.Undetected("", sha256)
            }
        }
    }

    private suspend fun uploadAndScanFile(file: File, sha256: String): VirusTotalResult {
        return withContext(Dispatchers.IO) {
            try {
                val requestBody = MultipartBody.Builder()
                    .setType(MultipartBody.FORM)
                    .addFormDataPart("file", file.name,
                        okhttp3.RequestBody.create("application/octet-stream".toMediaType(), file))
                    .build()

                val uploadRequest = Request.Builder()
                    .url("$BASE_URL/files")
                    .addHeader("x-apikey", API_KEY)
                    .post(requestBody)
                    .build()

                val uploadResponse = client.newCall(uploadRequest).execute()

                if (!uploadResponse.isSuccessful) {
                    return@withContext VirusTotalResult.Error("Upload failed: ${uploadResponse.code}")
                }

                val uploadBody = uploadResponse.body?.string() ?: return@withContext VirusTotalResult.Error("Empty upload response")
                val uploadJson = JSONObject(uploadBody)
                val analysisId = uploadJson.getJSONObject("data").optString("id")

                if (analysisId.isEmpty()) {
                    return@withContext VirusTotalResult.Error("No analysis ID returned")
                }

                Thread.sleep(2000)
                return@withContext pollAnalysisResult(analysisId, file.name, sha256)
            } catch (e: Exception) {
                Log.e(TAG, "Upload/scan error: ${e.message}")
                VirusTotalResult.Error(e.message ?: "Unknown error")
            }
        }
    }

    private suspend fun pollAnalysisResult(
        analysisId: String,
        fileName: String,
        sha256: String,
        maxAttempts: Int = 5
    ): VirusTotalResult {
        return withContext(Dispatchers.IO) {
            repeat(maxAttempts) { attempt ->
                try {
                    val request = Request.Builder()
                        .url("$BASE_URL/analyses/$analysisId")
                        .addHeader("x-apikey", API_KEY)
                        .build()

                    val response = client.newCall(request).execute()

                    if (response.isSuccessful) {
                        val responseBody = response.body?.string() ?: return@repeat
                        val jsonResponse = JSONObject(responseBody)
                        val data = jsonResponse.getJSONObject("data")
                        val attributes = data.getJSONObject("attributes")
                        val status = attributes.optString("status")

                        if (status == "completed") {
                            return@withContext parseDetections(attributes, fileName)
                        }
                    }

                    if (attempt < maxAttempts - 1) {
                        Thread.sleep(2000)
                    }
                } catch (e: Exception) {
                    Log.w(TAG, "Poll attempt $attempt failed: ${e.message}")
                }
            }

            VirusTotalResult.Error("Analysis timeout")
        }
    }

    private fun parseDetections(
        attributes: JSONObject,
        fileName: String
    ): VirusTotalResult {
        try {
            val stats = attributes.getJSONObject("last_analysis_stats")
            val maliciousCount = stats.optInt("malicious", 0)
            val totalCount = stats.optInt("undetected", 0) +
                    stats.optInt("malicious", 0) +
                    stats.optInt("suspicious", 0) +
                    stats.optInt("unanalyzed", 0)

            if (maliciousCount == 0) {
                return VirusTotalResult.Clean(
                    fileName = fileName,
                    sha256 = attributes.optString("sha256", ""),
                    scanDate = attributes.optString("last_submission_date", "Unknown")
                )
            }

            val detections = mutableListOf<Detection>()
            val results = attributes.getJSONObject("last_analysis_results")

            results.keys().forEach { engine ->
                val result = results.getJSONObject(engine)
                if (result.optString("category") != "undetected") {
                    detections.add(Detection(
                        engine = engine,
                        threatName = result.optString("result", "Unknown")
                    ))
                }
            }

            val virusTotalUrl = "https://www.virustotal.com/gui/file/${attributes.optString("sha256", "")}"

            return VirusTotalResult.Threat(
                fileName = fileName,
                sha256 = attributes.optString("sha256", ""),
                detectionRatio = "$maliciousCount/$totalCount",
                detections = detections,
                lastAnalysis = attributes.optString("last_analysis_date", "Unknown"),
                virusTotalLink = virusTotalUrl
            )
        } catch (e: Exception) {
            Log.e(TAG, "Parse error: ${e.message}")
            return VirusTotalResult.Error("Failed to parse results")
        }
    }

    private fun calculateSHA256(file: File): String {
        val messageDigest = MessageDigest.getInstance("SHA-256")
        file.inputStream().use { fis ->
            val buffer = ByteArray(8192)
            var read: Int
            while (fis.read(buffer).also { read = it } != -1) {
                messageDigest.update(buffer, 0, read)
            }
        }
        return messageDigest.digest().joinToString("") { "%02x".format(it) }
    }

    fun getThreatLevel(detectionRatio: String): MalwareDetectionService.Severity {
        return try {
            val (detected, total) = detectionRatio.split("/").map { it.toInt() }
            when {
                detected >= total / 2 -> MalwareDetectionService.Severity.CRITICAL
                detected >= total / 4 -> MalwareDetectionService.Severity.HIGH
                detected > 0 -> MalwareDetectionService.Severity.MEDIUM
                else -> MalwareDetectionService.Severity.LOW
            }
        } catch (e: Exception) {
            MalwareDetectionService.Severity.UNKNOWN
        }
    }
}
