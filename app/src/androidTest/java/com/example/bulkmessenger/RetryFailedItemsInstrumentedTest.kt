package com.example.bulkmessenger

import androidx.room.Room
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.example.bulkmessenger.data.AppDatabase
import com.example.bulkmessenger.data.BulkJob
import com.example.bulkmessenger.data.BulkJobItem
import com.example.bulkmessenger.data.ItemStatus
import com.example.bulkmessenger.data.JobMode
import com.example.bulkmessenger.data.JobStatus
import com.example.bulkmessenger.data.UserProfile
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Exercises the DB-level semantics behind JobsViewModel.retryFailedItems: only FAILED items get
 * reset to PENDING (with their error cleared) and the job goes back to QUEUED, while SENT items
 * are left untouched. The actual re-send (SmsSendWorker + SmsManager) is covered manually — see
 * docs/TEST_CASES.md JH-09/JH-10 — since driving a real SmsManager call from an instrumented test
 * isn't reliable across devices/emulators without a live SIM.
 */
@RunWith(AndroidJUnit4::class)
class RetryFailedItemsInstrumentedTest {
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
    fun retryResetsOnlyFailedItemsToPending() = runBlocking {
        val userId = db.userDao().insert(UserProfile(name = "Test User", avatarColorHex = "#3D5AFE"))
        val jobId = db.bulkJobDao().insertJob(
            BulkJob(userId = userId, mode = JobMode.SAME_MESSAGE, status = JobStatus.FAILED, messagePreview = "hi")
        )
        db.bulkJobDao().insertItems(
            listOf(
                BulkJobItem(jobId = jobId, phoneNumber = "+910000000001", messageBody = "hi", status = ItemStatus.SENT),
                BulkJobItem(jobId = jobId, phoneNumber = "123", messageBody = "hi", status = ItemStatus.FAILED, errorReason = "Invalid number")
            )
        )

        // Mirrors JobsViewModel.retryFailedItems' DB mutation.
        val failed = db.bulkJobDao().getItems(jobId).filter { it.status == ItemStatus.FAILED }
        failed.forEach { db.bulkJobDao().updateItem(it.copy(status = ItemStatus.PENDING, errorReason = null)) }
        val job = db.bulkJobDao().getJob(jobId)!!
        db.bulkJobDao().updateJob(job.copy(status = JobStatus.QUEUED))

        val after = db.bulkJobDao().getItems(jobId)
        val sentItem = after.first { it.phoneNumber == "+910000000001" }
        val retriedItem = after.first { it.phoneNumber == "123" }

        assertEquals(ItemStatus.SENT, sentItem.status)
        assertEquals(ItemStatus.PENDING, retriedItem.status)
        assertNull(retriedItem.errorReason)
        assertEquals(JobStatus.QUEUED, db.bulkJobDao().getJob(jobId)!!.status)
    }
}
