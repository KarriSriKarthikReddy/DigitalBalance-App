package com.digitalbalance.app.domain.reminder

import com.digitalbalance.app.domain.goal.GoalProgress
import com.digitalbalance.app.domain.goal.GoalType
import com.digitalbalance.app.domain.productivity.ProductivityScorePolicy
import kotlin.math.roundToInt

class ReminderEngine {
    fun evaluate(input: ReminderEvaluationInput): List<ReminderCandidate> {
        if (!input.settings.notificationsEnabled || !input.notificationPermissionGranted) {
            return emptyList()
        }

        val candidates = buildList {
            if (input.settings.goalAndLimitRemindersEnabled) {
                addAll(limitCandidates(input))
                productiveTargetCandidate(input)?.let(::add)
            }
            if (input.settings.focusSuggestionsEnabled) {
                focusSuggestionCandidate(input)?.let(::add)
            }
        }
        return candidates
            .filterNot { it.deliveryKey.storageKey in input.deliveredKeys }
            .sortedWith(compareBy<ReminderCandidate>({ priority(it.deliveryKey.type) }, { it.deliveryKey.identifier }))
            .take(ReminderPolicy.MAX_DELIVERIES_PER_EVALUATION)
    }

    private fun limitCandidates(input: ReminderEvaluationInput): List<ReminderCandidate> =
        input.goalProgress.mapNotNull { progress ->
            val goal = progress.goal
            if (goal.type.isMinimumTarget || goal.targetDurationMillis <= 0L) return@mapNotNull null
            if (
                goal.type == GoalType.AppDailyLimit &&
                goal.packageName in ProductivityScorePolicy.EXCLUDED_PACKAGES
            ) return@mapNotNull null
            val current = progress.currentDurationMillis?.coerceAtLeast(0L) ?: return@mapNotNull null
            val fraction = current.toDouble() / goal.targetDurationMillis
            when {
                fraction >= ReminderPolicy.EXCEEDED_LIMIT_FRACTION -> limitCandidate(
                    input.dateKey,
                    progress,
                    ReminderType.LimitExceeded,
                    "100",
                    exceededMessage(progress)
                )
                fraction >= ReminderPolicy.APPROACHING_LIMIT_FRACTION -> limitCandidate(
                    input.dateKey,
                    progress,
                    ReminderType.LimitApproaching,
                    "80",
                    approachingMessage(progress, fraction)
                )
                else -> null
            }
        }

    private fun productiveTargetCandidate(input: ReminderEvaluationInput): ReminderCandidate? {
        if (input.localHour < ReminderPolicy.PRODUCTIVE_REMINDER_START_HOUR) return null
        val nonSelfUsage = input.apps
            .filterNot { it.packageName in ProductivityScorePolicy.EXCLUDED_PACKAGES }
            .sumOf { it.durationMillis.coerceAtLeast(0L) }
        if (nonSelfUsage < ReminderPolicy.PRODUCTIVE_MIN_MEANINGFUL_USAGE_MILLIS) return null
        val progress = input.goalProgress.firstOrNull { it.goal.type == GoalType.ProductiveTime }
            ?: return null
        val current = progress.currentDurationMillis?.coerceAtLeast(0L) ?: return null
        val remaining = (progress.goal.targetDurationMillis - current).coerceAtLeast(0L)
        if (remaining < ReminderPolicy.PRODUCTIVE_MIN_REMAINING_MILLIS) return null
        return ReminderCandidate(
            deliveryKey = ReminderDeliveryKey(
                input.dateKey,
                ReminderType.ProductiveTarget,
                progress.goal.id,
                "late_day"
            ),
            title = "Productive target",
            message = "${formatDuration(remaining)} remain on your productive target today.",
            destination = ReminderDestination.Goals
        )
    }

    private fun focusSuggestionCandidate(input: ReminderEvaluationInput): ReminderCandidate? {
        if (
            input.localHour !in ReminderPolicy.FOCUS_SUGGESTION_START_HOUR..ReminderPolicy.FOCUS_SUGGESTION_END_HOUR ||
            input.completedFocusSessionsToday > 0 ||
            input.focusSessionActive
        ) return null
        val nonSelfUsage = input.apps
            .filterNot { it.packageName in ProductivityScorePolicy.EXCLUDED_PACKAGES }
            .sumOf { it.durationMillis.coerceAtLeast(0L) }
        if (nonSelfUsage < ReminderPolicy.FOCUS_SUGGESTION_MIN_USAGE_MILLIS) return null
        return ReminderCandidate(
            deliveryKey = ReminderDeliveryKey(
                input.dateKey,
                ReminderType.FocusSuggestion,
                "focus",
                "${ReminderPolicy.FOCUS_SUGGESTION_MIN_USAGE_MILLIS}"
            ),
            title = "A moment to refocus",
            message = "Want a break? Start a ${ReminderPolicy.FOCUS_SUGGESTION_MINUTES} min Focus session.",
            destination = ReminderDestination.Focus
        )
    }

    private fun limitCandidate(
        dateKey: String,
        progress: GoalProgress,
        type: ReminderType,
        threshold: String,
        message: String
    ) = ReminderCandidate(
        deliveryKey = ReminderDeliveryKey(dateKey, type, progress.goal.id, threshold),
        title = if (type == ReminderType.LimitExceeded) "Limit reached" else "Limit approaching",
        message = message,
        destination = if (progress.goal.type == GoalType.AppDailyLimit) {
            ReminderDestination.Apps
        } else {
            ReminderDestination.Goals
        }
    )

    private fun approachingMessage(progress: GoalProgress, fraction: Double): String = when (progress.goal.type) {
        GoalType.AppDailyLimit ->
            "You've used ${formatDuration(progress.currentDurationMillis ?: 0L)} of your " +
                "${formatDuration(progress.goal.targetDurationMillis)} ${progress.goal.appName ?: "app"} limit."
        GoalType.SocialMediaLimit ->
            "Social usage is at ${percent(fraction)}% of today's limit."
        GoalType.EntertainmentLimit ->
            "Entertainment usage is at ${percent(fraction)}% of today's limit."
        GoalType.OverallForegroundUsage ->
            "Foreground app usage is at ${percent(fraction)}% of today's target."
        GoalType.ProductiveTime -> error("Minimum targets do not use upper-limit reminders")
    }

    private fun exceededMessage(progress: GoalProgress): String = when (progress.goal.type) {
        GoalType.AppDailyLimit ->
            "${progress.goal.appName ?: "This app"} has reached your configured " +
                "${formatDuration(progress.goal.targetDurationMillis)} limit."
        GoalType.SocialMediaLimit -> "Social usage has reached today's configured limit."
        GoalType.EntertainmentLimit -> "Entertainment usage has reached today's configured limit."
        GoalType.OverallForegroundUsage -> "Foreground app usage has reached today's configured target."
        GoalType.ProductiveTime -> error("Minimum targets do not use upper-limit reminders")
    }

    private fun priority(type: ReminderType): Int = when (type) {
        ReminderType.LimitExceeded -> 0
        ReminderType.LimitApproaching -> 1
        ReminderType.ProductiveTarget -> 2
        ReminderType.FocusSuggestion -> 3
        ReminderType.DailySummary -> 4
    }

    private fun percent(value: Double): Int = (value * 100.0).roundToInt().coerceAtLeast(0)

    private fun formatDuration(durationMillis: Long): String {
        val totalMinutes = (durationMillis.coerceAtLeast(0L) / 60_000L).coerceAtLeast(1L)
        return when {
            totalMinutes < 60L -> "$totalMinutes min"
            totalMinutes % 60L == 0L -> "${totalMinutes / 60L}h"
            else -> "${totalMinutes / 60L}h ${totalMinutes % 60L}m"
        }
    }
}
