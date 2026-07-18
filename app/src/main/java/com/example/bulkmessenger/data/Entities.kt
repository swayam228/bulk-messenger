package com.example.bulkmessenger.data

import androidx.room.Entity
import androidx.room.PrimaryKey

enum class JobMode { SAME_MESSAGE, PERSONALIZED }
enum class JobStatus { SCHEDULED, QUEUED, SENDING, COMPLETED, FAILED }
enum class ItemStatus { PENDING, SENT, FAILED }

@Entity(tableName = "user_profiles")
data class UserProfile(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val avatarColorHex: String,
    val createdAt: Long = System.currentTimeMillis(),
    /** Mirrors viewmodel.ThemeMode by name (SYSTEM/LIGHT/DARK) — kept as a plain String so this entity has no dependency on the viewmodel layer. */
    val themeMode: String = "SYSTEM",
    val defaultSimSubscriptionId: Int? = null
)

@Entity(tableName = "drafts")
data class Draft(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val userId: Long,
    val title: String,
    val body: String,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "bulk_jobs")
data class BulkJob(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val userId: Long,
    val mode: JobMode,
    val createdAt: Long = System.currentTimeMillis(),
    val status: JobStatus = JobStatus.QUEUED,
    /** For SAME_MESSAGE jobs, the shared message text — lets Job History show a snippet without loading items. */
    val messagePreview: String? = null,
    /** When the send was scheduled for; null means it was (or will be) sent immediately. */
    val scheduledAt: Long? = null,
    /** SIM subscription id used to send, if a non-default SIM was chosen; null means the device default. */
    val simSubscriptionId: Int? = null
)

@Entity(tableName = "bulk_job_items")
data class BulkJobItem(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val jobId: Long,
    val contactName: String? = null,
    val phoneNumber: String,
    val messageBody: String,
    val status: ItemStatus = ItemStatus.PENDING,
    val sentAt: Long? = null,
    val errorReason: String? = null
)
