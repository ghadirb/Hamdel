package com.hamdel.ai.data.analysis

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.hamdel.ai.HamdelApplication
import com.hamdel.ai.data.settings.HamdelPreferences
import kotlinx.coroutines.flow.first
import java.util.concurrent.TimeUnit

class AutoAnalysisWorker(context: Context, params: WorkerParameters) : CoroutineWorker(context, params) {
    override suspend fun doWork(): Result {
        val preferences = HamdelPreferences(applicationContext)
        if (!preferences.autoAnalysisEnabled) return Result.success()
        val app = applicationContext as HamdelApplication
        val state = app.repository.dashboard.first()
        if (state.profiles.size < 2 || state.profiles.any { !it.consentGranted }) return Result.success()
        if (state.contactMessages.isEmpty() && state.reports.isEmpty()) return Result.success()
        return runCatching {
            app.repository.analyzeRelationshipMemory(state)
            Result.success()
        }.getOrElse { Result.retry() }
    }

    companion object {
        private const val WORK_NAME = "hamdel_auto_analysis"

        fun schedule(context: Context) {
            val request = PeriodicWorkRequestBuilder<AutoAnalysisWorker>(1, TimeUnit.DAYS).build()
            WorkManager.getInstance(context).enqueueUniquePeriodicWork(WORK_NAME, ExistingPeriodicWorkPolicy.UPDATE, request)
        }

        fun cancel(context: Context) {
            WorkManager.getInstance(context).cancelUniqueWork(WORK_NAME)
        }
    }
}
