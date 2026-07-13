package com.hamdel.ai.data.remote

import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject

/**
 * Liara AI Gateway chat completions (مستندات لیارا.txt) – used only as a fallback when every
 * gapgpt key/model attempt fails.
 */
class LiaraClient(
    private val httpClient: OkHttpClient,
    private val keysProvider: () -> List<String>,
    private val baseUrlProvider: () -> String?
) : ChatCompletionClient {

    override val providerName = "liara"

    override suspend fun complete(systemPrompt: String, userPrompt: String): String? =
        withContext(Dispatchers.IO) {
            val keys = keysProvider()
            val baseUrl = (baseUrlProvider() ?: DEFAULT_BASE_URL).trimEnd('/')
            for (key in keys.take(MAX_KEY_ATTEMPTS)) {
                for (model in MODEL_PRIORITY) {
                    val result = runCatching { callModel(baseUrl, key, model, systemPrompt, userPrompt) }
                        .onFailure { Log.w(TAG, "liara $model failed: ${it.message}") }
                        .getOrNull()
                    if (result != null) return@withContext result
                }
            }
            null
        }

    private fun callModel(baseUrl: String, key: String, model: String, systemPrompt: String, userPrompt: String): String? {
        val messages = JSONArray().apply {
            if (systemPrompt.isNotBlank()) {
                put(JSONObject().put("role", "system").put("content", systemPrompt))
            }
            put(JSONObject().put("role", "user").put("content", userPrompt))
        }
        val body = JSONObject()
            .put("model", model)
            .put("messages", messages)
            .put("max_tokens", 700)
            .put("temperature", 0.3)
            .toString()
            .toRequestBody(JSON_MEDIA_TYPE)

        val request = Request.Builder()
            .url("$baseUrl/chat/completions")
            .addHeader("Authorization", "Bearer $key")
            .addHeader("Content-Type", "application/json")
            .post(body)
            .build()

        httpClient.newCall(request).execute().use { response ->
            if (!response.isSuccessful) {
                Log.w(TAG, "liara $model returned HTTP ${response.code}")
                return null
            }
            val text = response.body?.string() ?: return null
            val json = JSONObject(text)
            val choices = json.optJSONArray("choices") ?: return null
            if (choices.length() == 0) return null
            val message = choices.getJSONObject(0).optJSONObject("message") ?: return null
            return message.optString("content").takeIf { it.isNotBlank() }
        }
    }

    companion object {
        private const val TAG = "LiaraClient"
        // Default project base URL from مستندات لیارا.txt; overridden if the keys file
        // ships a LIARA_BASE_URL entry (e.g. after rotating to a different Liara project).
        private const val DEFAULT_BASE_URL = "https://ai.liara.ir/api/69467b6ba99a2016cac892e1/v1"
        private val JSON_MEDIA_TYPE = "application/json".toMediaType()
        private const val MAX_KEY_ATTEMPTS = 1
        // Verified against this project's live /models endpoint. The older Gemini 2.0 id
        // documented in the original notes is no longer accepted by this Liara project.
        private val MODEL_PRIORITY = listOf("openai/gpt-4o-mini")
    }
}
