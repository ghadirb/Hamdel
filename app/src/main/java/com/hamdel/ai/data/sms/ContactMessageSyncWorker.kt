package com.hamdel.ai.data.sms

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.hamdel.ai.HamdelApplication
import com.hamdel.ai.data.settings.HamdelPreferences
import java.util.concurrent.TimeUnit

class ContactMessageSyncWorker(context: Context, params: WorkerParameters) : CoroutineWorker(context, params) {
    override suspend fun doWork(): Result {
        val preferences = HamdelPreferences(applicationContext)
        if (!preferences.messageSyncEnabled) return Result.success()
        return runCatching {
            val app = applicationContext as HamdelApplication
            app.repository.saveContactMessages(app.contactSmsImporter.importForConfiguredContact())
            Result.success()
        }.getOrElse { Result.retry() }
    }

    companion object {
        private const val WORK_NAME = "hamdel_contact_message_sync"

        fun schedule(context: Context) {
            val request = PeriodicWorkRequestBuilder<ContactMessageSyncWorker>(1, TimeUnit.HOURS).build()
            WorkManager.getInstance(context).enqueueUniquePeriodicWork(WORK_NAME, ExistingPeriodicWorkPolicy.UPDATE, request)
        }

        fun cancel(context: Context) {
            WorkManager.getInstance(context).cancelUniqueWork(WORK_NAME)
        }
    }
}
