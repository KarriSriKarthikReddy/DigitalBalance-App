package com.digitalbalance.app.domain.reminder

import com.digitalbalance.app.domain.category.AppCategory
import com.digitalbalance.app.domain.goal.GoalProgress

data class ReminderSettings(
    val notificationsEnabled: Boolean = false,
    val goalAndLimitRemindersEnabled: Boolean = true,
    val dailySummaryEnabled: Boolean = false,
    val focusSuggestionsEnabled: Boolean = false
)

enum class ReminderType {
    LimitApproaching,
    LimitExceeded,
    ProductiveTarget,
    DailySummary,
    FocusSuggestion
}

enum class ReminderDestination {
    Home,
    Apps,
    Goals,
    Insights,
    Focus
}

data class ReminderDeliveryKey(
    val dateKey: String,
    val type: ReminderType,
    val identifier: String,
    val threshold: String
) {
    val storageKey: String = listOf(
        dateKey,
        type.name,
        identifier.replace('|', '_'),
        threshold
    ).joinToString("|")

    companion object {
        fun belongsToDate(storageKey: String, dateKey: String): Boolean =
            storageKey.startsWith("$dateKey|")
    }
}

data class ReminderAppUsage(
    val packageName: String,
    val appName: String,
    val durationMillis: Long,
    val category: AppCategory
)

data class ReminderEvaluationInput(
    val dateKey: String,
    val localHour: Int,
    val settings: ReminderSettings,
    val notificationPermissionGranted: Boolean,
    val apps: List<ReminderAppUsage>,
    val goalProgress: List<GoalProgress>,
    val completedFocusSessionsToday: Int,
    val focusSessionActive: Boolean,
    val deliveredKeys: Set<String>
)

data class ReminderCandidate(
    val deliveryKey: ReminderDeliveryKey,
    val title: String,
    val message: String,
    val destination: ReminderDestination
)
