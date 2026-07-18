package com.example.bulkmessenger

import androidx.room.Room
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.example.bulkmessenger.data.AppDatabase
import com.example.bulkmessenger.data.BulkJob
import com.example.bulkmessenger.data.BulkJobItem
import com.example.bulkmessenger.data.Draft
import com.example.bulkmessenger.data.ItemStatus
import com.example.bulkmessenger.data.JobMode
import com.example.bulkmessenger.data.JobStatus
import com.example.bulkmessenger.data.UserProfile
import com.example.bulkmessenger.util.BackupHelper
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Covers the "get my data back after reinstall" promise: export everything to JSON, wipe the
 * database, import the JSON back, and confirm every row (across all four tables) is identical.
 * Runs against an in-memory Room instance, not the on-device DB.
 */
@RunWith(AndroidJUnit4::class)
class BackupRestoreInstrumentedTest {
    private lateinit var db: AppDatabase

    @Before
    fun setUp() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        db = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java).build()
    }

    @After
    fun tearDown() {
        db.close()
    }

    @Test
    fun exportThenImport_roundTripsAllData() = runBlocking {
        val userId = db.userDao().insert(
            UserProfile(name = "Test User", avatarColorHex = "#3D5AFE", themeMode = "DARK")
        )
        db.draftDao().insert(Draft(userId = userId, title = "Draft A", body = "Body A"))
        val jobId = db.bulkJobDao().insertJob(
            BulkJob(userId = userId, mode = JobMode.SAME_MESSAGE, status = JobStatus.COMPLETED, messagePreview = "hello")
        )
        db.bulkJobDao().insertItems(
            listOf(
                BulkJobItem(jobId = jobId, phoneNumber = "+910000000001", messageBody = "hello", status = ItemStatus.SENT)
            )
        )

        val json = BackupHelper.exportToJson(db)

        // importFromJson wipes all four tables internally before re-inserting, so this alone
        // simulates "restore onto a fresh install".
        BackupHelper.importFromJson(json, db)

        val users = db.userDao().getAll()
        val drafts = db.draftDao().getAll()
        val jobs = db.bulkJobDao().getAllJobs()
        val items = db.bulkJobDao().getAllItemsRaw()

        assertEquals(1, users.size)
        assertEquals("Test User", users[0].name)
        assertEquals("DARK", users[0].themeMode)
        assertEquals(1, drafts.size)
        assertEquals("Draft A", drafts[0].title)
        assertEquals(userId, drafts[0].userId)
        assertEquals(1, jobs.size)
        assertEquals(JobStatus.COMPLETED, jobs[0].status)
        assertEquals(userId, jobs[0].userId)
        assertEquals(1, items.size)
        assertEquals(ItemStatus.SENT, items[0].status)
        assertEquals(jobId, items[0].jobId)
    }
}
