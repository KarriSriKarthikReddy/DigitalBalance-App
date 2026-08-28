package com.digitalbalance.app.ui.apps

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
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.unit.dp
import com.digitalbalance.app.R
import com.digitalbalance.app.data.usage.AppUsage
import com.digitalbalance.app.domain.category.AppCategory
import com.digitalbalance.app.ui.category.labelRes
import com.digitalbalance.app.ui.components.AppIcon
import com.digitalbalance.app.ui.components.CompactAppRow
import com.digitalbalance.app.ui.components.LoadingContent
import com.digitalbalance.app.ui.components.MessageContent
import com.digitalbalance.app.ui.components.usageDuration
import com.digitalbalance.app.ui.usage.UsagePermissionCard
import com.digitalbalance.app.ui.usage.UsageUiState

@Composable
fun AppsScreen(
    state: UsageUiState,
    onOpenUsageSettings: () -> Unit,
    onRefresh: () -> Unit,
    onCategoryChanged: (String, AppCategory) -> Unit,
    modifier: Modifier = Modifier
) {
    var selectedPackage by rememberSaveable { mutableStateOf<String?>(null) }
    val selectedApp = (state as? UsageUiState.Content)?.apps
        ?.firstOrNull { it.packageName == selectedPackage }

    BackHandler(enabled = selectedApp != null) { selectedPackage = null }

    if (selectedApp != null) {
        AppDetail(
            usage = selectedApp,
            onBack = { selectedPackage = null },
            onCategoryChanged = { onCategoryChanged(selectedApp.packageName, it) },
            modifier = modifier
        )
        return
    }

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
                    onClick = { selectedPackage = usage.packageName },
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceContainerLow
                    )
                ) {
                    CompactAppRow(
                        usage = usage,
                        modifier = Modifier.padding(horizontal = 16.dp),
                        showOpenCount = true,
                        showCategory = true
                    )
                }
            }
        }
    }
}

@Composable
private fun AppDetail(
    usage: AppUsage,
    onBack: () -> Unit,
    onCategoryChanged: (AppCategory) -> Unit,
    modifier: Modifier = Modifier
) {
    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(horizontal = 20.dp, vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            TextButton(onClick = onBack) {
                Text(stringResource(R.string.back_to_apps))
            }
        }
        item {
            Row(
                modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                AppIcon(usage = usage, size = 64.dp)
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(
                        text = usage.appName,
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = stringResource(usage.category.labelRes()),
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }
        }
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceContainerLow
                )
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(20.dp),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    DetailMetric(
                        label = stringResource(R.string.todays_usage),
                        value = usageDuration(usage.foregroundDurationMillis)
                    )
                    DetailMetric(
                        label = stringResource(R.string.app_opens),
                        value = usage.openCount.toString()
                    )
                }
            }
        }
        item {
            Text(
                text = stringResource(R.string.choose_category),
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.padding(top = 12.dp)
            )
            Text(
                text = stringResource(R.string.category_override_note),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        items(AppCategory.entries, key = AppCategory::storageKey) { category ->
            val selected = category == usage.category
            Card(
                onClick = { onCategoryChanged(category) },
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = if (selected) {
                        MaterialTheme.colorScheme.primaryContainer
                    } else {
                        MaterialTheme.colorScheme.surfaceContainerLow
                    }
                )
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = stringResource(category.labelRes()),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal
                    )
                    if (selected) {
                        Text(
                            text = stringResource(R.string.selected),
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun DetailMetric(label: String, value: String) {
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            text = value,
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.SemiBold
        )
    }
}
