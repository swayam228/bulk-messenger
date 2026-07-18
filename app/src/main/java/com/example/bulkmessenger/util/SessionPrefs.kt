package com.example.bulkmessenger.util

import android.content.Context
import androidx.core.content.edit

/** Tracks which profile is active — needs to be readable synchronously before Room's first query resolves. */
object SessionPrefs {
    private const val PREFS_NAME = "bulk_messenger_prefs"
    private const val KEY_ACTIVE_USER_ID = "active_user_id"

    fun getActiveUserId(context: Context): Long? {
        val id = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .getLong(KEY_ACTIVE_USER_ID, -1L)
        return if (id == -1L) null else id
    }

    fun setActiveUserId(context: Context, userId: Long) {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit { putLong(KEY_ACTIVE_USER_ID, userId) }
    }
}
