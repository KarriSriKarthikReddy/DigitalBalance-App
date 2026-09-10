package com.digitalbalance.app.data.reminder

import com.digitalbalance.app.data.repository.localDateKey
import com.digitalbalance.app.data.usage.AppUsage
import com.digitalbalance.app.domain.focus.FocusSession
import com.digitalbalance.app.domain.focus.FocusSessionStatus
import com.digitalbalance.app.domain.goal.DigitalGoal
import com.digitalbalance.app.domain.goal.GoalProgress
import com.digitalbalance.app.domain.goal.GoalProgressCalculator
import com.digitalbalance.app.domain.productivity.ProductivityAppUsage
import com.digitalbalance.app.domain.productivity.ProductivityScoreEngine
import com.digitalbalance.app.domain.productivity.ProductivityScoreInput
import com.digitalbalance.app.domain.productivity.ProductivityScoreResult
import com.digitalbalance.app.domain.reminder.DailySummary
import com.digitalbalance.app.domain.reminder.DailySummaryGenerator
import com.digitalbalance.app.domain.reminder.DailySummaryInput
import com.digitalbalance.app.domain.reminder.ReminderAppUsage
import com.digitalbalance.app.domain.score.GoalAlignmentEngine
import com.digitalbalance.app.domain.score.GoalAlignmentInput
import com.digitalbalance.app.domain.score.GoalAlignmentResult
import com.digitalbalance.app.domain.score.ScoredAppUsage

data class ReminderSnapshot(
    val dateKey: String,
    val apps: List<ReminderAppUsage>,
    val goalProgress: List<GoalProgress>,
    val productivity: ProductivityScoreResult,
    val goalAlignment: GoalAlignmentResult,
    val completedFocusSessionsToday: Int,
    val focusSessionActive: Boolean,
    val dailySummary: DailySummary
)

class ReminderSnapshotFactory(
    private val goalCalculator: GoalProgressCalculator = GoalProgressCalculator(),
    private val productivityEngine: ProductivityScoreEngine = ProductivityScoreEngine(),
    private val goalAlignmentEngine: GoalAlignmentEngine = GoalAlignmentEngine(),
    private val summaryGenerator: DailySummaryGenerator = DailySummaryGenerator()
) {
    fun create(
        dateKey: String,
        apps: List<AppUsage>,
        totalForegroundDurationMillis: Long,
        goals: List<DigitalGoal>,
        focusSessions: List<FocusSession>
    ): ReminderSnapshot {
        val reminderApps = apps.map { app ->
            ReminderAppUsage(
                packageName = app.packageName,
                appName = app.appName,
                durationMillis = app.foregroundDurationMillis,
                category = app.category
            )
        }
        val goalProgress = goalCalculator.calculate(goals, apps, totalForegroundDurationMillis)
        val productivity = productivityEngine.calculate(
            ProductivityScoreInput(
                totalForegroundDurationMillis = totalForegroundDurationMillis,
                apps = apps.map { app ->
                    ProductivityAppUsage(
                        packageName = app.packageName,
                        appName = app.appName,
                        durationMillis = app.foregroundDurationMillis,
                        openCount = app.openCount,
                        category = app.category
                    )
                }
            )
        )
        val alignment = goalAlignmentEngine.calculate(
            GoalAlignmentInput(
                totalForegroundDurationMillis = totalForegroundDurationMillis,
                apps = apps.map { app ->
                    ScoredAppUsage(
                        packageName = app.packageName,
                        appName = app.appName,
                        durationMillis = app.foregroundDurationMillis,
                        category = app.category
                    )
                },
                goals = goals
            )
        )
        val completed = focusSessions.count { session ->
            session.status == FocusSessionStatus.Completed &&
                session.endedAtEpochMillis?.let(::localDateKey) == dateKey
        }
        val active = focusSessions.any {
            it.status == FocusSessionStatus.Running || it.status == FocusSessionStatus.Paused
        }
        val summary = summaryGenerator.generate(
            DailySummaryInput(
                dateKey = dateKey,
                foregroundUsageMillis = totalForegroundDurationMillis,
                apps = reminderApps,
                productivityScore = productivity.score,
                goalAlignmentScore = alignment.score,
                goalProgress = goalProgress,
                completedFocusSessions = completed,
                classificationCoverage = productivity.coverage.classifiedCoverage
                    .takeIf { totalForegroundDurationMillis > 0L }
            )
        )
        return ReminderSnapshot(
            dateKey = dateKey,
            apps = reminderApps,
            goalProgress = goalProgress,
            productivity = productivity,
            goalAlignment = alignment,
            completedFocusSessionsToday = completed,
            focusSessionActive = active,
            dailySummary = summary
        )
    }
}
