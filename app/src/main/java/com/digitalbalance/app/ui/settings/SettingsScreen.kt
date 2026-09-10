package com.digitalbalance.app.ui.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.Button
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.digitalbalance.app.R
import com.digitalbalance.app.domain.reminder.ReminderSettings
import com.digitalbalance.app.ui.components.PremiumCard
import com.digitalbalance.app.ui.components.ScreenHeader
import com.digitalbalance.app.ui.components.StatusPill
import com.digitalbalance.app.ui.theme.DigitalBalanceSpacing
import com.digitalbalance.app.ui.theme.digitalBalanceColors
import com.digitalbalance.app.ui.usage.GoalUiState
import com.digitalbalance.app.ui.usage.UsageUiState

@Composable
fun SettingsScreen(
    state: UsageUiState,
    goalState: GoalUiState,
    androidVersion: String,
    onOpenUsageSettings: () -> Unit,
    onOpenGoals: () -> Unit,
    onOpenCategories: () -> Unit,
    reminderSettings: ReminderSettings,
    notificationPermissionGranted: Boolean,
    onRequestEnableNotifications: () -> Unit,
    onDisableNotifications: () -> Unit,
    onGoalAndLimitRemindersChanged: (Boolean) -> Unit,
    onDailySummaryChanged: (Boolean) -> Unit,
    onFocusSuggestionsChanged: (Boolean) -> Unit,
    modifier: Modifier = Modifier
) {
    var showPermissionExplanation by remember { mutableStateOf(false) }
    if (showPermissionExplanation) {
        AlertDialog(
            onDismissRequest = { showPermissionExplanation = false },
            title = { Text(stringResource(R.string.notification_permission_explanation_title)) },
            text = { Text(stringResource(R.string.notification_permission_explanation)) },
            confirmButton = {
                TextButton(
                    onClick = {
                        showPermissionExplanation = false
                        onRequestEnableNotifications()
                    }
                ) { Text(stringResource(R.string.continue_action)) }
            },
            dismissButton = {
                TextButton(onClick = { showPermissionExplanation = false }) {
                    Text(stringResource(R.string.cancel))
                }
            }
        )
    }
    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(
            horizontal = DigitalBalanceSpacing.screen,
            vertical = DigitalBalanceSpacing.section
        ),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            ScreenHeader(
                title = stringResource(R.string.nav_settings),
                subtitle = stringResource(R.string.settings_subtitle)
            )
        }
        item {
            SettingsCard(stringResource(R.string.reminders_title)) {
                Text(
                    stringResource(R.string.reminders_description),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                ReminderToggleRow(
                    title = stringResource(R.string.notifications_enable),
                    description = stringResource(R.string.notifications_enable_description),
                    checked = reminderSettings.notificationsEnabled,
                    onCheckedChange = { enabled ->
                        if (enabled) showPermissionExplanation = true else onDisableNotifications()
                    }
                )
                if (reminderSettings.notificationsEnabled && !notificationPermissionGranted) {
                    Text(
                        stringResource(R.string.notification_permission_blocked),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.digitalBalanceColors.exceeded,
                        modifier = Modifier.padding(bottom = 8.dp)
                    )
                }
                ReminderToggleRow(
                    title = stringResource(R.string.goal_limit_reminders),
                    description = stringResource(R.string.goal_limit_reminders_description),
                    checked = reminderSettings.goalAndLimitRemindersEnabled,
                    enabled = reminderSettings.notificationsEnabled,
                    onCheckedChange = onGoalAndLimitRemindersChanged
                )
                ReminderToggleRow(
                    title = stringResource(R.string.daily_summary_setting),
                    description = stringResource(R.string.daily_summary_setting_description),
                    checked = reminderSettings.dailySummaryEnabled,
                    enabled = reminderSettings.notificationsEnabled,
                    onCheckedChange = onDailySummaryChanged
                )
                ReminderToggleRow(
                    title = stringResource(R.string.focus_suggestions_setting),
                    description = stringResource(R.string.focus_suggestions_setting_description),
                    checked = reminderSettings.focusSuggestionsEnabled,
                    enabled = reminderSettings.notificationsEnabled,
                    onCheckedChange = onFocusSuggestionsChanged
                )
            }
        }
        item {
            SettingsCard(stringResource(R.string.settings_personalization)) {
                SettingsActionRow(
                    title = stringResource(R.string.goals_title),
                    description = when (goalState) {
                        GoalUiState.Loading -> stringResource(R.string.goals_loading)
                        GoalUiState.Empty -> stringResource(R.string.no_goals_settings)
                        is GoalUiState.Content -> stringResource(R.string.goals_count, goalState.progress.size)
                    },
                    action = stringResource(R.string.manage_goals),
                    onClick = onOpenGoals
                )
                SettingsActionRow(
                    title = stringResource(R.string.categories_title),
                    description = stringResource(R.string.categories_settings_description),
                    action = stringResource(R.string.review_categories),
                    onClick = onOpenCategories
                )
            }
        }
        item {
            SettingsCard(stringResource(R.string.settings_usage_access)) {
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        stringResource(R.string.special_access_type),
                        modifier = Modifier.weight(1f),
                        style = MaterialTheme.typography.bodyLarge
                    )
                    StatusPill(
                        usageAccessLabel(state),
                        when (state) {
                            UsageUiState.PermissionRequired -> MaterialTheme.digitalBalanceColors.exceeded
                            UsageUiState.Loading -> MaterialTheme.digitalBalanceColors.informational
                            else -> MaterialTheme.digitalBalanceColors.positive
                        }
                    )
                }
                Button(
                    onClick = onOpenUsageSettings,
                    modifier = Modifier.fillMaxWidth().padding(top = 16.dp)
                ) { Text(stringResource(R.string.manage_usage_access)) }
            }
        }
        item {
            SettingsCard(stringResource(R.string.privacy_about_title)) {
                Text(
                    stringResource(R.string.privacy_settings_description),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(Modifier.height(18.dp))
                Text(stringResource(R.string.appearance_title), style = MaterialTheme.typography.titleSmall)
                Text(
                    stringResource(R.string.appearance_system),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(Modifier.height(18.dp))
                Text(
                    stringResource(R.string.android_version, androidVersion),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun ReminderToggleRow(
    title: String,
    description: String,
    checked: Boolean,
    enabled: Boolean = true,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Column(Modifier.weight(1f)) {
            Text(
                title,
                style = MaterialTheme.typography.titleSmall,
                color = if (enabled) MaterialTheme.colorScheme.onSurface else {
                    MaterialTheme.colorScheme.onSurface.copy(alpha = 0.48f)
                }
            )
            Text(
                description,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(
                    alpha = if (enabled) 1f else 0.48f
                )
            )
        }
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            enabled = enabled
        )
    }
}

@Composable
private fun SettingsCard(title: String, content: @Composable ColumnScope.() -> Unit) {
    PremiumCard(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(20.dp)) {
            Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
            Spacer(Modifier.height(10.dp))
            content()
        }
    }
}

@Composable
private fun SettingsActionRow(title: String, description: String, action: String, onClick: () -> Unit) {
    Column(Modifier.fillMaxWidth().padding(vertical = 7.dp)) {
        Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
        Text(description, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Button(onClick, Modifier.fillMaxWidth().padding(top = 12.dp)) { Text(action) }
    }
}

@Composable
private fun usageAccessLabel(state: UsageUiState): String = when (state) {
    UsageUiState.PermissionRequired -> stringResource(R.string.usage_access_not_granted)
    UsageUiState.Loading -> stringResource(R.string.usage_access_checking)
    else -> stringResource(R.string.usage_access_granted)
}
