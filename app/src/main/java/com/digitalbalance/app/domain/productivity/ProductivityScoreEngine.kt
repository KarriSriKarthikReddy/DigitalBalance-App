package com.digitalbalance.app.domain.productivity

import com.digitalbalance.app.domain.category.AppCategory
import kotlin.math.roundToInt

class ProductivityScoreEngine {
    fun calculate(input: ProductivityScoreInput): ProductivityScoreResult {
        val excludedDuration = input.apps
            .filter { it.packageName in ProductivityScorePolicy.EXCLUDED_PACKAGES }
            .sumOf { it.durationMillis.coerceAtLeast(0L) }
        val totalDuration = (input.totalForegroundDurationMillis - excludedDuration).coerceAtLeast(0L)
        val apps = input.apps
            .filterNot { it.packageName in ProductivityScorePolicy.EXCLUDED_PACKAGES }
            .map { app ->
            app.copy(
                durationMillis = app.durationMillis.coerceAtLeast(0L),
                openCount = app.openCount.coerceAtLeast(0)
            )
            }
        val coverage = coverage(apps, totalDuration)
        val components = buildList {
            categoryBalanceComponent(apps)?.let(::add)
            add(intensityComponent(totalDuration))
            usagePatternComponent(apps, totalDuration)?.let(::add)
        }
        val unavailableReasons = buildList {
            if (totalDuration < ProductivityScorePolicy.MIN_TRACKED_DURATION_MILLIS) {
                add("Use your phone a little longer so today’s productivity balance has enough data.")
            }
            if (coverage.classifiedCoverage < ProductivityScorePolicy.MIN_CLASSIFIED_COVERAGE) {
                add("Classify more Mixed or Other usage before DigitalBalance estimates productivity balance.")
            }
        }
        val activeWeight = components.sumOf(ProductivityComponent::activeWeight)
        if (unavailableReasons.isNotEmpty() || activeWeight <= 0.0) {
            return ProductivityScoreResult(
                status = ProductivityScoreStatus.NotEnoughData,
                score = null,
                components = components,
                reasons = unavailableReasons.distinct(),
                coverage = coverage,
                totalActiveWeight = activeWeight
            )
        }

        val score = (components.sumOf { it.score * it.activeWeight } / activeWeight)
            .coerceIn(ProductivityScorePolicy.MIN_SCORE, ProductivityScorePolicy.MAX_SCORE)
            .roundToInt()
        return ProductivityScoreResult(
            status = ProductivityScoreStatus.Ready,
            score = score,
            components = components,
            reasons = components.sortedByDescending(ProductivityComponent::activeWeight)
                .map(ProductivityComponent::explanation),
            coverage = coverage,
            totalActiveWeight = activeWeight
        )
    }

    private fun categoryBalanceComponent(
        apps: List<ProductivityAppUsage>
    ): ProductivityComponent? {
        val classified = apps.filter { it.category in ProductivityScorePolicy.CATEGORY_SCORES }
        val classifiedDuration = classified.sumOf(ProductivityAppUsage::durationMillis)
        if (classifiedDuration <= 0L) return null
        val score = classified.sumOf { app ->
            app.durationMillis * requireNotNull(ProductivityScorePolicy.CATEGORY_SCORES[app.category])
        } / classifiedDuration
        val positiveDuration = classified
            .filter { it.category == AppCategory.Education || it.category == AppCategory.Productivity }
            .sumOf(ProductivityAppUsage::durationMillis)
        val lowerBalanceDuration = classified
            .filter {
                it.category == AppCategory.Social ||
                    it.category == AppCategory.Entertainment ||
                    it.category == AppCategory.Gaming
            }
            .sumOf(ProductivityAppUsage::durationMillis)
        val positiveShare = positiveDuration.toDouble() / classifiedDuration
        val lowerBalanceShare = lowerBalanceDuration.toDouble() / classifiedDuration
        val explanation = when {
            positiveShare >= 0.50 ->
                "Education and Productivity represented ${percent(positiveShare)}% of meaningfully classified usage."
            lowerBalanceShare >= 0.50 ->
                "Social, Entertainment, and Gaming represented ${percent(lowerBalanceShare)}% of meaningfully classified usage."
            else -> "Today’s meaningfully classified usage was spread across several categories."
        }
        return ProductivityComponent(
            kind = ProductivityComponentKind.CategoryBalance,
            name = "Category balance",
            score = boundedScore(score),
            activeWeight = ProductivityScorePolicy.CATEGORY_BALANCE_WEIGHT,
            explanation = explanation
        )
    }

    private fun intensityComponent(totalDuration: Long): ProductivityComponent {
        val range = ProductivityScorePolicy.INTENSITY_FLOOR_AT_MILLIS -
            ProductivityScorePolicy.INTENSITY_FULL_SCORE_UNTIL_MILLIS
        val progress = (
            (totalDuration - ProductivityScorePolicy.INTENSITY_FULL_SCORE_UNTIL_MILLIS)
                .toDouble() / range
            ).coerceIn(0.0, 1.0)
        val smoothProgress = smoothstep(progress)
        val score = ProductivityScorePolicy.MAX_SCORE -
            (ProductivityScorePolicy.MAX_SCORE - ProductivityScorePolicy.INTENSITY_FLOOR_SCORE) *
            smoothProgress
        return ProductivityComponent(
            kind = ProductivityComponentKind.UsageIntensity,
            name = "Usage intensity",
            score = boundedScore(score),
            activeWeight = ProductivityScorePolicy.USAGE_INTENSITY_WEIGHT,
            explanation = "${formatDuration(totalDuration)} of foreground app usage has a secondary, smoothly weighted influence."
        )
    }

    private fun usagePatternComponent(
        apps: List<ProductivityAppUsage>,
        totalDuration: Long
    ): ProductivityComponent? {
        val totalOpens = apps.sumOf(ProductivityAppUsage::openCount)
        if (
            totalDuration < ProductivityScorePolicy.PATTERN_MIN_DURATION_MILLIS ||
            totalOpens < ProductivityScorePolicy.PATTERN_MIN_OPEN_COUNT
        ) return null
        val hours = totalDuration / (60.0 * 60.0 * 1_000.0)
        val opensPerHour = totalOpens / hours
        val progress = (
            (opensPerHour - ProductivityScorePolicy.PATTERN_CALM_OPENS_PER_HOUR) /
                (
                    ProductivityScorePolicy.PATTERN_FREQUENT_OPENS_PER_HOUR -
                        ProductivityScorePolicy.PATTERN_CALM_OPENS_PER_HOUR
                    )
            ).coerceIn(0.0, 1.0)
        val score = ProductivityScorePolicy.PATTERN_CALM_SCORE -
            (ProductivityScorePolicy.PATTERN_CALM_SCORE -
                ProductivityScorePolicy.PATTERN_FREQUENT_SCORE) * smoothstep(progress)
        return ProductivityComponent(
            kind = ProductivityComponentKind.UsagePattern,
            name = "Usage pattern",
            score = boundedScore(score),
            activeWeight = ProductivityScorePolicy.USAGE_PATTERN_WEIGHT,
            explanation = "$totalOpens app opens were reconstructed across ${formatDuration(totalDuration)} of usage."
        )
    }

    private fun coverage(
        apps: List<ProductivityAppUsage>,
        totalDuration: Long
    ): ProductivityCoverage {
        val classified = apps
            .filter { it.category in ProductivityScorePolicy.CATEGORY_SCORES }
            .sumOf(ProductivityAppUsage::durationMillis)
        val mixed = apps.filter { it.category == AppCategory.MixedContextDependent }
            .sumOf(ProductivityAppUsage::durationMillis)
        val explicitOther = apps.filter { it.category == AppCategory.Other }
            .sumOf(ProductivityAppUsage::durationMillis)
        val unattributed = (totalDuration - apps.sumOf(ProductivityAppUsage::durationMillis))
            .coerceAtLeast(0L)
        val otherOrUnknown = explicitOther + unattributed
        val classifiedCoverage = if (totalDuration > 0L) {
            (classified.toDouble() / totalDuration).coerceIn(0.0, 1.0)
        } else {
            0.0
        }
        val confidence = classifiedCoverage
        val level = when {
            confidence >= ProductivityScorePolicy.HIGH_CONFIDENCE_THRESHOLD ->
                ProductivityConfidence.High
            confidence >= ProductivityScorePolicy.MEDIUM_CONFIDENCE_THRESHOLD ->
                ProductivityConfidence.Medium
            else -> ProductivityConfidence.Low
        }
        return ProductivityCoverage(
            classifiedCoverage = classifiedCoverage,
            confidence = confidence,
            confidenceLevel = level,
            classifiedDurationMillis = classified,
            mixedDurationMillis = mixed,
            otherOrUnknownDurationMillis = otherOrUnknown,
            totalForegroundDurationMillis = totalDuration
        )
    }

    private fun smoothstep(value: Double): Double = value * value * (3.0 - 2.0 * value)

    private fun boundedScore(value: Double): Int = value
        .coerceIn(ProductivityScorePolicy.MIN_SCORE, ProductivityScorePolicy.MAX_SCORE)
        .roundToInt()

    private fun percent(value: Double): Int = (value.coerceIn(0.0, 1.0) * 100.0).roundToInt()

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
