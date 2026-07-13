package com.hamdel.ai.data.repository

import com.hamdel.ai.data.local.RelationshipDao
import com.hamdel.ai.data.model.AiReply
import com.hamdel.ai.data.model.ConversationReport
import com.hamdel.ai.data.model.DashboardState
import com.hamdel.ai.data.model.MessageSimulation
import com.hamdel.ai.data.model.PersonProfile
import com.hamdel.ai.data.model.RelationshipEvent
import com.hamdel.ai.data.model.RelationshipMetric
import com.hamdel.ai.domain.RelationshipAiEngine
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import java.util.UUID

class RelationshipRepository(
    private val dao: RelationshipDao,
    private val aiEngine: RelationshipAiEngine
) {
    val dashboard: Flow<DashboardState> = combine(
        dao.observeMetrics(),
        dao.observeEvents(),
        dao.observeReports(),
        dao.observeProfiles()
    ) { metrics, events, reports, profiles ->
        DashboardState(
            metrics = metrics,
            events = events,
            reports = reports,
            profiles = profiles,
            warnings = buildWarnings(metrics, reports),
            suggestions = dailySuggestions(metrics)
        )
    }

    /** Removes only the known records created by versions that shipped demo content. */
    suspend fun clearLegacyDemoData() {
        val isOnlyLegacyDemo = dao.profileCount() == 2 &&
            dao.legacyProfileCount() == 2 &&
            dao.reportCount() == 0
        if (isOnlyLegacyDemo) {
            dao.clearMetrics()
            dao.clearLegacyProfiles()
            dao.clearLegacyEvents()
        }
    }

    suspend fun analyzeConversation(title: String, text: String): ConversationReport {
        val report = aiEngine.analyzeConversation(title, text)
        dao.insertReport(report)
        updateMetricsFrom(report)
        dao.upsertEvents(
            listOf(
                RelationshipEvent(
                    id = UUID.randomUUID().toString(),
                    title = "تحلیل گفتگو",
                    description = report.sourceTitle,
                    category = "analysis",
                    timestamp = System.currentTimeMillis()
                )
            )
        )
        return report
    }

    suspend fun saveProfile(profile: PersonProfile) {
        dao.upsertProfile(profile)
    }

    suspend fun askAssistant(question: String, state: DashboardState): AiReply {
        return aiEngine.askAssistant(question, state)
    }

    suspend fun simulateMessage(message: String): MessageSimulation {
        return aiEngine.simulateMessage(message)
    }

    private suspend fun updateMetricsFrom(report: ConversationReport) {
        val stress = ((report.sarcasmRisk + report.controlRisk + (100 - report.empathy)) / 3).coerceIn(0, 100)
        val intimacy = ((report.empathy + report.emotionalSupport) / 2).coerceIn(0, 100)
        val trust = ((report.honesty + (100 - report.controlRisk)) / 2).coerceIn(0, 100)
        val compatibility = ((report.respect + intimacy + trust + (100 - stress)) / 4).coerceIn(0, 100)
        dao.upsertMetrics(
            listOf(
                RelationshipMetric("compatibility", "سازگاری", compatibility, 0, "برآورد از آخرین تحلیل‌های گفتگو و جلسه"),
                RelationshipMetric("respect", "احترام متقابل", report.respect, 0, "کیفیت مرزبندی، شنیدن و پاسخ دادن"),
                RelationshipMetric("intimacy", "صمیمیت", intimacy, 0, "نزدیکی عاطفی و حمایت احساسی"),
                RelationshipMetric("stress", "استرس رابطه", stress, 0, "ریسک تنش، طعنه و کنترل‌گری"),
                RelationshipMetric("trust", "اعتماد", trust, 0, "صداقت، ثبات و نبود کنترل‌گری")
            )
        )
    }

    private fun buildWarnings(metrics: List<RelationshipMetric>, reports: List<ConversationReport>): List<String> {
        val warnings = mutableListOf<String>()
        metrics.firstOrNull { it.id == "stress" && it.value > 60 }?.let {
            warnings += "استرس رابطه بالاست؛ گفتگوهای سنگین را زمان‌بندی کنید."
        }
        reports.firstOrNull { it.controlRisk > 55 }?.let {
            warnings += "در آخرین گفتگو نشانه‌هایی از کنترل‌گری دیده شد."
        }
        reports.firstOrNull { it.sarcasmRisk > 55 }?.let {
            warnings += "طعنه می‌تواند احساس امنیت را کم کند."
        }
        return warnings
    }

    private fun dailySuggestions(metrics: List<RelationshipMetric>): List<String> {
        if (metrics.isEmpty()) return emptyList()
        val stress = metrics.firstOrNull { it.id == "stress" }?.value ?: 0
        return if (stress > 50) {
            listOf("۱۰ دقیقه گفتگوی بدون قطع کردن تمرین کنید.", "یک موضوع حساس را به زمان آرام‌تری موکول کنید.", "هر نفر یک نیاز مشخص و قابل انجام بیان کند.")
        } else {
            listOf("امروز از یک رفتار خوب طرف مقابل مشخصا قدردانی کنید.", "یک برنامه کوتاه دونفره برای آخر هفته بچینید.", "درباره یکی از اهداف مشترک گفتگو کنید.")
        }
    }
}
