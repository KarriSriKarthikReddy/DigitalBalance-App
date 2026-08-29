package com.digitalbalance.app.domain.insight

import com.digitalbalance.app.domain.category.AppCategory
import com.digitalbalance.app.domain.goal.GoalProgress
import com.digitalbalance.app.domain.goal.GoalType
import kotlin.math.max
import kotlin.math.roundToInt

class SmartInsightEngine {
    fun generate(input: InsightInput): List<PersonalInsight> {
        val totalDuration = input.totalForegroundDurationMillis.coerceAtLeast(0L)
        if (totalDuration < InsightPolicy.MIN_TOTAL_USAGE_MILLIS) return emptyList()

        val apps = input.apps.map { app ->
            app.copy(
                durationMillis = app.durationMillis.coerceAtLeast(0L),
                openCount = app.openCount.coerceAtLeast(0)
            )
        }
        val classificationCoverage = input.productivityScoreResult?.coverage?.classifiedCoverage
            ?: inputCoverage(apps, totalDuration)
        val candidates = buildList {
            addAll(goalInsights(input.goalProgress))
            addAll(classificationInsight(apps, classificationCoverage))
            addAll(dominantInsights(apps))
            addAll(frequentOpenInsight(apps))
        }

        return candidates
            .distinctBy(PersonalInsight::deduplicationKey)
            .sortedWith(
                compareByDescending<PersonalInsight>(PersonalInsight::priority)
                    .thenBy { it.type.ordinal }
                    .thenBy(PersonalInsight::id)
            )
            .take(InsightPolicy.MAX_INSIGHTS)
    }

    private fun goalInsights(progress: List<GoalProgress>): List<PersonalInsight> {
        val valid = progress
            .filter { it.goal.targetDurationMillis > 0L && it.currentDurationMillis != null }
            .sortedBy { it.goal.id }
        val limitGoals = valid.filter { !it.goal.type.isMinimumTarget }
        val limitObservations = limitGoals.mapNotNull(::limitInsight)

        return buildList {
            valid.firstOrNull { it.goal.type == GoalType.ProductiveTime }
                ?.let(::productiveInsight)
                ?.let(::add)
            addAll(limitObservations)
            if (
                limitGoals.isNotEmpty() &&
                limitObservations.isEmpty() &&
                limitGoals.all {
                    requireNotNull(it.currentDurationMillis) <= it.goal.targetDurationMillis
                }
            ) {
                add(allLimitsRespected(limitGoals))
            }
        }
    }

    private fun productiveInsight(progress: GoalProgress): PersonalInsight? {
        val current = requireNotNull(progress.currentDurationMillis).coerceAtLeast(0L)
        val target = progress.goal.targetDurationMillis
        val ratio = current.toDouble() / target
        return when {
            ratio >= 1.0 -> PersonalInsight(
                id = "goal_success:${progress.goal.id}",
                type = InsightType.GoalSuccess,
                priority = InsightPolicy.PRIORITY_SUCCESS,
                severity = InsightSeverity.Positive,
                title = "Productive target completed",
                description = "You reached your ${formatDuration(target)} productive-time target today.",
                supportingMetric = "100% complete",
                relatedGoalId = progress.goal.id,
                recommendation = "Keep the target as-is or adjust it if your routine changes.",
                actionType = InsightActionType.OpenGoals,
                deduplicationKey = "productive_goal:${progress.goal.id}"
            )
            current >= InsightPolicy.MIN_GOAL_PROGRESS_USAGE_MILLIS -> {
                val percent = percent(ratio)
                val remaining = (target - current).coerceAtLeast(0L)
                PersonalInsight(
                    id = "goal_progress:${progress.goal.id}",
                    type = InsightType.GoalProgress,
                    priority = InsightPolicy.PRIORITY_GOAL_PROGRESS,
                    severity = InsightSeverity.Progress,
                    title = "Productive target progress",
                    description = "Your productive target is $percent% complete, with ${formatDuration(remaining)} remaining.",
                    supportingMetric = "$percent% complete",
                    relatedGoalId = progress.goal.id,
                    recommendation = "Continue toward the target when it fits your day.",
                    actionType = InsightActionType.OpenGoals,
                    deduplicationKey = "productive_goal:${progress.goal.id}"
                )
            }
            else -> null
        }
    }

    private fun limitInsight(progress: GoalProgress): PersonalInsight? {
        val current = requireNotNull(progress.currentDurationMillis).coerceAtLeast(0L)
        val target = progress.goal.targetDurationMillis
        val ratio = current.toDouble() / target
        val overage = (current - target).coerceAtLeast(0L)
        val meaningfulOverage = max(
            InsightPolicy.MIN_LIMIT_OVERAGE_MILLIS.toDouble(),
            target * InsightPolicy.MIN_LIMIT_OVERAGE_RATIO
        ).toLong()
        val label = goalLabel(progress)

        return when {
            overage >= meaningfulOverage -> PersonalInsight(
                id = "goal_exceeded:${progress.goal.id}",
                type = InsightType.GoalExceeded,
                priority = InsightPolicy.PRIORITY_EXCEEDED,
                severity = InsightSeverity.Attention,
                title = "$label exceeded",
                description = "Today’s usage is ${formatDuration(overage)} above the configured ${formatDuration(target)} limit.",
                supportingMetric = "+${formatDuration(overage)}",
                relatedPackageName = progress.goal.packageName,
                relatedGoalId = progress.goal.id,
                recommendation = exceededRecommendation(progress.goal.type),
                actionType = exceededAction(progress.goal.type),
                deduplicationKey = "limit:${progress.goal.id}"
            )
            overage > 0L -> null
            ratio >= InsightPolicy.LIMIT_PROGRESS_RATIO -> {
                val remaining = (target - current).coerceAtLeast(0L)
                val nearing = ratio >= InsightPolicy.LIMIT_NEARING_RATIO
                PersonalInsight(
                    id = "goal_progress:${progress.goal.id}",
                    type = InsightType.GoalProgress,
                    priority = InsightPolicy.PRIORITY_GOAL_PROGRESS,
                    severity = InsightSeverity.Progress,
                    title = if (nearing) "$label is close" else "$label progress",
                    description = "You have used ${formatDuration(current)} of the ${formatDuration(target)} limit; ${formatDuration(remaining)} remains.",
                    supportingMetric = "${percent(ratio)}% used",
                    relatedPackageName = progress.goal.packageName,
                    relatedGoalId = progress.goal.id,
                    recommendation = if (nearing) {
                        "Review the goal if today’s usage was intentional."
                    } else {
                        null
                    },
                    actionType = if (nearing) InsightActionType.OpenGoals else null,
                    deduplicationKey = "limit:${progress.goal.id}"
                )
            }
            else -> null
        }
    }

    private fun allLimitsRespected(limitGoals: List<GoalProgress>): PersonalInsight =
        PersonalInsight(
            id = "goal_success:all_limits",
            type = InsightType.GoalSuccess,
            priority = InsightPolicy.PRIORITY_SUCCESS,
            severity = InsightSeverity.Positive,
            title = "Configured limits are on track",
            description = "You’re currently within all ${limitGoals.size} configured daily limits.",
            supportingMetric = "${limitGoals.size} limits respected",
            recommendation = null,
            actionType = null,
            deduplicationKey = "all_limits"
        )

    private fun classificationInsight(
        apps: List<InsightAppUsage>,
        coverage: Double
    ): List<PersonalInsight> {
        val unclassified = apps
            .filter { it.category in unclassifiedCategories }
            .sumOf(InsightAppUsage::durationMillis)
        if (
            coverage >= InsightPolicy.LOW_CLASSIFICATION_COVERAGE ||
            unclassified < InsightPolicy.MIN_UNCLASSIFIED_USAGE_MILLIS
        ) return emptyList()

        val mixed = apps.filter { it.category == AppCategory.MixedContextDependent }
            .sumOf(InsightAppUsage::durationMillis)
        val primarilyMixed = mixed * 2L >= unclassified
        return listOf(
            PersonalInsight(
                id = "classification_coverage:today",
                type = InsightType.ClassificationCoverage,
                priority = InsightPolicy.PRIORITY_CLASSIFICATION,
                severity = InsightSeverity.Informational,
                title = if (primarilyMixed) {
                    "Much of today’s usage is context-dependent"
                } else {
                    "More usage context would improve insights"
                },
                description = if (primarilyMixed) {
                    "Classifying Mixed apps can make your score and insights more specific."
                } else {
                    "Reviewing apps categorized as Other or Mixed can improve classification coverage."
                },
                supportingMetric = "${percent(1.0 - coverage)}% needs context",
                recommendation = "Review app categories.",
                actionType = InsightActionType.ReviewCategories,
                deduplicationKey = "classification_coverage"
            )
        )
    }

    private fun dominantInsights(apps: List<InsightAppUsage>): List<PersonalInsight> = buildList {
        dominantCategory(apps)?.let(::add)
        dominantApp(apps)?.let(::add)
    }

    private fun dominantCategory(
        apps: List<InsightAppUsage>
    ): PersonalInsight? {
        val classifiedDuration = apps
            .filter { it.category !in unclassifiedCategories }
            .sumOf(InsightAppUsage::durationMillis)
        if (classifiedDuration <= 0L) return null
        val candidate = apps
            .filter { it.category !in unclassifiedCategories }
            .groupBy(InsightAppUsage::category)
            .mapValues { (_, categoryApps) -> categoryApps.sumOf(InsightAppUsage::durationMillis) }
            .entries
            .sortedWith(
                compareByDescending<Map.Entry<AppCategory, Long>> { it.value }
                    .thenBy { it.key.ordinal }
            )
            .firstOrNull() ?: return null
        val share = candidate.value.toDouble() / classifiedDuration
        if (
            share < InsightPolicy.DOMINANT_CATEGORY_SHARE ||
            candidate.value < InsightPolicy.MIN_DOMINANT_CATEGORY_USAGE_MILLIS
        ) return null

        return PersonalInsight(
            id = "dominant_category:${candidate.key.storageKey}",
            type = InsightType.DominantCategory,
            priority = InsightPolicy.PRIORITY_DOMINANT,
            severity = InsightSeverity.Informational,
            title = "${categoryLabel(candidate.key)} led today’s usage",
            description = "${categoryLabel(candidate.key)} apps account for ${percent(share)}% of today’s meaningfully classified usage.",
            supportingMetric = "${formatDuration(candidate.value)}",
            relatedCategory = candidate.key,
            recommendation = "View the app breakdown for more context.",
            actionType = InsightActionType.OpenApps,
            deduplicationKey = "dominant_category"
        )
    }

    private fun dominantApp(apps: List<InsightAppUsage>): PersonalInsight? {
        val candidates = apps
            .filter { it.category !in unclassifiedCategories }
            .groupBy(InsightAppUsage::category)
            .mapNotNull { (category, categoryApps) ->
                if (categoryApps.size < 2) return@mapNotNull null
                val categoryDuration = categoryApps.sumOf(InsightAppUsage::durationMillis)
                val top = categoryApps.sortedWith(
                    compareByDescending<InsightAppUsage>(InsightAppUsage::durationMillis)
                        .thenBy(InsightAppUsage::packageName)
                ).first()
                val share = if (categoryDuration > 0L) {
                    top.durationMillis.toDouble() / categoryDuration
                } else {
                    0.0
                }
                Triple(top, category, share)
            }
            .filter { (app, _, share) ->
                share >= InsightPolicy.DOMINANT_APP_SHARE &&
                    app.durationMillis >= InsightPolicy.MIN_DOMINANT_APP_USAGE_MILLIS
            }
            .sortedWith(
                compareByDescending<Triple<InsightAppUsage, AppCategory, Double>> { it.third }
                    .thenByDescending { it.first.durationMillis }
                    .thenBy { it.first.packageName }
            )
        val (app, category, share) = candidates.firstOrNull() ?: return null

        return PersonalInsight(
            id = "dominant_app:${category.storageKey}:${app.packageName}",
            type = InsightType.DominantApp,
            priority = InsightPolicy.PRIORITY_DOMINANT,
            severity = InsightSeverity.Informational,
            title = "${app.appName} led ${categoryLabel(category)} usage",
            description = "${app.appName} accounts for ${percent(share)}% of your ${categoryLabel(category)} usage today.",
            supportingMetric = "${formatDuration(app.durationMillis)}",
            relatedPackageName = app.packageName,
            relatedCategory = category,
            recommendation = "Open Apps to review its usage and category.",
            actionType = InsightActionType.OpenApps,
            deduplicationKey = "dominant_app:$category"
        )
    }

    private fun frequentOpenInsight(apps: List<InsightAppUsage>): List<PersonalInsight> {
        val app = apps
            .filter {
                it.openCount >= InsightPolicy.FREQUENT_OPEN_COUNT &&
                    it.durationMillis >= InsightPolicy.MIN_FREQUENT_OPEN_USAGE_MILLIS
            }
            .sortedWith(
                compareByDescending<InsightAppUsage>(InsightAppUsage::openCount)
                    .thenByDescending(InsightAppUsage::durationMillis)
                    .thenBy(InsightAppUsage::packageName)
            )
            .firstOrNull() ?: return emptyList()
        return listOf(
            PersonalInsight(
                id = "frequent_opens:${app.packageName}",
                type = InsightType.FrequentOpens,
                priority = InsightPolicy.PRIORITY_FREQUENT_OPENS,
                severity = InsightSeverity.Informational,
                title = "Frequent ${app.appName} check-ins",
                description = "You opened ${app.appName} ${app.openCount} times today.",
                supportingMetric = "${app.openCount} opens",
                relatedPackageName = app.packageName,
                relatedCategory = app.category,
                recommendation = "Review the app’s usage if you want more context.",
                actionType = InsightActionType.OpenApps,
                deduplicationKey = "frequent_opens"
            )
        )
    }

    private fun inputCoverage(apps: List<InsightAppUsage>, totalDuration: Long): Double {
        if (totalDuration <= 0L) return 0.0
        val classified = apps
            .filter { it.category !in unclassifiedCategories }
            .sumOf(InsightAppUsage::durationMillis)
        return (classified.toDouble() / totalDuration).coerceIn(0.0, 1.0)
    }

    private fun exceededRecommendation(type: GoalType): String = when (type) {
        GoalType.SocialMediaLimit,
        GoalType.EntertainmentLimit -> "Consider a 20-minute Focus session if a pause would help."
        GoalType.AppDailyLimit -> "Adjust the limit if today’s use was intentional."
        GoalType.OverallForegroundUsage -> "Review the target if today’s total reflects intentional use."
        GoalType.ProductiveTime -> error("Productive time is not a limit")
    }

    private fun exceededAction(type: GoalType): InsightActionType = when (type) {
        GoalType.SocialMediaLimit,
        GoalType.EntertainmentLimit -> InsightActionType.OpenFocus
        GoalType.AppDailyLimit,
        GoalType.OverallForegroundUsage -> InsightActionType.OpenGoals
        GoalType.ProductiveTime -> error("Productive time is not a limit")
    }

    private fun goalLabel(progress: GoalProgress): String = when (progress.goal.type) {
        GoalType.OverallForegroundUsage -> "Overall usage limit"
        GoalType.SocialMediaLimit -> "Social limit"
        GoalType.EntertainmentLimit -> "Entertainment limit"
        GoalType.AppDailyLimit -> "${progress.goal.appName ?: "App"} limit"
        GoalType.ProductiveTime -> "Productive target"
    }

    private fun categoryLabel(category: AppCategory): String = when (category) {
        AppCategory.Education -> "Education"
        AppCategory.Productivity -> "Productivity"
        AppCategory.Communication -> "Communication"
        AppCategory.Social -> "Social"
        AppCategory.Entertainment -> "Entertainment"
        AppCategory.Gaming -> "Gaming"
        AppCategory.Utility -> "Utility"
        AppCategory.Other -> "Other"
        AppCategory.MixedContextDependent -> "Mixed"
    }

    private fun percent(ratio: Double): Int = (ratio.coerceIn(0.0, 1.0) * 100.0).roundToInt()

    private fun formatDuration(durationMillis: Long): String {
        val totalMinutes = durationMillis.coerceAtLeast(0L) / 60_000L
        return when {
            totalMinutes < 1L -> "<1 min"
            totalMinutes < 60L -> "$totalMinutes min"
            totalMinutes % 60L == 0L -> "${totalMinutes / 60L}h"
            else -> "${totalMinutes / 60L}h ${totalMinutes % 60L}m"
        }
    }

    private companion object {
        val unclassifiedCategories = setOf(
            AppCategory.MixedContextDependent,
            AppCategory.Other
        )
    }
}
