package com.digitalbalance.app.ui.score

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
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
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
import com.digitalbalance.app.domain.productivity.ProductivityScoreResult
import com.digitalbalance.app.domain.productivity.ProductivityScoreStatus
import com.digitalbalance.app.domain.score.GoalAlignmentResult
import com.digitalbalance.app.domain.score.GoalAlignmentStatus
import com.digitalbalance.app.domain.score.ScoreComponent
import com.digitalbalance.app.ui.components.LoadingContent
import com.digitalbalance.app.ui.components.PremiumCard
import com.digitalbalance.app.ui.components.ScoreRing
import com.digitalbalance.app.ui.components.ScreenHeader
import com.digitalbalance.app.ui.theme.digitalBalanceColors
import com.digitalbalance.app.ui.usage.GoalAlignmentUiState
import com.digitalbalance.app.ui.usage.ProductivityUiState
import kotlin.math.roundToInt

@Composable
fun ScoreDetailsScreen(
    goalAlignmentState: GoalAlignmentUiState,
    productivityState: ProductivityUiState,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    BackHandler(onBack = onBack)
    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(horizontal = 20.dp, vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            TextButton(onClick = onBack) { Text(stringResource(R.string.back_to_home)) }
            ScreenHeader(
                title = stringResource(R.string.score_details_title),
                subtitle = stringResource(R.string.score_details_description)
            )
        }

        item {
            ScoreSectionHeading(
                title = stringResource(R.string.productivity_score),
                description = stringResource(R.string.productivity_score_details_description)
            )
        }
        when (productivityState) {
            ProductivityUiState.Loading -> item { LoadingContent() }
            is ProductivityUiState.Result -> {
                item { ProductivitySummary(productivityState.productivity) }
                item { ProductivityCoverageCard(productivityState.productivity) }
                if (productivityState.productivity.components.isNotEmpty()) {
                    item { ComponentHeading(R.string.productivity_components) }
                    items(
                        items = productivityState.productivity.components,
                        key = { it.kind.name }
                    ) { component ->
                        ComponentCard(
                            name = component.name,
                            score = component.score,
                            explanation = component.explanation
                        )
                    }
                }
            }
        }

        item {
            ScoreSectionHeading(
                title = stringResource(R.string.goal_alignment_score),
                description = stringResource(R.string.goal_alignment_details_description)
            )
        }
        when (goalAlignmentState) {
            GoalAlignmentUiState.Loading -> item { LoadingContent() }
            is GoalAlignmentUiState.Result -> {
                item { GoalAlignmentSummaryCard(goalAlignmentState.alignment) }
                item { GoalAlignmentCoverageCard(goalAlignmentState.alignment) }
                if (goalAlignmentState.alignment.components.isNotEmpty()) {
                    item { ComponentHeading(R.string.goal_alignment_components) }
                    items(
                        items = goalAlignmentState.alignment.components,
                        key = ScoreComponent::id
                    ) { component ->
                        ComponentCard(
                            name = component.name,
                            score = component.score,
                            explanation = component.explanation
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun ScoreSectionHeading(title: String, description: String) {
    Column(
        modifier = Modifier.padding(top = 10.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold
        )
        Text(
            text = description,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun ProductivitySummary(result: ProductivityScoreResult) {
    ScoreSummaryCard(
        ready = result.status == ProductivityScoreStatus.Ready,
        score = result.score,
        reasons = result.reasons
    )
}

@Composable
private fun GoalAlignmentSummaryCard(result: GoalAlignmentResult) {
    ScoreSummaryCard(
        ready = result.status == GoalAlignmentStatus.Ready,
        score = result.score,
        reasons = result.reasons
    )
}

@Composable
private fun ScoreSummaryCard(
    ready: Boolean,
    score: Int?,
    reasons: List<String>
) {
    PremiumCard(Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(22.dp),
            horizontalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            ScoreRing(
                score = score.takeIf { ready },
                label = stringResource(R.string.out_of_100_short),
                color = MaterialTheme.colorScheme.primary,
                size = 104.dp
            )
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            if (ready) {
                Text(
                    text = stringResource(R.string.score_out_of_100, requireNotNull(score)),
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
            } else {
                Text(
                    text = stringResource(R.string.not_enough_data),
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                )
            }
            reasons.forEach { reason ->
                Text(
                    text = reason,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            }
        }
    }
}

@Composable
private fun ProductivityCoverageCard(result: ProductivityScoreResult) {
    CoverageCard(
        coverage = result.coverage.classifiedCoverage,
        confidence = result.coverage.confidence,
        confidenceLabel = result.coverage.confidenceLevel.name,
        note = stringResource(R.string.productivity_coverage_note)
    )
}

@Composable
private fun GoalAlignmentCoverageCard(result: GoalAlignmentResult) {
    CoverageCard(
        coverage = result.coverage.classificationCoverage,
        confidence = result.coverage.confidence,
        confidenceLabel = result.coverage.confidenceLevel.name,
        note = stringResource(R.string.goal_alignment_coverage_note)
    )
}

@Composable
private fun CoverageCard(
    coverage: Double,
    confidence: Double,
    confidenceLabel: String,
    note: String
) {
    val coveragePercent = (coverage * 100).roundToInt()
    val confidencePercent = (confidence * 100).roundToInt()
    PremiumCard(Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = stringResource(R.string.classification_coverage),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold
            )
            Text(
                text = stringResource(R.string.coverage_percent, coveragePercent),
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold
            )
            LinearProgressIndicator(
                progress = { coverage.toFloat() },
                modifier = Modifier.fillMaxWidth()
            )
            Text(
                text = stringResource(
                    R.string.score_confidence_value,
                    confidenceLabel,
                    confidencePercent
                ),
                style = MaterialTheme.typography.bodyMedium
            )
            Text(
                text = note,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun ComponentHeading(titleResource: Int) {
    Text(
        text = stringResource(titleResource),
        style = MaterialTheme.typography.titleMedium,
        fontWeight = FontWeight.SemiBold,
        modifier = Modifier.padding(top = 4.dp)
    )
}

@Composable
private fun ComponentCard(name: String, score: Int, explanation: String) {
    PremiumCard(Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = name,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.weight(1f)
                )
                Text(
                    text = stringResource(R.string.component_score_value, score),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
            }
            LinearProgressIndicator(
                progress = { score / 100f },
                modifier = Modifier.fillMaxWidth(),
                color = MaterialTheme.digitalBalanceColors.informational,
                trackColor = MaterialTheme.digitalBalanceColors.informational.copy(alpha = 0.14f)
            )
            Text(
                text = explanation,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}
