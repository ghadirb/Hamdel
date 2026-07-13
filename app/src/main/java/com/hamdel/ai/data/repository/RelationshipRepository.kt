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

    suspend fun seedIfNeeded() {
        if (dao.metricCount() > 0) return
        dao.upsertMetrics(
            listOf(
                RelationshipMetric("compatibility", "سازگاری", 82, 6, "هم‌راستایی اهداف، ارزش‌ها و سبک ارتباطی"),
                RelationshipMetric("respect", "احترام متقابل", 76, 4, "کیفیت مرزبندی، شنیدن و پاسخ دادن"),
                RelationshipMetric("intimacy", "صمیمیت", 68, 8, "نزدیکی عاطفی و امنیت در بیان احساسات"),
                RelationshipMetric("stress", "استرس رابطه", 34, -5, "فشار، سوءتفاهم و تنش‌های حل‌نشده"),
                RelationshipMetric("trust", "اعتماد", 72, 3, "ثبات، صداقت و قابل اتکا بودن")
            )
        )
        dao.upsertProfiles(
            listOf(
                PersonProfile("person_a", "نفر اول", 29, "کارشناسی ارشد", "طراح محصول", "تهران", "رشد مشترک و آرامش", "کتاب، سفر، موسیقی", "صداقت، احترام، خانواده", "گفتگوی مستقیم", "آرام و تحلیلی", "کلام تاییدآمیز", "INFJ", "همدل، دقیق، گاهی حساس", "پیاده‌روی و یادداشت روزانه", true),
                PersonProfile("person_b", "نفر دوم", 31, "کارشناسی", "مهندس نرم‌افزار", "تهران", "ثبات مالی و خانواده", "ورزش، فیلم، تکنولوژی", "اعتماد، استقلال، مسئولیت", "حل مسئله", "منطقی و کوتاه", "وقت باکیفیت", "INTJ", "مسئول، مستقل، گاهی کم‌حرف", "ورزش صبحگاهی", true)
            )
        )
        dao.upsertEvents(
            listOf(
                RelationshipEvent("first_meet", "اولین آشنایی", "شروع رابطه و شناخت اولیه", "memory", System.currentTimeMillis() - 60L * 24 * 60 * 60 * 1000),
                RelationshipEvent("important_talk", "گفتگوی مهم", "صحبت درباره اهداف مشترک و مرزها", "session", System.currentTimeMillis() - 6L * 24 * 60 * 60 * 1000),
                RelationshipEvent("repair", "آشتی پس از اختلاف", "تمرین شنیدن فعال و عذرخواهی مشخص", "repair", System.currentTimeMillis() - 2L * 24 * 60 * 60 * 1000)
            )
        )
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
        return warnings.ifEmpty {
            listOf("هشدار جدی ثبت نشده؛ روند را با رضایت دوطرفه ادامه دهید.")
        }
    }

    private fun dailySuggestions(metrics: List<RelationshipMetric>): List<String> {
        val stress = metrics.firstOrNull { it.id == "stress" }?.value ?: 0
        return if (stress > 50) {
            listOf("۱۰ دقیقه گفتگوی بدون قطع کردن تمرین کنید.", "یک موضوع حساس را به زمان آرام‌تری موکول کنید.", "هر نفر یک نیاز مشخص و قابل انجام بیان کند.")
        } else {
            listOf("امروز از یک رفتار خوب طرف مقابل مشخصا قدردانی کنید.", "یک برنامه کوتاه دونفره برای آخر هفته بچینید.", "درباره یکی از اهداف مشترک گفتگو کنید.")
        }
    }
}
