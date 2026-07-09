package com.hamdel.ai.data.remote

import android.content.Context
import android.util.Base64
import android.util.Log
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import com.hamdel.ai.BuildConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.security.spec.KeySpec
import javax.crypto.Cipher
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.PBEKeySpec
import javax.crypto.spec.SecretKeySpec

/**
 * Downloads the encrypted keys file (produced by encrypt_keys.py) at app startup,
 * decrypts it locally, and exposes the resulting [AiKeys].
 *
 * File layout expected (matches encrypt_keys.py):
 *   base64( salt[16 bytes] + iv[12 bytes] + AES-GCM(ciphertext + 16-byte tag) )
 * Key derivation: PBKDF2-HMAC-SHA256, 20000 iterations, 32-byte key, from [BuildConfig.KEYS_DECRYPT_PASSWORD].
 *
 * Never logs or stores the raw keys anywhere except the encrypted local cache, and never
 * hardcodes any provider API key in source.
 */
class SecureKeyManager(
    context: Context,
    private val httpClient: OkHttpClient,
    private val keyUrls: List<String>
) {
    private val appContext = context.applicationContext
    private val _keys = MutableStateFlow(AiKeys())
    val keys: StateFlow<AiKeys> = _keys

    private val encryptedPrefs by lazy {
        val masterKey = MasterKey.Builder(appContext)
            .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
            .build()
        EncryptedSharedPreferences.create(
            appContext,
            "hamdel_secure_keys",
            masterKey,
            EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
            EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
        )
    }

    /**
     * Tries every configured URL in order until one downloads and decrypts successfully.
     * Falls back to the last successfully cached copy (encrypted on-disk) if all URLs fail,
     * e.g. when the device is offline.
     */
    suspend fun loadKeys(): AiKeys = withContext(Dispatchers.IO) {
        val password = BuildConfig.KEYS_DECRYPT_PASSWORD
        if (password.isBlank()) {
            Log.w(TAG, "No KEYS_DECRYPT_PASSWORD configured; skipping remote key fetch.")
            return@withContext restoreCached() ?: AiKeys()
        }

        for (url in keyUrls) {
            try {
                val encryptedB64 = downloadText(url) ?: continue
                val plaintext = decrypt(encryptedB64.trim(), password)
                val parsed = AiKeys.parse(plaintext)
                if (!parsed.isEmpty) {
                    cache(plaintext)
                    _keys.value = parsed
                    Log.i(TAG, "Keys loaded from $url (gapgpt=${parsed.gapgptKeys.size}, liara=${parsed.liaraKeys.size})")
                    return@withContext parsed
                }
            } catch (e: Exception) {
                Log.w(TAG, "Failed to load/decrypt keys from $url: ${e.message}")
            }
        }

        Log.w(TAG, "All remote key URLs failed, falling back to local cache.")
        val cached = restoreCached()
        if (cached != null) _keys.value = cached
        cached ?: AiKeys()
    }

    private fun downloadText(url: String): String? {
        val request = Request.Builder().url(url).get().build()
        httpClient.newCall(request).execute().use { response ->
            if (!response.isSuccessful) return null
            return response.body?.string()?.takeIf { it.isNotBlank() }
        }
    }

    private fun decrypt(encryptedB64: String, password: String): String {
        val data = Base64.decode(encryptedB64, Base64.DEFAULT)
        require(data.size > 28) { "Encrypted payload too short" }

        val salt = data.copyOfRange(0, 16)
        val iv = data.copyOfRange(16, 28)
        val cipherTextWithTag = data.copyOfRange(28, data.size)

        val spec: KeySpec = PBEKeySpec(password.toCharArray(), salt, 20000, 256)
        val factory = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256")
        val keyBytes = factory.generateSecret(spec).encoded
        val secretKey = SecretKeySpec(keyBytes, "AES")

        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        cipher.init(Cipher.DECRYPT_MODE, secretKey, GCMParameterSpec(128, iv))
        val plainBytes = cipher.doFinal(cipherTextWithTag)
        return String(plainBytes, Charsets.UTF_8)
    }

    private fun cache(plaintext: String) {
        try {
            encryptedPrefs.edit().putString(CACHE_KEY, plaintext).apply()
        } catch (e: Exception) {
            Log.w(TAG, "Could not cache decrypted keys locally: ${e.message}")
        }
    }

    private fun restoreCached(): AiKeys? {
        return try {
            val plaintext = encryptedPrefs.getString(CACHE_KEY, null) ?: return null
            AiKeys.parse(plaintext).takeIf { !it.isEmpty }
        } catch (e: Exception) {
            Log.w(TAG, "Could not restore cached keys: ${e.message}")
            null
        }
    }

    companion object {
        private const val TAG = "SecureKeyManager"
        private const val CACHE_KEY = "decrypted_keys_blob"
    }
}
