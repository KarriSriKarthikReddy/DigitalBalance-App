package com.digitalbalance.app

import android.content.ActivityNotFoundException
import android.content.Intent
import android.os.Bundle
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.lifecycle.ViewModelProvider
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

class MainActivity : ComponentActivity() {
    private val database by lazy { DigitalBalanceDatabase.getInstance(applicationContext) }
    private val usageRepository by lazy {
        UsageRepository(
            systemUsage = UsageStatsDataSource(applicationContext),
            usageDao = database.usageDao()
        )
    }
    private val focusRepository by lazy { FocusRepository(database.focusSessionDao()) }

    private val usageViewModel: UsageViewModel by lazy {
        ViewModelProvider(
            this,
            UsageViewModel.factory(
                usageRepository,
                GoalRepository(database.goalDao())
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

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            DigitalBalanceTheme {
                val state by usageViewModel.uiState.collectAsState()
                val goalState by usageViewModel.goalUiState.collectAsState()
                val goalAlignmentState by usageViewModel.goalAlignmentUiState.collectAsState()
                val productivityState by usageViewModel.productivityUiState.collectAsState()
                val insightState by usageViewModel.insightUiState.collectAsState()
                val focusState by focusViewModel.uiState.collectAsState()
                val analyticsState by analyticsViewModel.uiState.collectAsState()
                DigitalBalanceApp(
                    usageState = state,
                    goalState = goalState,
                    goalAlignmentState = goalAlignmentState,
                    productivityState = productivityState,
                    insightState = insightState,
                    focusState = focusState,
                    analyticsState = analyticsState,
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
                    onAnalyticsPeriodSelected = analyticsViewModel::selectPeriod
                )
            }
        }
    }

    override fun onResume() {
        super.onResume()
        usageViewModel.refresh()
        analyticsViewModel.refreshDate()
    }

    private fun openUsageAccessSettings() {
        try {
            startActivity(Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS))
        } catch (_: ActivityNotFoundException) {
            startActivity(Intent(Settings.ACTION_SETTINGS))
        }
    }
}
