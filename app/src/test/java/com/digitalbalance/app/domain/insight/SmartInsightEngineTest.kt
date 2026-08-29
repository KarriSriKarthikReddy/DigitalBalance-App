package com.digitalbalance.app.domain.insight

import com.digitalbalance.app.domain.category.AppCategory
import com.digitalbalance.app.domain.goal.DigitalGoal
import com.digitalbalance.app.domain.goal.GoalProgress
import com.digitalbalance.app.domain.goal.GoalType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class SmartInsightEngineTest {
    private val engine = SmartInsightEngine()

    @Test
    fun productiveTargetProgressUsesRealCalculatedProgress() {
        val result = generate(
            apps = listOf(app("work", 60, AppCategory.Productivity)),
            goals = listOf(progress(GoalType.ProductiveTime, 60, 30))
        )

        val insight = result.single { it.type == InsightType.GoalProgress }
        assertEquals("50% complete", insight.supportingMetric)
        assertTrue(insight.description.contains("30 min remaining"))
    }

    @Test
    fun productiveGoalCompletedCreatesPositiveSuccess() {
        val insight = generate(
            apps = listOf(app("work", 60, AppCategory.Productivity)),
            goals = listOf(progress(GoalType.ProductiveTime, 60, 60))
        ).single { it.id.startsWith("goal_success:productive") }

        assertEquals(InsightSeverity.Positive, insight.severity)
        assertEquals("100% complete", insight.supportingMetric)
    }

    @Test
    fun socialLimitNearingLimitCreatesProgressNotExceeded() {
        val result = generate(
            apps = listOf(app("social", 50, AppCategory.Social)),
            goals = listOf(progress(GoalType.SocialMediaLimit, 60, 50))
        )

        assertTrue(result.any { it.title == "Social limit is close" })
        assertFalse(result.any { it.type == InsightType.GoalExceeded })
    }

    @Test
    fun socialLimitExceededRanksAsAttentionWithFocusAction() {
        val insight = generate(
            apps = listOf(app("social", 90, AppCategory.Social)),
            goals = listOf(progress(GoalType.SocialMediaLimit, 60, 90))
        ).first()

        assertEquals(InsightType.GoalExceeded, insight.type)
        assertEquals(InsightSeverity.Attention, insight.severity)
        assertEquals(InsightActionType.OpenFocus, insight.actionType)
        assertTrue(insight.description.contains("30 min above"))
    }

    @Test
    fun tinyLimitOverageProducesNoMisleadingLimitInsight() {
        val result = generate(
            apps = listOf(app("social", 60, AppCategory.Social)),
            goals = listOf(
                progressMillis(
                    type = GoalType.SocialMediaLimit,
                    targetMillis = minutes(60),
                    currentMillis = minutes(60) + 30_000L
                )
            )
        )

        assertTrue(result.none { it.relatedGoalId == DigitalGoal.idFor(GoalType.SocialMediaLimit) })
        assertFalse(result.any { it.id == "goal_success:all_limits" })
    }

    @Test
    fun entertainmentLimitExceededIsSupported() {
        val insight = generate(
            apps = listOf(app("video", 80, AppCategory.Entertainment)),
            goals = listOf(progress(GoalType.EntertainmentLimit, 60, 80))
        ).first()

        assertEquals("Entertainment limit exceeded", insight.title)
        assertEquals(InsightActionType.OpenFocus, insight.actionType)
    }

    @Test
    fun perAppLimitExceededKeepsRelatedPackageAndNeutralRecommendation() {
        val goal = progress(
            type = GoalType.AppDailyLimit,
            targetMinutes = 30,
            currentMinutes = 45,
            packageName = "social.app",
            appName = "Social App"
        )
        val insight = generate(
            apps = listOf(app("social.app", 45, AppCategory.Social)),
            goals = listOf(goal)
        ).first()

        assertEquals(InsightType.GoalExceeded, insight.type)
        assertEquals("social.app", insight.relatedPackageName)
        assertTrue(requireNotNull(insight.recommendation).contains("intentional"))
    }

    @Test
    fun allConfiguredLimitsRespectedProducesOneAggregateSuccess() {
        val result = generate(
            apps = listOf(app("utility", 30, AppCategory.Utility)),
            goals = listOf(
                progress(GoalType.SocialMediaLimit, 60, 10),
                progress(GoalType.EntertainmentLimit, 60, 5)
            )
        )

        val success = result.single { it.id == "goal_success:all_limits" }
        assertEquals("2 limits respected", success.supportingMetric)
    }

    @Test
    fun dominantAppRequiresMeaningfulShareAndDuration() {
        val insight = generate(
            apps = listOf(
                app("instagram", 35, AppCategory.Social, "Instagram"),
                app("chat", 25, AppCategory.Social, "Chat")
            )
        ).single { it.type == InsightType.DominantApp }

        assertEquals("instagram", insight.relatedPackageName)
        assertTrue(insight.description.contains("58%"))
    }

    @Test
    fun dominantCategoryRequiresMeaningfulConcentration() {
        val insight = generate(
            apps = listOf(
                app("work", 40, AppCategory.Productivity),
                app("chat", 20, AppCategory.Communication)
            )
        ).single { it.type == InsightType.DominantCategory }

        assertEquals(AppCategory.Productivity, insight.relatedCategory)
        assertTrue(insight.description.contains("67%"))
    }

    @Test
    fun heavyMixedUsageCreatesClassificationActionWithoutInferringPurpose() {
        val insight = generate(
            apps = listOf(
                app("mixed", 40, AppCategory.MixedContextDependent),
                app("work", 20, AppCategory.Productivity)
            )
        ).single { it.type == InsightType.ClassificationCoverage }

        assertEquals(InsightActionType.ReviewCategories, insight.actionType)
        assertTrue(insight.title.contains("context-dependent"))
        assertFalse(insight.description.contains("unproductive", ignoreCase = true))
    }

    @Test
    fun frequentOpensRequiresBothOpenAndUsageThresholds() {
        val insight = generate(
            apps = listOf(app("chat", 15, AppCategory.Communication, opens = 12))
        ).single { it.type == InsightType.FrequentOpens }

        assertEquals("12 opens", insight.supportingMetric)
        assertEquals(InsightActionType.OpenApps, insight.actionType)
    }

    @Test
    fun lowUsageProducesNoNoisyInsight() {
        val result = generate(
            apps = listOf(app("chat", 9, AppCategory.Social, opens = 20)),
            goals = listOf(progress(GoalType.SocialMediaLimit, 10, 9))
        )

        assertTrue(result.isEmpty())
    }

    @Test
    fun overlappingLimitConditionsAreDeduplicated() {
        val goal = progress(GoalType.SocialMediaLimit, 60, 90)
        val result = generate(
            apps = listOf(app("social", 90, AppCategory.Social)),
            goals = listOf(goal, goal)
        )

        assertEquals(1, result.count { it.relatedGoalId == goal.goal.id })
        assertFalse(result.any { it.id == "goal_success:all_limits" })
    }

    @Test
    fun rankingPutsExceededGoalBeforeCoverageAndPatterns() {
        val result = generate(
            apps = listOf(
                app("social", 70, AppCategory.Social, opens = 20),
                app("mixed", 60, AppCategory.MixedContextDependent)
            ),
            goals = listOf(progress(GoalType.SocialMediaLimit, 60, 70))
        )

        assertEquals(InsightType.GoalExceeded, result.first().type)
    }

    @Test
    fun entertainmentBalanceObservationCoexistsWithPermissiveLimitProgress() {
        val result = generate(
            apps = listOf(app("video", 540, AppCategory.Entertainment)),
            goals = listOf(progress(GoalType.EntertainmentLimit, 600, 540))
        )

        assertTrue(result.any { it.type == InsightType.GoalProgress })
        assertTrue(
            result.any {
                it.type == InsightType.DominantCategory &&
                    it.relatedCategory == AppCategory.Entertainment
            }
        )
    }

    @Test
    fun noGoalsConfiguredDoesNotInventGoalAdvice() {
        val result = generate(
            apps = listOf(
                app("one", 5, AppCategory.Utility),
                app("two", 5, AppCategory.Communication)
            )
        )

        assertTrue(result.none { it.type in goalInsightTypes })
    }

    @Test
    fun missingClassificationDataProducesCoverageInsight() {
        val result = generate(apps = listOf(app("unknown", 60, AppCategory.Other)))

        assertTrue(result.any { it.type == InsightType.ClassificationCoverage })
    }

    @Test
    fun noHistoricalComparisonIsFabricated() {
        val result = generate(apps = listOf(app("work", 60, AppCategory.Productivity)))

        assertTrue(result.none { it.title.contains("yesterday", true) })
        assertTrue(result.none { it.description.contains("previous", true) })
    }

    @Test
    fun outputIsDeterministicRegardlessOfInputOrder() {
        val apps = listOf(
            app("social.a", 40, AppCategory.Social, opens = 15),
            app("social.b", 20, AppCategory.Social),
            app("mixed", 40, AppCategory.MixedContextDependent)
        )
        val goals = listOf(
            progress(GoalType.SocialMediaLimit, 50, 60),
            progress(GoalType.ProductiveTime, 60, 20)
        )

        val first = generate(apps, goals)
        val second = generate(apps.reversed(), goals.reversed())

        assertEquals(first, second)
    }

    private fun generate(
        apps: List<InsightAppUsage>,
        goals: List<GoalProgress> = emptyList()
    ): List<PersonalInsight> = engine.generate(
        InsightInput(
            totalForegroundDurationMillis = apps.sumOf(InsightAppUsage::durationMillis),
            apps = apps,
            goalProgress = goals,
            productivityScoreResult = null
        )
    )

    private fun app(
        packageName: String,
        minutes: Long,
        category: AppCategory,
        appName: String = packageName,
        opens: Int = 0
    ) = InsightAppUsage(packageName, appName, minutes(minutes), opens, category)

    private fun progress(
        type: GoalType,
        targetMinutes: Long,
        currentMinutes: Long,
        packageName: String? = null,
        appName: String? = null
    ): GoalProgress {
        val normalizedPackage = if (type == GoalType.AppDailyLimit) {
            requireNotNull(packageName)
        } else {
            null
        }
        val goal = DigitalGoal(
            id = DigitalGoal.idFor(type, normalizedPackage),
            type = type,
            targetDurationMillis = minutes(targetMinutes),
            packageName = normalizedPackage,
            appName = appName
        )
        return GoalProgress(
            goal = goal,
            currentDurationMillis = minutes(currentMinutes),
            progressFraction = currentMinutes.toFloat() / targetMinutes
        )
    }

    private fun progressMillis(
        type: GoalType,
        targetMillis: Long,
        currentMillis: Long
    ): GoalProgress {
        val goal = DigitalGoal(
            id = DigitalGoal.idFor(type),
            type = type,
            targetDurationMillis = targetMillis
        )
        return GoalProgress(
            goal = goal,
            currentDurationMillis = currentMillis,
            progressFraction = currentMillis.toFloat() / targetMillis
        )
    }

    private fun minutes(value: Long): Long = value * 60_000L

    private companion object {
        val goalInsightTypes = setOf(
            InsightType.GoalProgress,
            InsightType.GoalExceeded,
            InsightType.GoalSuccess
        )
    }
}
