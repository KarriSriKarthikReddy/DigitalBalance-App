package com.digitalbalance.app.data.reminder

import android.content.Context
import androidx.core.content.edit
import com.digitalbalance.app.domain.reminder.ReminderDeliveryKey
import com.digitalbalance.app.domain.reminder.ReminderSettings
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class ReminderPreferences(context: Context) {
    private val preferences = context.applicationContext.getSharedPreferences(
        PREFERENCES_NAME,
        Context.MODE_PRIVATE
    )
    private val mutableSettings = MutableStateFlow(readSettings())
    val settings: StateFlow<ReminderSettings> = mutableSettings.asStateFlow()

    fun currentSettings(): ReminderSettings = readSettings()

    fun setNotificationsEnabled(enabled: Boolean) = updateSettings {
        it.copy(notificationsEnabled = enabled)
    }

    fun setGoalAndLimitRemindersEnabled(enabled: Boolean) = updateSettings {
        it.copy(goalAndLimitRemindersEnabled = enabled)
    }

    fun setDailySummaryEnabled(enabled: Boolean) = updateSettings {
        it.copy(dailySummaryEnabled = enabled)
    }

    fun setFocusSuggestionsEnabled(enabled: Boolean) = updateSettings {
        it.copy(focusSuggestionsEnabled = enabled)
    }

    @Synchronized
    fun deliveredKeys(dateKey: String): Set<String> =
        preferences.getStringSet(KEY_DELIVERED, emptySet()).orEmpty()
            .filterTo(linkedSetOf()) { ReminderDeliveryKey.belongsToDate(it, dateKey) }

    @Synchronized
    fun markDelivered(key: ReminderDeliveryKey) {
        val retained = deliveredKeys(key.dateKey).toMutableSet().apply { add(key.storageKey) }
        preferences.edit { putStringSet(KEY_DELIVERED, retained) }
    }

    private fun updateSettings(transform: (ReminderSettings) -> ReminderSettings) {
        val updated = transform(readSettings())
        preferences.edit {
            putBoolean(KEY_NOTIFICATIONS, updated.notificationsEnabled)
            putBoolean(KEY_GOAL_LIMITS, updated.goalAndLimitRemindersEnabled)
            putBoolean(KEY_DAILY_SUMMARY, updated.dailySummaryEnabled)
            putBoolean(KEY_FOCUS_SUGGESTIONS, updated.focusSuggestionsEnabled)
        }
        mutableSettings.value = updated
    }

    private fun readSettings() = ReminderSettings(
        notificationsEnabled = preferences.getBoolean(KEY_NOTIFICATIONS, false),
        goalAndLimitRemindersEnabled = preferences.getBoolean(KEY_GOAL_LIMITS, true),
        dailySummaryEnabled = preferences.getBoolean(KEY_DAILY_SUMMARY, false),
        focusSuggestionsEnabled = preferences.getBoolean(KEY_FOCUS_SUGGESTIONS, false)
    )

    private companion object {
        const val PREFERENCES_NAME = "digital_balance_reminders"
        const val KEY_NOTIFICATIONS = "notifications_enabled"
        const val KEY_GOAL_LIMITS = "goal_limit_reminders_enabled"
        const val KEY_DAILY_SUMMARY = "daily_summary_enabled"
        const val KEY_FOCUS_SUGGESTIONS = "focus_suggestions_enabled"
        const val KEY_DELIVERED = "delivered_keys"
    }
}
