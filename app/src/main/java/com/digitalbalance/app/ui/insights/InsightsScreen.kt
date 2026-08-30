package com.digitalbalance.app.ui.insights

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.digitalbalance.app.R
import com.digitalbalance.app.data.usage.AppUsage
import com.digitalbalance.app.domain.analytics.AnalyticsCategoryTotal
import com.digitalbalance.app.domain.analytics.AnalyticsCategoryTrend
import com.digitalbalance.app.domain.analytics.AnalyticsComparison
import com.digitalbalance.app.domain.analytics.AnalyticsDayDetail
import com.digitalbalance.app.domain.analytics.AnalyticsDayTotal
import com.digitalbalance.app.domain.analytics.AnalyticsAppTrend
import com.digitalbalance.app.domain.analytics.AnalyticsPeriod
import com.digitalbalance.app.domain.analytics.AnalyticsProductivityPoint
import com.digitalbalance.app.domain.analytics.AnalyticsSnapshot
import com.digitalbalance.app.domain.analytics.AnalyticsTrendPoint
import com.digitalbalance.app.domain.analytics.AnalyticsWeeklyComparison
import com.digitalbalance.app.domain.insight.InsightActionType
import com.digitalbalance.app.domain.insight.InsightSeverity
import com.digitalbalance.app.domain.insight.PersonalInsight
import com.digitalbalance.app.domain.category.AppCategory
import com.digitalbalance.app.ui.category.labelRes
import com.digitalbalance.app.ui.components.CompactAppRow
import com.digitalbalance.app.ui.components.InsightLeadingIcon
import com.digitalbalance.app.ui.components.LoadingContent
import com.digitalbalance.app.ui.components.MessageContent
import com.digitalbalance.app.ui.components.PremiumCard
import com.digitalbalance.app.ui.components.ScreenHeader
import com.digitalbalance.app.ui.components.SectionHeading
import com.digitalbalance.app.ui.components.StatusPill
import com.digitalbalance.app.ui.components.usageDuration
import com.digitalbalance.app.ui.theme.DigitalBalanceSpacing
import com.digitalbalance.app.ui.theme.accentColor
import com.digitalbalance.app.ui.theme.digitalBalanceColors
import com.digitalbalance.app.ui.usage.InsightUiState

@Composable
fun InsightsScreen(
    insightState: InsightUiState,
    analyticsState: AnalyticsUiState,
    onPeriodSelected: (AnalyticsPeriod) -> Unit,
    onAction: (InsightActionType) -> Unit,
    modifier: Modifier = Modifier
) {
    when (analyticsState) {
        AnalyticsUiState.Loading -> Box(modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            LoadingContent()
        }
        is AnalyticsUiState.Content -> AnalyticsContent(
            analyticsState,
            insightState,
            onPeriodSelected,
            onAction,
            modifier
        )
    }
}

@Composable
private fun AnalyticsContent(
    analyticsState: AnalyticsUiState.Content,
    insightState: InsightUiState,
    onPeriodSelected: (AnalyticsPeriod) -> Unit,
    onAction: (InsightActionType) -> Unit,
    modifier: Modifier
) {
    val snapshot = analyticsState.snapshot
    val interactive = snapshot.interactive
    var selectedDayKey by rememberSaveable { mutableStateOf<String?>(null) }
    var selectedAppPackage by rememberSaveable { mutableStateOf<String?>(null) }
    var selectedCategoryKey by rememberSaveable { mutableStateOf<String?>(null) }
    val availableDayKeys = remember(interactive.dayDetails) { interactive.dayDetails.keys }
    LaunchedEffect(snapshot.period, interactive.defaultSelectedDateKey, availableDayKeys) {
        if (
            snapshot.period == AnalyticsPeriod.Last7Days &&
            selectedDayKey !in availableDayKeys
        ) {
            selectedDayKey = interactive.defaultSelectedDateKey
        }
    }
    val selectedDay = selectedDayKey?.let(interactive.dayDetails::get)
    val selectedAppTrend = selectedAppPackage?.let(interactive.appTrends::get)
    val selectedCategoryTrend = selectedCategoryKey
        ?.let { AppCategory.fromStorageKey(it) }
        ?.let(interactive.categoryTrends::get)
    val openAppTrend: (String) -> Unit = { packageName ->
        if (packageName in interactive.appTrends) {
            selectedAppPackage = packageName
            selectedCategoryKey = null
            if (snapshot.period != AnalyticsPeriod.Last7Days) {
                onPeriodSelected(AnalyticsPeriod.Last7Days)
            }
        }
    }
    val openCategoryTrend: (AppCategory) -> Unit = { category ->
        if (category in interactive.categoryTrends) {
            selectedCategoryKey = category.storageKey
            selectedAppPackage = null
            if (snapshot.period != AnalyticsPeriod.Last7Days) {
                onPeriodSelected(AnalyticsPeriod.Last7Days)
            }
        }
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
                title = "History & analytics",
                subtitle = "Foreground app usage stored privately on this device"
            )
        }
        item { PeriodSelector(snapshot.period, onPeriodSelected) }
        item { CoveragePill(snapshot) }
        if (!snapshot.hasAvailableData) {
            item {
                PremiumCard(Modifier.fillMaxWidth()) {
                    MessageContent(
                        title = "No history available for this period",
                        description = "DigitalBalance will show a day after real foreground usage has been captured and saved.",
                        modifier = Modifier.padding(horizontal = 18.dp)
                    )
                }
            }
        } else {
            item { UsageSummary(snapshot) }
            snapshot.comparison?.let { comparison ->
                item { ComparisonCard(comparison.differenceMillis, comparison.percentDifference) }
            }
            if (snapshot.period == AnalyticsPeriod.Last7Days) {
                item {
                    DailyTrendCard(
                        days = snapshot.dayTotals,
                        selectedDateKey = selectedDay?.day?.dateKey,
                        onDaySelected = { selectedDayKey = it }
                    )
                }
                selectedDay?.let { detail ->
                    item { SelectedDayCard(detail, openAppTrend, openCategoryTrend) }
                }
                interactive.weeklyComparison?.let { comparison ->
                    item { WeeklyComparisonCard(comparison) }
                }
                if (interactive.productivityTrend.any { it.score != null }) {
                    item { ProductivityTrendCard(interactive.productivityTrend) }
                }
                selectedAppTrend?.let { trend ->
                    item { AppTrendCard(trend) { selectedAppPackage = null } }
                }
                selectedCategoryTrend?.let { trend ->
                    item { CategoryTrendCard(trend) { selectedCategoryKey = null } }
                }
            }
            if (snapshot.categoryTotals.isNotEmpty()) {
                item {
                    CategoryBreakdown(
                        snapshot.categoryTotals,
                        snapshot.totalDurationMillis,
                        openCategoryTrend
                    )
                }
            }
            if (analyticsState.rankedApps.isNotEmpty()) {
                item {
                    TopAppsCard(
                        analyticsState.rankedApps,
                        snapshot.rankedApps.size,
                        openAppTrend
                    )
                }
            }
            snapshot.focusSummary?.let { summary ->
                item { FocusSummaryCard(summary.completedSessions, summary.focusedDurationMillis) }
            }
        }

        item {
            SectionHeading(
                stringResource(R.string.insights_today_title),
                Modifier.padding(top = 6.dp)
            )
        }
        when (insightState) {
            InsightUiState.Loading -> item { LoadingContent(Modifier.padding(vertical = 8.dp)) }
            is InsightUiState.Content -> if (insightState.insights.isEmpty()) {
                item {
                    Text(
                        text = "No notable pattern yet today. Insights use only real on-device activity.",
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            } else {
                items(insightState.insights, key = PersonalInsight::id) { insight ->
                    InsightCard(insight, onAction)
                }
            }
        }
    }
}

@Composable
private fun PeriodSelector(selected: AnalyticsPeriod, onSelected: (AnalyticsPeriod) -> Unit) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        AnalyticsPeriod.entries.forEach { period ->
            FilterChip(
                selected = selected == period,
                onClick = { onSelected(period) },
                label = {
                    Text(
                        when (period) {
                            AnalyticsPeriod.Today -> "Today"
                            AnalyticsPeriod.Yesterday -> "Yesterday"
                            AnalyticsPeriod.Last7Days -> "Last 7 days"
                        }
                    )
                }
            )
        }
    }
}

@Composable
private fun CoveragePill(snapshot: AnalyticsSnapshot) {
    val text = when (snapshot.period) {
        AnalyticsPeriod.Last7Days -> if (snapshot.availableDayCount == snapshot.expectedDayCount) {
            "7 of 7 days available"
        } else {
            "History available for ${snapshot.availableDayCount} of the last 7 days"
        }
        else -> if (snapshot.hasAvailableData) "Usage data available" else "Usage data unavailable"
    }
    StatusPill(text, MaterialTheme.colorScheme.primary)
}

@Composable
private fun UsageSummary(snapshot: AnalyticsSnapshot) {
    PremiumCard(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(DigitalBalanceSpacing.card), verticalArrangement = Arrangement.spacedBy(16.dp)) {
            Text("Foreground app usage", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.SemiBold)
            Text(
                usageDuration(snapshot.totalDurationMillis),
                style = MaterialTheme.typography.displaySmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                snapshot.dailyAverageMillis?.let {
                    SummaryMetric("Daily average", usageDuration(it), Modifier.weight(1f))
                }
                SummaryMetric("Active apps", snapshot.activeAppCount.toString(), Modifier.weight(1f))
            }
            snapshot.mostUsedApp?.let {
                SummaryLine("Most used app", it.appName, usageDuration(it.durationMillis))
            }
            snapshot.mostUsedCategory?.let {
                SummaryLine(
                    "Top category",
                    androidx.compose.ui.res.stringResource(it.category.labelRes()),
                    usageDuration(it.durationMillis)
                )
            }
        }
    }
}

@Composable
private fun SummaryMetric(label: String, value: String, modifier: Modifier = Modifier) {
    Column(modifier) {
        Text(value, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.SemiBold)
        Text(label, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
private fun SummaryLine(label: String, value: String, trailing: String) {
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Column(Modifier.weight(1f)) {
            Text(label, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text(value, style = MaterialTheme.typography.titleMedium, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
        Text(trailing, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
    }
}

@Composable
private fun ComparisonCard(differenceMillis: Long, percentDifference: Int?) {
    val absolute = kotlin.math.abs(differenceMillis)
    val description = when {
        differenceMillis > 0L -> "${usageDuration(absolute)} more than yesterday"
        differenceMillis < 0L -> "${usageDuration(absolute)} less than yesterday"
        else -> "The same foreground usage as yesterday"
    }
    PremiumCard(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(DigitalBalanceSpacing.card), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text("Compared with yesterday", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary)
            Text(description, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
            percentDifference?.let {
                if (it != 0) {
                    Text(
                        "${kotlin.math.abs(it)}% ${if (it > 0) "more" else "less"}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

@Composable
private fun DailyTrendCard(
    days: List<AnalyticsDayTotal>,
    selectedDateKey: String?,
    onDaySelected: (String) -> Unit
) {
    val maxDuration = days.mapNotNull(AnalyticsDayTotal::durationMillis).maxOrNull()?.coerceAtLeast(1L) ?: 1L
    val availableColor = MaterialTheme.colorScheme.secondary
    val todayColor = MaterialTheme.colorScheme.primary
    val missingColor = MaterialTheme.colorScheme.outlineVariant
    var started by rememberSaveable { mutableStateOf(false) }
    LaunchedEffect(Unit) { started = true }
    val progress by animateFloatAsState(if (started) 1f else 0f, tween(500), label = "historyBars")
    val summary = days.joinToString(" · ") { day ->
        val selection = if (day.day.dateKey == selectedDateKey) ", selected" else ""
        "${day.day.shortLabel}: ${day.durationMillis?.let(::plainDuration) ?: "unavailable"}$selection"
    }

    PremiumCard(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(DigitalBalanceSpacing.card), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text("Daily trend", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.SemiBold)
            Box(Modifier.fillMaxWidth().height(144.dp)) {
                Canvas(
                    Modifier.fillMaxSize()
                        .pointerInput(days) {
                            detectTapGestures { position ->
                                if (days.isEmpty()) return@detectTapGestures
                                val index = (position.x / (size.width / days.size)).toInt()
                                    .coerceIn(0, days.lastIndex)
                                days[index].takeIf { it.durationMillis != null }
                                    ?.let { onDaySelected(it.day.dateKey) }
                            }
                        }
                        .semantics { contentDescription = summary }
                ) {
                val slotWidth = size.width / days.size
                val barWidth = slotWidth * 0.48f
                val chartHeight = size.height - 8.dp.toPx()
                days.forEachIndexed { index, item ->
                    val left = index * slotWidth + (slotWidth - barWidth) / 2f
                    val duration = item.durationMillis
                    if (duration == null) {
                        drawRoundRect(
                            color = missingColor,
                            topLeft = Offset(left, chartHeight - 10.dp.toPx()),
                            size = Size(barWidth, 10.dp.toPx()),
                            cornerRadius = CornerRadius(5.dp.toPx()),
                            style = Stroke(1.5.dp.toPx())
                        )
                    } else {
                        val height = ((duration.toFloat() / maxDuration) * chartHeight * progress)
                            .coerceAtLeast(4.dp.toPx())
                        val selected = item.day.dateKey == selectedDateKey
                        drawRoundRect(
                            color = if (selected) todayColor else availableColor,
                            topLeft = Offset(left, chartHeight - height),
                            size = Size(barWidth, height),
                            cornerRadius = CornerRadius(7.dp.toPx())
                        )
                        if (selected) {
                            drawRoundRect(
                                color = todayColor.copy(alpha = 0.45f),
                                topLeft = Offset(left - 3.dp.toPx(), chartHeight - height - 3.dp.toPx()),
                                size = Size(barWidth + 6.dp.toPx(), height + 6.dp.toPx()),
                                cornerRadius = CornerRadius(9.dp.toPx()),
                                style = Stroke(2.dp.toPx())
                            )
                        }
                    }
                    }
                }
                Row(Modifier.fillMaxSize()) {
                    days.forEach { item ->
                        val available = item.durationMillis != null
                        val isSelected = item.day.dateKey == selectedDateKey
                        Box(
                            Modifier.weight(1f).fillMaxHeight()
                                .semantics {
                                    contentDescription = "${item.day.shortLabel}, " +
                                        (item.durationMillis?.let(::plainDuration) ?: "unavailable")
                                    selected = isSelected
                                }
                                .clickable(enabled = available) {
                                    onDaySelected(item.day.dateKey)
                                }
                        )
                    }
                }
            }
            Row(Modifier.fillMaxWidth()) {
                days.forEach { item ->
                    Text(
                        item.day.shortLabel,
                        modifier = Modifier.weight(1f),
                        style = MaterialTheme.typography.labelSmall,
                        color = if (item.day.isToday) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                        fontWeight = if (item.day.isToday) FontWeight.Bold else FontWeight.Normal,
                        textAlign = TextAlign.Center
                    )
                }
            }
            Text(summary, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
private fun SelectedDayCard(
    detail: AnalyticsDayDetail,
    onAppSelected: (String) -> Unit,
    onCategorySelected: (AppCategory) -> Unit
) {
    PremiumCard(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(DigitalBalanceSpacing.card), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Column {
                Text(
                    "${detail.day.longLabel} · ${detail.day.dateKey}",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    "${usageDuration(detail.totalDurationMillis)} foreground usage",
                    style = MaterialTheme.typography.headlineSmall,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.Bold
                )
            }
            ComparisonText(detail.comparison)
            detail.topApp?.let { app ->
                DetailLine(
                    label = "Top app",
                    value = app.appName,
                    trailing = usageDuration(app.durationMillis),
                    modifier = Modifier.clickable { onAppSelected(app.packageName) }
                )
            }
            detail.topCategory?.let { category ->
                DetailLine(
                    label = "Top category",
                    value = stringResource(category.category.labelRes()),
                    trailing = usageDuration(category.durationMillis),
                    modifier = Modifier.clickable { onCategorySelected(category.category) }
                )
            }
            Text(
                "${detail.activeAppCount} apps used",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            if (detail.classifiedCoverage >= 0.40) {
                Text(
                    "Education + Productivity: ${usageDuration(detail.productiveDurationMillis)}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            } else {
                Text(
                    "Productive category summary unavailable because classification coverage is limited.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            if (detail.topApps.isNotEmpty()) {
                Text("Top apps that day", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                detail.topApps.forEachIndexed { index, app ->
                    Row(
                        Modifier.fillMaxWidth().clickable { onAppSelected(app.packageName) },
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("${index + 1}.", Modifier.width(26.dp), color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text(app.appName, Modifier.weight(1f), maxLines = 1, overflow = TextOverflow.Ellipsis)
                        Text(usageDuration(app.durationMillis), fontWeight = FontWeight.SemiBold)
                    }
                }
            }
        }
    }
}

@Composable
private fun DetailLine(
    label: String,
    value: String,
    trailing: String,
    modifier: Modifier = Modifier
) {
    Row(modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Column(Modifier.weight(1f)) {
            Text(label, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text(value, style = MaterialTheme.typography.titleMedium, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
        Text(trailing, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
    }
}

@Composable
private fun ComparisonText(comparison: AnalyticsComparison?) {
    if (comparison == null) {
        Text(
            "No previous day available for comparison.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        return
    }
    val difference = comparison.differenceMillis
    val baseline = comparison.baselineLabel ?: comparison.baselineDateKey
    val message = when {
        difference > 0L -> "${usageDuration(difference)} more than $baseline"
        difference < 0L -> "${usageDuration(kotlin.math.abs(difference))} less than $baseline"
        else -> "About the same as $baseline"
    }
    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
        Text(message, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
        comparison.percentDifference?.takeIf { it != 0 }?.let { percent ->
            Text(
                "${if (percent > 0) "+" else ""}$percent%",
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.primary
            )
        }
    }
}

@Composable
private fun WeeklyComparisonCard(comparison: AnalyticsWeeklyComparison) {
    PremiumCard(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(DigitalBalanceSpacing.card), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text("Completed-week comparison", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.SemiBold)
            Text(
                "The latest seven complete days are compared with the seven before them.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                SummaryMetric("Recent period", usageDuration(comparison.recentTotalMillis), Modifier.weight(1f))
                SummaryMetric("Previous period", usageDuration(comparison.previousTotalMillis), Modifier.weight(1f))
            }
            val wording = when {
                comparison.differenceMillis > 0L -> "${usageDuration(comparison.differenceMillis)} more foreground usage"
                comparison.differenceMillis < 0L -> "${usageDuration(kotlin.math.abs(comparison.differenceMillis))} less foreground usage"
                else -> "About the same foreground usage"
            }
            Text(wording, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
        }
    }
}

@Composable
private fun ProductivityTrendCard(points: List<AnalyticsProductivityPoint>) {
    val readyCount = points.count { it.score != null }
    val chartPoints = points.map { AnalyticsTrendPoint(it.day, it.score?.toLong()) }
    val confidenceSummary = points.joinToString(" · ") { point ->
        if (point.score == null) {
            "${point.day.shortLabel}: unavailable"
        } else {
            "${point.day.shortLabel}: ${point.score}, ${point.confidence?.name?.lowercase()} confidence"
        }
    }
    PremiumCard(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(DigitalBalanceSpacing.card), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text("Productivity trend", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.SemiBold)
            Text(
                "Actual usage balance · same 0–100 engine as Today",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            TrendBars(chartPoints, fixedMaximum = 100L, valueFormatter = Long::toString)
            Text(
                confidenceSummary,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                "$readyCount of ${points.size} available days had enough classified usage to score. Goal Alignment is not reconstructed historically.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun AppTrendCard(trend: AnalyticsAppTrend, onClose: () -> Unit) {
    TrendDetailCard(
        title = "Last 7 days for ${trend.appName}",
        points = trend.points,
        total = trend.totalDurationMillis,
        average = trend.dailyAverageMillis,
        availableDays = trend.availableDayCount,
        comparison = trend.comparison,
        onClose = onClose
    )
}

@Composable
private fun CategoryTrendCard(trend: AnalyticsCategoryTrend, onClose: () -> Unit) {
    TrendDetailCard(
        title = stringResource(trend.category.labelRes()),
        points = trend.points,
        total = trend.totalDurationMillis,
        average = trend.dailyAverageMillis,
        availableDays = trend.availableDayCount,
        comparison = trend.comparison,
        onClose = onClose
    )
}

@Composable
private fun TrendDetailCard(
    title: String,
    points: List<AnalyticsTrendPoint>,
    total: Long,
    average: Long?,
    availableDays: Int,
    comparison: AnalyticsComparison?,
    onClose: () -> Unit
) {
    PremiumCard(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(DigitalBalanceSpacing.card), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Text(title, Modifier.weight(1f), style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.SemiBold)
                TextButton(onClick = onClose) { Text("Close") }
            }
            TrendBars(points)
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                SummaryMetric("7-day total", usageDuration(total), Modifier.weight(1f))
                average?.let {
                    SummaryMetric("Daily average", usageDuration(it), Modifier.weight(1f))
                }
            }
            Text(
                "$availableDays of 7 days available",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            ComparisonText(comparison)
        }
    }
}

@Composable
private fun TrendBars(
    points: List<AnalyticsTrendPoint>,
    fixedMaximum: Long? = null,
    valueFormatter: (Long) -> String = ::plainDuration
) {
    val max = fixedMaximum ?: points.mapNotNull(AnalyticsTrendPoint::durationMillis)
        .maxOrNull()?.coerceAtLeast(1L) ?: 1L
    val color = MaterialTheme.colorScheme.primary
    val missing = MaterialTheme.colorScheme.outlineVariant
    val summary = points.joinToString(" · ") {
        "${it.day.shortLabel}: ${it.durationMillis?.let(valueFormatter) ?: "unavailable"}"
    }
    Canvas(Modifier.fillMaxWidth().height(112.dp).semantics { contentDescription = summary }) {
        val slot = size.width / points.size.coerceAtLeast(1)
        val width = slot * 0.45f
        points.forEachIndexed { index, point ->
            val left = index * slot + (slot - width) / 2f
            val value = point.durationMillis
            if (value == null) {
                drawRoundRect(
                    missing,
                    Offset(left, size.height - 8.dp.toPx()),
                    Size(width, 8.dp.toPx()),
                    CornerRadius(4.dp.toPx()),
                    style = Stroke(1.dp.toPx())
                )
            } else {
                val height = ((value.toFloat() / max) * size.height).coerceAtLeast(3.dp.toPx())
                drawRoundRect(
                    color,
                    Offset(left, size.height - height),
                    Size(width, height),
                    CornerRadius(5.dp.toPx())
                )
            }
        }
    }
    Row(Modifier.fillMaxWidth()) {
        points.forEach {
            Text(
                it.day.shortLabel,
                Modifier.weight(1f),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )
        }
    }
    Text(summary, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
}

@Composable
private fun CategoryBreakdown(
    categories: List<AnalyticsCategoryTotal>,
    total: Long,
    onCategorySelected: (AppCategory) -> Unit
) {
    PremiumCard(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(DigitalBalanceSpacing.card), verticalArrangement = Arrangement.spacedBy(14.dp)) {
            Text("Category breakdown", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.SemiBold)
            categories.forEach { item ->
                val color = item.category.accentColor()
                Column(
                    modifier = Modifier.fillMaxWidth().clickable { onCategorySelected(item.category) },
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Row(Modifier.fillMaxWidth()) {
                        Text(
                            androidx.compose.ui.res.stringResource(item.category.labelRes()),
                            Modifier.weight(1f),
                            style = MaterialTheme.typography.bodyMedium
                        )
                        Text(usageDuration(item.durationMillis), style = MaterialTheme.typography.labelLarge)
                    }
                    Box(Modifier.fillMaxWidth().height(6.dp).background(MaterialTheme.colorScheme.outlineVariant, CircleShape)) {
                        Box(
                            Modifier.fillMaxWidth(
                                if (total > 0L) (item.durationMillis.toFloat() / total).coerceIn(0f, 1f) else 0f
                            ).height(6.dp).background(color, CircleShape)
                        )
                    }
                }
            }
            Text(
                "Current category overrides are applied across stored history.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun TopAppsCard(
    apps: List<AppUsage>,
    totalAppCount: Int,
    onAppSelected: (String) -> Unit
) {
    PremiumCard(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(DigitalBalanceSpacing.card)) {
            SectionHeading(if (totalAppCount > apps.size) "Top ${apps.size} apps" else "Top apps")
            val max = apps.firstOrNull()?.foregroundDurationMillis
            apps.forEach { app ->
                CompactAppRow(
                    app,
                    modifier = Modifier.clickable { onAppSelected(app.packageName) },
                    showCategory = true,
                    maxDurationMillis = max
                )
            }
        }
    }
}

@Composable
private fun FocusSummaryCard(completedSessions: Int, focusedDurationMillis: Long) {
    PremiumCard(Modifier.fillMaxWidth()) {
        Row(Modifier.fillMaxWidth().padding(DigitalBalanceSpacing.card), verticalAlignment = Alignment.CenterVertically) {
            Box(
                Modifier.size(42.dp).background(MaterialTheme.colorScheme.primaryContainer, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Text(completedSessions.toString(), fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onPrimaryContainer)
            }
            Spacer(Modifier.width(14.dp))
            Column(Modifier.weight(1f)) {
                Text("Completed focus sessions", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                Text(
                    "$completedSessions sessions · ${usageDuration(focusedDurationMillis)} focused",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun InsightCard(insight: PersonalInsight, onAction: (InsightActionType) -> Unit) {
    PremiumCard(Modifier.fillMaxWidth()) {
        Row(Modifier.fillMaxWidth().padding(20.dp), horizontalArrangement = Arrangement.spacedBy(14.dp)) {
            InsightLeadingIcon(R.drawable.ic_nav_insights, insight.accentColor())
            Column(Modifier.weight(1f)) {
                Text(insight.title, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.SemiBold)
                Spacer(Modifier.height(6.dp))
                Text(insight.description, style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
                insight.supportingMetric?.let {
                    Spacer(Modifier.height(10.dp))
                    Text(it, style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.SemiBold)
                }
                insight.recommendation?.let {
                    Spacer(Modifier.height(12.dp))
                    Text(it, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                insight.actionType?.let { action ->
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

private fun plainDuration(durationMillis: Long): String {
    val minutes = durationMillis / 60_000L
    return if (minutes < 60L) "${minutes}m" else "${minutes / 60L}h ${minutes % 60L}m"
}
