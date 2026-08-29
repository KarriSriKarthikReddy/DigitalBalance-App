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
import com.digitalbalance.app.domain.productivity.ProductivityScoreStatus
import com.digitalbalance.app.domain.score.GoalAlignmentStatus
import com.digitalbalance.app.domain.score.GoalAlignmentSummary
import com.digitalbalance.app.domain.score.overallSummary
import com.digitalbalance.app.ui.usage.InsightUiState
import com.digitalbalance.app.ui.components.CompactAppRow
import com.digitalbalance.app.ui.components.ForegroundUsageRing
import com.digitalbalance.app.ui.components.InsightLeadingIcon
import com.digitalbalance.app.ui.components.LoadingContent
import com.digitalbalance.app.ui.components.MessageContent
import com.digitalbalance.app.ui.components.PremiumCard
import com.digitalbalance.app.ui.components.ScoreRing
import com.digitalbalance.app.ui.components.SectionHeading
import com.digitalbalance.app.ui.components.usageDuration
import com.digitalbalance.app.ui.usage.UsagePermissionCard
import com.digitalbalance.app.ui.usage.GoalUiState
import com.digitalbalance.app.ui.usage.UsageUiState
import com.digitalbalance.app.ui.usage.GoalAlignmentUiState
import com.digitalbalance.app.ui.usage.ProductivityUiState
import com.digitalbalance.app.ui.goals.labelRes
import com.digitalbalance.app.ui.theme.DigitalBalanceSpacing
import com.digitalbalance.app.ui.theme.digitalBalanceColors

@Composable
fun HomeScreen(
    state: UsageUiState,
    goalState: GoalUiState,
    goalAlignmentState: GoalAlignmentUiState,
    productivityState: ProductivityUiState,
    insightState: InsightUiState,
    onOpenUsageSettings: () -> Unit,
    onRefresh: () -> Unit,
    onOpenApps: () -> Unit,
    onOpenFocus: () -> Unit,
    onOpenGoals: () -> Unit,
    onOpenScore: () -> Unit,
    onOpenInsights: () -> Unit,
    modifier: Modifier = Modifier
) {
    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(
            horizontal = DigitalBalanceSpacing.screen,
            vertical = DigitalBalanceSpacing.section
        ),
        verticalArrangement = Arrangement.spacedBy(DigitalBalanceSpacing.section)
    ) {
        item {
            Text(
                text = stringResource(R.string.home_greeting),
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.SemiBold
            )
            Text(
                text = stringResource(R.string.home_today_context),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
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
                    ScoresCard(
                        goalAlignmentState = goalAlignmentState,
                        productivityState = productivityState,
                        onOpenDetails = onOpenScore
                    )
                }
                item { HomeGoalsSection(state = goalState, onOpenGoals = onOpenGoals) }
                item {
                    SectionHeading(
                        title = stringResource(R.string.top_apps),
                        action = {
                            TextButton(onClick = onOpenApps) {
                                Text(stringResource(R.string.view_all))
                            }
                        }
                    )
                    PremiumCard(
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)) {
                            state.apps.take(3).forEachIndexed { index, app ->
                                CompactAppRow(
                                    usage = app,
                                    showCategory = true,
                                    maxDurationMillis = state.apps.firstOrNull()?.foregroundDurationMillis
                                )
                                if (index < minOf(2, state.apps.lastIndex)) {
                                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                                }
                            }
                        }
                    }
                }
            }
        }

        if (state is UsageUiState.Content) item {
            InsightCard(state = insightState, onOpenInsights = onOpenInsights)
        }
        if (state is UsageUiState.Content) item {
            PremiumCard(
                modifier = Modifier.fillMaxWidth(),
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(18.dp),
                    verticalAlignment = androidx.compose.ui.Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = stringResource(R.string.start_focus),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                    Spacer(Modifier.height(4.dp))
                    Text(
                        text = stringResource(R.string.focus_quick_description),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    }
                    TextButton(onClick = onOpenFocus) {
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
    val progressColor = when {
        progress.progressFraction == null -> MaterialTheme.digitalBalanceColors.informational
        goal.type == GoalType.ProductiveTime -> MaterialTheme.digitalBalanceColors.productive
        progress.progressFraction > 1f -> MaterialTheme.digitalBalanceColors.exceeded
        progress.progressFraction >= 0.8f -> MaterialTheme.digitalBalanceColors.warning
        else -> MaterialTheme.digitalBalanceColors.positive
    }
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
                    text = if (current > goal.targetDurationMillis) {
                        stringResource(
                            R.string.home_goal_exceeded,
                            usageDuration(current - goal.targetDurationMillis),
                            usageDuration(goal.targetDurationMillis)
                        )
                    } else {
                        stringResource(
                            R.string.home_goal_remaining,
                            if (remaining == 0L) stringResource(R.string.zero_minutes) else usageDuration(remaining),
                            usageDuration(goal.targetDurationMillis)
                        )
                    },
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            LinearProgressIndicator(
                progress = { (progress.progressFraction ?: 0f).coerceIn(0f, 1f) },
                modifier = Modifier.fillMaxWidth(),
                color = progressColor,
                trackColor = progressColor.copy(alpha = 0.14f)
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
    GoalType.OverallForegroundUsage,
    GoalType.SocialMediaLimit,
    GoalType.EntertainmentLimit,
    GoalType.AppDailyLimit
)

private const val MAX_HOME_GOALS = 5

@Composable
private fun UsageHero(state: UsageUiState.Content) {
    PremiumCard(
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
            horizontalAlignment = androidx.compose.ui.Alignment.CenterHorizontally
        ) {
            ForegroundUsageRing(
                apps = state.apps,
                totalDurationMillis = state.totalDurationMillis,
                totalLabel = usageDuration(state.totalDurationMillis),
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(Modifier.height(14.dp))
            Text(
                text = stringResource(R.string.home_usage_explanation),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = androidx.compose.ui.text.style.TextAlign.Center
            )
            Spacer(Modifier.height(12.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = stringResource(R.string.apps_used_today),
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = state.apps.size.toString(),
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }
        }
    }
}

@Composable
private fun ScoresCard(
    goalAlignmentState: GoalAlignmentUiState,
    productivityState: ProductivityUiState,
    onOpenDetails: () -> Unit
) {
    PremiumCard(
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            SectionHeading(title = stringResource(R.string.scores_title))
            Spacer(Modifier.height(12.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                ProductivityScoreMetric(
                    state = productivityState,
                    modifier = Modifier.weight(1f)
                )
                GoalAlignmentMetric(
                    state = goalAlignmentState,
                    modifier = Modifier.weight(1f)
                )
            }
            Spacer(Modifier.height(6.dp))
            TextButton(onClick = onOpenDetails) {
                Text(stringResource(R.string.view_score_details))
            }
        }
    }
}

@Composable
private fun ProductivityScoreMetric(
    state: ProductivityUiState,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(7.dp), horizontalAlignment = androidx.compose.ui.Alignment.CenterHorizontally) {
        Text(
            text = stringResource(R.string.productivity_score),
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.SemiBold
        )
        Text(
            text = stringResource(R.string.productivity_score_home_description),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        when (state) {
            ProductivityUiState.Loading -> Text(stringResource(R.string.score_calculating))
            is ProductivityUiState.Result -> {
                val result = state.productivity
                ScoreRing(
                    score = result.score.takeIf { result.status == ProductivityScoreStatus.Ready },
                    label = stringResource(R.string.out_of_100_short),
                    color = MaterialTheme.digitalBalanceColors.productive
                )
                Text(
                    text = if (result.status == ProductivityScoreStatus.Ready) {
                        stringResource(R.string.productivity_score_home_summary)
                    } else {
                        stringResource(R.string.productivity_score_home_unavailable)
                    },
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun GoalAlignmentMetric(
    state: GoalAlignmentUiState,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(7.dp), horizontalAlignment = androidx.compose.ui.Alignment.CenterHorizontally) {
        Text(
            text = stringResource(R.string.goal_alignment_score),
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.SemiBold
        )
        Text(
            text = stringResource(R.string.goal_alignment_home_description),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        when (state) {
            GoalAlignmentUiState.Loading -> Text(stringResource(R.string.score_calculating))
            is GoalAlignmentUiState.Result -> {
                val result = state.alignment
                ScoreRing(
                    score = result.score.takeIf { result.status == GoalAlignmentStatus.Ready },
                    label = stringResource(R.string.out_of_100_short),
                    color = MaterialTheme.colorScheme.primary
                )
                Text(
                    text = if (result.status == GoalAlignmentStatus.Ready) {
                        stringResource(result.overallSummary().messageResource)
                    } else {
                        stringResource(R.string.goal_alignment_not_configured)
                    },
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

private val GoalAlignmentSummary.messageResource: Int
    get() = when (this) {
        GoalAlignmentSummary.WithinAllLimits -> R.string.score_summary_within_limits
        GoalAlignmentSummary.GoalsProgressing -> R.string.score_summary_goals_progressing
        GoalAlignmentSummary.LimitsExceeded -> R.string.score_summary_limits_exceeded
        GoalAlignmentSummary.ProductiveTargetInProgress ->
            R.string.score_summary_productive_in_progress
    }

@Composable
private fun InsightCard(
    state: InsightUiState,
    onOpenInsights: () -> Unit
) {
    PremiumCard(
        modifier = Modifier.fillMaxWidth(),
    ) {
        Row(modifier = Modifier.fillMaxWidth().padding(20.dp), horizontalArrangement = Arrangement.spacedBy(14.dp)) {
            InsightLeadingIcon(R.drawable.ic_nav_insights, MaterialTheme.colorScheme.secondary)
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = stringResource(R.string.todays_insight),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold
                )
            Spacer(Modifier.height(6.dp))
            when (state) {
                InsightUiState.Loading -> Text(
                    text = stringResource(R.string.insights_loading),
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                is InsightUiState.Content -> {
                    val insight = state.insights.firstOrNull()
                    if (insight == null) {
                        Text(
                            text = stringResource(R.string.insight_calm_fallback),
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    } else {
                        Text(
                            text = insight.title,
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.SemiBold
                        )
                        Spacer(Modifier.height(4.dp))
                        Text(
                            text = insight.description,
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        insight.supportingMetric?.let { metric ->
                            Spacer(Modifier.height(8.dp))
                            Text(
                                text = metric,
                                style = MaterialTheme.typography.labelLarge,
                                color = MaterialTheme.colorScheme.primary,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }
                }
            }
            Spacer(Modifier.height(8.dp))
            TextButton(onClick = onOpenInsights) {
                Text(stringResource(R.string.view_all_insights))
            }
            }
        }
    }
}
