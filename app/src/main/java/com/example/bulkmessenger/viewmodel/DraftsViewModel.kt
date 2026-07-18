package com.example.bulkmessenger.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.bulkmessenger.data.AppDatabase
import com.example.bulkmessenger.data.Draft
import com.example.bulkmessenger.data.MessageRepository
import com.example.bulkmessenger.util.SessionPrefs
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class DraftsViewModel(app: Application) : AndroidViewModel(app) {
    private val repo = MessageRepository(AppDatabase.getInstance(app))
    private val userId = SessionPrefs.getActiveUserId(app) ?: -1L

    val drafts: StateFlow<List<Draft>> = repo.drafts.observeAll(userId)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun save(title: String, body: String, existingId: Long? = null) {
        viewModelScope.launch { repo.saveDraft(userId, title, body, existingId) }
    }

    fun delete(draft: Draft) {
        viewModelScope.launch { repo.drafts.delete(draft) }
    }
}
