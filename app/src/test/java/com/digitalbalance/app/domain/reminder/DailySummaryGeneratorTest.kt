package com.digitalbalance.app.domain.reminder

import com.digitalbalance.app.domain.category.AppCategory
import com.digitalbalance.app.domain.goal.DigitalGoal
import com.digitalbalance.app.domain.goal.GoalProgress
import com.digitalbalance.app.domain.goal.GoalType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class DailySummaryGeneratorTest {
    private val generator = DailySummaryGenerator()

    @Test
    fun generatesDeterministicSummaryFromAvailableData() {
        val socialGoal = DigitalGoal(
            id = "social_media_limit",
            type = GoalType.SocialMediaLimit,
            targetDurationMillis = minutes(60)
        )
        val summary = generator.generate(
            DailySummaryInput(
                dateKey = "2026-08-30",
                foregroundUsageMillis = hours(4) + minutes(38),
                apps = listOf(
                    app("instagram", "Instagram", minutes(72), AppCategory.Social),
                    app("docs", "Docs", minutes(60), AppCategory.Productivity)
                ),
                productivityScore = 68,
                goalAlignmentScore = 82,
                goalProgress = listOf(GoalProgress(socialGoal, minutes(48), 0.8f)),
                completedFocusSessions = 1,
                classificationCoverage = 0.75
            )
        )

        assertEquals("Instagram", summary.topApp?.appName)
        assertEquals(AppCategory.Social, summary.topCategory?.category)
        assertEquals(1, summary.completedFocusSessions)
        assertTrue(DailySummaryFormatter.notificationBody(summary).contains("Productivity 68"))
        val lines = DailySummaryFormatter.detailLines(summary)
        assertTrue(lines.contains("Goal Alignment: 82"))
        assertTrue(lines.contains("Social limit: within target"))
        assertTrue(lines.contains("Focus: 1 completed session"))
        assertTrue(lines.contains("Classification coverage: 75%"))
    }

    @Test
    fun missingProductivityScoreIsOmittedRatherThanInvented() {
        val summary = generator.generate(emptyInput(productivity = null, alignment = 70))

        assertNull(summary.productivityScore)
        assertFalse(DailySummaryFormatter.notificationBody(summary).contains("Productivity"))
    }

    @Test
    fun missingGoalAlignmentIsOmittedRatherThanInvented() {
        val summary = generator.generate(emptyInput(productivity = 65, alignment = null))

        assertNull(summary.goalAlignmentScore)
        assertFalse(DailySummaryFormatter.notificationBody(summary).contains("Goal Alignment"))
    }

    @Test
    fun digitalBalanceCanBeTopAppButDoesNotBecomeTopCategory() {
        val summary = generator.generate(
            emptyInput(
                productivity = 60,
                alignment = null,
                apps = listOf(
                    app(
                        "com.digitalbalance.app",
                        "DigitalBalance",
                        minutes(90),
                        AppCategory.Productivity
                    ),
                    app("messages", "Messages", minutes(20), AppCategory.Communication)
                )
            )
        )

        assertEquals("DigitalBalance", summary.topApp?.appName)
        assertEquals(AppCategory.Communication, summary.topCategory?.category)
    }

    private fun emptyInput(
        productivity: Int?,
        alignment: Int?,
        apps: List<ReminderAppUsage> = emptyList()
    ) = DailySummaryInput(
        dateKey = "2026-08-30",
        foregroundUsageMillis = apps.sumOf(ReminderAppUsage::durationMillis),
        apps = apps,
        productivityScore = productivity,
        goalAlignmentScore = alignment,
        goalProgress = emptyList(),
        completedFocusSessions = 0,
        classificationCoverage = null
    )

    private fun app(
        packageName: String,
        appName: String,
        duration: Long,
        category: AppCategory
    ) = ReminderAppUsage(packageName, appName, duration, category)

    private fun minutes(value: Int) = value * 60_000L
    private fun hours(value: Int) = value * 60L * 60_000L
}
