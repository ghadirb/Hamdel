package com.hamdel.ai.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.hamdel.ai.data.model.AiReply
import com.hamdel.ai.data.model.DashboardState
import com.hamdel.ai.data.model.MessageSimulation
import com.hamdel.ai.data.repository.RelationshipRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class RelationshipViewModel(
    private val repository: RelationshipRepository
) : ViewModel() {
    val dashboard: StateFlow<DashboardState> = repository.dashboard.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = DashboardState()
    )

    val assistantReply = MutableStateFlow<AiReply?>(null)
    val simulation = MutableStateFlow<MessageSimulation?>(null)

    init {
        viewModelScope.launch {
            repository.seedIfNeeded()
        }
    }

    fun analyzeConversation(title: String, text: String) {
        viewModelScope.launch {
            repository.analyzeConversation(title, text)
        }
    }

    fun askAssistant(question: String) {
        viewModelScope.launch {
            assistantReply.value = repository.askAssistant(question, dashboard.value)
        }
    }

    fun simulateMessage(message: String) {
        viewModelScope.launch {
            simulation.value = repository.simulateMessage(message)
        }
    }
}
