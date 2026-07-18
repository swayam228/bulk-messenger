package com.example.bulkmessenger.worker

import android.content.Context
import android.content.pm.ServiceInfo
import android.os.Build
import android.telephony.SmsManager
import androidx.work.CoroutineWorker
import androidx.work.ForegroundInfo
import androidx.work.WorkerParameters
import com.example.bulkmessenger.data.AppDatabase
import com.example.bulkmessenger.data.ItemStatus
import com.example.bulkmessenger.data.JobStatus
import com.example.bulkmessenger.util.NotificationHelper
import com.example.bulkmessenger.util.SENDING_NOTIFICATION_ID
import kotlinx.coroutines.delay

/**
 * Sends every BulkJobItem belonging to [KEY_JOB_ID], one at a time, with a small delay
 * between sends to stay under carrier spam-filter thresholds. Updates Room after each
 * send so the UI can show live progress.
 */
class SmsSendWorker(appContext: Context, params: WorkerParameters) :
    CoroutineWorker(appContext, params) {

    companion object {
        const val KEY_JOB_ID = "job_id"
        const val KEY_DELAY_MS = "delay_ms"
        const val DEFAULT_DELAY_MS = 3000L
    }

    override suspend fun doWork(): Result {
        val jobId = inputData.getLong(KEY_JOB_ID, -1L)
        if (jobId == -1L) return Result.failure()
        val delayMs = inputData.getLong(KEY_DELAY_MS, DEFAULT_DELAY_MS)

        val db = AppDatabase.getInstance(applicationContext)
        val dao = db.bulkJobDao()
        val job = dao.getJob(jobId) ?: return Result.failure()

        dao.updateJob(job.copy(status = JobStatus.SENDING))

        val items = dao.getItems(jobId)
        val defaultManager = applicationContext.getSystemService(SmsManager::class.java)
            ?: SmsManager.getDefault()
        val smsManager = job.simSubscriptionId?.let { subId ->
            runCatching { SmsManager.getSmsManagerForSubscriptionId(subId) }.getOrDefault(defaultManager)
        } ?: defaultManager

        // Promotes this worker to a foreground service for the duration of the send. Without this,
        // aggressive OEM battery managers (Vivo/Oppo/Realme and similar) can kill the app process
        // mid-send — WorkManager eventually recovers and finishes the job, but only once the OS lets
        // the process run again, which can be a long, unpredictable wait.
        setForeground(createForegroundInfo(sent = 0, total = items.size))

        var anyFailed = false
        var sentSoFar = 0

        for (item in items) {
            if (item.status != ItemStatus.PENDING) continue
            try {
                val parts = smsManager.divideMessage(item.messageBody)
                if (parts.size > 1) {
                    smsManager.sendMultipartTextMessage(item.phoneNumber, null, parts, null, null)
                } else {
                    smsManager.sendTextMessage(item.phoneNumber, null, item.messageBody, null, null)
                }
                dao.updateItem(
                    item.copy(status = ItemStatus.SENT, sentAt = System.currentTimeMillis())
                )
            } catch (e: Exception) {
                anyFailed = true
                dao.updateItem(
                    item.copy(status = ItemStatus.FAILED, errorReason = e.message ?: "Unknown error")
                )
            }
            sentSoFar++
            setForeground(createForegroundInfo(sent = sentSoFar, total = items.size))
            // Throttle: avoid firing sends back-to-back, which is what trips carrier
            // spam detection and Android's own "app is sending many messages" prompt.
            delay(delayMs)
        }

        val finalJob = dao.getJob(jobId)
        if (finalJob != null) {
            dao.updateJob(finalJob.copy(status = if (anyFailed) JobStatus.FAILED else JobStatus.COMPLETED))
        }

        val finalItems = dao.getItems(jobId)
        val sentCount = finalItems.count { it.status == ItemStatus.SENT }
        val failedCount = finalItems.count { it.status == ItemStatus.FAILED }
        NotificationHelper.notifyJobComplete(applicationContext, jobId, job.mode, sentCount, failedCount)

        return Result.success()
    }

    private fun createForegroundInfo(sent: Int, total: Int): ForegroundInfo {
        NotificationHelper.ensureChannel(applicationContext)
        val notification = NotificationHelper.buildSendingNotification(applicationContext, sent, total)
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            ForegroundInfo(SENDING_NOTIFICATION_ID, notification, ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC)
        } else {
            ForegroundInfo(SENDING_NOTIFICATION_ID, notification)
        }
    }
}
