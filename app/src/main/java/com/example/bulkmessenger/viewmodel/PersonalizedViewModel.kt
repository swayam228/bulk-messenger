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
import com.example.bulkmessenger.util.SessionPrefs
import com.example.bulkmessenger.worker.SmsSendWorker
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.util.UUID
import java.util.concurrent.TimeUnit

data class PersonalizedRow(
    val rowId: String = UUID.randomUUID().toString(),
    val phoneNumber: String = "",
    val message: String = ""
)

data class PersonalizedUiState(
    val rows: List<PersonalizedRow> = emptyList(),
    val lastCreatedJobId: Long? = null
)


class PersonalizedViewModel(app: Application) : AndroidViewModel(app) {
    private val repo = MessageRepository(AppDatabase.getInstance(app))
    private val userId = SessionPrefs.getActiveUserId(app) ?: -1L

    private val _state = MutableStateFlow(PersonalizedUiState())
    val state: StateFlow<PersonalizedUiState> = _state.asStateFlow()

    fun getRow(rowId: String): PersonalizedRow? = _state.value.rows.find { it.rowId == rowId }

    /** Adds a new row, or updates an existing one if [rowId] matches one already in the list. */
    fun saveRow(rowId: String?, phoneNumber: String, message: String) {
        val rows = _state.value.rows
        _state.value = if (rowId != null && rows.any { it.rowId == rowId }) {
            _state.value.copy(rows = rows.map {
                if (it.rowId == rowId) it.copy(phoneNumber = phoneNumber, message = message) else it
            })
        } else {
            _state.value.copy(rows = rows + PersonalizedRow(phoneNumber = phoneNumber, message = message))
        }
    }

    fun removeRow(rowId: String) {
        _state.value = _state.value.copy(rows = _state.value.rows.filterNot { it.rowId == rowId })
    }

    /** Splits a pasted, newline-separated block of numbers into blank-message rows, skipping blanks and numbers already in the list. */
    fun addNumbersAsRows(rawText: String): AddNumbersResult {
        val existing = _state.value.rows.map { it.phoneNumber }.toMutableSet()
        var added = 0
        var skipped = 0
        val toAdd = mutableListOf<PersonalizedRow>()
        rawText.lineSequence()
            .map { it.trim() }
            .filter { it.isNotEmpty() }
            .forEach { number ->
                if (existing.add(number)) {
                    toAdd.add(PersonalizedRow(phoneNumber = number))
                    added++
                } else {
                    skipped++
                }
            }
        if (toAdd.isNotEmpty()) {
            _state.value = _state.value.copy(rows = _state.value.rows + toAdd)
        }
        return AddNumbersResult(added, skipped)
    }

    /** Imports CSV lines of the form "phoneNumber,message" (one per line), appending to rows. */
    fun importCsv(csvText: String) {
        val imported = csvText.lineSequence()
            .map { it.trim() }
            .filter { it.isNotEmpty() }
            .mapNotNull { line ->
                val idx = line.indexOf(',')
                if (idx <= 0) return@mapNotNull null
                val number = line.substring(0, idx).trim()
                val message = line.substring(idx + 1).trim()
                if (number.isEmpty() || message.isEmpty()) null
                else PersonalizedRow(phoneNumber = number, message = message)
            }.toList()
        if (imported.isNotEmpty()) {
            _state.value = _state.value.copy(rows = _state.value.rows + imported)
        }
    }

    fun sendAll(scheduledAtMillis: Long? = null, simSubscriptionId: Int? = null) {
        val validRows = _state.value.rows.filter { it.phoneNumber.isNotBlank() && it.message.isNotBlank() }
        if (validRows.isEmpty()) return

        viewModelScope.launch {
            val recipientPairs = validRows.map { it.phoneNumber to it.message }
            val jobId = repo.createJob(userId, JobMode.PERSONALIZED, recipientPairs, scheduledAtMillis, simSubscriptionId)

            val delayMs = scheduledAtMillis?.minus(System.currentTimeMillis())?.coerceAtLeast(0) ?: 0
            val requestBuilder = OneTimeWorkRequestBuilder<SmsSendWorker>()
                .setInputData(workDataOf(SmsSendWorker.KEY_JOB_ID to jobId))
            if (delayMs > 0) requestBuilder.setInitialDelay(delayMs, TimeUnit.MILLISECONDS)
            WorkManager.getInstance(getApplication()).enqueue(requestBuilder.build())

            _state.value = PersonalizedUiState(lastCreatedJobId = jobId)
        }
    }
}
