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

class MainActivity : ComponentActivity() {
    private val usageViewModel: UsageViewModel by lazy {
        val database = DigitalBalanceDatabase.getInstance(applicationContext)
        ViewModelProvider(
            this,
            UsageViewModel.factory(
                UsageRepository(
                    systemUsage = UsageStatsDataSource(applicationContext),
                    usageDao = database.usageDao()
                ),
                GoalRepository(database.goalDao())
            )
        )[UsageViewModel::class.java]
    }

    private val focusViewModel: FocusViewModel by lazy {
        val database = DigitalBalanceDatabase.getInstance(applicationContext)
        ViewModelProvider(
            this,
            FocusViewModel.factory(FocusRepository(database.focusSessionDao()))
        )[FocusViewModel::class.java]
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
                DigitalBalanceApp(
                    usageState = state,
                    goalState = goalState,
                    goalAlignmentState = goalAlignmentState,
                    productivityState = productivityState,
                    insightState = insightState,
                    focusState = focusState,
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
                    onPrepareFocusSuggestion = focusViewModel::applySuggestion
                )
            }
        }
    }

    override fun onResume() {
        super.onResume()
        usageViewModel.refresh()
    }

    private fun openUsageAccessSettings() {
        try {
            startActivity(Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS))
        } catch (_: ActivityNotFoundException) {
            startActivity(Intent(Settings.ACTION_SETTINGS))
        }
    }
}
