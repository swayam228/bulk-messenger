package com.example.bulkmessenger.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverter
import androidx.room.TypeConverters

class Converters {
    @TypeConverter
    fun fromJobMode(v: JobMode): String = v.name
    @TypeConverter
    fun toJobMode(v: String): JobMode = JobMode.valueOf(v)

    @TypeConverter
    fun fromJobStatus(v: JobStatus): String = v.name
    @TypeConverter
    fun toJobStatus(v: String): JobStatus = JobStatus.valueOf(v)

    @TypeConverter
    fun fromItemStatus(v: ItemStatus): String = v.name
    @TypeConverter
    fun toItemStatus(v: String): ItemStatus = ItemStatus.valueOf(v)
}

@Database(
    entities = [UserProfile::class, Draft::class, BulkJob::class, BulkJobItem::class],
    version = 3,
    exportSchema = false
)
@TypeConverters(Converters::class)
abstract class AppDatabase : RoomDatabase() {
    abstract fun userDao(): UserDao
    abstract fun draftDao(): DraftDao
    abstract fun bulkJobDao(): BulkJobDao

    companion object {
        @Volatile private var INSTANCE: AppDatabase? = null

        fun getInstance(context: Context): AppDatabase =
            INSTANCE ?: synchronized(this) {
                INSTANCE ?: Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "bulk_messenger.db"
                )
                    // MVP app, no user-facing migration path yet — safe to drop local job
                    // history/drafts on schema bumps rather than hand-writing migrations.
                    .fallbackToDestructiveMigration()
                    .build().also { INSTANCE = it }
            }
    }
}

/**
 * Single access point for the UI layer. Keeps ViewModels free of Room/WorkManager details.
 */
class MessageRepository(private val db: AppDatabase) {
    val users = db.userDao()
    val drafts = db.draftDao()
    val jobs = db.bulkJobDao()

    suspend fun saveDraft(userId: Long, title: String, body: String, existingId: Long? = null) {
        if (existingId != null) {
            drafts.update(Draft(id = existingId, userId = userId, title = title, body = body, updatedAt = System.currentTimeMillis()))
        } else {
            drafts.insert(Draft(userId = userId, title = title, body = body))
        }
    }

    suspend fun createJob(
        userId: Long,
        mode: JobMode,
        recipients: List<Pair<String, String>>,
        scheduledAt: Long? = null,
        simSubscriptionId: Int? = null
    ): Long {
        // recipients = list of (phoneNumber, messageBody)
        val isFutureSchedule = scheduledAt != null && scheduledAt > System.currentTimeMillis()
        val messagePreview = if (mode == JobMode.SAME_MESSAGE) recipients.firstOrNull()?.second else null
        val jobId = jobs.insertJob(
            BulkJob(
                userId = userId,
                mode = mode,
                status = if (isFutureSchedule) JobStatus.SCHEDULED else JobStatus.QUEUED,
                messagePreview = messagePreview,
                scheduledAt = scheduledAt,
                simSubscriptionId = simSubscriptionId
            )
        )
        val items = recipients.map { (number, body) ->
            BulkJobItem(jobId = jobId, phoneNumber = number, messageBody = body)
        }
        jobs.insertItems(items)
        return jobId
    }
}
