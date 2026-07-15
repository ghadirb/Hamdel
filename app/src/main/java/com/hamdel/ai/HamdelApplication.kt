package com.hamdel.ai

import android.app.Application
import com.hamdel.ai.data.local.HamdelDatabase
import com.hamdel.ai.data.remote.GapgptAudioClient
import com.hamdel.ai.data.remote.GapgptClient
import com.hamdel.ai.data.remote.LiaraClient
import com.hamdel.ai.data.remote.PurchaseVerificationClient
import com.hamdel.ai.data.remote.SecureKeyManager
import com.hamdel.ai.data.remote.StartupMessageClient
import com.hamdel.ai.data.repository.RelationshipRepository
import com.hamdel.ai.data.sms.ContactSmsImporter
import com.hamdel.ai.domain.RemoteRelationshipAiEngine
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import okhttp3.OkHttpClient
import java.util.concurrent.TimeUnit

class HamdelApplication : Application() {

    /** App-wide coroutine scope for startup work (key fetch) that must survive individual screens. */
    private val applicationScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    private val httpClient by lazy {
        OkHttpClient.Builder()
            .connectTimeout(10, TimeUnit.SECONDS)
            .readTimeout(75, TimeUnit.SECONDS)
            .writeTimeout(75, TimeUnit.SECONDS)
            .callTimeout(90, TimeUnit.SECONDS)
            .build()
    }

    /**
     * Encrypted keys are downloaded from the primary URL first; if that fails (network error,
     * link moved, etc.) the backup URL is used automatically.
     */
    private val keyManager by lazy {
        SecureKeyManager(
            context = this,
            httpClient = httpClient,
            keyUrls = listOf(
                "https://abrehamrahi.ir/o/public/eUFcsXOX",
                "https://gist.githubusercontent.com/ghadirb/626a804df3009e49045a2948dad89fe5/raw/c93c06d1b2f38c65ee30f092c134a89998326d12/keys.txt"
            )
        )
    }

    private val gapgptClient by lazy {
        GapgptClient(httpClient) { keyManager.keys.value.gapgptKeys }
    }

    private val liaraClient by lazy {
        LiaraClient(
            httpClient = httpClient,
            keysProvider = { keyManager.keys.value.liaraKeys },
            baseUrlProvider = { keyManager.keys.value.liaraBaseUrl }
        )
    }

    /** Ready for the Sessions screen's speech-to-text / text-to-speech workflow. */
    val audioClient by lazy {
        GapgptAudioClient(httpClient) { keyManager.keys.value.gapgptKeys }
    }

    val startupMessageClient by lazy {
        StartupMessageClient(
            httpClient = httpClient,
            url = "https://abrehamrahi.ir/o/public/NdnIkby5/"
        )
    }

    val contactSmsImporter by lazy { ContactSmsImporter(this) }

    val purchaseVerificationClient by lazy { PurchaseVerificationClient(httpClient) }

    val database by lazy { HamdelDatabase.getDatabase(this) }

    val repository by lazy {
        RelationshipRepository(
            database.relationshipDao(),
            RemoteRelationshipAiEngine(gapgpt = gapgptClient, liara = liaraClient)
        )
    }

    override fun onCreate() {
        super.onCreate()
        // Fetch + decrypt the remote keys as early as possible so gapgpt/Liara are ready
        // by the time the user reaches the assistant/analysis screens. If this fails
        // (offline, both links down), the repository keeps working via the local demo engine.
        applicationScope.launch {
            keyManager.loadKeys()
        }
    }
}
