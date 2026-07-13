package com.hamdel.ai.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
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
import kotlinx.coroutines.launch
import java.io.File

class RelationshipViewModel(
    private val repository: RelationshipRepository,
    private val audioClient: GapgptAudioClient,
    private val startupMessageClient: StartupMessageClient
) : ViewModel() {
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
