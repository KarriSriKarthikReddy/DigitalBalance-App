package com.digitalbalance.app.domain.analytics

import com.digitalbalance.app.domain.category.AppCategory
import com.digitalbalance.app.domain.category.categoryWithOverride
import com.digitalbalance.app.domain.productivity.ProductivityAppUsage
import com.digitalbalance.app.domain.productivity.ProductivityScoreEngine
import com.digitalbalance.app.domain.productivity.ProductivityScoreInput
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class AnalyticsEngineTest {
    private val engine = AnalyticsEngine()
    private val days = (1..7).map { day ->
        AnalyticsDay("2026-08-0$day", "D$day", day == 7)
    }
    private val dates = AnalyticsDateContext(
        today = days[6],
        yesterday = days[5],
        last7Days = days
    )

    @Test
    fun missingDaysRemainUnavailableInsteadOfZero() {
        val result = calculate(
            AnalyticsPeriod.Last7Days,
            records = listOf(record(1, "app.a", 60)),
            available = setOf(days[0].dateKey)
        )

        assertEquals(1, result.availableDayCount)
        assertEquals(60L, result.dayTotals.first().durationMillis)
        assertNull(result.dayTotals[1].durationMillis)
    }

    @Test
    fun sevenDayTotalAndAverageUseAvailableDaysOnly() {
        val result = calculate(
            AnalyticsPeriod.Last7Days,
            records = listOf(record(1, "app.a", 60), record(3, "app.a", 180)),
            available = setOf(days[0].dateKey, days[2].dateKey)
        )

        assertEquals(240L, result.totalDurationMillis)
        assertEquals(120L, result.dailyAverageMillis)
    }

    @Test
    fun appRowsAggregateAcrossDaysAndChooseMostUsedApp() {
        val result = calculate(
            AnalyticsPeriod.Last7Days,
            records = listOf(
                record(1, "app.a", 100, opens = 2),
                record(2, "app.a", 200, opens = 3),
                record(2, "app.b", 250)
            ),
            available = setOf(days[0].dateKey, days[1].dateKey)
        )

        assertEquals("app.a", result.mostUsedApp?.packageName)
        assertEquals(300L, result.mostUsedApp?.durationMillis)
        assertEquals(5, result.mostUsedApp?.openCount)
    }

    @Test
    fun categoriesAggregateAndChooseMostUsedCategory() {
        val result = calculate(
            AnalyticsPeriod.Last7Days,
            records = listOf(
                record(1, "learn", 300, AppCategory.Education),
                record(2, "chat", 100, AppCategory.Communication),
                record(3, "learn.two", 200, AppCategory.Education)
            ),
            available = setOf(days[0].dateKey, days[1].dateKey, days[2].dateKey)
        )

        assertEquals(AppCategory.Education, result.mostUsedCategory?.category)
        assertEquals(500L, result.mostUsedCategory?.durationMillis)
    }

    @Test
    fun todayComparisonUsesYesterdayWhenBothAreAvailable() {
        val result = calculate(
            AnalyticsPeriod.Today,
            records = listOf(record(6, "app", 600), record(7, "app", 900)),
            available = setOf(days[5].dateKey, days[6].dateKey)
        )

        assertEquals(300L, result.comparison?.differenceMillis)
        assertEquals(50, result.comparison?.percentDifference)
    }

    @Test
    fun comparisonIsAbsentWhenYesterdayIsUnavailable() {
        val result = calculate(
            AnalyticsPeriod.Today,
            records = listOf(record(7, "app", 900)),
            available = setOf(days[6].dateKey)
        )

        assertNull(result.comparison)
    }

    @Test
    fun categoryOverrideCanBeAppliedBeforeAnalyticsAndChangesBreakdown() {
        val override = categoryWithOverride(AppCategory.Other, AppCategory.Productivity)
        val result = calculate(
            AnalyticsPeriod.Today,
            records = listOf(record(7, "app", 500, override)),
            available = setOf(days[6].dateKey)
        )

        assertEquals(AppCategory.Productivity, result.categoryTotals.single().category)
    }

    @Test
    fun completedFocusSessionsAreSeparateAndPeriodBounded() {
        val result = engine.calculate(
            period = AnalyticsPeriod.Last7Days,
            dates = dates,
            usageRecords = listOf(record(1, "app", 100)),
            availableDateKeys = setOf(days[0].dateKey),
            focusRecords = listOf(
                AnalyticsFocusRecord(days[0].dateKey, 1_000, completed = true),
                AnalyticsFocusRecord(days[1].dateKey, 2_000, completed = false),
                AnalyticsFocusRecord("2026-07-01", 4_000, completed = true)
            )
        )

        assertEquals(1, result.focusSummary?.completedSessions)
        assertEquals(1_000L, result.focusSummary?.focusedDurationMillis)
        assertEquals(100L, result.totalDurationMillis)
    }

    @Test
    fun negativeOrInvalidRowsCannotInflateAnalytics() {
        val result = calculate(
            AnalyticsPeriod.Today,
            records = listOf(record(7, "bad", -10), record(7, "good", 20)),
            available = setOf(days[6].dateKey)
        )

        assertEquals(20L, result.totalDurationMillis)
        assertTrue(result.rankedApps.none { it.packageName == "bad" })
    }

    @Test
    fun defaultSelectionPrefersTodayThenLatestAvailableDay() {
        val today = calculate(
            AnalyticsPeriod.Last7Days,
            listOf(record(7, "app", 100)),
            setOf(days[6].dateKey)
        )
        val latest = calculate(
            AnalyticsPeriod.Last7Days,
            listOf(record(2, "app", 100), record(5, "app", 100)),
            setOf(days[1].dateKey, days[4].dateKey)
        )

        assertEquals(days[6].dateKey, today.interactive.defaultSelectedDateKey)
        assertEquals(days[4].dateKey, latest.interactive.defaultSelectedDateKey)
    }

    @Test
    fun selectedDayUsesPreviousAvailableDayAndSkipsMissingDates() {
        val result = calculate(
            AnalyticsPeriod.Last7Days,
            listOf(record(2, "app", 100), record(7, "app", 150)),
            setOf(days[1].dateKey, days[6].dateKey)
        )
        val detail = result.interactive.dayDetails.getValue(days[6].dateKey)

        assertEquals(days[1].dateKey, detail.comparison?.baselineDateKey)
        assertEquals(50L, detail.comparison?.differenceMillis)
        assertEquals(50, detail.comparison?.percentDifference)
    }

    @Test
    fun selectedDayComparisonSupportsLessEqualAndNoPreviousDay() {
        val less = calculate(
            AnalyticsPeriod.Last7Days,
            listOf(record(1, "app", 200), record(2, "app", 100)),
            setOf(days[0].dateKey, days[1].dateKey)
        ).interactive.dayDetails.getValue(days[1].dateKey)
        val equal = calculate(
            AnalyticsPeriod.Last7Days,
            listOf(record(1, "app", 200), record(2, "app", 200)),
            setOf(days[0].dateKey, days[1].dateKey)
        ).interactive.dayDetails.getValue(days[1].dateKey)
        val first = calculate(
            AnalyticsPeriod.Last7Days,
            listOf(record(1, "app", 200)),
            setOf(days[0].dateKey)
        ).interactive.dayDetails.getValue(days[0].dateKey)

        assertEquals(-100L, less.comparison?.differenceMillis)
        assertEquals(0L, equal.comparison?.differenceMillis)
        assertNull(first.comparison)
    }

    @Test
    fun zeroPreviousDurationDoesNotProducePercentage() {
        val result = calculate(
            AnalyticsPeriod.Last7Days,
            listOf(record(2, "app", 100)),
            setOf(days[0].dateKey, days[1].dateKey)
        )

        assertEquals(100L, result.interactive.dayDetails.getValue(days[1].dateKey).comparison?.differenceMillis)
        assertNull(result.interactive.dayDetails.getValue(days[1].dateKey).comparison?.percentDifference)
    }

    @Test
    fun appTrendAggregatesSevenDaysAndAveragesOnlyAvailableDays() {
        val result = calculate(
            AnalyticsPeriod.Last7Days,
            listOf(record(1, "app", 100), record(3, "app", 300)),
            setOf(days[0].dateKey, days[1].dateKey, days[2].dateKey)
        )
        val trend = result.interactive.appTrends.getValue("app")

        assertEquals(400L, trend.totalDurationMillis)
        assertEquals(133L, trend.dailyAverageMillis)
        assertEquals(0L, trend.points[1].durationMillis)
        assertNull(trend.points[3].durationMillis)
    }

    @Test
    fun categoryTrendAggregatesWithoutTreatingMissingDayAsZero() {
        val result = calculate(
            AnalyticsPeriod.Last7Days,
            listOf(
                record(1, "one", 100, AppCategory.Social),
                record(3, "two", 200, AppCategory.Social)
            ),
            setOf(days[0].dateKey, days[2].dateKey)
        )
        val trend = result.interactive.categoryTrends.getValue(AppCategory.Social)

        assertEquals(300L, trend.totalDurationMillis)
        assertEquals(150L, trend.dailyAverageMillis)
        assertNull(trend.points[1].durationMillis)
    }

    @Test
    fun weeklyComparisonRequiresTwoCompleteSevenDayPeriods() {
        val extendedDays = (1..15).map { day ->
            AnalyticsDay("2026-08-${day.toString().padStart(2, '0')}", "D$day", day == 15)
        }
        val extendedDates = AnalyticsDateContext(
            today = extendedDays[14],
            yesterday = extendedDays[13],
            last7Days = extendedDays.subList(8, 15),
            recentCompleted7Days = extendedDays.subList(7, 14),
            previousCompleted7Days = extendedDays.subList(0, 7)
        )
        val records = extendedDays.take(14).mapIndexed { index, day ->
            AnalyticsUsageRecord(
                day.dateKey,
                "app",
                "App",
                if (index < 7) 100L else 80L,
                1,
                AppCategory.Utility
            )
        }
        val complete = engine.calculate(
            AnalyticsPeriod.Last7Days,
            extendedDates,
            records,
            extendedDays.take(14).mapTo(hashSetOf(), AnalyticsDay::dateKey)
        )
        val incomplete = engine.calculate(
            AnalyticsPeriod.Last7Days,
            extendedDates,
            records,
            extendedDays.take(13).mapTo(hashSetOf(), AnalyticsDay::dateKey)
        )

        assertEquals(560L, complete.interactive.weeklyComparison?.recentTotalMillis)
        assertEquals(700L, complete.interactive.weeklyComparison?.previousTotalMillis)
        assertEquals(-140L, complete.interactive.weeklyComparison?.differenceMillis)
        assertNull(incomplete.interactive.weeklyComparison)
    }

    @Test
    fun historicalProductivityUsesExistingEngineAndDefersLowCoverageDay() {
        val duration = 60L * 60L * 1_000L
        val result = calculate(
            AnalyticsPeriod.Last7Days,
            listOf(
                record(6, "mixed", duration, AppCategory.MixedContextDependent),
                record(7, "work", duration, AppCategory.Productivity, opens = 4)
            ),
            setOf(days[5].dateKey, days[6].dateKey)
        )
        val expected = ProductivityScoreEngine().calculate(
            ProductivityScoreInput(
                duration,
                listOf(ProductivityAppUsage("work", "work", duration, 4, AppCategory.Productivity))
            )
        )

        assertNull(result.interactive.productivityTrend[5].score)
        assertEquals(expected.score, result.interactive.productivityTrend[6].score)
        assertTrue(
            AnalyticsInteractiveData::class.java.declaredFields.none {
                it.name.contains("goal", ignoreCase = true)
            }
        )
    }

    @Test
    fun digitalBalanceIsIncludedInUsageAnalyticsButExcludedFromHistoricalProductivity() {
        val instagramDuration = 60L * 60L * 1_000L
        val ownDuration = 20L * 60L * 1_000L
        val result = calculate(
            AnalyticsPeriod.Last7Days,
            listOf(
                record(7, "com.instagram.android", instagramDuration, AppCategory.Social, opens = 3),
                record(7, "com.digitalbalance.app", ownDuration, AppCategory.Productivity, opens = 2)
            ),
            setOf(days[6].dateKey)
        )
        val expectedProductivity = ProductivityScoreEngine().calculate(
            ProductivityScoreInput(
                totalForegroundDurationMillis = instagramDuration,
                apps = listOf(
                    ProductivityAppUsage(
                        "com.instagram.android",
                        "com.instagram.android",
                        instagramDuration,
                        3,
                        AppCategory.Social
                    )
                )
            )
        )
        val dayDetail = result.interactive.dayDetails.getValue(days[6].dateKey)

        assertEquals(instagramDuration + ownDuration, result.totalDurationMillis)
        assertEquals(2, result.activeAppCount)
        assertTrue(result.rankedApps.any { it.packageName == "com.digitalbalance.app" })
        assertEquals(
            ownDuration,
            result.interactive.appTrends.getValue("com.digitalbalance.app").totalDurationMillis
        )
        assertEquals(0L, dayDetail.productiveDurationMillis)
        assertEquals(1.0, dayDetail.classifiedCoverage, 0.0)
        assertEquals(expectedProductivity.score, result.interactive.productivityTrend[6].score)
        assertEquals(expectedProductivity.coverage.classifiedCoverage, result.interactive.productivityTrend[6].coverage)
    }


    private fun calculate(
        period: AnalyticsPeriod,
        records: List<AnalyticsUsageRecord>,
        available: Set<String>
    ) = engine.calculate(period, dates, records, available)

    private fun record(
        day: Int,
        packageName: String,
        duration: Long,
        category: AppCategory = AppCategory.Other,
        opens: Int = 1
    ) = AnalyticsUsageRecord(
        dateKey = days[day - 1].dateKey,
        packageName = packageName,
        appName = packageName,
        durationMillis = duration,
        openCount = opens,
        category = category
    )
}
