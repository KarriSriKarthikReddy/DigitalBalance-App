package com.digitalbalance.app.domain.reminder

import com.digitalbalance.app.domain.category.AppCategory
import com.digitalbalance.app.domain.goal.GoalProgress
import com.digitalbalance.app.domain.goal.GoalType
import com.digitalbalance.app.domain.productivity.ProductivityScorePolicy
import kotlin.math.roundToInt

data class DailySummaryApp(
    val appName: String,
    val durationMillis: Long
)

data class DailySummaryCategory(
    val category: AppCategory,
    val durationMillis: Long
)

data class DailySummary(
    val dateKey: String,
    val foregroundUsageMillis: Long,
    val productivityScore: Int?,
    val goalAlignmentScore: Int?,
    val topApp: DailySummaryApp?,
    val topCategory: DailySummaryCategory?,
    val goalProgress: List<GoalProgress>,
    val completedFocusSessions: Int,
    val classificationCoverage: Double?
)

data class DailySummaryInput(
    val dateKey: String,
    val foregroundUsageMillis: Long,
    val apps: List<ReminderAppUsage>,
    val productivityScore: Int?,
    val goalAlignmentScore: Int?,
    val goalProgress: List<GoalProgress>,
    val completedFocusSessions: Int,
    val classificationCoverage: Double?
)

class DailySummaryGenerator {
    fun generate(input: DailySummaryInput): DailySummary {
        val validApps = input.apps.map { it.copy(durationMillis = it.durationMillis.coerceAtLeast(0L)) }
        val topApp = validApps.maxByOrNull(ReminderAppUsage::durationMillis)
            ?.takeIf { it.durationMillis > 0L }
            ?.let { DailySummaryApp(it.appName, it.durationMillis) }
        val topCategory = validApps
            .filterNot { it.packageName in ProductivityScorePolicy.EXCLUDED_PACKAGES }
            .groupBy(ReminderAppUsage::category)
            .mapValues { (_, apps) -> apps.sumOf(ReminderAppUsage::durationMillis) }
            .maxByOrNull { it.value }
            ?.takeIf { it.value > 0L }
            ?.let { DailySummaryCategory(it.key, it.value) }
        return DailySummary(
            dateKey = input.dateKey,
            foregroundUsageMillis = input.foregroundUsageMillis.coerceAtLeast(0L),
            productivityScore = input.productivityScore?.coerceIn(0, 100),
            goalAlignmentScore = input.goalAlignmentScore?.coerceIn(0, 100),
            topApp = topApp,
            topCategory = topCategory,
            goalProgress = input.goalProgress,
            completedFocusSessions = input.completedFocusSessions.coerceAtLeast(0),
            classificationCoverage = input.classificationCoverage?.coerceIn(0.0, 1.0)
        )
    }
}

object DailySummaryFormatter {
    fun notificationBody(summary: DailySummary): String = buildList {
        add("Today: ${formatDuration(summary.foregroundUsageMillis)} foreground usage")
        summary.productivityScore?.let { add("Productivity $it") }
        summary.goalAlignmentScore?.let { add("Goal Alignment $it") }
    }.joinToString(" · ")

    fun detailLines(summary: DailySummary): List<String> = buildList {
        add("${formatDuration(summary.foregroundUsageMillis)} foreground usage")
        summary.productivityScore?.let { add("Productivity: $it") }
        summary.goalAlignmentScore?.let { add("Goal Alignment: $it") }
        summary.topApp?.let { add("Top app: ${it.appName} — ${formatDuration(it.durationMillis)}") }
        summary.topCategory?.let {
            add("Top category: ${categoryName(it.category)} — ${formatDuration(it.durationMillis)}")
        }
        summary.goalProgress.forEach { progress ->
            val current = progress.currentDurationMillis ?: return@forEach
            val label = goalName(progress)
            val status = if (progress.goal.type.isMinimumTarget) {
                if (current >= progress.goal.targetDurationMillis) "target reached" else "in progress"
            } else {
                if (current <= progress.goal.targetDurationMillis) "within target" else "limit reached"
            }
            add("$label: $status")
        }
        if (summary.completedFocusSessions > 0) {
            val noun = if (summary.completedFocusSessions == 1) "session" else "sessions"
            add("Focus: ${summary.completedFocusSessions} completed $noun")
        }
        summary.classificationCoverage?.let {
            add("Classification coverage: ${(it * 100.0).roundToInt()}%")
        }
    }

    private fun goalName(progress: GoalProgress): String = when (progress.goal.type) {
        GoalType.OverallForegroundUsage -> "Overall usage"
        GoalType.ProductiveTime -> "Productive target"
        GoalType.SocialMediaLimit -> "Social limit"
        GoalType.EntertainmentLimit -> "Entertainment limit"
        GoalType.AppDailyLimit -> "${progress.goal.appName ?: "App"} limit"
    }

    private fun categoryName(category: AppCategory): String = when (category) {
        AppCategory.Education -> "Education"
        AppCategory.Productivity -> "Productivity"
        AppCategory.Communication -> "Communication"
        AppCategory.Social -> "Social"
        AppCategory.Entertainment -> "Entertainment"
        AppCategory.Gaming -> "Gaming"
        AppCategory.Utility -> "Utility"
        AppCategory.Other -> "Other"
        AppCategory.MixedContextDependent -> "Mixed / Context-dependent"
    }

    private fun formatDuration(durationMillis: Long): String {
        val totalMinutes = durationMillis.coerceAtLeast(0L) / 60_000L
        return when {
            totalMinutes < 1L -> "<1 min"
            totalMinutes < 60L -> "$totalMinutes min"
            totalMinutes % 60L == 0L -> "${totalMinutes / 60L}h"
            else -> "${totalMinutes / 60L}h ${totalMinutes % 60L}m"
        }
    }
}
