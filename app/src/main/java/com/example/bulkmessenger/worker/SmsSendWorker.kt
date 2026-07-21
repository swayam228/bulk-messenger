package com.example.bulkmessenger.worker

import android.app.Activity
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.ServiceInfo
import android.os.Build
import android.telephony.SmsManager
import androidx.core.content.ContextCompat
import androidx.work.CoroutineWorker
import androidx.work.ForegroundInfo
import androidx.work.WorkerParameters
import com.example.bulkmessenger.data.AppDatabase
import com.example.bulkmessenger.data.ItemStatus
import com.example.bulkmessenger.data.JobStatus
import com.example.bulkmessenger.util.AppLogger
import com.example.bulkmessenger.util.NotificationHelper
import com.example.bulkmessenger.util.SENDING_NOTIFICATION_ID
import kotlinx.coroutines.delay
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withTimeoutOrNull
import java.util.UUID
import java.util.concurrent.atomic.AtomicInteger

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
        AppLogger.log(applicationContext, "Job $jobId: starting send (${job.mode})")

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

            // sendTextMessage/sendMultipartTextMessage return as soon as the radio accepts the
            // request, not once it's actually sent — real outcomes (including carrier-side
            // rejections) only arrive via the sentIntent broadcast below. Awaiting it is what
            // catches the "app shows sent but it never arrived" case.
            val outcome = sendSmsAwaitingResult(applicationContext, smsManager, item.phoneNumber, item.messageBody)
            outcome.fold(
                onSuccess = {
                    dao.updateItem(item.copy(status = ItemStatus.SENT, sentAt = System.currentTimeMillis(), errorReason = null))
                    AppLogger.log(applicationContext, "Job $jobId: SENT to ${item.phoneNumber} (item ${item.id})")
                },
                onFailure = { e ->
                    anyFailed = true
                    val reason = e.message ?: "Unknown error"
                    dao.updateItem(item.copy(status = ItemStatus.FAILED, errorReason = reason))
                    AppLogger.log(applicationContext, "Job $jobId: FAILED to ${item.phoneNumber} (item ${item.id}): $reason")
                }
            )
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
        AppLogger.log(applicationContext, "Job $jobId: finished — $sentCount sent, $failedCount failed")
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

private const val EXTRA_PART_INDEX = "part_index"
private const val SENT_RESULT_TIMEOUT_MS = 20_000L
private val requestCodeCounter = AtomicInteger(0)

/**
 * Sends [messageBody] and suspends until the carrier/radio actually confirms it, resolving to a
 * failure (with a human-readable reason) if any part comes back with a non-OK result code, or if
 * no confirmation arrives within [SENT_RESULT_TIMEOUT_MS].
 */
private suspend fun sendSmsAwaitingResult(
    context: Context,
    smsManager: SmsManager,
    phoneNumber: String,
    messageBody: String
): Result<Unit> {
    val action = "com.example.bulkmessenger.SMS_SENT_${UUID.randomUUID()}"
    val parts = smsManager.divideMessage(messageBody)
    val partCount = maxOf(parts.size, 1)

    val outcome = withTimeoutOrNull(SENT_RESULT_TIMEOUT_MS) {
        suspendCancellableCoroutine { cont ->
            val resultCodes = IntArray(partCount) { Int.MIN_VALUE }
            var received = 0

            val receiver = object : BroadcastReceiver() {
                override fun onReceive(receiverContext: Context, intent: Intent) {
                    val idx = intent.getIntExtra(EXTRA_PART_INDEX, 0).coerceIn(0, partCount - 1)
                    resultCodes[idx] = resultCode
                    received++
                    if (received >= partCount) {
                        runCatching { context.unregisterReceiver(this) }
                        val failureCode = resultCodes.firstOrNull { it != Activity.RESULT_OK }
                        val result = if (failureCode == null) {
                            Result.success(Unit)
                        } else {
                            Result.failure(Exception(describeSmsResultCode(failureCode)))
                        }
                        if (cont.isActive) cont.resume(result, onCancellation = null)
                    }
                }
            }
            ContextCompat.registerReceiver(context, receiver, IntentFilter(action), ContextCompat.RECEIVER_NOT_EXPORTED)
            cont.invokeOnCancellation { runCatching { context.unregisterReceiver(receiver) } }

            val sentIntents = ArrayList((0 until partCount).map { idx ->
                val intent = Intent(action).apply {
                    putExtra(EXTRA_PART_INDEX, idx)
                    setPackage(context.packageName)
                }
                PendingIntent.getBroadcast(
                    context,
                    requestCodeCounter.incrementAndGet(),
                    intent,
                    PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                )
            })

            try {
                if (parts.size > 1) {
                    smsManager.sendMultipartTextMessage(phoneNumber, null, parts, sentIntents, null)
                } else {
                    smsManager.sendTextMessage(phoneNumber, null, messageBody, sentIntents[0], null)
                }
            } catch (e: Exception) {
                runCatching { context.unregisterReceiver(receiver) }
                if (cont.isActive) cont.resume(Result.failure(e), onCancellation = null)
            }
        }
    }

    return outcome ?: Result.failure(Exception("No confirmation from carrier (timed out)"))
}

private fun describeSmsResultCode(code: Int): String = when (code) {
    SmsManager.RESULT_ERROR_GENERIC_FAILURE -> "Generic failure"
    SmsManager.RESULT_ERROR_NO_SERVICE -> "No service"
    SmsManager.RESULT_ERROR_NULL_PDU -> "Null PDU"
    SmsManager.RESULT_ERROR_RADIO_OFF -> "Radio off"
    SmsManager.RESULT_ERROR_LIMIT_EXCEEDED -> "Carrier send limit exceeded"
    SmsManager.RESULT_ERROR_FDN_CHECK_FAILURE -> "Blocked by fixed dialing numbers"
    SmsManager.RESULT_ERROR_SHORT_CODE_NOT_ALLOWED -> "Short code not allowed"
    SmsManager.RESULT_ERROR_SHORT_CODE_NEVER_ALLOWED -> "Short code never allowed"
    else -> "Send failed (carrier code $code)"
}
