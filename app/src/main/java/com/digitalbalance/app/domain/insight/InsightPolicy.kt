package com.digitalbalance.app.domain.insight

object InsightPolicy {
    const val MAX_INSIGHTS = 5

    const val MIN_TOTAL_USAGE_MILLIS = 10L * 60L * 1_000L
    const val MIN_GOAL_PROGRESS_USAGE_MILLIS = 5L * 60L * 1_000L

    const val LIMIT_PROGRESS_RATIO = 0.50
    const val LIMIT_NEARING_RATIO = 0.80
    const val MIN_LIMIT_OVERAGE_MILLIS = 60L * 1_000L
    const val MIN_LIMIT_OVERAGE_RATIO = 0.02

    const val DOMINANT_APP_SHARE = 0.50
    const val DOMINANT_CATEGORY_SHARE = 0.55
    const val MIN_DOMINANT_APP_USAGE_MILLIS = 15L * 60L * 1_000L
    const val MIN_DOMINANT_CATEGORY_USAGE_MILLIS = 20L * 60L * 1_000L

    const val LOW_CLASSIFICATION_COVERAGE = 0.60
    const val MIN_UNCLASSIFIED_USAGE_MILLIS = 20L * 60L * 1_000L

    const val FREQUENT_OPEN_COUNT = 12
    const val MIN_FREQUENT_OPEN_USAGE_MILLIS = 10L * 60L * 1_000L

    const val PRIORITY_EXCEEDED = 100
    const val PRIORITY_GOAL_PROGRESS = 80
    const val PRIORITY_CLASSIFICATION = 70
    const val PRIORITY_DOMINANT = 60
    const val PRIORITY_FREQUENT_OPENS = 55
    const val PRIORITY_SUCCESS = 40
}
