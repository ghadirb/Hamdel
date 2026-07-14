package com.hamdel.ai.domain

import android.util.Log
import com.hamdel.ai.data.model.AiReply
import com.hamdel.ai.data.model.ConversationReport
import com.hamdel.ai.data.model.DashboardState
import com.hamdel.ai.data.model.MessageSimulation
import com.hamdel.ai.data.model.ProfileSuggestionDraft
import com.hamdel.ai.data.remote.ChatCompletionClient
import org.json.JSONObject
import java.io.IOException
import java.util.UUID

class RemoteRelationshipAiEngine(
    private val gapgpt: ChatCompletionClient,
    private val liara: ChatCompletionClient
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
        val user = "عنوان: ${title.ifBlank { "گفتگوی دستی" }}\nمتن گفتگو:\n${text.take(MAX_CONVERSATION_CHARS)}"

        val json = completeAsJson(system, user)
        return ConversationReport(
            id = UUID.randomUUID().toString(),
            sourceTitle = title.ifBlank { "گفتگوی دستی" },
            summary = json.optString("summary").ifBlank { "تحلیل آنلاین انجام شد." },
            respect = json.optInt("respect", 70).coerceIn(0, 100),
            empathy = json.optInt("empathy", 70).coerceIn(0, 100),
            honesty = json.optInt("honesty", 70).coerceIn(0, 100),
            sarcasmRisk = json.optInt("sarcasmRisk", 20).coerceIn(0, 100),
            controlRisk = json.optInt("controlRisk", 20).coerceIn(0, 100),
            emotionalSupport = json.optInt("emotionalSupport", 60).coerceIn(0, 100),
            createdAt = System.currentTimeMillis(),
            transcript = text
        )
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
            .ifBlank { "هنوز شاخصی ثبت نشده است." }
        val reportsSummary = context.reports.take(3).joinToString("\n") {
            "- ${it.sourceTitle}: احترام ${it.respect}، همدلی ${it.empathy}، کنترل‌گری ${it.controlRisk}، خلاصه: ${it.summary.take(500)}\nبخش مرتبط متن: ${it.transcript.take(MEMORY_TRANSCRIPT_CHARS)}"
        }
        val profilesSummary = context.profiles.joinToString("\n") {
            "- ${it.name}: سبک ارتباط ${it.communicationStyle.take(200)}، زبان عشق ${it.loveLanguage.take(200)}، ارزش‌ها ${it.values.take(300)}"
        }
        val eventsSummary = context.events.take(10).joinToString("\n") {
            "- ${it.title.take(120)}: ${it.description.take(300)}"
        }
        val messagesSummary = context.contactMessages.take(20).reversed().joinToString("\n") {
            "- ${it.direction}: ${it.body.take(700)}"
        }.ifBlank { "پیامی از مخاطب انتخاب‌شده در حافظه ثبت نشده است." }
        val user = """
            سوال کاربر: $question

            شاخص‌های فعلی رابطه:
            $metricsSummary

            پروفایل‌ها:
            $profilesSummary

            آخرین تحلیل‌ها:
            $reportsSummary

            رویدادهای حافظه رابطه:
            $eventsSummary

            پیام‌های ذخیره‌شده با رضایت کاربر:
            $messagesSummary
        """.trimIndent()

        val json = completeAsJson(system, user)
        return AiReply(
            answer = json.optString("answer").ifBlank { "پاسخ آنلاین دریافت نشد." },
            confidence = json.optDouble("confidence", 0.7).toFloat().coerceIn(0f, 1f),
            reasons = json.optJSONArray("reasons")?.let { arr ->
                (0 until arr.length()).map { arr.optString(it) }.filter { it.isNotBlank() }
            } ?: emptyList()
        )
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
        return MessageSimulation(
            conflictRisk = json.optInt("conflictRisk", 30).coerceIn(0, 100),
            misunderstandingRisk = json.optInt("misunderstandingRisk", 30).coerceIn(0, 100),
            hurtRisk = json.optInt("hurtRisk", 25).coerceIn(0, 100),
            improvedMessage = json.optString("improvedMessage").ifBlank { message },
            notes = json.optJSONArray("notes")?.let { arr ->
                (0 until arr.length()).map { arr.optString(it) }.filter { it.isNotBlank() }
            } ?: emptyList()
        )
    }

    override suspend fun suggestProfileUpdates(context: DashboardState): List<ProfileSuggestionDraft> {
        val system = """
            تو بر اساس شواهد محدودِ پیام‌ها و گفتگوها، فقط پیشنهادهای قابل بازبینی برای پروفایل رابطه می‌سازی.
            هرگز نام، سن، شغل، محل زندگی، اهداف، ارزش‌ها یا باورها را پیشنهاد نده و هیچ واقعیتی را قطعی فرض نکن.
            فقط یکی از این fieldها مجاز است: communicationStyle, loveLanguage, traits, dailyHabits, personalityType.
            فقط JSON معتبر برگردان:
            {"suggestions":[{"profileId":"شناسه پروفایل", "field":"یکی از فیلدهای مجاز", "proposedValue":"پیشنهاد کوتاه", "reason":"شواهد کوتاه", "confidence":0.0}]}
            حداکثر 5 پیشنهاد بده. اگر شواهد کافی نیست suggestions را خالی برگردان.
        """.trimIndent()
        val profiles = context.profiles.joinToString("\n") { "${it.id} | ${it.name} | سبک فعلی=${it.communicationStyle} | زبان عشق=${it.loveLanguage} | ویژگی‌ها=${it.traits}" }
        val messages = context.contactMessages.take(40).reversed().joinToString("\n") { "[${it.direction}] ${it.body.take(350)}" }
        val reports = context.reports.take(5).joinToString("\n") { it.summary.take(500) }
        val json = completeAsJson(system, "پروفایل‌ها:\n$profiles\nپیام‌ها:\n$messages\nتحلیل‌های قبلی:\n$reports")
        val array = json.optJSONArray("suggestions") ?: return emptyList()
        return (0 until array.length()).mapNotNull { index ->
            val item = array.optJSONObject(index) ?: return@mapNotNull null
            ProfileSuggestionDraft(
                profileId = item.optString("profileId"),
                field = item.optString("field"),
                proposedValue = item.optString("proposedValue"),
                reason = item.optString("reason"),
                confidence = item.optDouble("confidence", 0.5).toFloat()
            )
        }
    }

    private suspend fun completeAsJson(system: String, user: String): JSONObject {
        val raw = gapgpt.complete(system, user) ?: liara.complete(system, user)
        if (raw == null) {
            Log.w(TAG, "Both online providers failed.")
            throw IOException("پاسخ از سرویس آنلاین دریافت نشد.")
        }
        return extractJson(raw) ?: throw IOException("پاسخ سرویس آنلاین در قالب مورد انتظار نبود.")
    }

    private fun extractJson(raw: String): JSONObject? {
        val start = raw.indexOf('{')
        val end = raw.lastIndexOf('}')
        if (start == -1 || end == -1 || end < start) return null
        return runCatching { JSONObject(raw.substring(start, end + 1)) }.getOrNull()
    }

    companion object {
        private const val TAG = "RemoteRelationshipAi"
        private const val MAX_CONVERSATION_CHARS = 12_000
        private const val MEMORY_TRANSCRIPT_CHARS = 900
    }
}
