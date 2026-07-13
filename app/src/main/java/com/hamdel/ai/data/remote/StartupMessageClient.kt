package com.hamdel.ai.data.remote

import com.hamdel.ai.data.model.StartupMessage
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject

class StartupMessageClient(
    private val httpClient: OkHttpClient,
    private val url: String
) {
    suspend fun fetch(): StartupMessage = withContext(Dispatchers.IO) {
        runCatching {
            val request = Request.Builder().url(url).get().build()
            httpClient.newCall(request).execute().use { response ->
                if (!response.isSuccessful) return@runCatching StartupMessage()
                val body = response.body?.string().orEmpty()
                if (body.isBlank()) return@runCatching StartupMessage()
                parse(JSONObject(body))
            }
        }.getOrDefault(StartupMessage())
    }

    private fun parse(json: JSONObject): StartupMessage {
        return StartupMessage(
            version = json.optInt("version", 1),
            title = json.optString("title", StartupMessage().title),
            message = json.optString("message", StartupMessage().message),
            primaryAction = json.optString("primaryAction", StartupMessage().primaryAction),
            secondaryAction = json.optString("secondaryAction").takeIf { it.isNotBlank() },
            showEveryLaunch = json.optBoolean("showEveryLaunch", true)
        )
    }
}
