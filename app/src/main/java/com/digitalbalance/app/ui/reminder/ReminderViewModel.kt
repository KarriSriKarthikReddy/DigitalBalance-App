package com.digitalbalance.app.ui.reminder

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.digitalbalance.app.data.reminder.DailySummaryScheduler
import com.digitalbalance.app.data.reminder.ReminderNotificationManager
import com.digitalbalance.app.data.reminder.ReminderPreferences
import com.digitalbalance.app.data.reminder.ReminderSnapshotFactory
import com.digitalbalance.app.data.repository.CurrentDayUsage
import com.digitalbalance.app.data.repository.FocusRepository
import com.digitalbalance.app.data.repository.GoalRepository
import com.digitalbalance.app.data.repository.UsageRepository
import com.digitalbalance.app.domain.category.AppCategory
import com.digitalbalance.app.domain.category.categoryWithOverride
import com.digitalbalance.app.domain.focus.FocusSession
import com.digitalbalance.app.domain.goal.DigitalGoal
import com.digitalbalance.app.domain.reminder.DailySummary
import com.digitalbalance.app.domain.reminder.ReminderEngine
import com.digitalbalance.app.domain.reminder.ReminderEvaluationInput
import com.digitalbalance.app.domain.reminder.ReminderSettings
import java.util.Calendar
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch

class ReminderViewModel(
    usageRepository: UsageRepository,
    goalRepository: GoalRepository,
    focusRepository: FocusRepository,
    private val preferences: ReminderPreferences,
    private val notifier: ReminderNotificationManager,
    private val scheduler: DailySummaryScheduler,
    private val snapshotFactory: ReminderSnapshotFactory = ReminderSnapshotFactory(),
    private val engine: ReminderEngine = ReminderEngine(),
    private val clock: () -> Long = System::currentTimeMillis
) : ViewModel() {
    val settings: StateFlow<ReminderSettings> = preferences.settings
    private val notificationPermissionGranted = MutableStateFlow(notifier.canNotify())
    private val mutableDailySummary = MutableStateFlow<DailySummary?>(null)
    val dailySummary: StateFlow<DailySummary?> = mutableDailySummary

    private val sources = combine(
        usageRepository.currentTodayUsage,
        usageRepository.observeCategoryOverrides(),
        goalRepository.observeGoals(),
        focusRepository.observeSessions()
    ) { today, overrides, goals, focusSessions ->
        ReminderSources(today, overrides, goals, focusSessions)
    }

    init {
        scheduler.sync(preferences.currentSettings(), clock())
        viewModelScope.launch(Dispatchers.Default) {
            combine(sources, settings, notificationPermissionGranted) { sources, settings, allowed ->
                EvaluationSources(sources, settings, allowed)
            }.collect { evaluation -> evaluate(evaluation) }
        }
    }

    fun setNotificationsEnabled(enabled: Boolean) {
        if (enabled) notifier.ensureChannel()
        preferences.setNotificationsEnabled(enabled)
        scheduler.sync(preferences.currentSettings(), clock())
    }

    fun setGoalAndLimitRemindersEnabled(enabled: Boolean) {
        preferences.setGoalAndLimitRemindersEnabled(enabled)
    }

    fun setDailySummaryEnabled(enabled: Boolean) {
        preferences.setDailySummaryEnabled(enabled)
        scheduler.sync(preferences.currentSettings(), clock())
    }

    fun setFocusSuggestionsEnabled(enabled: Boolean) {
        preferences.setFocusSuggestionsEnabled(enabled)
    }

    fun updateNotificationPermission(granted: Boolean = notifier.canNotify()) {
        notificationPermissionGranted.value = granted
    }

    fun syncSchedule() {
        scheduler.sync(preferences.currentSettings(), clock())
    }

    private fun evaluate(evaluation: EvaluationSources) {
        val today = evaluation.sources.today ?: return
        val apps = today.usage.apps.map { app ->
            app.copy(
                category = categoryWithOverride(
                    app.category,
                    evaluation.sources.overrides[app.packageName]
                )
            )
        }
        val snapshot = snapshotFactory.create(
            dateKey = today.dateKey,
            apps = apps,
            totalForegroundDurationMillis = today.usage.totalForegroundDurationMillis,
            goals = evaluation.sources.goals,
            focusSessions = evaluation.sources.focusSessions
        )
        mutableDailySummary.value = snapshot.dailySummary
        val now = clock()
        val candidates = engine.evaluate(
            ReminderEvaluationInput(
                dateKey = snapshot.dateKey,
                localHour = Calendar.getInstance().apply { timeInMillis = now }
                    .get(Calendar.HOUR_OF_DAY),
                settings = evaluation.settings,
                notificationPermissionGranted = evaluation.notificationPermissionGranted,
                apps = snapshot.apps,
                goalProgress = snapshot.goalProgress,
                completedFocusSessionsToday = snapshot.completedFocusSessionsToday,
                focusSessionActive = snapshot.focusSessionActive,
                deliveredKeys = preferences.deliveredKeys(snapshot.dateKey)
            )
        )
        candidates.forEach { candidate ->
            if (notifier.notify(candidate)) preferences.markDelivered(candidate.deliveryKey)
        }
    }

    private data class ReminderSources(
        val today: CurrentDayUsage?,
        val overrides: Map<String, AppCategory>,
        val goals: List<DigitalGoal>,
        val focusSessions: List<FocusSession>
    )

    private data class EvaluationSources(
        val sources: ReminderSources,
        val settings: ReminderSettings,
        val notificationPermissionGranted: Boolean
    )

    companion object {
        fun factory(
            usageRepository: UsageRepository,
            goalRepository: GoalRepository,
            focusRepository: FocusRepository,
            preferences: ReminderPreferences,
            notifier: ReminderNotificationManager,
            scheduler: DailySummaryScheduler
        ): ViewModelProvider.Factory = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                require(modelClass.isAssignableFrom(ReminderViewModel::class.java))
                return ReminderViewModel(
                    usageRepository,
                    goalRepository,
                    focusRepository,
                    preferences,
                    notifier,
                    scheduler
                ) as T
            }
        }
    }
}
