package com.hamdel.ai.domain

import android.util.Log
import com.hamdel.ai.data.model.AiReply
import com.hamdel.ai.data.model.ConversationReport
import com.hamdel.ai.data.model.DashboardState
import com.hamdel.ai.data.model.MessageSimulation
import com.hamdel.ai.data.remote.ChatCompletionClient
import org.json.JSONObject
import java.util.UUID

class RemoteRelationshipAiEngine(
    private val gapgpt: ChatCompletionClient,
    private val liara: ChatCompletionClient,
    private val localFallback: RelationshipAiEngine = DemoRelationshipAiEngine()
) : RelationshipAiEngine {

    override suspend fun analyzeConversation(title: String, text: String): ConversationReport {
        val system = """
            تو یک تحلیلگر حرفه‌ای روابط عاطفی فارسی‌زبان هستی.
            فقط یک JSON معتبر برگردان و هیچ متن اضافه یا Markdown ننویس.
            ساختار دقیق:
            {
              "summary": "خلاصه فارسی کوتاه و کاربردی",
              "respect": 0-100,
              "empathy": 0-100,
              "honesty": 0-100,
              "sarcasmRisk": 0-100,
              "controlRisk": 0-100,
              "emotionalSupport": 0-100
            }
            تحلیل نباید حکم قطعی بدهد؛ فقط نشانه‌ها، ریسک‌ها و پیشنهادهای محتاطانه را گزارش کند.
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
            تو دستیار هوشمند رابطه و ازدواج هستی.
            فقط یک JSON معتبر برگردان و هیچ متن اضافه یا Markdown ننویس.
            ساختار دقیق:
            {
              "answer": "پاسخ فارسی، عملی، محترمانه و شخصی‌سازی‌شده",
              "confidence": 0.0-1.0,
              "reasons": ["دلیل اول", "دلیل دوم"]
            }
            برای تصمیم‌های مهم مثل ازدواج، جدایی یا بحران روانی حکم قطعی نده.
            در صورت مشاهده خطر، فقط هشدار، دلیل و پیشنهاد مراجعه به متخصص انسانی بده.
        """.trimIndent()
        val metricsSummary = context.metrics.joinToString("، ") { "${it.title}: ${it.value}" }
        val reportsSummary = context.reports.take(3).joinToString("\n") {
            "- ${it.sourceTitle}: احترام ${it.respect}، همدلی ${it.empathy}، کنترل‌گری ${it.controlRisk}"
        }
        val profilesSummary = context.profiles.joinToString("\n") {
            "- ${it.name}: سبک ارتباط ${it.communicationStyle}، زبان عشق ${it.loveLanguage}، ارزش‌ها ${it.values}"
        }
        val user = """
            سوال کاربر: $question

            شاخص‌های فعلی رابطه:
            $metricsSummary

            پروفایل‌ها:
            $profilesSummary

            آخرین تحلیل‌ها:
            $reportsSummary
        """.trimIndent()

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
            تو شبیه‌ساز اثر پیام در رابطه هستی.
            فقط یک JSON معتبر برگردان و هیچ متن اضافه یا Markdown ننویس.
            ساختار دقیق:
            {
              "conflictRisk": 0-100,
              "misunderstandingRisk": 0-100,
              "hurtRisk": 0-100,
              "improvedMessage": "نسخه بهتر پیام به فارسی",
              "notes": ["نکته اول", "نکته دوم"]
            }
            هدف کاهش سوءتفاهم، سرزنش، طعنه و فشار روانی است.
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

    private suspend fun completeAsJson(system: String, user: String): JSONObject? {
        val raw = gapgpt.complete(system, user) ?: liara.complete(system, user)
        if (raw == null) {
            Log.w(TAG, "Both gapgpt and Liara failed; using local fallback.")
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
