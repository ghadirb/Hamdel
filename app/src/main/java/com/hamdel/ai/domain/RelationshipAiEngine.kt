package com.hamdel.ai.domain

import com.hamdel.ai.data.model.AiReply
import com.hamdel.ai.data.model.ConversationReport
import com.hamdel.ai.data.model.DashboardState
import com.hamdel.ai.data.model.MessageSimulation
import java.util.UUID
import kotlin.math.max
import kotlin.math.min

interface RelationshipAiEngine {
    suspend fun analyzeConversation(title: String, text: String): ConversationReport
    suspend fun askAssistant(question: String, context: DashboardState): AiReply
    suspend fun simulateMessage(message: String): MessageSimulation
}

class DemoRelationshipAiEngine : RelationshipAiEngine {
    override suspend fun analyzeConversation(title: String, text: String): ConversationReport {
        val lower = text.lowercase()
        val supportive = score(lower, listOf("متوجه", "میفهمم", "ممنون", "دوست", "حمایت", "حق داری", "ببخش"))
        val harsh = score(lower, listOf("همیشه", "هیچوقت", "تقصیر", "باید", "ساکت", "تهدید", "دروغ"))
        val sarcasm = score(lower, listOf("عجب", "باشه بابا", "هرچی", "مثلا", "واقعا که"))
        val control = score(lower, listOf("اجازه", "نذار", "حق نداری", "باید بگی", "کجا بودی"))
        val respect = clamp(78 + supportive - harsh - control / 2)
        val empathy = clamp(70 + supportive - sarcasm)
        val honesty = clamp(74 + supportive / 2 - harsh / 2)

        return ConversationReport(
            id = UUID.randomUUID().toString(),
            sourceTitle = title.ifBlank { "گفتگوی دستی" },
            summary = if (text.isBlank()) {
                "متنی برای تحلیل وارد نشده است. نمونه تحلیلی بر پایه الگوهای عمومی نمایش داده می‌شود."
            } else {
                "گفتگو از نظر احترام، همدلی، نشانه‌های کنترل‌گری و حمایت عاطفی بررسی شد. نتیجه برای تصمیم قطعی نیست و باید در کنار گفتگوی مستقیم و رضایت دوطرفه دیده شود."
            },
            respect = respect,
            empathy = empathy,
            honesty = honesty,
            sarcasmRisk = clamp(20 + sarcasm),
            controlRisk = clamp(16 + control),
            emotionalSupport = clamp(64 + supportive - harsh / 2),
            createdAt = System.currentTimeMillis()
        )
    }

    override suspend fun askAssistant(question: String, context: DashboardState): AiReply {
        val stress = context.metrics.firstOrNull { it.title.contains("استرس") }?.value ?: 34
        val respect = context.metrics.firstOrNull { it.title.contains("احترام") }?.value ?: 76
        val answer = buildString {
            append("بر اساس داده‌های فعلی رابطه، بهتر است با مشاهده و گفتگوی آرام جلو بروید. ")
            if (stress > 60) append("سطح استرس بالاست؛ گفتگو را کوتاه، مشخص و بدون سرزنش شروع کنید. ")
            if (respect < 55) append("نشانه‌های افت احترام دیده می‌شود؛ مرزهای رفتاری را روشن و محترمانه بیان کنید. ")
            append("برای تصمیم‌های بزرگ مثل ازدواج، این تحلیل فقط یک چراغ راهنماست و جای مشاور انسانی یا رضایت دوطرفه را نمی‌گیرد.")
        }
        return AiReply(
            answer = if (question.isBlank()) "سوالت را بنویس تا پاسخ شخصی‌سازی شده‌تری بدهم." else answer,
            confidence = 0.72f,
            reasons = listOf("خلاصه روند رابطه", "آخرین هشدارها", "شاخص‌های احترام، صمیمیت و استرس")
        )
    }

    override suspend fun simulateMessage(message: String): MessageSimulation {
        val lower = message.lowercase()
        val pressure = score(lower, listOf("باید", "همیشه", "هیچوقت", "تقصیر", "چرا نمیفهمی", "حق نداری"))
        val care = score(lower, listOf("لطفا", "احساس", "دوست دارم", "میخوام بفهمم", "ممنون", "اگر موافقی"))
        val risk = clamp(28 + pressure - care)
        return MessageSimulation(
            conflictRisk = risk,
            misunderstandingRisk = clamp(32 + pressure / 2),
            hurtRisk = clamp(24 + pressure - care / 2),
            improvedMessage = "می‌خوام درباره این موضوع آرام حرف بزنیم. احساس من این است که نیاز دارم بهتر همدیگر را بفهمیم. چه زمانی برایت مناسب است؟",
            notes = listOf("از کلمات مطلق مثل همیشه و هیچوقت کمتر استفاده کن.", "احساس خودت را بگو، نه حکم قطعی درباره طرف مقابل.", "زمان مناسب برای گفتگو را بپرس.")
        )
    }

    private fun score(text: String, words: List<String>): Int {
        val scorer: (String) -> Int = { word -> if (text.contains(word)) 9 else 0 }
        return words.sumOf(scorer)
    }

    private fun clamp(value: Int): Int = min(100, max(0, value))
}
