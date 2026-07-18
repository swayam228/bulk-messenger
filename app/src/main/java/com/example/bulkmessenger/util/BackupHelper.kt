package com.example.bulkmessenger.util

import androidx.room.withTransaction
import com.example.bulkmessenger.data.AppDatabase
import com.example.bulkmessenger.data.BulkJob
import com.example.bulkmessenger.data.BulkJobItem
import com.example.bulkmessenger.data.Draft
import com.example.bulkmessenger.data.ItemStatus
import com.example.bulkmessenger.data.JobMode
import com.example.bulkmessenger.data.JobStatus
import com.example.bulkmessenger.data.UserProfile
import org.json.JSONArray
import org.json.JSONObject

private const val BACKUP_VERSION = 1

/**
 * Full local-data export/import as JSON, written via Storage Access Framework — the
 * "get my data back after reinstall" safety net for a local-only sideloaded app. Restore
 * always replaces everything on-device rather than merging, since the realistic use case is
 * restoring onto an already-empty database.
 */
object BackupHelper {

    suspend fun exportToJson(db: AppDatabase): JSONObject {
        val users = db.userDao().getAll()
        val drafts = db.draftDao().getAll()
        val jobs = db.bulkJobDao().getAllJobs()
        val items = db.bulkJobDao().getAllItemsRaw()

        val usersArray = JSONArray().apply {
            users.forEach { u ->
                put(JSONObject().apply {
                    put("id", u.id)
                    put("name", u.name)
                    put("avatarColorHex", u.avatarColorHex)
                    put("createdAt", u.createdAt)
                    put("themeMode", u.themeMode)
                    put("defaultSimSubscriptionId", u.defaultSimSubscriptionId ?: JSONObject.NULL)
                })
            }
        }
        val draftsArray = JSONArray().apply {
            drafts.forEach { d ->
                put(JSONObject().apply {
                    put("id", d.id)
                    put("userId", d.userId)
                    put("title", d.title)
                    put("body", d.body)
                    put("createdAt", d.createdAt)
                    put("updatedAt", d.updatedAt)
                })
            }
        }
        val jobsArray = JSONArray().apply {
            jobs.forEach { j ->
                put(JSONObject().apply {
                    put("id", j.id)
                    put("userId", j.userId)
                    put("mode", j.mode.name)
                    put("createdAt", j.createdAt)
                    put("status", j.status.name)
                    put("messagePreview", j.messagePreview ?: JSONObject.NULL)
                    put("scheduledAt", j.scheduledAt ?: JSONObject.NULL)
                    put("simSubscriptionId", j.simSubscriptionId ?: JSONObject.NULL)
                })
            }
        }
        val itemsArray = JSONArray().apply {
            items.forEach { i ->
                put(JSONObject().apply {
                    put("id", i.id)
                    put("jobId", i.jobId)
                    put("contactName", i.contactName ?: JSONObject.NULL)
                    put("phoneNumber", i.phoneNumber)
                    put("messageBody", i.messageBody)
                    put("status", i.status.name)
                    put("sentAt", i.sentAt ?: JSONObject.NULL)
                    put("errorReason", i.errorReason ?: JSONObject.NULL)
                })
            }
        }

        return JSONObject().apply {
            put("backupVersion", BACKUP_VERSION)
            put("exportedAt", System.currentTimeMillis())
            put("users", usersArray)
            put("drafts", draftsArray)
            put("jobs", jobsArray)
            put("items", itemsArray)
        }
    }

    /** Replaces all local data with the contents of [json] inside a single transaction. */
    suspend fun importFromJson(json: JSONObject, db: AppDatabase) {
        val users = json.getJSONArray("users").mapObjects { o ->
            UserProfile(
                id = o.getLong("id"),
                name = o.getString("name"),
                avatarColorHex = o.getString("avatarColorHex"),
                createdAt = o.getLong("createdAt"),
                themeMode = o.optString("themeMode", "SYSTEM"),
                defaultSimSubscriptionId = o.optIntOrNull("defaultSimSubscriptionId")
            )
        }
        val drafts = json.getJSONArray("drafts").mapObjects { o ->
            Draft(
                id = o.getLong("id"),
                userId = o.getLong("userId"),
                title = o.getString("title"),
                body = o.getString("body"),
                createdAt = o.getLong("createdAt"),
                updatedAt = o.getLong("updatedAt")
            )
        }
        val jobs = json.getJSONArray("jobs").mapObjects { o ->
            BulkJob(
                id = o.getLong("id"),
                userId = o.getLong("userId"),
                mode = JobMode.valueOf(o.getString("mode")),
                createdAt = o.getLong("createdAt"),
                status = JobStatus.valueOf(o.getString("status")),
                messagePreview = o.optStringOrNull("messagePreview"),
                scheduledAt = o.optLongOrNull("scheduledAt"),
                simSubscriptionId = o.optIntOrNull("simSubscriptionId")
            )
        }
        val items = json.getJSONArray("items").mapObjects { o ->
            BulkJobItem(
                id = o.getLong("id"),
                jobId = o.getLong("jobId"),
                contactName = o.optStringOrNull("contactName"),
                phoneNumber = o.getString("phoneNumber"),
                messageBody = o.getString("messageBody"),
                status = ItemStatus.valueOf(o.getString("status")),
                sentAt = o.optLongOrNull("sentAt"),
                errorReason = o.optStringOrNull("errorReason")
            )
        }

        db.withTransaction {
            db.bulkJobDao().deleteAllItems()
            db.bulkJobDao().deleteAllJobs()
            db.draftDao().deleteAll()
            db.userDao().deleteAll()

            users.forEach { db.userDao().insert(it) }
            drafts.forEach { db.draftDao().insert(it) }
            jobs.forEach { db.bulkJobDao().insertJob(it) }
            if (items.isNotEmpty()) db.bulkJobDao().insertItems(items)
        }
    }
}

private inline fun <T> JSONArray.mapObjects(transform: (JSONObject) -> T): List<T> =
    (0 until length()).map { transform(getJSONObject(it)) }

private fun JSONObject.optStringOrNull(key: String): String? = if (isNull(key)) null else getString(key)
private fun JSONObject.optLongOrNull(key: String): Long? = if (isNull(key)) null else getLong(key)
private fun JSONObject.optIntOrNull(key: String): Int? = if (isNull(key)) null else getInt(key)
