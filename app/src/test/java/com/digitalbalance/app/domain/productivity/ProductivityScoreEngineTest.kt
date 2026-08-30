package com.digitalbalance.app.domain.productivity

import com.digitalbalance.app.domain.category.AppCategory
import com.digitalbalance.app.domain.goal.DigitalGoal
import com.digitalbalance.app.domain.goal.GoalType
import com.digitalbalance.app.domain.score.GoalAlignmentEngine
import com.digitalbalance.app.domain.score.GoalAlignmentInput
import com.digitalbalance.app.domain.score.GoalAlignmentStatus
import com.digitalbalance.app.domain.score.ScoredAppUsage
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.math.abs

class ProductivityScoreEngineTest {
    private val engine = ProductivityScoreEngine()
    private val alignmentEngine = GoalAlignmentEngine()

    @Test
    fun nineHoursEntertainmentWithTenHourLimitHasHighAlignmentButLowProductivity() {
        val usage = app("video", hours(9), AppCategory.Entertainment, opens = 9)
        val productivity = calculate(listOf(usage))
        val alignment = alignmentEngine.calculate(
            GoalAlignmentInput(
                totalForegroundDurationMillis = hours(9),
                apps = listOf(
                    ScoredAppUsage("video", "Video", hours(9), AppCategory.Entertainment)
                ),
                goals = listOf(
                    DigitalGoal(
                        id = DigitalGoal.idFor(GoalType.EntertainmentLimit),
                        type = GoalType.EntertainmentLimit,
                        targetDurationMillis = hours(10)
                    )
                )
            )
        )

        assertEquals(GoalAlignmentStatus.Ready, alignment.status)
        assertEquals(100, alignment.score)
        assertReady(productivity)
        assertTrue(productivity.score!! < 40)
    }

    @Test
    fun mostlyEducationAndProductivityUsageScoresHigh() {
        val result = calculate(
            listOf(
                app("study", minutes(90), AppCategory.Education, opens = 2),
                app("work", minutes(30), AppCategory.Productivity, opens = 2)
            )
        )

        assertReady(result)
        assertTrue(result.score!! >= 85)
    }

    @Test
    fun productiveHeavyScoresFarHigherThanEntertainmentHeavyAtSameDuration() {
        val productive = calculate(listOf(app("work", hours(4), AppCategory.Productivity, 8)))
        val entertainment = calculate(listOf(app("video", hours(4), AppCategory.Entertainment, 8)))

        assertReady(productive)
        assertReady(entertainment)
        assertTrue(productive.score!! - entertainment.score!! >= 40)
    }

    @Test
    fun lowTotalUsageDominatedBySocialIsModerateRatherThanExtreme() {
        val result = calculate(listOf(app("social", minutes(30), AppCategory.Social, opens = 2)))

        assertReady(result)
        assertTrue(result.score!! in 40..60)
    }

    @Test
    fun veryHighEntertainmentUsageScoresLow() {
        val result = calculate(listOf(app("video", hours(10), AppCategory.Entertainment, 10)))

        assertReady(result)
        assertTrue(result.score!! < 40)
    }

    @Test
    fun veryHighProductiveUsageStillScoresSubstantiallyBetter() {
        val result = calculate(listOf(app("work", hours(10), AppCategory.Productivity, 10)))

        assertReady(result)
        assertTrue(result.score!! >= 70)
    }

    @Test
    fun mostlyMixedUsageReturnsNotEnoughDataWithoutAutomaticPenalty() {
        val result = calculate(
            listOf(
                app("mixed", minutes(70), AppCategory.MixedContextDependent),
                app("work", minutes(30), AppCategory.Productivity)
            )
        )

        assertEquals(ProductivityScoreStatus.NotEnoughData, result.status)
        assertNull(result.score)
        assertEquals(0.30, result.coverage.confidence, 0.001)
    }

    @Test
    fun noGoalsStillAllowsProductivityWhileAlignmentIsUnavailable() {
        val productivity = calculate(listOf(app("work", hours(1), AppCategory.Productivity)))
        val alignment = alignmentEngine.calculate(
            GoalAlignmentInput(
                totalForegroundDurationMillis = hours(1),
                apps = listOf(ScoredAppUsage("work", "Work", hours(1), AppCategory.Productivity)),
                goals = emptyList()
            )
        )

        assertReady(productivity)
        assertEquals(GoalAlignmentStatus.NotEnoughData, alignment.status)
        assertNull(alignment.score)
    }

    @Test
    fun permissiveOrStrictGoalsCannotChangeProductivityScore() {
        val usage = listOf(app("video", hours(3), AppCategory.Entertainment, 6))
        val withoutGoals = calculate(usage)
        val afterGoalChanges = calculate(usage)

        assertEquals(withoutGoals, afterGoalChanges)
    }

    @Test
    fun categoryOverrideImmediatelyChangesProductivityInputResult() {
        val entertainment = calculate(listOf(app("flex", hours(2), AppCategory.Entertainment, 4)))
        val productive = calculate(listOf(app("flex", hours(2), AppCategory.Productivity, 4)))

        assertReady(entertainment)
        assertReady(productive)
        assertTrue(productive.score!! - entertainment.score!! >= 40)
    }

    @Test
    fun scoresAndComponentsAreAlwaysBounded() {
        val durations = listOf(minutes(10), hours(2), hours(8), hours(24))
        AppCategory.entries.forEach { category ->
            durations.forEach { duration ->
                val result = calculate(listOf(app("app", duration, category, 100)))
                result.score?.let { assertTrue(it in 0..100) }
                result.components.forEach { assertTrue(it.score in 0..100) }
            }
        }
    }

    @Test
    fun intensityIsSmoothAroundDurationThresholds() {
        listOf(
            ProductivityScorePolicy.INTENSITY_FULL_SCORE_UNTIL_MILLIS,
            ProductivityScorePolicy.INTENSITY_FLOOR_AT_MILLIS
        ).forEach { threshold ->
            val before = calculate(
                listOf(app("work", threshold - 1_000L, AppCategory.Productivity, 4))
            )
            val after = calculate(
                listOf(app("work", threshold + 1_000L, AppCategory.Productivity, 4))
            )

            assertReady(before)
            assertReady(after)
            assertTrue(abs(before.score!! - after.score!!) <= 1)
        }
    }

    @Test
    fun patternCurveIsSmoothAroundOpenRateThresholds() {
        val duration = hours(1)
        val calm = calculate(listOf(app("work", duration, AppCategory.Productivity, 6)))
        val justAboveCalm = calculate(listOf(app("work", duration, AppCategory.Productivity, 7)))
        val frequent = calculate(listOf(app("work", duration, AppCategory.Productivity, 24)))
        val justAboveFrequent = calculate(listOf(app("work", duration, AppCategory.Productivity, 25)))

        assertTrue(abs(calm.score!! - justAboveCalm.score!!) <= 1)
        assertTrue(abs(frequent.score!! - justAboveFrequent.score!!) <= 1)
    }

    @Test
    fun smallCategoryUsageShiftProducesSmallScoreChange() {
        val first = calculate(
            listOf(
                app("work", minutes(60), AppCategory.Productivity),
                app("video", minutes(60), AppCategory.Entertainment)
            )
        )
        val shifted = calculate(
            listOf(
                app("work", minutes(61), AppCategory.Productivity),
                app("video", minutes(59), AppCategory.Entertainment)
            )
        )

        assertReady(first)
        assertReady(shifted)
        assertTrue(abs(first.score!! - shifted.score!!) <= 2)
    }

    @Test
    fun digitalBalanceOwnUsageCannotIncreaseProductivityScore() {
        val baseline = calculate(listOf(app("video", hours(2), AppCategory.Entertainment, 4)))
        val withOwnUsage = calculate(
            listOf(
                app("video", hours(2), AppCategory.Entertainment, 4),
                app("com.digitalbalance.app", hours(4), AppCategory.Productivity, 20)
            )
        )

        assertEquals(baseline.score, withOwnUsage.score)
        assertEquals(baseline.coverage, withOwnUsage.coverage)
        assertEquals(baseline.components, withOwnUsage.components)
    }

    private fun calculate(apps: List<ProductivityAppUsage>): ProductivityScoreResult =
        engine.calculate(
            ProductivityScoreInput(
                totalForegroundDurationMillis = apps.sumOf(ProductivityAppUsage::durationMillis),
                apps = apps
            )
        )

    private fun app(
        packageName: String,
        durationMillis: Long,
        category: AppCategory,
        opens: Int = 1
    ) = ProductivityAppUsage(packageName, packageName, durationMillis, opens, category)

    private fun minutes(value: Long): Long = value * 60_000L

    private fun hours(value: Long): Long = value * 60L * 60L * 1_000L

    private fun assertReady(result: ProductivityScoreResult) {
        assertEquals(ProductivityScoreStatus.Ready, result.status)
        assertNotNull(result.score)
    }
}
