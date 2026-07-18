package com.example.bulkmessenger.util

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.example.bulkmessenger.MainActivity
import com.example.bulkmessenger.data.JobMode

private const val CHANNEL_ID = "job_updates"
private const val SENDING_CHANNEL_ID = "job_sending"
private const val ACCENT_COLOR = 0xFF3D5AFE.toInt()
const val SENDING_NOTIFICATION_ID = 9001

object NotificationHelper {
    fun ensureChannel(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val manager = context.getSystemService(NotificationManager::class.java)
            manager?.createNotificationChannel(
                NotificationChannel(
                    CHANNEL_ID,
                    "Job updates",
                    NotificationManager.IMPORTANCE_DEFAULT
                ).apply {
                    description = "Notifies you when a broadcast or personalized batch finishes sending."
                }
            )
            manager?.createNotificationChannel(
                NotificationChannel(
                    SENDING_CHANNEL_ID,
                    "Sending in progress",
                    NotificationManager.IMPORTANCE_LOW
                ).apply {
                    description = "Ongoing progress while a job is actively sending — keeps the app alive so sends aren't interrupted."
                }
            )
        }
    }

    /** Ongoing, low-priority progress notification — also what promotes SmsSendWorker to a foreground service. */
    fun buildSendingNotification(context: Context, sent: Int, total: Int) =
        NotificationCompat.Builder(context, SENDING_CHANNEL_ID)
            .setSmallIcon(android.R.drawable.stat_sys_upload)
            .setContentTitle("Sending messages")
            .setContentText("$sent of $total sent")
            .setProgress(total, sent, false)
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .setColor(ACCENT_COLOR)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .build()

    fun notifyJobComplete(context: Context, jobId: Long, mode: JobMode, sent: Int, failed: Int) {
        ensureChannel(context)

        val modeLabel = if (mode == JobMode.SAME_MESSAGE) "Broadcast" else "Personalized batch"
        val title = if (failed == 0) "$modeLabel sent" else "$modeLabel finished with errors"
        val text = if (failed == 0) {
            "All $sent message(s) sent successfully."
        } else {
            "$sent sent, $failed failed. Tap to view details."
        }

        val openIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val pendingIntent = PendingIntent.getActivity(
            context, jobId.toInt(), openIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val icon = if (failed == 0) {
            android.R.drawable.stat_sys_upload_done
        } else {
            android.R.drawable.stat_notify_error
        }

        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(icon)
            .setContentTitle(title)
            .setContentText(text)
            .setStyle(NotificationCompat.BigTextStyle().bigText(text))
            .setColor(ACCENT_COLOR)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .build()

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            ActivityCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
        ) {
            return
        }

        NotificationManagerCompat.from(context).notify(jobId.toInt(), notification)
    }
}
