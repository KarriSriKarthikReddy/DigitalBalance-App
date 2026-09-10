package com.digitalbalance.app

import android.Manifest
import android.content.ActivityNotFoundException
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.core.content.ContextCompat
import androidx.lifecycle.ViewModelProvider
import com.digitalbalance.app.data.reminder.DailySummaryScheduler
import com.digitalbalance.app.data.reminder.ReminderNotificationManager
import com.digitalbalance.app.data.reminder.ReminderPreferences
import com.digitalbalance.app.data.usage.UsageStatsDataSource
import com.digitalbalance.app.data.local.DigitalBalanceDatabase
import com.digitalbalance.app.data.repository.UsageRepository
import com.digitalbalance.app.data.repository.GoalRepository
import com.digitalbalance.app.data.repository.FocusRepository
import com.digitalbalance.app.ui.DigitalBalanceApp
import com.digitalbalance.app.ui.theme.DigitalBalanceTheme
import com.digitalbalance.app.ui.usage.UsageViewModel
import com.digitalbalance.app.ui.focus.FocusViewModel
import com.digitalbalance.app.ui.insights.AnalyticsViewModel
import com.digitalbalance.app.ui.reminder.ReminderViewModel
import com.digitalbalance.app.domain.reminder.ReminderDestination
import kotlinx.coroutines.flow.MutableStateFlow

class MainActivity : ComponentActivity() {
    private val database by lazy { DigitalBalanceDatabase.getInstance(applicationContext) }
    private val usageRepository by lazy {
        UsageRepository(
            systemUsage = UsageStatsDataSource(applicationContext),
            usageDao = database.usageDao()
        )
    }
    private val focusRepository by lazy { FocusRepository(database.focusSessionDao()) }
    private val goalRepository by lazy { GoalRepository(database.goalDao()) }
    private val reminderPreferences by lazy { ReminderPreferences(applicationContext) }
    private val reminderNotifier by lazy { ReminderNotificationManager(applicationContext) }
    private val dailySummaryScheduler by lazy { DailySummaryScheduler(applicationContext) }
    private val notificationPermissionGranted = MutableStateFlow(false)
    private val notificationDestination = MutableStateFlow<ReminderDestination?>(null)

    private val notificationPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        notificationPermissionGranted.value = granted && reminderNotifier.canNotify()
        reminderViewModel.updateNotificationPermission(notificationPermissionGranted.value)
        if (granted) reminderViewModel.setNotificationsEnabled(true)
    }

    private val usageViewModel: UsageViewModel by lazy {
        ViewModelProvider(
            this,
            UsageViewModel.factory(
                usageRepository,
                goalRepository
            )
        )[UsageViewModel::class.java]
    }

    private val focusViewModel: FocusViewModel by lazy {
        ViewModelProvider(
            this,
            FocusViewModel.factory(focusRepository)
        )[FocusViewModel::class.java]
    }

    private val analyticsViewModel: AnalyticsViewModel by lazy {
        ViewModelProvider(
            this,
            AnalyticsViewModel.factory(usageRepository, focusRepository)
        )[AnalyticsViewModel::class.java]
    }

    private val reminderViewModel: ReminderViewModel by lazy {
        ViewModelProvider(
            this,
            ReminderViewModel.factory(
                usageRepository = usageRepository,
                goalRepository = goalRepository,
                focusRepository = focusRepository,
                preferences = reminderPreferences,
                notifier = reminderNotifier,
                scheduler = dailySummaryScheduler
            )
        )[ReminderViewModel::class.java]
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        updateNotificationPermission()
        notificationDestination.value = ReminderNotificationManager.destinationFrom(intent)
        setContent {
            DigitalBalanceTheme {
                val state by usageViewModel.uiState.collectAsState()
                val goalState by usageViewModel.goalUiState.collectAsState()
                val goalAlignmentState by usageViewModel.goalAlignmentUiState.collectAsState()
                val productivityState by usageViewModel.productivityUiState.collectAsState()
                val insightState by usageViewModel.insightUiState.collectAsState()
                val focusState by focusViewModel.uiState.collectAsState()
                val analyticsState by analyticsViewModel.uiState.collectAsState()
                val reminderSettings by reminderViewModel.settings.collectAsState()
                val notificationsAllowed by notificationPermissionGranted.collectAsState()
                val requestedDestination by notificationDestination.collectAsState()
                DigitalBalanceApp(
                    usageState = state,
                    goalState = goalState,
                    goalAlignmentState = goalAlignmentState,
                    productivityState = productivityState,
                    insightState = insightState,
                    focusState = focusState,
                    analyticsState = analyticsState,
                    reminderSettings = reminderSettings,
                    notificationPermissionGranted = notificationsAllowed,
                    notificationDestination = requestedDestination,
                    onOpenUsageSettings = ::openUsageAccessSettings,
                    onRefreshUsage = usageViewModel::refresh,
                    onCategoryChanged = usageViewModel::setCategory,
                    onSaveGoal = usageViewModel::saveGoal,
                    onDeleteGoal = usageViewModel::deleteGoal,
                    onSelectFocusPreset = focusViewModel::selectPreset,
                    onSelectFocusDuration = focusViewModel::selectSuggestedDuration,
                    onSelectCustomFocusDuration = focusViewModel::selectCustomDuration,
                    onCustomFocusDurationChanged = focusViewModel::setCustomDuration,
                    onStartFocus = focusViewModel::start,
                    onPauseFocus = focusViewModel::pause,
                    onResumeFocus = focusViewModel::resume,
                    onStopFocus = focusViewModel::stopEarly,
                    onFocusReflectionChanged = focusViewModel::setReflection,
                    onFocusDone = focusViewModel::done,
                    onStartAnotherFocus = focusViewModel::startAnother,
                    onFocusTick = focusViewModel::tick,
                    onPrepareFocusSuggestion = focusViewModel::applySuggestion,
                    onAnalyticsPeriodSelected = analyticsViewModel::selectPeriod,
                    onRequestEnableNotifications = ::requestEnableNotifications,
                    onDisableNotifications = { reminderViewModel.setNotificationsEnabled(false) },
                    onGoalAndLimitRemindersChanged = reminderViewModel::setGoalAndLimitRemindersEnabled,
                    onDailySummaryChanged = reminderViewModel::setDailySummaryEnabled,
                    onFocusSuggestionsChanged = reminderViewModel::setFocusSuggestionsEnabled,
                    onNotificationDestinationHandled = { notificationDestination.value = null }
                )
            }
        }
    }

    override fun onResume() {
        super.onResume()
        usageViewModel.refresh()
        analyticsViewModel.refreshDate()
        updateNotificationPermission()
        reminderViewModel.syncSchedule()
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        notificationDestination.value = ReminderNotificationManager.destinationFrom(intent)
    }

    private fun requestEnableNotifications() {
        if (
            Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU ||
            ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) ==
            PackageManager.PERMISSION_GRANTED
        ) {
            reminderViewModel.setNotificationsEnabled(true)
            updateNotificationPermission()
        } else {
            notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
    }

    private fun updateNotificationPermission() {
        val allowed = reminderNotifier.canNotify()
        notificationPermissionGranted.value = allowed
        reminderViewModel.updateNotificationPermission(allowed)
    }

    private fun openUsageAccessSettings() {
        try {
            startActivity(Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS))
        } catch (_: ActivityNotFoundException) {
            startActivity(Intent(Settings.ACTION_SETTINGS))
        }
    }
}
