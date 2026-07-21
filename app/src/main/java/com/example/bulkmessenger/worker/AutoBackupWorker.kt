package com.example.bulkmessenger.worker

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.example.bulkmessenger.data.AppDatabase
import com.example.bulkmessenger.util.AppLogger
import com.example.bulkmessenger.util.BackupHelper
import com.example.bulkmessenger.util.BackupPrefs
import java.util.concurrent.TimeUnit

/**
 * Silently overwrites the user-chosen backup file every 6 hours, so a reinstall never loses more
 * than a few hours of data without requiring a manual backup. No-ops until a location has been
 * chosen (onboarding or Settings); failures are swallowed rather than retried or surfaced, since
 * this runs unattended in the background.
 */
class AutoBackupWorker(appContext: Context, params: WorkerParameters) :
    CoroutineWorker(appContext, params) {

    companion object {
        private const val UNIQUE_WORK_NAME = "auto_backup"

        /** Safe to call repeatedly (e.g. on every app launch) — KEEP means it's a no-op once scheduled. */
        fun scheduleIfConfigured(context: Context) {
            if (BackupPrefs.getBackupFolderUri(context) == null) return
            val request = PeriodicWorkRequestBuilder<AutoBackupWorker>(6, TimeUnit.HOURS).build()
            WorkManager.getInstance(context)
                .enqueueUniquePeriodicWork(UNIQUE_WORK_NAME, ExistingPeriodicWorkPolicy.KEEP, request)
        }
    }

    override suspend fun doWork(): Result {
        if (BackupPrefs.getBackupFolderUri(applicationContext) == null) return Result.success()
        return try {
            val db = AppDatabase.getInstance(applicationContext)
            val json = BackupHelper.exportToJson(db)
            val file = BackupPrefs.getOrCreateChildFile(applicationContext, BackupPrefs.BACKUP_FILE_NAME, "application/json")
                ?: return Result.success()
            applicationContext.contentResolver.openOutputStream(file.uri, "wt")?.use { out ->
                out.write(json.toString(2).toByteArray())
            }
            AppLogger.log(applicationContext, "Automatic backup completed")
            Result.success()
        } catch (e: Exception) {
            AppLogger.log(applicationContext, "Automatic backup failed: ${e.message}")
            Result.success()
        }
    }
}
