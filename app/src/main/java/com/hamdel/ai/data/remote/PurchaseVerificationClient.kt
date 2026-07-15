package com.hamdel.ai.data.remote

import android.util.Log
import com.hamdel.ai.BuildConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject

/**
 * Talks to the Hamdel purchase-verification backend (see server/apps-script/Code.gs — a free
 * Google Apps Script Web App; server/index.js is a drop-in Node alternative for later if the
 * install count outgrows Apps Script's PropertiesService quota). The store SDK
 * (myket-billing-client) only tells us what the *device* thinks it purchased; this asks a
 * server we control to check the purchase token against Bazaar/Myket directly, which is the
 * verification step both stores' security docs recommend.
 *
 * Apps Script Web Apps expose a single URL and can't read custom HTTP headers, so both the
 * shared secret and the requested action travel inside the JSON POST body instead of headers.
 *
 * If HAMDEL_SERVER_BASE_URL isn't configured (local/dev builds without server.properties),
 * every call simply returns null and callers fall back to client-only entitlement checks.
 */
class PurchaseVerificationClient(private val httpClient: OkHttpClient) {

    data class Entitlement(val plan: String, val expiresAt: Long?)

    private val baseUrl: String get() = BuildConfig.HAMDEL_SERVER_BASE_URL
    private val serverKey: String get() = BuildConfig.HAMDEL_SERVER_API_KEY
    val isConfigured: Boolean get() = baseUrl.isNotBlank() && serverKey.isNotBlank()

    /** Call right after a store purchase completes. Returns the verified entitlement, or null on any failure. */
    suspend fun verifyPurchase(
        installId: String,
        store: String,
        sku: String,
        purchaseToken: String
    ): Entitlement? = withContext(Dispatchers.IO) {
        if (!isConfigured) return@withContext null
        val body = JSONObject()
            .put("action", "verify")
            .put("serverKey", serverKey)
            .put("installId", installId)
            .put("store", store)
            .put("sku", sku)
            .put("purchaseToken", purchaseToken)
        postForEntitlement(body) { it.optBoolean("valid", false) }
    }

    /** Call on app launch to refresh entitlement from the server (authoritative source, especially for Myket). */
    suspend fun fetchEntitlement(installId: String): Entitlement? = withContext(Dispatchers.IO) {
        if (!isConfigured) return@withContext null
        val body = JSONObject()
            .put("action", "entitlement")
            .put("serverKey", serverKey)
            .put("installId", installId)
        postForEntitlement(body) { it.optString("plan", "free") != "free" }
    }

    private fun postForEntitlement(body: JSONObject, isEntitled: (JSONObject) -> Boolean): Entitlement? =
        runCatching {
            val request = Request.Builder()
                .url(baseUrl)
                .post(body.toString().toRequestBody(JSON_MEDIA_TYPE))
                .build()
            httpClient.newCall(request).execute().use { response ->
                val text = response.body?.string() ?: return@use null
                val json = JSONObject(text)
                if (!response.isSuccessful || !isEntitled(json)) return@use null
                Entitlement(
                    plan = json.optString("plan", "free"),
                    expiresAt = json.optLong("expiresAt", 0L).takeIf { it > 0 }
                )
            }
        }.onFailure { Log.w(TAG, "purchase verification call failed: ${it.message}") }.getOrNull()

    companion object {
        private const val TAG = "PurchaseVerification"
        private val JSON_MEDIA_TYPE = "application/json".toMediaType()
    }
}
