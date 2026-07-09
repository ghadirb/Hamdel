package com.hamdel.ai.data.remote

/** A provider that can turn a prompt into a text completion. Returns null on any failure so
 *  the caller can fall through to the next provider in the priority chain. */
interface ChatCompletionClient {
    val providerName: String
    suspend fun complete(systemPrompt: String, userPrompt: String): String?
}
