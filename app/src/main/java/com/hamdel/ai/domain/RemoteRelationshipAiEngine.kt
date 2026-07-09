package com.hamdel.ai.domain

import android.util.Log
import com.hamdel.ai.data.model.AiReply
import com.hamdel.ai.data.model.ConversationReport
import com.hamdel.ai.data.model.DashboardState
import com.hamdel.ai.data.model.MessageSimulation
import com.hamdel.ai.data.remote.ChatCompletionClient
import org.json.JSONObject
import java.util.UUID

/**
 * Real AI engine: tries gapgpt first, falls back to Liara, and finally falls back to the local
 * [DemoRelationshipAiEngine] if both remote providers are unreachable (no keys yet, offline, etc.)
 * so the app always stays usable.
 */
class RemoteRelationshipAiEngine(
    private val gapgpt: ChatCompletionClient,
    private val liara: ChatCompletionClient,
    private val localFallback: RelationshipAiEngine = DemoRelationshipAiEngine()
) : RelationshipAiEngine {

    override suspend fun analyzeConversation(title: String, text: String): ConversationReport {
        val system = """
            تو یک تحلیلگر روابط عاطفی فارسی‌زبان هستی. فقط یک JSON معتبر با این فیلدها برگردان،
            بدون هیچ متن اضافه یا Markdown:
            {"summary": "string فارسی", "respect": 0-100, "empathy": 0-100, "honesty": 0-100,
             "sarcasmRisk": 0-100, "controlRisk": 0-100, "emotionalSupport": 0-100}
        """.trimIndent()
        val user = "عنوان: ${title.ifBlank { "گفتگوی دستی" }}\nمتن گفتگو:\n$text"

        val json = completeAsJson(system, user)
        if (json != null) {
            return runCatching {
                ConversationReport(
                    id = UUID.randomUUID().toString(),
                    sourceTitle = title.ifBlank { "گفتگوی دستی" },
                    summary = json.optString("summary").ifBlank { "تحلیل انجام شد." },
                    respect = json.optInt("respect", 70).coerceIn(0, 100),
                    empathy = json.optInt("empathy", 70).coerceIn(0, 100),
                    honesty = json.optInt("honesty", 70).coerceIn(0, 100),
                    sarcasmRisk = json.optInt("sarcasmRisk", 20).coerceIn(0, 100),
                    controlRisk = json.optInt("controlRisk", 20).coerceIn(0, 100),
                    emotionalSupport = json.optInt("emotionalSupport", 60).coerceIn(0, 100),
                    createdAt = System.currentTimeMillis()
                )
            }.getOrNull() ?: localFallback.analyzeConversation(title, text)
        }
        return localFallback.analyzeConversation(title, text)
    }

    override suspend fun askAssistant(question: String, context: DashboardState): AiReply {
        val system = """
            تو دستیار هوشمند رابطه هستی. فقط یک JSON معتبر برگردان، بدون متن اضافه:
            {"answer": "پاسخ فارسی و کاربردی", "confidence": 0.0-1.0, "reasons": ["...", "..."]}
            یادآوری: برای تصمیم‌های بزرگ (مثل ازدواج)، این فقط یک راهنماست، نه جایگزین مشاور انسانی.
        """.trimIndent()
        val metricsSummary = context.metrics.joinToString("، ") { "${it.title}: ${it.value}" }
        val user = "سوال کاربر: $question\nوضعیت فعلی رابطه: $metricsSummary"

        val json = completeAsJson(system, user)
        if (json != null) {
            return runCatching {
                AiReply(
                    answer = json.optString("answer").ifBlank { "پاسخی دریافت نشد." },
                    confidence = json.optDouble("confidence", 0.7).toFloat().coerceIn(0f, 1f),
                    reasons = json.optJSONArray("reasons")?.let { arr ->
                        (0 until arr.length()).map { arr.optString(it) }.filter { it.isNotBlank() }
                    } ?: emptyList()
                )
            }.getOrNull() ?: localFallback.askAssistant(question, context)
        }
        return localFallback.askAssistant(question, context)
    }

    override suspend fun simulateMessage(message: String): MessageSimulation {
        val system = """
            تو شبیه‌ساز اثر پیام در رابطه هستی. فقط یک JSON معتبر برگردان، بدون متن اضافه:
            {"conflictRisk": 0-100, "misunderstandingRisk": 0-100, "hurtRisk": 0-100,
             "improvedMessage": "نسخه بهتر پیام به فارسی", "notes": ["...", "..."]}
        """.trimIndent()

        val json = completeAsJson(system, message)
        if (json != null) {
            return runCatching {
                MessageSimulation(
                    conflictRisk = json.optInt("conflictRisk", 30).coerceIn(0, 100),
                    misunderstandingRisk = json.optInt("misunderstandingRisk", 30).coerceIn(0, 100),
                    hurtRisk = json.optInt("hurtRisk", 25).coerceIn(0, 100),
                    improvedMessage = json.optString("improvedMessage").ifBlank { message },
                    notes = json.optJSONArray("notes")?.let { arr ->
                        (0 until arr.length()).map { arr.optString(it) }.filter { it.isNotBlank() }
                    } ?: emptyList()
                )
            }.getOrNull() ?: localFallback.simulateMessage(message)
        }
        return localFallback.simulateMessage(message)
    }

    /** gapgpt first, Liara second, matching the required provider priority. */
    private suspend fun completeAsJson(system: String, user: String): JSONObject? {
        val raw = gapgpt.complete(system, user) ?: liara.complete(system, user)
        if (raw == null) {
            Log.w(TAG, "Both gapgpt and Liara failed; using local demo engine.")
            return null
        }
        return extractJson(raw)
    }

    private fun extractJson(raw: String): JSONObject? {
        val start = raw.indexOf('{')
        val end = raw.lastIndexOf('}')
        if (start == -1 || end == -1 || end < start) return null
        return runCatching { JSONObject(raw.substring(start, end + 1)) }.getOrNull()
    }

    companion object {
        private const val TAG = "RemoteRelationshipAi"
    }
}
