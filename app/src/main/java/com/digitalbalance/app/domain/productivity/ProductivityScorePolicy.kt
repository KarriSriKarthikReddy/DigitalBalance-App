package com.digitalbalance.app.domain.productivity

import com.digitalbalance.app.domain.category.AppCategory

object ProductivityScorePolicy {
    const val MIN_SCORE = 0.0
    const val MAX_SCORE = 100.0

    const val CATEGORY_BALANCE_WEIGHT = 0.65
    const val USAGE_INTENSITY_WEIGHT = 0.25
    const val USAGE_PATTERN_WEIGHT = 0.10

    val CATEGORY_SCORES = mapOf(
        AppCategory.Education to 100.0,
        AppCategory.Productivity to 100.0,
        AppCategory.Communication to 60.0,
        AppCategory.Utility to 60.0,
        AppCategory.Social to 30.0,
        AppCategory.Entertainment to 25.0,
        AppCategory.Gaming to 25.0
    )

    const val MIN_TRACKED_DURATION_MILLIS = 10L * 60L * 1_000L
    const val MIN_CLASSIFIED_COVERAGE = 0.40
    const val MEDIUM_CONFIDENCE_THRESHOLD = 0.60
    const val HIGH_CONFIDENCE_THRESHOLD = 0.80

    /** DigitalBalance heuristic: duration has full credit through two hours. */
    const val INTENSITY_FULL_SCORE_UNTIL_MILLIS = 2L * 60L * 60L * 1_000L

    /** DigitalBalance heuristic: the smooth duration curve reaches its floor at eight hours. */
    const val INTENSITY_FLOOR_AT_MILLIS = 8L * 60L * 60L * 1_000L
    const val INTENSITY_FLOOR_SCORE = 20.0

    const val PATTERN_MIN_DURATION_MILLIS = 30L * 60L * 1_000L
    const val PATTERN_MIN_OPEN_COUNT = 3
    const val PATTERN_CALM_OPENS_PER_HOUR = 6.0
    const val PATTERN_FREQUENT_OPENS_PER_HOUR = 24.0
    const val PATTERN_CALM_SCORE = 70.0
    const val PATTERN_FREQUENT_SCORE = 30.0
}
