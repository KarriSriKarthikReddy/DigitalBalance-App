package com.digitalbalance.app.domain.reminder

object ReminderPolicy {
    /** First upper-limit reminder, expressed as a fraction of the configured limit. */
    const val APPROACHING_LIMIT_FRACTION = 0.80

    /** A configured upper limit is reached at this fraction. */
    const val EXCEEDED_LIMIT_FRACTION = 1.00

    /** Productive-target reminders are considered only from 6 PM local time. */
    const val PRODUCTIVE_REMINDER_START_HOUR = 18
    const val PRODUCTIVE_MIN_REMAINING_MILLIS = 15L * 60L * 1_000L
    const val PRODUCTIVE_MIN_MEANINGFUL_USAGE_MILLIS = 10L * 60L * 1_000L

    /** Focus suggestions use a coarse threshold and never poll continuously. */
    const val FOCUS_SUGGESTION_MIN_USAGE_MILLIS = 2L * 60L * 60L * 1_000L
    const val FOCUS_SUGGESTION_START_HOUR = 12
    const val FOCUS_SUGGESTION_END_HOUR = 20
    const val FOCUS_SUGGESTION_MINUTES = 25

    /** Avoid a burst when several goals cross thresholds during one refresh. */
    const val MAX_DELIVERIES_PER_EVALUATION = 1
}

object DailySummaryPolicy {
    const val SUMMARY_HOUR = 20
    const val SUMMARY_MINUTE = 30
}
