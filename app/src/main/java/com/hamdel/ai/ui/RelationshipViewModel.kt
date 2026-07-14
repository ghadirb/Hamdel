package com.hamdel.ai.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import android.content.Context
import android.net.Uri
import com.hamdel.ai.data.backup.AutoBackupWorker
import com.hamdel.ai.data.backup.BackupManager
import com.hamdel.ai.data.settings.HamdelPreferences
import com.hamdel.ai.data.sms.ContactMessageSyncWorker
import com.hamdel.ai.data.sms.ContactSmsImporter
import com.hamdel.ai.data.model.AiReply
import com.hamdel.ai.data.model.DashboardState
import com.hamdel.ai.data.model.MessageSimulation
import com.hamdel.ai.data.model.PersonProfile
import com.hamdel.ai.data.model.StartupMessage
import com.hamdel.ai.data.remote.GapgptAudioClient
import com.hamdel.ai.data.remote.StartupMessageClient
import com.hamdel.ai.data.repository.RelationshipRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

class RelationshipViewModel(
    private val repository: RelationshipRepository,
    private val audioClient: GapgptAudioClient,
    private val startupMessageClient: StartupMessageClient,
    private val appContext: Context,
    private val contactSmsImporter: ContactSmsImporter
) : ViewModel() {
    private val preferences = HamdelPreferences(appContext)
    val dashboard: StateFlow<DashboardState> = repository.dashboard.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = DashboardState()
    )

    val assistantReply = MutableStateFlow<AiReply?>(null)
    val simulation = MutableStateFlow<MessageSimulation?>(null)
    val statusMessage = MutableStateFlow<String?>(null)
    val transcribedText = MutableStateFlow<String?>(null)
    val isBusy = MutableStateFlow(false)
    val isAssistantBusy = MutableStateFlow(false)
    val isSimulationBusy = MutableStateFlow(false)
    val startupMessage = MutableStateFlow<StartupMessage?>(null)
    val autoBackupEnabled = MutableStateFlow(preferences.autoBackupEnabled)
    val messageSyncEnabled = MutableStateFlow(preferences.messageSyncEnabled)
    val monitoredContactName = MutableStateFlow(preferences.monitoredContactName)

    init {
        viewModelScope.launch {
            repository.clearLegacyDemoData()
        }
        viewModelScope.launch {
            startupMessage.value = startupMessageClient.fetch()
        }
    }

    fun dismissStartupMessage() {
        startupMessage.value = null
    }

    fun setStatus(message: String?) {
        statusMessage.value = message
    }

    fun analyzeConversation(title: String, text: String) {
        viewModelScope.launch {
            if (!hasMutualConsent()) {
                statusMessage.value = "برای تحلیل رابطه، رضایت هر دو نفر باید فعال باشد."
                return@launch
            }
            isBusy.value = true
            runCatching { repository.analyzeConversation(title, text) }
                .onSuccess { statusMessage.value = "تحلیل آنلاین ذخیره شد و داشبورد به‌روزرسانی شد." }
                .onFailure { statusMessage.value = "تحلیل انجام نشد: ${it.message ?: "خطای نامشخص"}" }
            isBusy.value = false
        }
    }

    fun transcribeAndAnalyze(title: String, audioFile: File) {
        viewModelScope.launch {
            if (!hasMutualConsent()) {
                statusMessage.value = "برای تحلیل جلسه، رضایت هر دو نفر باید فعال باشد."
                return@launch
            }
            isBusy.value = true
            val text = runCatching { audioClient.transcribe(audioFile) }.getOrNull()
            if (text.isNullOrBlank()) {
                statusMessage.value = "رونویسی صدا انجام نشد. اتصال اینترنت را بررسی و دوباره تلاش کنید."
            } else {
                transcribedText.value = text
                repository.analyzeConversation(title, text)
                statusMessage.value = "صدا با سرویس آنلاین رونویسی و تحلیل شد و در حافظه رابطه ثبت شد."
            }
            isBusy.value = false
        }
    }

    fun saveProfile(profile: PersonProfile) {
        viewModelScope.launch {
            repository.saveProfile(profile)
            statusMessage.value = "پروفایل ذخیره شد."
        }
    }

    fun backupTo(uri: Uri) {
        viewModelScope.launch {
            isBusy.value = true
            val success = runCatching { withContext(Dispatchers.IO) { BackupManager.backupToUri(appContext, uri) } }.getOrDefault(false)
            if (success) {
                preferences.backupUri = uri.toString()
                statusMessage.value = "پشتیبان کامل ذخیره شد."
            } else {
                statusMessage.value = "تهیه پشتیبان ناموفق بود."
            }
            isBusy.value = false
        }
    }

    fun restoreFrom(uri: Uri) {
        viewModelScope.launch {
            isBusy.value = true
            val success = runCatching { withContext(Dispatchers.IO) { BackupManager.restoreFromUri(appContext, uri) } }.getOrDefault(false)
            statusMessage.value = if (success) "بازیابی انجام شد؛ برنامه را یک‌بار ببندید و دوباره باز کنید." else "بازیابی ناموفق بود."
            isBusy.value = false
        }
    }

    fun setAutoBackup(enabled: Boolean) {
        if (enabled && preferences.backupUri == null) {
            statusMessage.value = "ابتدا یک پشتیبان دستی و مقصد ذخیره‌سازی انتخاب کنید."
            return
        }
        preferences.autoBackupEnabled = enabled
        autoBackupEnabled.value = enabled
        if (enabled) AutoBackupWorker.schedule(appContext) else AutoBackupWorker.cancel(appContext)
    }

    fun configureContactMessageSync(exactName: String, enabled: Boolean) {
        preferences.monitoredContactName = exactName
        preferences.messageSyncEnabled = enabled && exactName.isNotBlank()
        monitoredContactName.value = preferences.monitoredContactName
        messageSyncEnabled.value = preferences.messageSyncEnabled
        if (preferences.messageSyncEnabled) ContactMessageSyncWorker.schedule(appContext) else ContactMessageSyncWorker.cancel(appContext)
    }

    fun importContactMessages() {
        viewModelScope.launch {
            if (!hasMutualConsent()) {
                statusMessage.value = "برای افزودن پیام‌ها به حافظه رابطه، رضایت هر دو نفر باید فعال باشد."
                return@launch
            }
            isBusy.value = true
            runCatching { withContext(Dispatchers.IO) { contactSmsImporter.importForConfiguredContact() } }
                .onSuccess {
                    repository.saveContactMessages(it)
                    statusMessage.value = if (it.isEmpty()) "پیامی برای نام دقیق واردشده پیدا نشد." else "${it.size} پیام به حافظه رابطه افزوده شد."
                }
                .onFailure { statusMessage.value = "خواندن پیام‌ها ناموفق بود. مجوز پیامک و مخاطبان را بررسی کنید." }
            isBusy.value = false
        }
    }

    fun askAssistant(question: String) {
        viewModelScope.launch {
            if (!hasMutualConsent()) {
                statusMessage.value = "برای استفاده دستیار از حافظه رابطه، رضایت هر دو نفر باید فعال باشد."
                return@launch
            }
            isAssistantBusy.value = true
            runCatching { repository.askAssistant(question, dashboard.value) }
                .onSuccess { assistantReply.value = it }
                .onFailure { statusMessage.value = "پاسخ دستیار دریافت نشد. اتصال اینترنت را بررسی کنید." }
            isAssistantBusy.value = false
        }
    }

    fun simulateMessage(message: String) {
        viewModelScope.launch {
            isSimulationBusy.value = true
            runCatching { repository.simulateMessage(message) }
                .onSuccess { simulation.value = it }
                .onFailure { statusMessage.value = "شبیه‌سازی انجام نشد. اتصال اینترنت را بررسی کنید." }
            isSimulationBusy.value = false
        }
    }

    private fun hasMutualConsent(): Boolean {
        val profiles = dashboard.value.profiles
        return profiles.size >= 2 && profiles.all { it.consentGranted }
    }
}
