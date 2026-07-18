package com.example.bulkmessenger.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.workDataOf
import com.example.bulkmessenger.data.AppDatabase
import com.example.bulkmessenger.data.JobMode
import com.example.bulkmessenger.data.MessageRepository
import com.example.bulkmessenger.util.PickedContact
import com.example.bulkmessenger.util.SessionPrefs
import com.example.bulkmessenger.worker.SmsSendWorker
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.util.concurrent.TimeUnit

data class BroadcastUiState(
    val recipients: List<PickedContact> = emptyList(),
    val message: String = "",
    val lastCreatedJobId: Long? = null
)

class BroadcastViewModel(app: Application) : AndroidViewModel(app) {
    private val repo = MessageRepository(AppDatabase.getInstance(app))
    private val userId = SessionPrefs.getActiveUserId(app) ?: -1L

    private val _state = MutableStateFlow(BroadcastUiState())
    val state: StateFlow<BroadcastUiState> = _state.asStateFlow()

    fun addRecipient(contact: PickedContact) {
        _state.value = _state.value.copy(recipients = _state.value.recipients + contact)
    }

    fun removeRecipient(contact: PickedContact) {
        _state.value = _state.value.copy(recipients = _state.value.recipients - contact)
    }

    fun addManualNumber(number: String) {
        if (number.isBlank()) return
        addRecipient(PickedContact(name = null, phoneNumber = number.trim()))
    }

    fun updateMessage(text: String) {
        _state.value = _state.value.copy(message = text)
    }

    fun loadDraftText(text: String) {
        updateMessage(text)
    }

    /** Creates the job in Room, then enqueues the throttled sender worker (optionally delayed until [scheduledAtMillis]). */
    fun sendToAll(scheduledAtMillis: Long? = null, simSubscriptionId: Int? = null) {
        val current = _state.value
        if (current.recipients.isEmpty() || current.message.isBlank()) return

        viewModelScope.launch {
            val recipientPairs = current.recipients.map { it.phoneNumber to current.message }
            val jobId = repo.createJob(userId, JobMode.SAME_MESSAGE, recipientPairs, scheduledAtMillis, simSubscriptionId)

            val delayMs = scheduledAtMillis?.minus(System.currentTimeMillis())?.coerceAtLeast(0) ?: 0
            val requestBuilder = OneTimeWorkRequestBuilder<SmsSendWorker>()
                .setInputData(workDataOf(SmsSendWorker.KEY_JOB_ID to jobId))
            if (delayMs > 0) requestBuilder.setInitialDelay(delayMs, TimeUnit.MILLISECONDS)
            WorkManager.getInstance(getApplication()).enqueue(requestBuilder.build())

            _state.value = BroadcastUiState(lastCreatedJobId = jobId)
        }
    }
}
