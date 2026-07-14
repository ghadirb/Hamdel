package com.hamdel.ai.data.backup

import android.content.Context
import android.net.Uri
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.hamdel.ai.data.settings.HamdelPreferences
import java.util.concurrent.TimeUnit

class AutoBackupWorker(context: Context, params: WorkerParameters) : CoroutineWorker(context, params) {
    override suspend fun doWork(): Result {
        val preferences = HamdelPreferences(applicationContext)
        if (!preferences.autoBackupEnabled) return Result.success()
        val uri = preferences.backupUri?.let(Uri::parse) ?: return Result.success()
        return if (BackupManager.backupToUri(applicationContext, uri)) Result.success() else Result.retry()
    }

    companion object {
        private const val WORK_NAME = "hamdel_auto_backup"

        fun schedule(context: Context) {
            val request = PeriodicWorkRequestBuilder<AutoBackupWorker>(1, TimeUnit.DAYS).build()
            WorkManager.getInstance(context).enqueueUniquePeriodicWork(WORK_NAME, ExistingPeriodicWorkPolicy.UPDATE, request)
        }

        fun cancel(context: Context) {
            WorkManager.getInstance(context).cancelUniqueWork(WORK_NAME)
        }
    }
}
