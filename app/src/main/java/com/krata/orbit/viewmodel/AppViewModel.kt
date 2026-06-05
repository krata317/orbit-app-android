package com.krata.orbit.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.krata.orbit.data.local.UserPreferencesRepository
import com.krata.orbit.data.local.NotifPrefs
import com.krata.orbit.notifications.NotificationHelper
import com.krata.orbit.notifications.NotificationScheduler
import com.krata.orbit.ui.theme.AppTheme
import com.krata.orbit.workers.MidnightResetWorker
import com.krata.orbit.workers.BudgetMonthResetWorker
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

data class AppUiState(
    val username: String        = "",
    val isFirstLaunch: Boolean  = true,
    val appTheme: AppTheme      = AppTheme.DARK,
    val isReady: Boolean        = false
)

class AppViewModel(application: Application) : AndroidViewModel(application) {

    private val prefs = UserPreferencesRepository(application)

    private val _state = MutableStateFlow(AppUiState())
    val state: StateFlow<AppUiState> = _state.asStateFlow()

    init {
        // Create notification channels immediately
        NotificationHelper.createChannels(application)

        // Schedule background workers
        MidnightResetWorker.schedule(application)
        BudgetMonthResetWorker.schedule(application)

        // Observe preferences
        viewModelScope.launch {
            combine(
                prefs.usernameFlow,
                prefs.isFirstLaunchFlow,
                prefs.appThemeFlow
            ) { username, isFirst, themeStr ->
                AppUiState(
                    username      = username,
                    isFirstLaunch = isFirst,
                    appTheme      = when (themeStr) {
                        "LIGHT"  -> AppTheme.LIGHT
                        "SYSTEM" -> AppTheme.SYSTEM
                        else     -> AppTheme.DARK
                    },
                    isReady       = true
                )
            }.collect { _state.value = it }
        }
    }

    fun setUsername(name: String) {
        viewModelScope.launch {
            prefs.setUsername(name.trim())
        }
    }

    fun completeOnboarding(username: String) {
        viewModelScope.launch {
            prefs.setUsername(username.trim())
            prefs.setFirstLaunchDone()
        }
    }

    fun setTheme(theme: AppTheme) {
        viewModelScope.launch {
            prefs.setAppTheme(theme.name)
        }
    }

    fun resetApp() {
        viewModelScope.launch {
            val context = getApplication<Application>()
            // Clear prefs
            prefs.clearAll()
            // Clear database
            com.krata.orbit.data.local.AppDatabase.getInstance(context).clearAllTables()
        }
    }

    fun applyNotifPrefs(notifPrefs: NotifPrefs) {
        val context = getApplication<Application>()
        viewModelScope.launch {
            prefs.setNotifPrefs(notifPrefs)
            // Re-schedule alarms
            if (notifPrefs.tasksEnabled) {
                NotificationScheduler.scheduleTasksNotification(context, notifPrefs.tasksHour, notifPrefs.tasksMinute)
            } else {
                NotificationScheduler.cancelTasksNotification(context)
            }
            if (notifPrefs.potdEnabled) {
                NotificationScheduler.schedulePotdNotification(context, notifPrefs.potdHour, notifPrefs.potdMinute)
            } else {
                NotificationScheduler.cancelPotdNotification(context)
            }
        }
    }

    fun getNotifPrefsFlow() = prefs.notifPrefsFlow
}
