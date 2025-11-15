package com.aktarjabed.nextgenzip.ai

import com.aktarjabed.nextgenzip.data.CloudUsageStore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import org.json.JSONArray
import java.io.IOException
import java.util.concurrent.TimeUnit
import kotlin.system.measureTimeMillis

object CloudAIClient {
    private val client = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(35, TimeUnit.SECONDS)
        .writeTimeout(30, TimeUnit.SECONDS)
        .build()

    private val JSON = "application/json; charset=utf-8".toMediaType()

    /**
     * Generates text from provider. Records latency and errors to CloudUsageStore (no sensitive data).
     */
    suspend fun generate(
        providerId: String,
        apiKey: String,
        prompt: String,
        customEndpoint: String? = null
    ): String = withContext(Dispatchers.IO) {
        var result = ""
        try {
            val elapsed = measureTimeMillis {
                result = when (providerId.lowercase()) {
                    "openai" -> callOpenAI(apiKey, prompt)
                    "gemini" -> callGemini(apiKey, prompt) // placeholder
                    "deepseek" -> callDeepSeek(apiKey, prompt) // placeholder
                    "custom" -> callCustom(customEndpoint, apiKey, prompt)
                    else -> throw IllegalArgumentException("Provider not supported: $providerId")
                }
            }
            // record success
            CloudUsageStore.recordSuccess(elapsed)
            return@withContext result
        } catch (e: Exception) {
            // record error (message only)
            CloudUsageStore.recordError(e.localizedMessage)
            throw e
        }
    }

    private fun callOpenAI(apiKey: String, prompt: String): String {
        val url = "https://api.openai.com/v1/chat/completions"
        val json = JSONObject().apply {
            put("model", "gpt-4o-mini")
            val messages = JSONArray()
            messages.put(JSONObject().put("role", "user").put("content", prompt))
            put("messages", messages)
            put("temperature", 0.6)
            put("max_tokens", 512)
        }
        val body = json.toString().toRequestBody(JSON)
        val request = Request.Builder()
            .url(url)
            .header("Authorization", "Bearer $apiKey")
            .post(body)
            .build()

        client.newCall(request).execute().use { resp ->
            val text = resp.body?.string() ?: ""
            if (!resp.isSuccessful) {
                throw IOException("OpenAI error ${resp.code}: ${text.take(200)}")
            }
            val obj = JSONObject(text)
            val choices = obj.optJSONArray("choices")
            if (choices != null && choices.length() > 0) {
                val first = choices.getJSONObject(0)
                val message = first.optJSONObject("message")
                if (message != null) return message.optString("content", "").trim()
                return first.optString("text", "").trim()
            }
            return ""
        }
    }

    // Placeholder for Gemini. Replace with real endpoint/shape when available.
    private fun callGemini(apiKey: String, prompt: String): String {
        // GEMS: add real Gemini endpoint & JSON shape when available.
        throw UnsupportedOperationException("Gemini provider not implemented yet.")
    }

    // Placeholder for DeepSeek provider (custom shape)
    private fun callDeepSeek(apiKey: String, prompt: String): String {
        // Implement API shape for DeepSeek here if needed.
        throw UnsupportedOperationException("DeepSeek provider not implemented yet.")
    }

    private fun callCustom(endpoint: String?, apiKey: String, prompt: String): String {
        require(!endpoint.isNullOrBlank()) { "Custom endpoint required for provider 'custom'" }
        val json = JSONObject().put("prompt", prompt).toString()
        val body = json.toRequestBody(JSON)
        val request = Request.Builder()
            .url(endpoint)
            .header("Authorization", "Bearer $apiKey")
            .post(body)
            .build()

        client.newCall(request).execute().use { resp ->
            val text = resp.body?.string() ?: ""
            if (!resp.isSuccessful) {
                throw IOException("Custom provider error ${resp.code}: ${text.take(200)}")
            }
            return try {
                val obj = JSONObject(text)
                obj.optString("text", text).trim()
            } catch (e: Exception) {
                text.trim()
            }
        }
    }
}
