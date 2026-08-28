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
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.digitalbalance.app.R
import com.digitalbalance.app.data.usage.AppUsage
import com.digitalbalance.app.domain.goal.GoalType
import com.digitalbalance.app.ui.goals.GoalsScreen
import com.digitalbalance.app.ui.usage.GoalUiState
import com.digitalbalance.app.ui.usage.UsageUiState

@Composable
fun SettingsScreen(
    state: UsageUiState,
    goalState: GoalUiState,
    apps: List<AppUsage>,
    androidVersion: String,
    onOpenUsageSettings: () -> Unit,
    onSaveGoal: (GoalType, Long, String?, String?) -> Unit,
    onDeleteGoal: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    var showGoals by rememberSaveable { mutableStateOf(false) }
    if (showGoals) {
        GoalsScreen(
            state = goalState,
            apps = apps,
            onBack = { showGoals = false },
            onSaveGoal = onSaveGoal,
            onDeleteGoal = onDeleteGoal,
            modifier = modifier
        )
        return
    }

    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(horizontal = 20.dp, vertical = 24.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Text(
                text = stringResource(R.string.nav_settings),
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold
            )
        }
        item {
            SettingsCard(title = stringResource(R.string.settings_usage_access)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = stringResource(R.string.special_access_type),
                        modifier = Modifier.weight(1f),
                        style = MaterialTheme.typography.bodyLarge
                    )
                    Surface(
                        shape = MaterialTheme.shapes.small,
                        color = when (state) {
                            UsageUiState.PermissionRequired -> MaterialTheme.colorScheme.errorContainer
                            UsageUiState.Loading -> MaterialTheme.colorScheme.surfaceContainerHighest
                            else -> MaterialTheme.colorScheme.primaryContainer
                        }
                    ) {
                        Text(
                            text = usageAccessLabel(state),
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
                Spacer(Modifier.height(16.dp))
                Button(
                    onClick = onOpenUsageSettings,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(stringResource(R.string.manage_usage_access))
                }
            }
        }
        item {
            SettingsCard(title = stringResource(R.string.goals_title)) {
                Text(
                    text = when (goalState) {
                        GoalUiState.Loading -> stringResource(R.string.goals_loading)
                        GoalUiState.Empty -> stringResource(R.string.no_goals_settings)
                        is GoalUiState.Content -> stringResource(
                            R.string.goals_count,
                            goalState.progress.size
                        )
                    },
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(Modifier.height(16.dp))
                Button(
                    onClick = { showGoals = true },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(stringResource(R.string.manage_goals))
                }
            }
        }
        item {
            SettingsCard(title = stringResource(R.string.privacy_title)) {
                Text(
                    text = stringResource(R.string.privacy_settings_description),
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
        item {
            SettingsCard(title = stringResource(R.string.appearance_title)) {
                Text(
                    text = stringResource(R.string.appearance_system),
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
        item {
            SettingsCard(title = stringResource(R.string.system_title)) {
                Text(
                    text = stringResource(R.string.android_version, androidVersion),
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun SettingsCard(
    title: String,
    content: @Composable ColumnScope.() -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerLow
        )
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold
            )
            Spacer(Modifier.height(10.dp))
            content()
        }
    }
}

@Composable
private fun usageAccessLabel(state: UsageUiState): String = when (state) {
    UsageUiState.PermissionRequired -> stringResource(R.string.usage_access_not_granted)
    UsageUiState.Loading -> stringResource(R.string.usage_access_checking)
    else -> stringResource(R.string.usage_access_granted)
}
