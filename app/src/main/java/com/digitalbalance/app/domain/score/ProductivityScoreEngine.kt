package com.digitalbalance.app.domain.score

import com.digitalbalance.app.domain.category.AppCategory
import com.digitalbalance.app.domain.goal.DigitalGoal
import com.digitalbalance.app.domain.goal.GoalType
import kotlin.math.exp
import kotlin.math.max
import kotlin.math.roundToInt
import kotlin.math.sqrt

class ProductivityScoreEngine {
    fun calculate(input: ProductivityScoreInput): ProductivityScoreResult {
        val normalizedApps = input.apps.map { app ->
            app.copy(durationMillis = app.durationMillis.coerceAtLeast(0L))
        }
        val totalDuration = input.totalForegroundDurationMillis.coerceAtLeast(0L)
        val goals = input.goals
            .asSequence()
            .filter { it.targetDurationMillis > 0L }
            .distinctBy(DigitalGoal::id)
            .toList()
        val components = buildComponents(goals, normalizedApps, totalDuration)
        val activeWeight = components.sumOf(ScoreComponent::activeWeight)
        val coverage = coverage(normalizedApps, totalDuration, components, activeWeight)

        val unavailableReasons = buildList {
            if (components.isEmpty()) add("Configure at least one goal to calculate a personal score.")
            if (totalDuration < ScorePolicy.MIN_TRACKED_DURATION_MILLIS) {
                add("Use your phone a little longer so today’s score has enough real usage data.")
            }
            if (components.isNotEmpty() && coverage.confidence < ScorePolicy.MIN_READY_CONFIDENCE) {
                add("Classify more Mixed or Other usage to make category-based goals reliable.")
            }
        }
        if (unavailableReasons.isNotEmpty()) {
            return ProductivityScoreResult(
                status = ProductivityScoreStatus.NotEnoughData,
                score = null,
                components = components,
                reasons = unavailableReasons.distinct(),
                coverage = coverage,
                totalActiveWeight = activeWeight
            )
        }

        val normalizedScore = components.sumOf { it.score * it.activeWeight } / activeWeight
        val score = normalizedScore
            .coerceIn(ScorePolicy.MIN_SCORE, ScorePolicy.MAX_SCORE)
            .roundToInt()
        return ProductivityScoreResult(
            status = ProductivityScoreStatus.Ready,
            score = score,
            components = components,
            reasons = summaryReasons(components),
            coverage = coverage,
            totalActiveWeight = activeWeight
        )
    }

    private fun buildComponents(
        goals: List<DigitalGoal>,
        apps: List<ScoredAppUsage>,
        totalDuration: Long
    ): List<ScoreComponent> {
        val productiveDuration = apps
            .filter { it.category == AppCategory.Education || it.category == AppCategory.Productivity }
            .sumOf(ScoredAppUsage::durationMillis)
        val socialDuration = apps
            .filter { it.category == AppCategory.Social }
            .sumOf(ScoredAppUsage::durationMillis)
        val entertainmentDuration = apps
            .filter { it.category == AppCategory.Entertainment }
            .sumOf(ScoredAppUsage::durationMillis)
        val perAppGoals = goals.filter { it.type == GoalType.AppDailyLimit && !it.packageName.isNullOrBlank() }
        val perAppWeight = if (perAppGoals.isEmpty()) {
            0.0
        } else {
            ScorePolicy.PER_APP_LIMITS_TOTAL_WEIGHT / perAppGoals.size
        }

        return buildList {
            goals.firstOrNull { it.type == GoalType.ProductiveTime }?.let { goal ->
                add(
                    productiveComponent(
                        goal = goal,
                        actualDuration = productiveDuration,
                        weight = ScorePolicy.PRODUCTIVE_GOAL_WEIGHT
                    )
                )
            }
            goals.firstOrNull { it.type == GoalType.OverallForegroundUsage }?.let { goal ->
                add(
                    limitComponent(
                        goal = goal,
                        kind = ScoreComponentKind.OverallLimit,
                        name = "Overall foreground usage",
                        actualDuration = totalDuration,
                        weight = ScorePolicy.OVERALL_LIMIT_WEIGHT,
                        requiresClassification = false
                    )
                )
            }
            goals.firstOrNull { it.type == GoalType.SocialMediaLimit }?.let { goal ->
                add(
                    limitComponent(
                        goal = goal,
                        kind = ScoreComponentKind.SocialLimit,
                        name = "Social-media limit",
                        actualDuration = socialDuration,
                        weight = ScorePolicy.SOCIAL_LIMIT_WEIGHT,
                        requiresClassification = true
                    )
                )
            }
            goals.firstOrNull { it.type == GoalType.EntertainmentLimit }?.let { goal ->
                add(
                    limitComponent(
                        goal = goal,
                        kind = ScoreComponentKind.EntertainmentLimit,
                        name = "Entertainment limit",
                        actualDuration = entertainmentDuration,
                        weight = ScorePolicy.ENTERTAINMENT_LIMIT_WEIGHT,
                        requiresClassification = true
                    )
                )
            }
            perAppGoals.sortedBy(DigitalGoal::id).forEach { goal ->
                val actual = apps
                    .filter { it.packageName == goal.packageName }
                    .sumOf(ScoredAppUsage::durationMillis)
                add(
                    limitComponent(
                        goal = goal,
                        kind = ScoreComponentKind.PerAppLimit,
                        name = goal.appName ?: "App limit",
                        actualDuration = actual,
                        weight = perAppWeight,
                        requiresClassification = false
                    )
                )
            }
        }
    }

    private fun productiveComponent(
        goal: DigitalGoal,
        actualDuration: Long,
        weight: Double
    ): ScoreComponent {
        val ratio = actualDuration.toDouble() / goal.targetDurationMillis
        val boundedRatio = ratio.coerceIn(0.0, 1.0)
        val smoothProgress = boundedRatio * boundedRatio * (3.0 - 2.0 * boundedRatio)
        val rawScore = ScorePolicy.MAX_SCORE * smoothProgress
        return ScoreComponent(
            id = goal.id,
            kind = ScoreComponentKind.ProductiveGoal,
            name = "Productive-time target",
            score = boundedScore(rawScore),
            activeWeight = weight,
            actualDurationMillis = actualDuration,
            targetDurationMillis = goal.targetDurationMillis,
            explanation = "${formatDuration(actualDuration)} of the ${formatDuration(goal.targetDurationMillis)} productive target is complete.",
            requiresClassification = true
        )
    }

    private fun limitComponent(
        goal: DigitalGoal,
        kind: ScoreComponentKind,
        name: String,
        actualDuration: Long,
        weight: Double,
        requiresClassification: Boolean
    ): ScoreComponent {
        val ratio = actualDuration.toDouble() / goal.targetDurationMillis
        val excessRatio = max(0.0, ratio - 1.0)
        val smoothedExcessRatio = sqrt(
            excessRatio * excessRatio +
                ScorePolicy.LIMIT_BOUNDARY_GRACE_RATIO * ScorePolicy.LIMIT_BOUNDARY_GRACE_RATIO
        ) - ScorePolicy.LIMIT_BOUNDARY_GRACE_RATIO
        val rawScore = ScorePolicy.MAX_SCORE * exp(
            -ScorePolicy.LIMIT_EXCESS_DECAY_RATE * smoothedExcessRatio
        )
        val explanation = if (actualDuration <= goal.targetDurationMillis) {
            "${formatDuration(actualDuration)} used within the ${formatDuration(goal.targetDurationMillis)} configured limit."
        } else {
            val excess = actualDuration - goal.targetDurationMillis
            "${formatDuration(actualDuration)} used, ${formatDuration(excess)} above the ${formatDuration(goal.targetDurationMillis)} configured limit."
        }
        return ScoreComponent(
            id = goal.id,
            kind = kind,
            name = name,
            score = boundedScore(rawScore),
            activeWeight = weight,
            actualDurationMillis = actualDuration,
            targetDurationMillis = goal.targetDurationMillis,
            explanation = explanation,
            requiresClassification = requiresClassification
        )
    }

    private fun coverage(
        apps: List<ScoredAppUsage>,
        totalDuration: Long,
        components: List<ScoreComponent>,
        activeWeight: Double
    ): ScoreCoverage {
        val mixed = apps.filter { it.category == AppCategory.MixedContextDependent }
            .sumOf(ScoredAppUsage::durationMillis)
        val explicitOther = apps.filter { it.category == AppCategory.Other }
            .sumOf(ScoredAppUsage::durationMillis)
        val appDuration = apps.sumOf(ScoredAppUsage::durationMillis)
        val unattributed = (totalDuration - appDuration).coerceAtLeast(0L)
        val otherOrUnknown = explicitOther + unattributed
        val classified = apps
            .filter { it.category != AppCategory.MixedContextDependent && it.category != AppCategory.Other }
            .sumOf(ScoredAppUsage::durationMillis)
        val classificationCoverage = if (totalDuration > 0L) {
            (classified.toDouble() / totalDuration).coerceIn(0.0, 1.0)
        } else {
            0.0
        }
        val classificationDependentWeight = components
            .filter(ScoreComponent::requiresClassification)
            .sumOf(ScoreComponent::activeWeight)
        val classificationIndependentWeight = activeWeight - classificationDependentWeight
        val confidence = if (activeWeight > 0.0) {
            (
                classificationIndependentWeight +
                    classificationDependentWeight * classificationCoverage
                ) / activeWeight
        } else {
            0.0
        }.coerceIn(0.0, 1.0)
        val level = when {
            confidence >= ScorePolicy.HIGH_CONFIDENCE_THRESHOLD -> ScoreConfidence.High
            confidence >= ScorePolicy.MEDIUM_CONFIDENCE_THRESHOLD -> ScoreConfidence.Medium
            else -> ScoreConfidence.Low
        }
        return ScoreCoverage(
            classificationCoverage = classificationCoverage,
            confidence = confidence,
            confidenceLevel = level,
            classifiedDurationMillis = classified,
            mixedDurationMillis = mixed,
            otherOrUnknownDurationMillis = otherOrUnknown,
            totalForegroundDurationMillis = totalDuration
        )
    }

    private fun summaryReasons(components: List<ScoreComponent>): List<String> {
        if (components.isEmpty()) return emptyList()
        val lowest = components.minWith(compareBy(ScoreComponent::score, ScoreComponent::id))
        val highest = components.maxWith(compareBy(ScoreComponent::score, ScoreComponent::id))
        return listOf(lowest, highest)
            .distinctBy(ScoreComponent::id)
            .take(ScorePolicy.MAX_SUMMARY_REASONS)
            .map(ScoreComponent::explanation)
    }

    private fun boundedScore(score: Double): Int = score
        .coerceIn(ScorePolicy.MIN_SCORE, ScorePolicy.MAX_SCORE)
        .roundToInt()

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
