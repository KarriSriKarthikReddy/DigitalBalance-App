package com.digitalbalance.app.domain.score

object GoalAlignmentPolicy {
    const val MIN_SCORE = 0.0
    const val MAX_SCORE = 100.0

    const val PRODUCTIVE_GOAL_WEIGHT = 3.0
    const val OVERALL_LIMIT_WEIGHT = 2.0
    const val SOCIAL_LIMIT_WEIGHT = 2.0
    const val ENTERTAINMENT_LIMIT_WEIGHT = 2.0
    const val PER_APP_LIMITS_TOTAL_WEIGHT = 2.0

    /** Controls how quickly a limit component declines as the overage grows. */
    const val LIMIT_EXCESS_DECAY_RATE = 1.25

    /**
     * Softens the curve immediately above a limit without adding extra allowance.
     * A value of 0.05 creates a gentle shoulder over roughly the first 5% of overage.
     */
    const val LIMIT_BOUNDARY_GRACE_RATIO = 0.05

    const val MIN_TRACKED_DURATION_MILLIS = 5L * 60L * 1_000L
    const val MIN_READY_CONFIDENCE = 0.25
    const val MEDIUM_CONFIDENCE_THRESHOLD = 0.50
    const val HIGH_CONFIDENCE_THRESHOLD = 0.80
    const val MAX_SUMMARY_REASONS = 2
}
