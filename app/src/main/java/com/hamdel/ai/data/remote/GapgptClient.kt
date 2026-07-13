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
 * gapgpt.app chat completions – always tried first per the requested model priority
 * (see مستندات gapgpt.txt): gpt-5-nano, then gpt-4o-mini.
 */
class GapgptClient(
    private val httpClient: OkHttpClient,
    private val keysProvider: () -> List<String>
) : ChatCompletionClient {

    override val providerName = "gapgpt"

    override suspend fun complete(systemPrompt: String, userPrompt: String): String? =
        withContext(Dispatchers.IO) {
            val keys = keysProvider()
            for (key in keys.take(MAX_KEY_ATTEMPTS)) {
                for (model in MODEL_PRIORITY) {
                    val result = runCatching { callModel(key, model, systemPrompt, userPrompt) }
                        .onFailure { Log.w(TAG, "gapgpt $model failed: ${it.message}") }
                        .getOrNull()
                    if (result != null) return@withContext result
                }
            }
            null
        }

    private fun callModel(key: String, model: String, systemPrompt: String, userPrompt: String): String? {
        val messages = JSONArray().apply {
            if (systemPrompt.isNotBlank()) {
                put(JSONObject().put("role", "system").put("content", systemPrompt))
            }
            put(JSONObject().put("role", "user").put("content", userPrompt))
        }
        val body = JSONObject()
            .put("model", model)
            .put("messages", messages)
            .toString()
            .toRequestBody(JSON_MEDIA_TYPE)

        val request = Request.Builder()
            .url("$BASE_URL/chat/completions")
            .addHeader("Authorization", "Bearer $key")
            .addHeader("Content-Type", "application/json")
            .post(body)
            .build()

        httpClient.newCall(request).execute().use { response ->
            if (!response.isSuccessful) return null
            val text = response.body?.string() ?: return null
            val json = JSONObject(text)
            val choices = json.optJSONArray("choices") ?: return null
            if (choices.length() == 0) return null
            val message = choices.getJSONObject(0).optJSONObject("message") ?: return null
            return message.optString("content").takeIf { it.isNotBlank() }
        }
    }

    companion object {
        private const val TAG = "GapgptClient"
        private const val BASE_URL = "https://api.gapgpt.app/v1"
        private val JSON_MEDIA_TYPE = "application/json".toMediaType()
        private const val MAX_KEY_ATTEMPTS = 1
        private val MODEL_PRIORITY = listOf("gpt-4o-mini")
    }
}
