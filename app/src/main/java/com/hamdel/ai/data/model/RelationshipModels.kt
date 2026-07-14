package com.hamdel.ai.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "profiles")
data class PersonProfile(
    @PrimaryKey val id: String,
    val name: String,
    val age: Int,
    val education: String,
    val job: String,
    val city: String,
    val lifeGoals: String,
    val interests: String,
    val values: String,
    val beliefs: String,
    val communicationStyle: String,
    val loveLanguage: String,
    val personalityType: String,
    val traits: String,
    val dailyHabits: String,
    val consentGranted: Boolean
)

@Entity(tableName = "relationship_metrics")
data class RelationshipMetric(
    @PrimaryKey val id: String,
    val title: String,
    val value: Int,
    val trend: Int,
    val description: String
)

@Entity(tableName = "relationship_events")
data class RelationshipEvent(
    @PrimaryKey val id: String,
    val title: String,
    val description: String,
    val category: String,
    val timestamp: Long
)

@Entity(tableName = "conversation_reports")
data class ConversationReport(
    @PrimaryKey val id: String,
    val sourceTitle: String,
    val summary: String,
    val respect: Int,
    val empathy: Int,
    val honesty: Int,
    val sarcasmRisk: Int,
    val controlRisk: Int,
    val emotionalSupport: Int,
    val createdAt: Long,
    val transcript: String = ""
)

@Entity(tableName = "contact_messages")
data class ContactMessage(
    @PrimaryKey val id: String,
    val contactName: String,
    val address: String,
    val body: String,
    val timestamp: Long,
    val direction: String
)

data class DashboardState(
    val metrics: List<RelationshipMetric> = emptyList(),
    val warnings: List<String> = emptyList(),
    val suggestions: List<String> = emptyList(),
    val events: List<RelationshipEvent> = emptyList(),
    val reports: List<ConversationReport> = emptyList(),
    val profiles: List<PersonProfile> = emptyList(),
    val contactMessages: List<ContactMessage> = emptyList()
)

data class AiReply(
    val answer: String,
    val confidence: Float,
    val reasons: List<String>
)

data class MessageSimulation(
    val conflictRisk: Int,
    val misunderstandingRisk: Int,
    val hurtRisk: Int,
    val improvedMessage: String,
    val notes: List<String>
)
