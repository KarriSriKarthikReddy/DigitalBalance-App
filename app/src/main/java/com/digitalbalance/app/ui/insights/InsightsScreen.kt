package com.digitalbalance.app.ui.insights

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.digitalbalance.app.R
import com.digitalbalance.app.domain.insight.InsightActionType
import com.digitalbalance.app.domain.insight.InsightSeverity
import com.digitalbalance.app.domain.insight.PersonalInsight
import com.digitalbalance.app.ui.components.LoadingContent
import com.digitalbalance.app.ui.components.InsightLeadingIcon
import com.digitalbalance.app.ui.components.PremiumCard
import com.digitalbalance.app.ui.components.ScreenHeader
import com.digitalbalance.app.ui.theme.DigitalBalanceSpacing
import com.digitalbalance.app.ui.theme.digitalBalanceColors
import com.digitalbalance.app.ui.usage.InsightUiState

@Composable
fun InsightsScreen(
    state: InsightUiState,
    onAction: (InsightActionType) -> Unit,
    modifier: Modifier = Modifier
) {
    when (state) {
        InsightUiState.Loading -> Box(
            modifier = modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            LoadingContent()
        }
        is InsightUiState.Content -> if (state.insights.isEmpty()) {
            EmptyInsights(modifier)
        } else {
            InsightList(state.insights, onAction, modifier)
        }
    }
}

@Composable
private fun InsightList(
    insights: List<PersonalInsight>,
    onAction: (InsightActionType) -> Unit,
    modifier: Modifier
) {
    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(horizontal = DigitalBalanceSpacing.screen, vertical = DigitalBalanceSpacing.section),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            ScreenHeader(
                title = stringResource(R.string.insights_today_title),
                subtitle = stringResource(R.string.insights_today_description)
            )
        }
        items(items = insights, key = PersonalInsight::id) { insight ->
            InsightCard(insight, onAction)
        }
    }
}

@Composable
private fun InsightCard(
    insight: PersonalInsight,
    onAction: (InsightActionType) -> Unit
) {
    PremiumCard(Modifier.fillMaxWidth()) {
        Row(modifier = Modifier.fillMaxWidth().padding(20.dp), horizontalArrangement = Arrangement.spacedBy(14.dp)) {
            InsightLeadingIcon(R.drawable.ic_nav_insights, insight.accentColor())
            Column(modifier = Modifier.weight(1f)) {
            Text(
                text = insight.title,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.SemiBold
            )
            Spacer(Modifier.height(6.dp))
            Text(
                text = insight.description,
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            insight.supportingMetric?.let { metric ->
                Spacer(Modifier.height(10.dp))
                Text(
                    text = metric,
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.SemiBold
                )
            }
            insight.recommendation?.let { recommendation ->
                Spacer(Modifier.height(12.dp))
                Text(
                    text = recommendation,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            insight.actionType?.let { action ->
                Spacer(Modifier.height(4.dp))
                TextButton(onClick = { onAction(action) }) {
                    Text(stringResource(action.labelResource))
                }
            }
        }
    }
}
}

@Composable
private fun PersonalInsight.accentColor(): Color = when (severity) {
    InsightSeverity.Attention -> MaterialTheme.digitalBalanceColors.exceeded
    InsightSeverity.Progress -> MaterialTheme.colorScheme.primary
    InsightSeverity.Positive -> MaterialTheme.digitalBalanceColors.positive
    InsightSeverity.Informational -> MaterialTheme.digitalBalanceColors.informational
}

private val InsightActionType.labelResource: Int
    get() = when (this) {
        InsightActionType.OpenGoals -> R.string.insight_action_goals
        InsightActionType.OpenApps -> R.string.insight_action_apps
        InsightActionType.ReviewCategories -> R.string.insight_action_categories
        InsightActionType.OpenFocus -> R.string.insight_action_focus
    }

@Composable
private fun EmptyInsights(modifier: Modifier) {
    Box(
        modifier = modifier.fillMaxSize().padding(28.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = Modifier.widthIn(max = 460.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = stringResource(R.string.nav_insights),
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.primary
            )
            Text(
                text = stringResource(R.string.insights_empty_title),
                modifier = Modifier.padding(top = 10.dp),
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center
            )
            Text(
                text = stringResource(R.string.insights_empty_description),
                modifier = Modifier.padding(top = 12.dp),
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )
        }
    }
}
