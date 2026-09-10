package com.digitalbalance.app.ui

import android.os.Build
import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.spring
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.digitalbalance.app.R
import com.digitalbalance.app.ui.apps.AppsScreen
import com.digitalbalance.app.ui.focus.FocusScreen
import com.digitalbalance.app.ui.home.HomeScreen
import com.digitalbalance.app.ui.goals.GoalsScreen
import com.digitalbalance.app.ui.insights.InsightsScreen
import com.digitalbalance.app.ui.settings.SettingsScreen
import com.digitalbalance.app.ui.score.ScoreDetailsScreen
import com.digitalbalance.app.ui.usage.UsageUiState
import com.digitalbalance.app.ui.usage.GoalUiState
import com.digitalbalance.app.ui.usage.GoalAlignmentUiState
import com.digitalbalance.app.ui.usage.ProductivityUiState
import com.digitalbalance.app.domain.category.AppCategory
import com.digitalbalance.app.domain.goal.GoalType
import com.digitalbalance.app.domain.insight.InsightActionType
import com.digitalbalance.app.ui.usage.InsightUiState
import com.digitalbalance.app.ui.focus.FocusUiState
import com.digitalbalance.app.domain.focus.FocusLaunchSuggestion
import com.digitalbalance.app.domain.focus.FocusPreset
import com.digitalbalance.app.domain.focus.FocusReflection
import com.digitalbalance.app.domain.analytics.AnalyticsPeriod
import com.digitalbalance.app.ui.insights.AnalyticsUiState
import com.digitalbalance.app.domain.reminder.ReminderDestination
import com.digitalbalance.app.domain.reminder.ReminderSettings

private enum class AppDestination(
    @param:StringRes val labelRes: Int,
    @param:DrawableRes val iconRes: Int
) {
    Home(R.string.nav_home, R.drawable.ic_nav_home),
    Apps(R.string.nav_apps, R.drawable.ic_nav_apps),
    Insights(R.string.nav_insights, R.drawable.ic_nav_insights),
    Focus(R.string.nav_focus, R.drawable.ic_nav_focus),
    Settings(R.string.nav_settings, R.drawable.ic_nav_settings)
}

@Composable
fun DigitalBalanceApp(
    usageState: UsageUiState,
    goalState: GoalUiState,
    goalAlignmentState: GoalAlignmentUiState,
    productivityState: ProductivityUiState,
    insightState: InsightUiState,
    focusState: FocusUiState,
    analyticsState: AnalyticsUiState,
    reminderSettings: ReminderSettings,
    notificationPermissionGranted: Boolean,
    notificationDestination: ReminderDestination?,
    onOpenUsageSettings: () -> Unit,
    onRefreshUsage: () -> Unit,
    onCategoryChanged: (String, AppCategory) -> Unit,
    onSaveGoal: (GoalType, Long, String?, String?) -> Unit,
    onDeleteGoal: (String) -> Unit,
    onSelectFocusPreset: (FocusPreset) -> Unit,
    onSelectFocusDuration: (Int) -> Unit,
    onSelectCustomFocusDuration: () -> Unit,
    onCustomFocusDurationChanged: (String) -> Unit,
    onStartFocus: () -> Unit,
    onPauseFocus: () -> Unit,
    onResumeFocus: () -> Unit,
    onStopFocus: () -> Unit,
    onFocusReflectionChanged: (FocusReflection?) -> Unit,
    onFocusDone: () -> Unit,
    onStartAnotherFocus: () -> Unit,
    onFocusTick: () -> Unit,
    onPrepareFocusSuggestion: (FocusLaunchSuggestion) -> Unit,
    onAnalyticsPeriodSelected: (AnalyticsPeriod) -> Unit,
    onRequestEnableNotifications: () -> Unit,
    onDisableNotifications: () -> Unit,
    onGoalAndLimitRemindersChanged: (Boolean) -> Unit,
    onDailySummaryChanged: (Boolean) -> Unit,
    onFocusSuggestionsChanged: (Boolean) -> Unit,
    onNotificationDestinationHandled: () -> Unit
) {
    var destination by rememberSaveable { mutableStateOf(AppDestination.Home) }
    var goalsOpen by rememberSaveable { mutableStateOf(false) }
    var scoreDetailsOpen by rememberSaveable { mutableStateOf(false) }
    LaunchedEffect(notificationDestination) {
        if (notificationDestination != null) goalsOpen = false
        when (notificationDestination) {
            ReminderDestination.Home -> destination = AppDestination.Home
            ReminderDestination.Apps -> destination = AppDestination.Apps
            ReminderDestination.Goals -> {
                destination = AppDestination.Settings
                goalsOpen = true
            }
            ReminderDestination.Insights -> destination = AppDestination.Insights
            ReminderDestination.Focus -> {
                onPrepareFocusSuggestion(
                    FocusLaunchSuggestion(
                        preset = FocusPreset.Focus,
                        durationMinutes = 25,
                        autoStart = false
                    )
                )
                destination = AppDestination.Focus
            }
            null -> return@LaunchedEffect
        }
        scoreDetailsOpen = false
        onNotificationDestinationHandled()
    }
    val handleInsightAction: (InsightActionType) -> Unit = { action ->
        when (action) {
            InsightActionType.OpenGoals -> {
                destination = AppDestination.Settings
                goalsOpen = true
            }
            InsightActionType.OpenApps,
            InsightActionType.ReviewCategories -> destination = AppDestination.Apps
            InsightActionType.OpenFocus -> {
                FocusLaunchSuggestion.forInsightAction(action)?.let(onPrepareFocusSuggestion)
                destination = AppDestination.Focus
            }
        }
        scoreDetailsOpen = false
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        bottomBar = {
            NavigationBar(
                containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
                tonalElevation = 0.dp
            ) {
                AppDestination.entries.forEach { item ->
                    val label = stringResource(item.labelRes)
                    NavigationBarItem(
                        selected = destination == item,
                        onClick = {
                            destination = item
                            goalsOpen = false
                            scoreDetailsOpen = false
                        },
                        icon = {
                            val iconSize by animateDpAsState(
                                targetValue = if (destination == item) 26.dp else 23.dp,
                                animationSpec = spring(),
                                label = "navIcon"
                            )
                            Icon(
                                painter = painterResource(item.iconRes),
                                contentDescription = label,
                                modifier = Modifier.size(iconSize)
                            )
                        },
                        label = { Text(label) }
                    )
                }
            }
        }
    ) { contentPadding ->
        val modifier = Modifier.padding(contentPadding)
        when (destination) {
            AppDestination.Home -> if (scoreDetailsOpen) {
                ScoreDetailsScreen(
                    goalAlignmentState = goalAlignmentState,
                    productivityState = productivityState,
                    onBack = { scoreDetailsOpen = false },
                    modifier = modifier
                )
            } else {
                HomeScreen(
                    state = usageState,
                    goalState = goalState,
                    goalAlignmentState = goalAlignmentState,
                    productivityState = productivityState,
                    insightState = insightState,
                    onOpenUsageSettings = onOpenUsageSettings,
                    onRefresh = onRefreshUsage,
                    onOpenApps = { destination = AppDestination.Apps },
                    onOpenFocus = { destination = AppDestination.Focus },
                    onOpenGoals = {
                        destination = AppDestination.Settings
                        goalsOpen = true
                    },
                    onOpenScore = { scoreDetailsOpen = true },
                    onOpenInsights = { destination = AppDestination.Insights },
                    modifier = modifier
                )
            }
            AppDestination.Apps -> AppsScreen(
                state = usageState,
                onOpenUsageSettings = onOpenUsageSettings,
                onRefresh = onRefreshUsage,
                onCategoryChanged = onCategoryChanged,
                modifier = modifier
            )
            AppDestination.Insights -> InsightsScreen(
                insightState = insightState,
                analyticsState = analyticsState,
                onPeriodSelected = onAnalyticsPeriodSelected,
                onAction = handleInsightAction,
                modifier = modifier
            )
            AppDestination.Focus -> FocusScreen(
                state = focusState,
                onSelectPreset = onSelectFocusPreset,
                onSelectDuration = onSelectFocusDuration,
                onSelectCustomDuration = onSelectCustomFocusDuration,
                onCustomDurationChanged = onCustomFocusDurationChanged,
                onStart = onStartFocus,
                onPause = onPauseFocus,
                onResume = onResumeFocus,
                onStopEarly = onStopFocus,
                onReflectionChanged = onFocusReflectionChanged,
                onDone = onFocusDone,
                onStartAnother = onStartAnotherFocus,
                onTick = onFocusTick,
                modifier = modifier
            )
            AppDestination.Settings -> if (goalsOpen) {
                GoalsScreen(
                    state = goalState,
                    apps = (usageState as? UsageUiState.Content)?.apps.orEmpty(),
                    onBack = { goalsOpen = false },
                    onSaveGoal = onSaveGoal,
                    onDeleteGoal = onDeleteGoal,
                    modifier = modifier
                )
            } else {
                SettingsScreen(
                    state = usageState,
                    goalState = goalState,
                    androidVersion = Build.VERSION.RELEASE,
                    onOpenUsageSettings = onOpenUsageSettings,
                    onOpenGoals = { goalsOpen = true },
                    onOpenCategories = { destination = AppDestination.Apps },
                    reminderSettings = reminderSettings,
                    notificationPermissionGranted = notificationPermissionGranted,
                    onRequestEnableNotifications = onRequestEnableNotifications,
                    onDisableNotifications = onDisableNotifications,
                    onGoalAndLimitRemindersChanged = onGoalAndLimitRemindersChanged,
                    onDailySummaryChanged = onDailySummaryChanged,
                    onFocusSuggestionsChanged = onFocusSuggestionsChanged,
                    modifier = modifier
                )
            }
        }
    }
}
