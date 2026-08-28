package com.digitalbalance.app.ui

import android.os.Build
import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import com.digitalbalance.app.R
import com.digitalbalance.app.ui.apps.AppsScreen
import com.digitalbalance.app.ui.focus.FocusScreen
import com.digitalbalance.app.ui.home.HomeScreen
import com.digitalbalance.app.ui.insights.InsightsScreen
import com.digitalbalance.app.ui.settings.SettingsScreen
import com.digitalbalance.app.ui.usage.UsageUiState
import com.digitalbalance.app.ui.usage.GoalUiState
import com.digitalbalance.app.domain.category.AppCategory
import com.digitalbalance.app.domain.goal.GoalType

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
    onOpenUsageSettings: () -> Unit,
    onRefreshUsage: () -> Unit,
    onCategoryChanged: (String, AppCategory) -> Unit,
    onSaveGoal: (GoalType, Long, String?, String?) -> Unit,
    onDeleteGoal: (String) -> Unit
) {
    var destination by rememberSaveable { mutableStateOf(AppDestination.Home) }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        bottomBar = {
            NavigationBar {
                AppDestination.entries.forEach { item ->
                    val label = stringResource(item.labelRes)
                    NavigationBarItem(
                        selected = destination == item,
                        onClick = { destination = item },
                        icon = {
                            Icon(
                                painter = painterResource(item.iconRes),
                                contentDescription = label
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
            AppDestination.Home -> HomeScreen(
                state = usageState,
                onOpenUsageSettings = onOpenUsageSettings,
                onRefresh = onRefreshUsage,
                onOpenApps = { destination = AppDestination.Apps },
                onOpenFocus = { destination = AppDestination.Focus },
                modifier = modifier
            )
            AppDestination.Apps -> AppsScreen(
                state = usageState,
                onOpenUsageSettings = onOpenUsageSettings,
                onRefresh = onRefreshUsage,
                onCategoryChanged = onCategoryChanged,
                modifier = modifier
            )
            AppDestination.Insights -> InsightsScreen(modifier)
            AppDestination.Focus -> FocusScreen(modifier)
            AppDestination.Settings -> SettingsScreen(
                state = usageState,
                goalState = goalState,
                apps = (usageState as? UsageUiState.Content)?.apps.orEmpty(),
                androidVersion = Build.VERSION.RELEASE,
                onOpenUsageSettings = onOpenUsageSettings,
                onSaveGoal = onSaveGoal,
                onDeleteGoal = onDeleteGoal,
                modifier = modifier
            )
        }
    }
}
