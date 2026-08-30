package com.digitalbalance.app.domain.score

import com.digitalbalance.app.domain.category.AppCategory
import com.digitalbalance.app.domain.goal.DigitalGoal
import com.digitalbalance.app.domain.goal.GoalType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.math.abs
import kotlin.math.roundToInt

class GoalAlignmentEngineTest {
    private val engine = GoalAlignmentEngine()

    @Test
    fun `no goals configured returns not enough data`() {
        val result = calculate(apps = listOf(app("work", 60, AppCategory.Productivity)))

        assertEquals(GoalAlignmentStatus.NotEnoughData, result.status)
        assertNull(result.score)
    }

    @Test
    fun `productive alignment follows smooth progress checkpoints`() {
        val checkpoints = listOf(
            0L to 0,
            15L to 16,
            30L to 50,
            45L to 84,
            60L to 100
        )

        checkpoints.forEach { (productiveMinutes, expectedScore) ->
            val result = calculate(
                apps = listOf(
                    app("work", productiveMinutes, AppCategory.Productivity),
                    app("messages", 60L - productiveMinutes, AppCategory.Communication)
                ),
                goals = listOf(goal(GoalType.ProductiveTime, 60))
            )

            assertReady(result)
            assertEquals(
                "Unexpected score at $productiveMinutes minutes",
                expectedScore,
                result.components.single().score
            )
        }
    }

    @Test
    fun `social limit exceeded reduces its component`() {
        val result = calculate(
            apps = listOf(app("social", 90, AppCategory.Social)),
            goals = listOf(goal(GoalType.SocialMediaLimit, 60))
        )

        assertReady(result)
        assertTrue(result.components.single().score < 100)
    }

    @Test
    fun `entertainment limit exceeded reduces its component`() {
        val result = calculate(
            apps = listOf(app("video", 120, AppCategory.Entertainment)),
            goals = listOf(goal(GoalType.EntertainmentLimit, 60))
        )

        assertReady(result)
        assertTrue(result.components.single().score < 50)
    }

    @Test
    fun `overall limit exceeded reduces only configured overall component`() {
        val result = calculate(
            apps = listOf(app("work", 120, AppCategory.Productivity)),
            goals = listOf(goal(GoalType.OverallForegroundUsage, 60))
        )

        assertReady(result)
        assertEquals(ScoreComponentKind.OverallLimit, result.components.single().kind)
        assertTrue(result.score!! < 50)
    }

    @Test
    fun `per app limit exceeded uses matching package only`() {
        val appGoal = appGoal("limited", "Limited", 30)
        val result = calculate(
            apps = listOf(
                app("limited", 60, AppCategory.MixedContextDependent),
                app("other", 180, AppCategory.Other)
            ),
            goals = listOf(appGoal)
        )

        assertReady(result)
        assertEquals(minutes(60), result.components.single().actualDurationMillis)
        assertTrue(result.score!! < 50)
    }

    @Test
    fun `high total usage is not penalized when productive goal is completed`() {
        val result = calculate(
            apps = listOf(
                app("work", 300, AppCategory.Productivity),
                app("browser", 60, AppCategory.MixedContextDependent)
            ),
            goals = listOf(goal(GoalType.ProductiveTime, 180))
        )

        assertReady(result)
        assertEquals(100, result.score)
    }

    @Test
    fun `heavy mixed usage makes category-only score unavailable instead of negative`() {
        val result = calculate(
            apps = listOf(
                app("mixed", 50, AppCategory.MixedContextDependent),
                app("work", 10, AppCategory.Productivity)
            ),
            goals = listOf(goal(GoalType.ProductiveTime, 60))
        )

        assertEquals(GoalAlignmentStatus.NotEnoughData, result.status)
        assertNull(result.score)
        assertEquals(1.0 / 6.0, result.coverage.classificationCoverage, 0.001)
    }

    @Test
    fun `category override changes score through categorized input`() {
        val goals = listOf(
            goal(GoalType.ProductiveTime, 60),
            goal(GoalType.SocialMediaLimit, 20)
        )
        val before = calculate(listOf(app("flex", 30, AppCategory.Social)), goals)
        val after = calculate(listOf(app("flex", 30, AppCategory.Productivity)), goals)

        assertReady(before)
        assertReady(after)
        assertTrue(after.score!! > before.score!!)
    }

    @Test
    fun `unknown usage lowers coverage but not category-independent goal score`() {
        val result = calculate(
            apps = listOf(app("unknown", 30, AppCategory.Other)),
            goals = listOf(goal(GoalType.OverallForegroundUsage, 60))
        )

        assertReady(result)
        assertEquals(100, result.score)
        assertEquals(0.0, result.coverage.classificationCoverage, 0.0)
        assertEquals(1.0, result.coverage.confidence, 0.0)
    }

    @Test
    fun `missing individual goals add no components or penalty`() {
        val result = calculate(
            apps = listOf(app("social", 30, AppCategory.Social)),
            goals = listOf(goal(GoalType.SocialMediaLimit, 60))
        )

        assertReady(result)
        assertEquals(1, result.components.size)
        assertEquals(GoalAlignmentPolicy.SOCIAL_LIMIT_WEIGHT, result.totalActiveWeight, 0.0)
        assertEquals(100, result.score)
    }

    @Test
    fun `score always remains within zero and one hundred`() {
        val goals = listOf(
            goal(GoalType.ProductiveTime, 60),
            goal(GoalType.OverallForegroundUsage, 60),
            goal(GoalType.SocialMediaLimit, 60),
            goal(GoalType.EntertainmentLimit, 60),
            appGoal("app", "App", 60)
        )
        listOf(0L, 1L, 60L, 600L, 6_000L).forEach { usageMinutes ->
            val result = calculate(
                apps = listOf(app("app", usageMinutes, AppCategory.Social)),
                goals = goals
            )
            result.score?.let { assertTrue(it in 0..100) }
            result.components.forEach { assertTrue(it.score in 0..100) }
        }
    }

    @Test
    fun `limit score is full just below and exactly at the boundary`() {
        val goal = goal(GoalType.OverallForegroundUsage, 60)
        val justBelow = calculate(
            listOf(appMillis("app", minutes(60) - 1_000L, AppCategory.Utility)),
            listOf(goal)
        )
        val exactlyAt = calculate(
            listOf(app("app", 60, AppCategory.Utility)),
            listOf(goal)
        )

        assertReady(justBelow)
        assertReady(exactlyAt)
        assertEquals(100, justBelow.components.single().score)
        assertEquals(100, exactlyAt.components.single().score)
    }

    @Test
    fun `slight limit overage is softened while significant overage declines`() {
        val goal = goal(GoalType.OverallForegroundUsage, 60)
        val onePercentAbove = calculate(
            listOf(appMillis("app", minutes(60) + 36_000L, AppCategory.Utility)),
            listOf(goal)
        )
        val fivePercentAbove = calculate(
            listOf(app("app", 63, AppCategory.Utility)),
            listOf(goal)
        )
        val twiceTheLimit = calculate(
            listOf(app("app", 120, AppCategory.Utility)),
            listOf(goal)
        )

        assertReady(onePercentAbove)
        assertReady(fivePercentAbove)
        assertReady(twiceTheLimit)
        assertEquals(100, onePercentAbove.components.single().score)
        assertEquals(97, fivePercentAbove.components.single().score)
        assertEquals(30, twiceTheLimit.components.single().score)
    }

    @Test
    fun `limit decay is monotonic as overage increases`() {
        val goal = goal(GoalType.OverallForegroundUsage, 60)
        val scores = (60L..180L).map { usageMinutes ->
            calculate(
                listOf(app("app", usageMinutes, AppCategory.Utility)),
                listOf(goal)
            ).components.single().score
        }

        scores.zipWithNext().forEach { (earlier, later) ->
            assertTrue("Limit score improved from $earlier to $later", later <= earlier)
        }
    }

    @Test
    fun `limit boundary has no abrupt score change`() {
        val goal = goal(GoalType.OverallForegroundUsage, 60)
        val oneSecondBefore = calculate(
            listOf(appMillis("app", minutes(60) - 1_000L, AppCategory.Utility)),
            listOf(goal)
        )
        val atLimit = calculate(
            listOf(app("app", 60, AppCategory.Utility)),
            listOf(goal)
        )
        val oneSecondLater = calculate(
            listOf(appMillis("app", minutes(60) + 1_000L, AppCategory.Utility)),
            listOf(goal)
        )

        assertReady(oneSecondBefore)
        assertReady(atLimit)
        assertReady(oneSecondLater)
        assertTrue(abs(oneSecondBefore.score!! - atLimit.score!!) <= 1)
        assertTrue(abs(atLimit.score!! - oneSecondLater.score!!) <= 1)
    }

    @Test
    fun `active component weights are normalized over configured goals only`() {
        val result = calculate(
            apps = listOf(app("work", 120, AppCategory.Productivity)),
            goals = listOf(
                goal(GoalType.ProductiveTime, 240),
                goal(GoalType.OverallForegroundUsage, 60)
            )
        )

        assertReady(result)
        assertEquals(5.0, result.totalActiveWeight, 0.0)
        val expected = result.components.sumOf { it.score * it.activeWeight } /
            result.totalActiveWeight
        assertTrue(abs(expected.roundToInt() - result.score!!) <= 1)
    }

    @Test
    fun `multiple per app limits share one fixed group weight`() {
        val result = calculate(
            apps = listOf(
                app("one", 30, AppCategory.Other),
                app("two", 30, AppCategory.Other)
            ),
            goals = listOf(
                appGoal("one", "One", 60),
                appGoal("two", "Two", 60)
            )
        )

        assertReady(result)
        assertEquals(GoalAlignmentPolicy.PER_APP_LIMITS_TOTAL_WEIGHT, result.totalActiveWeight, 0.0)
        result.components.forEach {
            assertEquals(GoalAlignmentPolicy.PER_APP_LIMITS_TOTAL_WEIGHT / 2.0, it.activeWeight, 0.0)
        }
    }

    @Test
    fun `confidence combines classification coverage with component dependency`() {
        val result = calculate(
            apps = listOf(
                app("work", 30, AppCategory.Productivity),
                app("mixed", 30, AppCategory.MixedContextDependent)
            ),
            goals = listOf(
                goal(GoalType.ProductiveTime, 60),
                goal(GoalType.OverallForegroundUsage, 120)
            )
        )

        assertReady(result)
        assertEquals(0.5, result.coverage.classificationCoverage, 0.0)
        assertEquals(0.7, result.coverage.confidence, 0.001)
        assertEquals(ScoreConfidence.Medium, result.coverage.confidenceLevel)
    }

    @Test
    fun `digital balance cannot contribute to category goal alignment or coverage`() {
        val goals = listOf(goal(GoalType.ProductiveTime, 60))
        val baseline = calculate(
            apps = listOf(app("work", 60, AppCategory.Productivity)),
            goals = goals
        )
        val withOwnUsage = calculate(
            apps = listOf(
                app("work", 60, AppCategory.Productivity),
                app("com.digitalbalance.app", 20, AppCategory.Productivity)
            ),
            goals = goals
        )

        assertReady(baseline)
        assertReady(withOwnUsage)
        assertEquals(baseline.score, withOwnUsage.score)
        assertEquals(baseline.components, withOwnUsage.components)
        assertEquals(baseline.coverage, withOwnUsage.coverage)
        assertEquals(minutes(60), withOwnUsage.components.single().actualDurationMillis)
    }

    @Test
    fun `overall and explicit per app goals include digital balance`() {
        val result = calculate(
            apps = listOf(
                app("other", 60, AppCategory.Utility),
                app("com.digitalbalance.app", 20, AppCategory.Utility)
            ),
            goals = listOf(
                goal(GoalType.OverallForegroundUsage, 60),
                appGoal("com.digitalbalance.app", "DigitalBalance", 10)
            )
        )

        assertReady(result)
        val overall = result.components.single { it.kind == ScoreComponentKind.OverallLimit }
        val ownLimit = result.components.single { it.kind == ScoreComponentKind.PerAppLimit }
        assertEquals(minutes(80), overall.actualDurationMillis)
        assertEquals(minutes(20), ownLimit.actualDurationMillis)
    }

    private fun calculate(
        apps: List<ScoredAppUsage> = emptyList(),
        goals: List<DigitalGoal> = emptyList()
    ): GoalAlignmentResult = engine.calculate(
        GoalAlignmentInput(
            totalForegroundDurationMillis = apps.sumOf(ScoredAppUsage::durationMillis),
            apps = apps,
            goals = goals
        )
    )

    private fun goal(type: GoalType, targetMinutes: Long) = DigitalGoal(
        id = DigitalGoal.idFor(type),
        type = type,
        targetDurationMillis = minutes(targetMinutes)
    )

    private fun appGoal(packageName: String, appName: String, targetMinutes: Long) = DigitalGoal(
        id = DigitalGoal.idFor(GoalType.AppDailyLimit, packageName),
        type = GoalType.AppDailyLimit,
        targetDurationMillis = minutes(targetMinutes),
        packageName = packageName,
        appName = appName
    )

    private fun app(packageName: String, durationMinutes: Long, category: AppCategory) =
        appMillis(packageName, minutes(durationMinutes), category)

    private fun appMillis(packageName: String, durationMillis: Long, category: AppCategory) =
        ScoredAppUsage(packageName, packageName, durationMillis, category)

    private fun minutes(value: Long): Long = value * 60_000L

    private fun assertReady(result: GoalAlignmentResult) {
        assertEquals(GoalAlignmentStatus.Ready, result.status)
        assertNotNull(result.score)
    }
}
