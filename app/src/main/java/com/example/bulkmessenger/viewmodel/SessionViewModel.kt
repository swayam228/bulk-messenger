package com.example.bulkmessenger.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.bulkmessenger.data.AppDatabase
import com.example.bulkmessenger.data.UserProfile
import com.example.bulkmessenger.util.SessionPrefs
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

enum class ThemeMode { SYSTEM, LIGHT, DARK }

/**
 * Owns the active profile and everything that's scoped to it (theme, default SIM). Replaces the
 * old standalone ThemeViewModel now that theme lives per-profile instead of app-global.
 */
class SessionViewModel(app: Application) : AndroidViewModel(app) {
    private val userDao = AppDatabase.getInstance(app).userDao()

    private val _activeUserId = MutableStateFlow(SessionPrefs.getActiveUserId(app))
    val activeUserId: StateFlow<Long?> = _activeUserId.asStateFlow()

    val users: StateFlow<List<UserProfile>> = userDao.observeAll()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val activeUser: StateFlow<UserProfile?> = combine(users, _activeUserId) { list, id ->
        list.find { it.id == id }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    /** Flips true on Room's first response — lets the splash screen wait for a real answer instead of guessing. */
    val isReady: StateFlow<Boolean> = users
        .map { true }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), false)

    val themeMode: StateFlow<ThemeMode> = activeUser
        .map { user ->
            user?.themeMode?.let { runCatching { ThemeMode.valueOf(it) }.getOrDefault(ThemeMode.SYSTEM) }
                ?: ThemeMode.SYSTEM
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), ThemeMode.SYSTEM)

    fun switchUser(userId: Long) {
        SessionPrefs.setActiveUserId(getApplication(), userId)
        _activeUserId.value = userId
    }

    fun createUser(name: String, avatarColorHex: String, onCreated: () -> Unit = {}) {
        viewModelScope.launch {
            val id = userDao.insert(UserProfile(name = name.trim(), avatarColorHex = avatarColorHex))
            switchUser(id)
            onCreated()
        }
    }

    fun cycleTheme() {
        val current = activeUser.value ?: return
        val next = when (ThemeMode.valueOf(current.themeMode)) {
            ThemeMode.SYSTEM -> ThemeMode.LIGHT
            ThemeMode.LIGHT -> ThemeMode.DARK
            ThemeMode.DARK -> ThemeMode.SYSTEM
        }
        viewModelScope.launch { userDao.update(current.copy(themeMode = next.name)) }
    }

    fun setDefaultSim(subscriptionId: Int?) {
        val current = activeUser.value ?: return
        viewModelScope.launch { userDao.update(current.copy(defaultSimSubscriptionId = subscriptionId)) }
    }
}
