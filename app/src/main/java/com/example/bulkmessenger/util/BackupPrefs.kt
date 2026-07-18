package com.example.bulkmessenger.util

import android.content.Context
import android.net.Uri
import android.provider.OpenableColumns
import androidx.core.content.edit

/** Persists the SAF file location auto-backup writes to, chosen during onboarding or Settings. */
object BackupPrefs {
    private const val PREFS_NAME = "bulk_messenger_prefs"
    private const val KEY_BACKUP_URI = "auto_backup_uri"

    fun getBackupUri(context: Context): Uri? {
        val raw = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .getString(KEY_BACKUP_URI, null) ?: return null
        return Uri.parse(raw)
    }

    fun setBackupUri(context: Context, uri: Uri) {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit { putString(KEY_BACKUP_URI, uri.toString()) }
    }

    /** Best-effort display name for the chosen file, for showing in Settings. */
    fun displayName(context: Context, uri: Uri): String {
        context.contentResolver.query(uri, arrayOf(OpenableColumns.DISPLAY_NAME), null, null, null)?.use { cursor ->
            val nameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
            if (nameIndex >= 0 && cursor.moveToFirst()) return cursor.getString(nameIndex)
        }
        return uri.lastPathSegment ?: "the chosen file"
    }
}
