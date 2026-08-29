package com.digitalbalance.app.ui.home

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
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
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.digitalbalance.app.R
import com.digitalbalance.app.domain.goal.GoalProgress
import com.digitalbalance.app.domain.goal.GoalType
import com.digitalbalance.app.domain.score.ProductivityScoreStatus
import com.digitalbalance.app.domain.score.ProductivityScoreSummary
import com.digitalbalance.app.domain.score.overallSummary
import com.digitalbalance.app.ui.components.CompactAppRow
import com.digitalbalance.app.ui.components.LoadingContent
import com.digitalbalance.app.ui.components.MessageContent
import com.digitalbalance.app.ui.components.SectionHeading
import com.digitalbalance.app.ui.components.usageDuration
import com.digitalbalance.app.ui.usage.UsagePermissionCard
import com.digitalbalance.app.ui.usage.GoalUiState
import com.digitalbalance.app.ui.usage.UsageUiState
import com.digitalbalance.app.ui.usage.ScoreUiState
import com.digitalbalance.app.ui.goals.labelRes

@Composable
fun HomeScreen(
    state: UsageUiState,
    goalState: GoalUiState,
    scoreState: ScoreUiState,
    onOpenUsageSettings: () -> Unit,
    onRefresh: () -> Unit,
    onOpenApps: () -> Unit,
    onOpenFocus: () -> Unit,
    onOpenGoals: () -> Unit,
    onOpenScore: () -> Unit,
    modifier: Modifier = Modifier
) {
    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(horizontal = 20.dp, vertical = 24.dp),
        verticalArrangement = Arrangement.spacedBy(18.dp)
    ) {
        item {
            Text(
                text = stringResource(R.string.home_greeting),
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold
            )
        }

        when (state) {
            UsageUiState.Loading -> item { LoadingContent() }
            UsageUiState.PermissionRequired -> item {
                UsagePermissionCard(onOpenUsageSettings)
            }
            UsageUiState.Empty -> item {
                MessageContent(
                    title = stringResource(R.string.empty_usage_title),
                    description = stringResource(R.string.empty_usage_description),
                    actionLabel = stringResource(R.string.refresh),
                    onAction = onRefresh
                )
            }
            UsageUiState.Error -> item {
                MessageContent(
                    title = stringResource(R.string.usage_error_title),
                    description = stringResource(R.string.usage_error_description),
                    actionLabel = stringResource(R.string.try_again),
                    onAction = onRefresh
                )
            }
            is UsageUiState.Content -> {
                item { UsageHero(state) }
                item {
                    SectionHeading(
                        title = stringResource(R.string.top_apps),
                        action = {
                            TextButton(onClick = onOpenApps) {
                                Text(stringResource(R.string.view_all))
                            }
                        }
                    )
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surfaceContainerLow
                        )
                    ) {
                        Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)) {
                            state.apps.take(3).forEachIndexed { index, app ->
                                CompactAppRow(app)
                                if (index < minOf(2, state.apps.lastIndex)) {
                                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                                }
                            }
                        }
                    }
                }
            }
        }

        item {
            HomeGoalsSection(
                state = goalState,
                onOpenGoals = onOpenGoals
            )
        }
        item {
            ProductivityScoreCard(
                state = scoreState,
                onOpenDetails = onOpenScore
            )
        }
        item { InsightCard() }
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.tertiaryContainer
                )
            ) {
                Column(modifier = Modifier.padding(20.dp)) {
                    Text(
                        text = stringResource(R.string.start_focus),
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onTertiaryContainer
                    )
                    Spacer(Modifier.height(4.dp))
                    Text(
                        text = stringResource(R.string.focus_quick_description),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onTertiaryContainer
                    )
                    Spacer(Modifier.height(16.dp))
                    Button(onClick = onOpenFocus) {
                        Text(stringResource(R.string.nav_focus))
                    }
                }
            }
        }
    }
}

@Composable
private fun HomeGoalsSection(
    state: GoalUiState,
    onOpenGoals: () -> Unit
) {
    SectionHeading(
        title = stringResource(R.string.todays_goals),
        action = if (state is GoalUiState.Content) {
            {
                TextButton(onClick = onOpenGoals) {
                    Text(stringResource(R.string.view_all))
                }
            }
        } else {
            null
        }
    )
    when (state) {
        GoalUiState.Loading -> Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceContainerLow
            )
        ) {
            Text(
                text = stringResource(R.string.goals_loading),
                modifier = Modifier.padding(20.dp),
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        GoalUiState.Empty -> Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.secondaryContainer
            )
        ) {
            Column(
                modifier = Modifier.padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = stringResource(R.string.set_digital_goals),
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSecondaryContainer
                )
                Text(
                    text = stringResource(R.string.home_goals_empty_description),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSecondaryContainer
                )
                Button(onClick = onOpenGoals) {
                    Text(stringResource(R.string.add_goal))
                }
            }
        }
        is GoalUiState.Content -> {
            val homeGoals = relevantHomeGoals(state.progress)
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceContainerLow
                )
            ) {
                Column(modifier = Modifier.padding(horizontal = 18.dp, vertical = 6.dp)) {
                    homeGoals.forEachIndexed { index, progress ->
                        CompactGoalProgress(progress)
                        if (index < homeGoals.lastIndex) {
                            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                        }
                    }
                    TextButton(
                        onClick = onOpenGoals,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(stringResource(R.string.add_edit_goals))
                    }
                }
            }
        }
    }
}

@Composable
private fun CompactGoalProgress(progress: GoalProgress) {
    val goal = progress.goal
    val current = progress.currentDurationMillis
    Column(
        modifier = Modifier.fillMaxWidth().padding(vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Text(
            text = if (goal.type == GoalType.AppDailyLimit) {
                goal.appName ?: stringResource(R.string.goal_per_app)
            } else {
                stringResource(goal.type.labelRes())
            },
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.SemiBold
        )
        if (current == null) {
            Text(
                text = stringResource(R.string.goal_usage_unavailable),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        } else {
            Text(
                text = if (goal.type in allowanceGoalTypes) {
                    stringResource(R.string.home_goal_used, usageDuration(current))
                } else {
                    stringResource(
                        R.string.home_goal_target_progress,
                        usageDuration(current),
                        usageDuration(goal.targetDurationMillis)
                    )
                },
                style = MaterialTheme.typography.bodyMedium
            )
            if (goal.type in allowanceGoalTypes) {
                val remaining = (goal.targetDurationMillis - current).coerceAtLeast(0L)
                Text(
                    text = stringResource(
                        R.string.home_goal_remaining,
                        if (remaining == 0L) {
                            stringResource(R.string.zero_minutes)
                        } else {
                            usageDuration(remaining)
                        },
                        usageDuration(goal.targetDurationMillis)
                    ),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            LinearProgressIndicator(
                progress = { (progress.progressFraction ?: 0f).coerceIn(0f, 1f) },
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}

private fun relevantHomeGoals(progress: List<GoalProgress>): List<GoalProgress> =
    progress.sortedWith(
        compareBy<GoalProgress>(
            { homeGoalPriority(it.goal.type) },
            { it.goal.appName.orEmpty() },
            { it.goal.id }
        )
    ).take(MAX_HOME_GOALS)

private fun homeGoalPriority(type: GoalType): Int = when (type) {
    GoalType.OverallForegroundUsage -> 0
    GoalType.ProductiveTime -> 1
    GoalType.SocialMediaLimit -> 2
    GoalType.EntertainmentLimit -> 3
    GoalType.AppDailyLimit -> 4
}

private val allowanceGoalTypes = setOf(
    GoalType.SocialMediaLimit,
    GoalType.EntertainmentLimit,
    GoalType.AppDailyLimit
)

private const val MAX_HOME_GOALS = 5

@Composable
private fun UsageHero(state: UsageUiState.Content) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.extraLarge,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer
        )
    ) {
        Column(modifier = Modifier.padding(24.dp)) {
            Text(
                text = stringResource(R.string.tracked_today),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.onPrimaryContainer
            )
            Spacer(Modifier.height(6.dp))
            Text(
                text = usageDuration(state.totalDurationMillis),
                style = MaterialTheme.typography.displayMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onPrimaryContainer
            )
            Spacer(Modifier.height(8.dp))
            Text(
                text = stringResource(R.string.home_usage_explanation),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.78f)
            )
            Spacer(Modifier.height(18.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = stringResource(R.string.apps_used_today),
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onPrimaryContainer
                )
                Text(
                    text = state.apps.size.toString(),
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onPrimaryContainer
                )
            }
        }
    }
}

@Composable
private fun ProductivityScoreCard(
    state: ScoreUiState,
    onOpenDetails: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainer
        )
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Text(
                text = stringResource(R.string.productivity_score),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold
            )
            Spacer(Modifier.height(8.dp))
            when (state) {
                ScoreUiState.Loading -> Text(
                    text = stringResource(R.string.score_calculating),
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                is ScoreUiState.Result -> {
                    val result = state.score
                    if (result.status == ProductivityScoreStatus.Ready) {
                        Text(
                            text = stringResource(
                                R.string.score_out_of_100,
                                requireNotNull(result.score)
                            ),
                            style = MaterialTheme.typography.displaySmall,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(Modifier.height(8.dp))
                        LinearProgressIndicator(
                            progress = { requireNotNull(result.score) / 100f },
                            modifier = Modifier.fillMaxWidth()
                        )
                    } else {
                        Text(
                            text = stringResource(R.string.not_enough_data),
                            style = MaterialTheme.typography.headlineSmall,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    Spacer(Modifier.height(8.dp))
                    Text(
                        text = if (result.status == ProductivityScoreStatus.Ready) {
                            stringResource(result.overallSummary().messageResource)
                        } else {
                            result.reasons.firstOrNull()
                                ?: stringResource(R.string.score_neutral_explanation)
                        },
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        style = MaterialTheme.typography.bodyMedium
                    )
                    Spacer(Modifier.height(8.dp))
                    TextButton(onClick = onOpenDetails) {
                        Text(stringResource(R.string.view_score_details))
                    }
                }
            }
        }
    }
}

private val ProductivityScoreSummary.messageResource: Int
    get() = when (this) {
        ProductivityScoreSummary.WithinAllLimits -> R.string.score_summary_within_limits
        ProductivityScoreSummary.GoalsProgressing -> R.string.score_summary_goals_progressing
        ProductivityScoreSummary.LimitsExceeded -> R.string.score_summary_limits_exceeded
        ProductivityScoreSummary.ProductiveTargetInProgress ->
            R.string.score_summary_productive_in_progress
    }

@Composable
private fun InsightCard() {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerLow
        )
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Text(
                text = stringResource(R.string.todays_insight),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold
            )
            Spacer(Modifier.height(6.dp))
            Text(
                text = stringResource(R.string.insight_placeholder_description),
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}
