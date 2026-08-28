package com.digitalbalance.app.ui.apps

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.digitalbalance.app.R
import com.digitalbalance.app.data.usage.AppUsage
import com.digitalbalance.app.ui.components.CompactAppRow
import com.digitalbalance.app.ui.components.LoadingContent
import com.digitalbalance.app.ui.components.MessageContent
import com.digitalbalance.app.ui.usage.UsagePermissionCard
import com.digitalbalance.app.ui.usage.UsageUiState

@Composable
fun AppsScreen(
    state: UsageUiState,
    onOpenUsageSettings: () -> Unit,
    onRefresh: () -> Unit,
    modifier: Modifier = Modifier
) {
    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(horizontal = 20.dp, vertical = 24.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        item {
            Text(
                text = stringResource(R.string.all_apps_title),
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = stringResource(R.string.all_apps_subtitle),
                style = MaterialTheme.typography.bodyLarge,
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
            is UsageUiState.Content -> items(
                items = state.apps,
                key = AppUsage::packageName
            ) { usage ->
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceContainerLow
                    )
                ) {
                    CompactAppRow(
                        usage = usage,
                        modifier = Modifier.padding(horizontal = 16.dp),
                        showOpenCount = true
                    )
                }
            }
        }
    }
}
