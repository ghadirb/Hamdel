package com.hamdel.ai.domain

import com.hamdel.ai.data.model.AiReply
import com.hamdel.ai.data.model.ConversationReport
import com.hamdel.ai.data.model.DashboardState
import com.hamdel.ai.data.model.MessageSimulation

/** Contract implemented by the online relationship AI engine. */
interface RelationshipAiEngine {
    suspend fun analyzeConversation(title: String, text: String): ConversationReport
    suspend fun askAssistant(question: String, context: DashboardState): AiReply
    suspend fun simulateMessage(message: String): MessageSimulation
}
