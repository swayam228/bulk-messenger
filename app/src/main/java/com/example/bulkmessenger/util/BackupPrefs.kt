package com.example.bulkmessenger.util

import android.content.Context
import android.net.Uri
import androidx.core.content.edit
import androidx.documentfile.provider.DocumentFile

/**
 * Persists the SAF folder auto-backup writes into, chosen during onboarding or Settings. The
 * debug log (see [AppLogger]) is written as a sibling file in the same folder, so both live
 * together where the user picked them.
 */
object BackupPrefs {
    private const val PREFS_NAME = "bulk_messenger_prefs"
    private const val KEY_BACKUP_FOLDER_URI = "auto_backup_folder_uri"

    const val BACKUP_FILE_NAME = "bulkmessenger-backup.json"

    fun getBackupFolderUri(context: Context): Uri? {
        val raw = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .getString(KEY_BACKUP_FOLDER_URI, null) ?: return null
        return Uri.parse(raw)
    }

    fun setBackupFolderUri(context: Context, uri: Uri) {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit { putString(KEY_BACKUP_FOLDER_URI, uri.toString()) }
    }

    /** Best-effort display name for the chosen folder, for showing in Settings. */
    fun displayName(context: Context, treeUri: Uri): String {
        val name = runCatching { DocumentFile.fromTreeUri(context, treeUri)?.name }.getOrNull()
        return name ?: treeUri.lastPathSegment ?: "the chosen folder"
    }

    /** Finds-or-creates [name] as a direct child of the chosen backup folder; null if unset or inaccessible. */
    fun getOrCreateChildFile(context: Context, name: String, mimeType: String): DocumentFile? {
        val treeUri = getBackupFolderUri(context) ?: return null
        val dir = runCatching { DocumentFile.fromTreeUri(context, treeUri) }.getOrNull() ?: return null
        if (!dir.exists()) return null
        return dir.findFile(name) ?: dir.createFile(mimeType, name)
    }
}
