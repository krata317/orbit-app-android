package com.krata.orbit.data.local

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.*
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import java.io.IOException

val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "orbit_prefs")

object PreferenceKeys {
    val USERNAME              = stringPreferencesKey("username")
    val IS_FIRST_LAUNCH       = booleanPreferencesKey("is_first_launch")
    val APP_THEME             = stringPreferencesKey("app_theme")   // SYSTEM / DARK / LIGHT

    // Notification preferences
    val NOTIF_TASKS_ENABLED   = booleanPreferencesKey("notif_tasks_enabled")
    val NOTIF_TASKS_HOUR      = intPreferencesKey("notif_tasks_hour")
    val NOTIF_TASKS_MINUTE    = intPreferencesKey("notif_tasks_minute")

    val NOTIF_POTD_ENABLED    = booleanPreferencesKey("notif_potd_enabled")
    val NOTIF_POTD_HOUR       = intPreferencesKey("notif_potd_hour")
    val NOTIF_POTD_MINUTE     = intPreferencesKey("notif_potd_minute")

    val NOTIF_EVENTS_ENABLED  = booleanPreferencesKey("notif_events_enabled")
    val NOTIF_EVENTS_MINUTES  = intPreferencesKey("notif_events_minutes_before") // minutes before

    val NOTIF_CONTESTS_ENABLED = booleanPreferencesKey("notif_contests_enabled")
    val NOTIF_CONTESTS_MINUTES = intPreferencesKey("notif_contests_minutes_before")

    // Budget
    val DEFAULT_BUDGET        = floatPreferencesKey("default_budget")
}

class UserPreferencesRepository(private val context: Context) {

    val usernameFlow: Flow<String> = context.dataStore.data
        .catch { if (it is IOException) emit(emptyPreferences()) else throw it }
        .map { it[PreferenceKeys.USERNAME] ?: "" }

    val isFirstLaunchFlow: Flow<Boolean> = context.dataStore.data
        .catch { if (it is IOException) emit(emptyPreferences()) else throw it }
        .map { it[PreferenceKeys.IS_FIRST_LAUNCH] ?: true }

    val appThemeFlow: Flow<String> = context.dataStore.data
        .catch { if (it is IOException) emit(emptyPreferences()) else throw it }
        .map { it[PreferenceKeys.APP_THEME] ?: "DARK" }

    val notifPrefsFlow: Flow<NotifPrefs> = context.dataStore.data
        .catch { if (it is IOException) emit(emptyPreferences()) else throw it }
        .map { prefs ->
            NotifPrefs(
                tasksEnabled        = prefs[PreferenceKeys.NOTIF_TASKS_ENABLED] ?: false,
                tasksHour           = prefs[PreferenceKeys.NOTIF_TASKS_HOUR] ?: 8,
                tasksMinute         = prefs[PreferenceKeys.NOTIF_TASKS_MINUTE] ?: 0,
                potdEnabled         = prefs[PreferenceKeys.NOTIF_POTD_ENABLED] ?: false,
                potdHour            = prefs[PreferenceKeys.NOTIF_POTD_HOUR] ?: 9,
                potdMinute          = prefs[PreferenceKeys.NOTIF_POTD_MINUTE] ?: 0,
                eventsEnabled       = prefs[PreferenceKeys.NOTIF_EVENTS_ENABLED] ?: false,
                eventsMinutesBefore = prefs[PreferenceKeys.NOTIF_EVENTS_MINUTES] ?: 30,
                contestsEnabled     = prefs[PreferenceKeys.NOTIF_CONTESTS_ENABLED] ?: false,
                contestsMinutesBefore = prefs[PreferenceKeys.NOTIF_CONTESTS_MINUTES] ?: 60
            )
        }

    suspend fun setUsername(name: String) {
        context.dataStore.edit { it[PreferenceKeys.USERNAME] = name }
    }

    suspend fun setFirstLaunchDone() {
        context.dataStore.edit { it[PreferenceKeys.IS_FIRST_LAUNCH] = false }
    }

    suspend fun setAppTheme(theme: String) {
        context.dataStore.edit { it[PreferenceKeys.APP_THEME] = theme }
    }

    suspend fun setNotifPrefs(prefs: NotifPrefs) {
        context.dataStore.edit {
            it[PreferenceKeys.NOTIF_TASKS_ENABLED]      = prefs.tasksEnabled
            it[PreferenceKeys.NOTIF_TASKS_HOUR]         = prefs.tasksHour
            it[PreferenceKeys.NOTIF_TASKS_MINUTE]       = prefs.tasksMinute
            it[PreferenceKeys.NOTIF_POTD_ENABLED]       = prefs.potdEnabled
            it[PreferenceKeys.NOTIF_POTD_HOUR]          = prefs.potdHour
            it[PreferenceKeys.NOTIF_POTD_MINUTE]        = prefs.potdMinute
            it[PreferenceKeys.NOTIF_EVENTS_ENABLED]     = prefs.eventsEnabled
            it[PreferenceKeys.NOTIF_EVENTS_MINUTES]     = prefs.eventsMinutesBefore
            it[PreferenceKeys.NOTIF_CONTESTS_ENABLED]   = prefs.contestsEnabled
            it[PreferenceKeys.NOTIF_CONTESTS_MINUTES]   = prefs.contestsMinutesBefore
        }
    }

    suspend fun clearAll() {
        context.dataStore.edit { it.clear() }
    }
}

data class NotifPrefs(
    val tasksEnabled: Boolean = false,
    val tasksHour: Int = 8,
    val tasksMinute: Int = 0,
    val potdEnabled: Boolean = false,
    val potdHour: Int = 9,
    val potdMinute: Int = 0,
    val eventsEnabled: Boolean = false,
    val eventsMinutesBefore: Int = 30,
    val contestsEnabled: Boolean = false,
    val contestsMinutesBefore: Int = 60
)
