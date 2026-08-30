package com.digitalbalance.app.domain.goal

import com.digitalbalance.app.data.usage.AppUsage
import com.digitalbalance.app.domain.category.AppCategory
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class GoalProgressCalculatorTest {
    private val calculator = GoalProgressCalculator()

    @Test
    fun `no goals produces a true empty state`() {
        assertTrue(calculator.noGoals(emptyList()))
        assertTrue(calculator.calculate(emptyList(), emptyList(), 0L).isEmpty())
    }

    @Test
    fun `category changes immediately change category goal progress`() {
        val productiveGoal = goal(GoalType.ProductiveTime, "productive")
        val socialGoal = goal(GoalType.SocialMediaLimit, "social")
        val socialApp = app("chat.app", 30L, AppCategory.Social)

        val before = calculator.calculate(listOf(productiveGoal, socialGoal), listOf(socialApp), 30L)
        val after = calculator.calculate(
            listOf(productiveGoal, socialGoal),
            listOf(socialApp.copy(category = AppCategory.Productivity)),
            30L
        )

        assertEquals(0L, before[0].currentDurationMillis)
        assertEquals(30L, before[1].currentDurationMillis)
        assertEquals(30L, after[0].currentDurationMillis)
        assertEquals(0L, after[1].currentDurationMillis)
    }

    @Test
    fun `mixed apps do not count toward category goals`() {
        val mixed = app("mixed.app", 40L, AppCategory.MixedContextDependent)
        GoalType.entries
            .filter { it in categoryGoalTypes }
            .forEach { type ->
                val progress = calculator.calculate(listOf(goal(type, type.storageKey)), listOf(mixed), 40L)
                assertEquals(0L, progress.single().currentDurationMillis)
            }
    }

    @Test
    fun `per app limit counts only its package`() {
        val goal = DigitalGoal(
            id = DigitalGoal.idFor(GoalType.AppDailyLimit, "target.app"),
            type = GoalType.AppDailyLimit,
            targetDurationMillis = 100L,
            packageName = "target.app",
            appName = "Target"
        )
        val progress = calculator.calculate(
            listOf(goal),
            listOf(
                app("target.app", 60L, AppCategory.Other),
                app("other.app", 90L, AppCategory.Other)
            ),
            150L
        ).single()

        assertEquals(60L, progress.currentDurationMillis)
        assertEquals(0.6f, progress.progressFraction)
    }

    @Test
    fun `unavailable live usage does not invent zero progress`() {
        val progress = calculator.calculate(
            listOf(goal(GoalType.OverallForegroundUsage, "overall")),
            apps = null,
            totalForegroundDurationMillis = null
        ).single()

        assertEquals(null, progress.currentDurationMillis)
        assertEquals(null, progress.progressFraction)
    }

    @Test
    fun `digital balance is ignored by category goals`() {
        val ownUsageByCategory = listOf(
            GoalType.ProductiveTime to AppCategory.Productivity,
            GoalType.SocialMediaLimit to AppCategory.Social,
            GoalType.EntertainmentLimit to AppCategory.Entertainment
        )

        ownUsageByCategory.forEach { (type, category) ->
            val progress = calculator.calculate(
                listOf(goal(type, type.storageKey)),
                listOf(app("com.digitalbalance.app", 80L, category)),
                80L
            ).single()
            assertEquals(0L, progress.currentDurationMillis)
        }
    }

    @Test
    fun `overall goal includes digital balance in foreground total`() {
        val progress = calculator.calculate(
            listOf(goal(GoalType.OverallForegroundUsage, "overall")),
            listOf(
                app("com.instagram.android", 60L, AppCategory.Social),
                app("com.digitalbalance.app", 20L, AppCategory.Utility)
            ),
            80L
        ).single()

        assertEquals(80L, progress.currentDurationMillis)
    }

    @Test
    fun `explicit per app goal can target digital balance`() {
        val ownGoal = DigitalGoal(
            id = DigitalGoal.idFor(GoalType.AppDailyLimit, "com.digitalbalance.app"),
            type = GoalType.AppDailyLimit,
            targetDurationMillis = 100L,
            packageName = "com.digitalbalance.app",
            appName = "DigitalBalance"
        )
        val progress = calculator.calculate(
            listOf(ownGoal),
            listOf(app("com.digitalbalance.app", 20L, AppCategory.Utility)),
            20L
        ).single()

        assertEquals(20L, progress.currentDurationMillis)
        assertEquals(0.2f, progress.progressFraction)
    }

    private fun goal(type: GoalType, id: String) = DigitalGoal(
        id = id,
        type = type,
        targetDurationMillis = 100L
    )

    private fun app(packageName: String, duration: Long, category: AppCategory) = AppUsage(
        packageName = packageName,
        appName = packageName,
        foregroundDurationMillis = duration,
        openCount = 1,
        icon = null,
        category = category
    )

    private companion object {
        val categoryGoalTypes = setOf(
            GoalType.ProductiveTime,
            GoalType.SocialMediaLimit,
            GoalType.EntertainmentLimit
        )
    }
}
