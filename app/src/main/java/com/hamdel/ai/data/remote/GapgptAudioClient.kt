package com.hamdel.ai.data.remote

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.MultipartBody
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.asRequestBody
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import java.io.File

/**
 * Speech-to-text / text-to-speech via GapGPT.
 * The Sessions screen uses this for recorded or imported audio files.
 */
class GapgptAudioClient(
    private val httpClient: OkHttpClient,
    private val keysProvider: () -> List<String>
) {
    suspend fun transcribe(audioFile: File): String? = withContext(Dispatchers.IO) {
        for (key in keysProvider()) {
            for (model in listOf("whisper-1", "gapgpt/whisper-1")) {
                val text = runCatching { transcribeWith(key, model, audioFile) }.getOrNull()
                if (text != null) return@withContext text
            }
        }
        null
    }

    private fun transcribeWith(key: String, model: String, audioFile: File): String? {
        val requestBody = MultipartBody.Builder()
            .setType(MultipartBody.FORM)
            .addFormDataPart("model", model)
            .addFormDataPart(
                "file",
                audioFile.name,
                audioFile.asRequestBody("audio/mp4".toMediaType())
            )
            .build()

        val request = Request.Builder()
            .url("$BASE_URL/audio/transcriptions")
            .addHeader("Authorization", "Bearer $key")
            .post(requestBody)
            .build()

        httpClient.newCall(request).execute().use { response ->
            if (!response.isSuccessful) return null
            val text = response.body?.string() ?: return null
            return JSONObject(text).optString("text").takeIf { it.isNotBlank() }
        }
    }

    suspend fun synthesize(text: String, outputFile: File, voice: String = "alloy"): Boolean =
        withContext(Dispatchers.IO) {
            for (key in keysProvider()) {
                for (model in listOf("tts-1", "gpt-4o-mini-tts")) {
                    val ok = runCatching { synthesizeWith(key, model, text, voice, outputFile) }.getOrDefault(false)
                    if (ok) return@withContext true
                }
            }
            false
        }

    private fun synthesizeWith(key: String, model: String, text: String, voice: String, outputFile: File): Boolean {
        val body = JSONObject()
            .put("model", model)
            .put("voice", voice)
            .put("input", text)
            .toString()
            .toRequestBody("application/json".toMediaType())

        val request = Request.Builder()
            .url("$BASE_URL/audio/speech")
            .addHeader("Authorization", "Bearer $key")
            .addHeader("Content-Type", "application/json")
            .post(body)
            .build()

        httpClient.newCall(request).execute().use { response ->
            if (!response.isSuccessful) return false
            val bytes = response.body?.bytes() ?: return false
            outputFile.writeBytes(bytes)
            return true
        }
    }

    companion object {
        private const val BASE_URL = "https://api.gapgpt.app/v1"
    }
}
