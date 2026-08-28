package com.digitalbalance.app.domain.goal

import com.digitalbalance.app.data.usage.AppUsage
import com.digitalbalance.app.domain.category.AppCategory

class GoalProgressCalculator {
    fun calculate(
        goals: List<DigitalGoal>,
        apps: List<AppUsage>?,
        totalForegroundDurationMillis: Long?
    ): List<GoalProgress> = goals.map { goal ->
        val current = when {
            apps == null || totalForegroundDurationMillis == null -> null
            goal.type == GoalType.OverallForegroundUsage -> totalForegroundDurationMillis
            goal.type == GoalType.ProductiveTime -> apps
                .filter { it.category in productiveCategories }
                .sumOf(AppUsage::foregroundDurationMillis)
            goal.type == GoalType.SocialMediaLimit -> apps
                .filter { it.category == AppCategory.Social }
                .sumOf(AppUsage::foregroundDurationMillis)
            goal.type == GoalType.EntertainmentLimit -> apps
                .filter { it.category == AppCategory.Entertainment }
                .sumOf(AppUsage::foregroundDurationMillis)
            goal.type == GoalType.AppDailyLimit -> apps
                .filter { it.packageName == goal.packageName }
                .sumOf(AppUsage::foregroundDurationMillis)
            else -> 0L
        }
        GoalProgress(
            goal = goal,
            currentDurationMillis = current,
            progressFraction = current?.let {
                (it.toDouble() / goal.targetDurationMillis.toDouble()).toFloat()
            }
        )
    }

    fun noGoals(goals: List<DigitalGoal>): Boolean = goals.isEmpty()

    private companion object {
        val productiveCategories = setOf(
            AppCategory.Education,
            AppCategory.Productivity
        )
    }
}
