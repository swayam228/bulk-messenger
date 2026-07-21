package com.example.bulkmessenger.util

import android.content.Context
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Append-only plain-text debug log — mainly for diagnosing "app says sent but it never arrived"
 * reports, since that only shows up in the actual carrier result code, not in the UI. Written as
 * a sibling of the auto-backup file in the same user-chosen SAF folder so it's easy to find and
 * pull off-device; falls back to app-private storage until a backup folder is configured.
 */
object AppLogger {
    const val LOG_FILE_NAME = "bulkmessenger-log.txt"
    private val timeFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS", Locale.US)

    @Synchronized
    fun log(context: Context, message: String) {
        val line = "${timeFormat.format(Date())}  $message\n"
        val wroteToFolder = runCatching { appendToFolder(context, line) }.getOrDefault(false)
        if (!wroteToFolder) {
            runCatching { appendToInternal(context, line) }
        }
    }

    private fun appendToFolder(context: Context, line: String): Boolean {
        val file = BackupPrefs.getOrCreateChildFile(context, LOG_FILE_NAME, "text/plain") ?: return false
        context.contentResolver.openOutputStream(file.uri, "wa")?.use { out ->
            out.write(line.toByteArray(Charsets.UTF_8))
        } ?: return false
        return true
    }

    private fun appendToInternal(context: Context, line: String) {
        File(context.filesDir, LOG_FILE_NAME).appendText(line, Charsets.UTF_8)
    }
}
