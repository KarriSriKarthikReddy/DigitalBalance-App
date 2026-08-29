package com.digitalbalance.app.ui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.digitalbalance.app.data.usage.AppUsage
import com.digitalbalance.app.R
import com.digitalbalance.app.domain.category.AppCategory
import com.digitalbalance.app.ui.theme.digitalBalanceColors

private data class RingSegment(val label: String, val duration: Long, val color: Color)

@Composable
fun ForegroundUsageRing(
    apps: List<AppUsage>,
    totalDurationMillis: Long,
    totalLabel: String,
    modifier: Modifier = Modifier
) {
    val colors = MaterialTheme.digitalBalanceColors
    val trackColor = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.55f)
    val productiveLabel = stringResource(R.string.ring_productive)
    val socialLabel = stringResource(R.string.category_social)
    val entertainmentLabel = stringResource(R.string.category_entertainment)
    val gamingLabel = stringResource(R.string.category_gaming)
    val communicationLabel = stringResource(R.string.ring_communication_utility)
    val mixedLabel = stringResource(R.string.ring_mixed_other)
    val centerLabel = stringResource(R.string.foreground_usage_center)
    val accessibilityLabel = stringResource(R.string.foreground_usage_accessibility, totalLabel)
    val segments = remember(
        apps, colors, productiveLabel, socialLabel, entertainmentLabel,
        gamingLabel, communicationLabel, mixedLabel
    ) {
        val totals = apps.groupingBy { it.category }.fold(0L) { acc, app -> acc + app.foregroundDurationMillis }
        listOf(
            RingSegment(productiveLabel, totals.duration(AppCategory.Education, AppCategory.Productivity), colors.productive),
            RingSegment(socialLabel, totals.duration(AppCategory.Social), colors.social),
            RingSegment(entertainmentLabel, totals.duration(AppCategory.Entertainment), colors.entertainment),
            RingSegment(gamingLabel, totals.duration(AppCategory.Gaming), colors.gaming),
            RingSegment(communicationLabel, totals.duration(AppCategory.Communication, AppCategory.Utility), colors.communication),
            RingSegment(mixedLabel, totals.duration(AppCategory.MixedContextDependent, AppCategory.Other), colors.mixed)
        ).filter { it.duration > 0L }
    }
    var started by rememberSaveable { mutableStateOf(false) }
    LaunchedEffect(Unit) { started = true }
    val reveal by animateFloatAsState(if (started) 1f else 0f, tween(700), label = "usageRing")

    Column(modifier, horizontalAlignment = Alignment.CenterHorizontally) {
        Box(
            modifier = Modifier.size(224.dp).semantics {
                contentDescription = accessibilityLabel
            },
            contentAlignment = Alignment.Center
        ) {
            Canvas(Modifier.matchParentSize().padding(12.dp)) {
                val stroke = 17.dp.toPx()
                drawArc(
                    color = trackColor,
                    startAngle = -90f,
                    sweepAngle = 360f,
                    useCenter = false,
                    style = Stroke(stroke, cap = StrokeCap.Round)
                )
                if (totalDurationMillis > 0L && segments.isNotEmpty()) {
                    val minimum = 2f
                    val raw = segments.map { 360f * it.duration / totalDurationMillis.coerceAtLeast(1L) }
                    val stable = raw.map { it.coerceAtLeast(minimum) }
                    val scale = 360f / stable.sum()
                    var start = -90f
                    segments.forEachIndexed { index, segment ->
                        val fullSweep = stable[index] * scale
                        val gap = minOf(2.2f, fullSweep * 0.22f)
                        drawArc(
                            color = segment.color,
                            startAngle = start + gap / 2f,
                            sweepAngle = (fullSweep - gap).coerceAtLeast(0.5f) * reveal,
                            useCenter = false,
                            style = Stroke(stroke, cap = StrokeCap.Round)
                        )
                        start += fullSweep
                    }
                }
            }
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(totalLabel, style = MaterialTheme.typography.displaySmall, fontWeight = FontWeight.SemiBold)
                Text(centerLabel, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
        Column(
            modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            segments.chunked(2).forEach { pair ->
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                    pair.forEach { segment -> RingLegendItem(segment, Modifier.weight(1f)) }
                    if (pair.size == 1) Box(Modifier.weight(1f))
                }
            }
        }
    }
}

@Composable
private fun RingLegendItem(segment: RingSegment, modifier: Modifier) {
    Row(modifier, verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(7.dp)) {
        CategoryDot(segment.color)
        Column {
            Text(segment.label, style = MaterialTheme.typography.labelMedium, maxLines = 1)
            Text(usageDuration(segment.duration), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

private fun Map<AppCategory, Long>.duration(vararg categories: AppCategory): Long =
    categories.sumOf { this[it] ?: 0L }
