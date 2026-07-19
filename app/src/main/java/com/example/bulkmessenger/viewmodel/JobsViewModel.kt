package com.example.bulkmessenger.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.workDataOf
import com.example.bulkmessenger.data.AppDatabase
import com.example.bulkmessenger.data.BulkJob
import com.example.bulkmessenger.data.BulkJobItem
import com.example.bulkmessenger.data.ItemStatus
import com.example.bulkmessenger.data.JobStatus
import com.example.bulkmessenger.data.MessageRepository
import com.example.bulkmessenger.util.SessionPrefs
import com.example.bulkmessenger.util.startOfTodayMillis
import com.example.bulkmessenger.worker.SmsSendWorker
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

data class DateSentCount(val dateLabel: String, val count: Int)

class JobsViewModel(app: Application) : AndroidViewModel(app) {
    private val repo = MessageRepository(AppDatabase.getInstance(app))
    private val userId = SessionPrefs.getActiveUserId(app) ?: -1L

    /** Null until the first emission arrives — lets the UI distinguish "still loading" from "genuinely empty". */
    val jobs: StateFlow<List<BulkJob>?> = repo.jobs.observeJobs(userId)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    private val itemFlows = mutableMapOf<Long, StateFlow<List<BulkJobItem>>>()

    fun itemsFor(jobId: Long): StateFlow<List<BulkJobItem>> =
        itemFlows.getOrPut(jobId) {
            repo.jobs.observeItems(jobId)
                .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
        }

    private val allItems: StateFlow<List<BulkJobItem>> = repo.jobs.observeAllItems(userId)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val totalSentCount: StateFlow<Int> = allItems
        .map { items -> items.count { it.status == ItemStatus.SENT } }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)

    val sentByDate: StateFlow<List<DateSentCount>> = allItems
        .map { items ->
            val dayKeyFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
            val dayLabelFormat = SimpleDateFormat("dd MMM yyyy", Locale.getDefault())
            items.filter { it.status == ItemStatus.SENT && it.sentAt != null }
                .groupBy { dayKeyFormat.format(Date(it.sentAt!!)) }
                .toSortedMap(compareByDescending { it })
                .map { (key, group) ->
                    val label = dayLabelFormat.format(dayKeyFormat.parse(key)!!)
                    DateSentCount(label, group.size)
                }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    /** How many times each number has been messaged today, shown beside the number in Job History. */
    val sentTodayCounts: StateFlow<Map<String, Int>> = allItems
        .map { items ->
            val startOfDay = startOfTodayMillis()
            items.filter { it.status == ItemStatus.SENT && (it.sentAt ?: 0L) >= startOfDay }
                .groupingBy { it.phoneNumber }
                .eachCount()
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyMap())

    /**
     * Resets a job's FAILED items back to PENDING and re-enqueues the sender worker for just
     * those — e.g. after hitting a carrier's daily SMS cap partway through a batch, or picking a
     * different SIM to finish the rest on.
     */
    fun retryFailedItems(jobId: Long, simSubscriptionId: Int?) {
        viewModelScope.launch {
            val dao = repo.jobs
            val job = dao.getJob(jobId) ?: return@launch
            val failedItems = dao.getItems(jobId).filter { it.status == ItemStatus.FAILED }
            if (failedItems.isEmpty()) return@launch

            failedItems.forEach { item ->
                dao.updateItem(item.copy(status = ItemStatus.PENDING, errorReason = null))
            }
            dao.updateJob(
                job.copy(
                    status = JobStatus.QUEUED,
                    simSubscriptionId = simSubscriptionId ?: job.simSubscriptionId
                )
            )

            val request = OneTimeWorkRequestBuilder<SmsSendWorker>()
                .setInputData(workDataOf(SmsSendWorker.KEY_JOB_ID to jobId))
                .build()
            WorkManager.getInstance(getApplication()).enqueue(request)
        }
    }
}
