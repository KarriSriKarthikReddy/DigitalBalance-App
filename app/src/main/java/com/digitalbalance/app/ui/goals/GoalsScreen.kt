package com.digitalbalance.app.ui.goals

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.digitalbalance.app.R
import com.digitalbalance.app.data.usage.AppUsage
import com.digitalbalance.app.domain.goal.DigitalGoal
import com.digitalbalance.app.domain.goal.GoalProgress
import com.digitalbalance.app.domain.goal.GoalType
import com.digitalbalance.app.ui.components.AppIcon
import com.digitalbalance.app.ui.components.LoadingContent
import com.digitalbalance.app.ui.components.PremiumCard
import com.digitalbalance.app.ui.components.ScreenHeader
import com.digitalbalance.app.ui.components.StatusPill
import com.digitalbalance.app.ui.components.usageDuration
import com.digitalbalance.app.ui.theme.digitalBalanceColors
import com.digitalbalance.app.ui.usage.GoalUiState

@Composable
fun GoalsScreen(
    state: GoalUiState,
    apps: List<AppUsage>,
    onBack: () -> Unit,
    onSaveGoal: (GoalType, Long, String?, String?) -> Unit,
    onDeleteGoal: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    var editorGoalId by rememberSaveable { mutableStateOf<String?>(null) }
    var addingGoal by rememberSaveable { mutableStateOf(false) }
    val progress = (state as? GoalUiState.Content)?.progress.orEmpty()
    val editorGoal = progress.firstOrNull { it.goal.id == editorGoalId }?.goal
    val editorOpen = addingGoal || editorGoal != null

    BackHandler {
        if (editorOpen) {
            addingGoal = false
            editorGoalId = null
        } else {
            onBack()
        }
    }

    if (editorOpen) {
        GoalEditor(
            existingGoal = editorGoal,
            existingGoals = progress.map(GoalProgress::goal),
            apps = apps,
            onCancel = {
                addingGoal = false
                editorGoalId = null
            },
            onSave = { type, duration, packageName, appName ->
                onSaveGoal(type, duration, packageName, appName)
                addingGoal = false
                editorGoalId = null
            },
            modifier = modifier
        )
        return
    }

    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(horizontal = 20.dp, vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            TextButton(onClick = onBack) { Text(stringResource(R.string.back_to_settings)) }
            ScreenHeader(
                title = stringResource(R.string.goals_title),
                subtitle = stringResource(R.string.goals_description)
            )
        }
        when (state) {
            GoalUiState.Loading -> item { LoadingContent() }
            GoalUiState.Empty -> item {
                GoalEmptyState(onAdd = { addingGoal = true })
            }
            is GoalUiState.Content -> {
                items(state.progress, key = { it.goal.id }) { item ->
                    GoalProgressCard(
                        progress = item,
                        onEdit = { editorGoalId = item.goal.id },
                        onDelete = { onDeleteGoal(item.goal.id) }
                    )
                }
                item {
                    Button(
                        onClick = { addingGoal = true },
                        modifier = Modifier.fillMaxWidth().padding(top = 8.dp)
                    ) {
                        Text(stringResource(R.string.add_goal))
                    }
                }
            }
        }
    }
}

@Composable
private fun GoalProgressCard(
    progress: GoalProgress,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    val goal = progress.goal
    val progressColor = goalProgressColor(progress)
    PremiumCard(Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Text(
                text = if (goal.type == GoalType.AppDailyLimit) {
                    goal.appName ?: stringResource(R.string.goal_per_app)
                } else {
                    stringResource(goal.type.labelRes())
                },
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold
            )
            val current = progress.currentDurationMillis
            if (current == null) {
                Text(
                    text = stringResource(R.string.goal_usage_unavailable),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            } else {
                Text(
                    text = stringResource(
                        R.string.goal_progress_value,
                        usageDuration(current),
                        usageDuration(goal.targetDurationMillis)
                    ),
                    style = MaterialTheme.typography.bodyLarge
                )
                LinearProgressIndicator(
                    progress = { (progress.progressFraction ?: 0f).coerceIn(0f, 1f) },
                    modifier = Modifier.fillMaxWidth(),
                    color = progressColor,
                    trackColor = progressColor.copy(alpha = 0.14f)
                )
            }
            StatusPill(
                text = stringResource(if (goal.type.isMinimumTarget) R.string.daily_target else R.string.daily_limit),
                color = progressColor
            )
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                TextButton(onClick = onEdit) { Text(stringResource(R.string.edit)) }
                TextButton(onClick = onDelete) { Text(stringResource(R.string.remove)) }
            }
        }
    }
}

@Composable
private fun GoalEmptyState(onAdd: () -> Unit) {
    PremiumCard(Modifier.fillMaxWidth().padding(top = 12.dp)) {
        Column(
            modifier = Modifier.padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Text(
                text = stringResource(R.string.no_goals_title),
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.SemiBold
            )
            Text(
                text = stringResource(R.string.no_goals_description),
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Button(onClick = onAdd) { Text(stringResource(R.string.add_goal)) }
        }
    }
}

@Composable
private fun GoalEditor(
    existingGoal: DigitalGoal?,
    existingGoals: List<DigitalGoal>,
    apps: List<AppUsage>,
    onCancel: () -> Unit,
    onSave: (GoalType, Long, String?, String?) -> Unit,
    modifier: Modifier = Modifier
) {
    var selectedTypeKey by rememberSaveable(existingGoal?.id) {
        mutableStateOf(existingGoal?.type?.storageKey)
    }
    var selectedPackage by rememberSaveable(existingGoal?.id) {
        mutableStateOf(existingGoal?.packageName)
    }
    var selectedAppName by rememberSaveable(existingGoal?.id) {
        mutableStateOf(existingGoal?.appName)
    }
    var minutesText by rememberSaveable(existingGoal?.id) {
        mutableStateOf(
            existingGoal?.targetDurationMillis?.div(MILLIS_PER_MINUTE)?.toString().orEmpty()
        )
    }
    val selectedType = selectedTypeKey?.let(GoalType::fromStorageKey)
    val minutes = minutesText.toLongOrNull()
    val durationMillis = minutes?.takeIf { it in 1..MAX_GOAL_MINUTES }
        ?.times(MILLIS_PER_MINUTE)
    val canSave = selectedType != null && durationMillis != null &&
        (selectedType != GoalType.AppDailyLimit || selectedPackage != null)
    val occupiedGlobalTypes = existingGoals
        .filter { it.id != existingGoal?.id && it.type != GoalType.AppDailyLimit }
        .mapTo(mutableSetOf(), DigitalGoal::type)
    val occupiedAppPackages = existingGoals
        .filter { it.id != existingGoal?.id && it.type == GoalType.AppDailyLimit }
        .mapNotNullTo(mutableSetOf(), DigitalGoal::packageName)

    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(horizontal = 20.dp, vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            TextButton(onClick = onCancel) { Text(stringResource(R.string.cancel)) }
            Text(
                text = stringResource(
                    if (existingGoal == null) R.string.add_goal else R.string.edit_goal
                ),
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold
            )
        }
        item {
            Text(
                text = stringResource(R.string.goal_type),
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.SemiBold
            )
        }
        items(
            if (existingGoal != null) {
                listOf(existingGoal.type)
            } else {
                GoalType.entries.filter { type ->
                    type == GoalType.AppDailyLimit || type !in occupiedGlobalTypes
                }
            },
            key = GoalType::storageKey
        ) { type ->
            SelectableCard(
                selected = type == selectedType,
                title = stringResource(type.labelRes()),
                description = stringResource(type.descriptionRes()),
                onClick = {
                    selectedTypeKey = type.storageKey
                    if (type != GoalType.AppDailyLimit) {
                        selectedPackage = null
                        selectedAppName = null
                    }
                }
            )
        }
        if (selectedType == GoalType.AppDailyLimit) {
            item {
                Text(
                    text = stringResource(R.string.choose_app),
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.padding(top = 8.dp)
                )
            }
            val selectableApps = if (existingGoal != null) {
                apps.filter { it.packageName == existingGoal.packageName }
            } else {
                apps.filter { it.packageName !in occupiedAppPackages }
            }
            if (existingGoal != null && selectableApps.isEmpty()) {
                item {
                    Text(
                        text = existingGoal.appName ?: stringResource(R.string.goal_per_app),
                        style = MaterialTheme.typography.titleMedium
                    )
                }
            }
            if (selectableApps.isEmpty() && existingGoal == null) {
                item {
                    Text(
                        text = stringResource(R.string.no_apps_for_goal),
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            } else {
                items(selectableApps, key = AppUsage::packageName) { app ->
                    Card(
                        onClick = {
                            selectedPackage = app.packageName
                            selectedAppName = app.appName
                        },
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(
                            containerColor = if (selectedPackage == app.packageName) {
                                MaterialTheme.colorScheme.primaryContainer
                            } else {
                                MaterialTheme.colorScheme.surfaceContainerLow
                            }
                        )
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(14.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            AppIcon(usage = app)
                            Text(
                                text = app.appName,
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }
                }
            }
        }
        item {
            OutlinedTextField(
                value = minutesText,
                onValueChange = { value ->
                    if (value.all(Char::isDigit) && value.length <= 5) minutesText = value
                },
                modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                label = { Text(stringResource(R.string.target_minutes)) },
                supportingText = { Text(stringResource(R.string.target_minutes_note)) },
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
            )
        }
        item {
            Button(
                onClick = {
                    onSave(
                        requireNotNull(selectedType),
                        requireNotNull(durationMillis),
                        selectedPackage,
                        selectedAppName
                    )
                },
                enabled = canSave,
                modifier = Modifier.fillMaxWidth().padding(top = 8.dp)
            ) {
                Text(stringResource(R.string.save_goal))
            }
        }
    }
}

@Composable
private fun SelectableCard(
    selected: Boolean,
    title: String,
    description: String,
    onClick: () -> Unit
) {
    Card(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = if (selected) {
                MaterialTheme.colorScheme.primaryContainer
            } else {
                MaterialTheme.colorScheme.surfaceContainerLow
            }
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
            Text(
                description,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

private const val MILLIS_PER_MINUTE = 60_000L
private const val MAX_GOAL_MINUTES = 99_999L

@Composable
private fun goalProgressColor(progress: GoalProgress) = when {
    progress.progressFraction == null -> MaterialTheme.digitalBalanceColors.informational
    progress.goal.type.isMinimumTarget -> MaterialTheme.digitalBalanceColors.productive
    progress.progressFraction > 1f -> MaterialTheme.digitalBalanceColors.exceeded
    progress.progressFraction >= 0.8f -> MaterialTheme.digitalBalanceColors.warning
    else -> MaterialTheme.digitalBalanceColors.positive
}
