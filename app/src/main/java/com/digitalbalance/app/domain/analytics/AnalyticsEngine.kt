package com.digitalbalance.app.domain.analytics

import com.digitalbalance.app.domain.category.AppCategory
import com.digitalbalance.app.domain.productivity.ProductivityAppUsage
import com.digitalbalance.app.domain.productivity.ProductivityScoreEngine
import com.digitalbalance.app.domain.productivity.ProductivityScoreInput
import com.digitalbalance.app.domain.productivity.ProductivityScorePolicy
import kotlin.math.roundToInt

class AnalyticsEngine(
    private val productivityScoreEngine: ProductivityScoreEngine = ProductivityScoreEngine()
) {
    fun calculate(
        period: AnalyticsPeriod,
        dates: AnalyticsDateContext,
        usageRecords: List<AnalyticsUsageRecord>,
        availableDateKeys: Set<String>,
        focusRecords: List<AnalyticsFocusRecord> = emptyList()
    ): AnalyticsSnapshot {
        val selectedDays = dates.daysFor(period)
        val selectedKeys = selectedDays.mapTo(linkedSetOf(), AnalyticsDay::dateKey)
        val validRecords = usageRecords.filter {
            it.durationMillis >= 0L &&
                it.openCount >= 0
        }
        val selectedRecords = validRecords.filter { it.dateKey in selectedKeys }
        val availableKeys = availableDateKeys.intersect(selectedKeys)
        val dayTotals = selectedDays.map { day ->
            AnalyticsDayTotal(
                day = day,
                durationMillis = if (day.dateKey in availableKeys) {
                    selectedRecords.filter { it.dateKey == day.dateKey }.sumOf { it.durationMillis }
                } else {
                    null
                }
            )
        }
        val rankedApps = selectedRecords
            .groupBy(AnalyticsUsageRecord::packageName)
            .map { (packageName, rows) ->
                val representative = rows.maxByOrNull(AnalyticsUsageRecord::durationMillis)!!
                AnalyticsAppTotal(
                    packageName = packageName,
                    appName = representative.appName,
                    durationMillis = rows.sumOf(AnalyticsUsageRecord::durationMillis),
                    openCount = rows.sumOf(AnalyticsUsageRecord::openCount),
                    category = representative.category
                )
            }
            .filter { it.durationMillis > 0L }
            .sortedWith(
                compareByDescending<AnalyticsAppTotal>(AnalyticsAppTotal::durationMillis)
                    .thenBy(String.CASE_INSENSITIVE_ORDER, AnalyticsAppTotal::appName)
            )
        val categoryTotals = selectedRecords
            .groupBy(AnalyticsUsageRecord::category)
            .map { (category, rows) ->
                AnalyticsCategoryTotal(category, rows.sumOf(AnalyticsUsageRecord::durationMillis))
            }
            .filter { it.durationMillis > 0L }
            .sortedByDescending(AnalyticsCategoryTotal::durationMillis)
        val total = dayTotals.sumOf { it.durationMillis ?: 0L }
        val average = if (period == AnalyticsPeriod.Last7Days && availableKeys.isNotEmpty()) {
            total / availableKeys.size
        } else {
            null
        }
        val comparison = if (
            period == AnalyticsPeriod.Today &&
            dates.today.dateKey in availableDateKeys &&
            dates.yesterday.dateKey in availableDateKeys
        ) {
            val todayTotal = validRecords
                .filter { it.dateKey == dates.today.dateKey }
                .sumOf(AnalyticsUsageRecord::durationMillis)
            val yesterdayTotal = validRecords
                .filter { it.dateKey == dates.yesterday.dateKey }
                .sumOf(AnalyticsUsageRecord::durationMillis)
            AnalyticsComparison(
                baselineDateKey = dates.yesterday.dateKey,
                differenceMillis = todayTotal - yesterdayTotal,
                percentDifference = if (yesterdayTotal > 0L) {
                    (((todayTotal - yesterdayTotal).toDouble() / yesterdayTotal) * 100.0).roundToInt()
                } else {
                    null
                },
                baselineLabel = dates.yesterday.longLabel
            )
        } else {
            null
        }
        val completedFocus = focusRecords.filter {
            it.completed && it.endedDateKey in selectedKeys && it.focusedDurationMillis >= 0L
        }
        val focusSummary = completedFocus.takeIf(List<AnalyticsFocusRecord>::isNotEmpty)?.let {
            AnalyticsFocusSummary(
                completedSessions = it.size,
                focusedDurationMillis = it.sumOf(AnalyticsFocusRecord::focusedDurationMillis)
            )
        }

        return AnalyticsSnapshot(
            period = period,
            dayTotals = dayTotals,
            availableDayCount = availableKeys.size,
            expectedDayCount = selectedDays.size,
            totalDurationMillis = total,
            dailyAverageMillis = average,
            rankedApps = rankedApps,
            categoryTotals = categoryTotals,
            activeAppCount = rankedApps.size,
            comparison = comparison,
            focusSummary = focusSummary,
            interactive = buildInteractive(dates, validRecords, availableDateKeys)
        )
    }

    private fun buildInteractive(
        dates: AnalyticsDateContext,
        records: List<AnalyticsUsageRecord>,
        availableDateKeys: Set<String>
    ): AnalyticsInteractiveData {
        val last7Keys = dates.last7Days.mapTo(linkedSetOf(), AnalyticsDay::dateKey)
        val last7Records = records.filter { it.dateKey in last7Keys }
        val knownDays = (
            dates.previousCompleted7Days +
                dates.recentCompleted7Days +
                dates.last7Days
            ).distinctBy(AnalyticsDay::dateKey).sortedBy(AnalyticsDay::dateKey)
        val defaultSelected = when {
            dates.today.dateKey in availableDateKeys -> dates.today.dateKey
            else -> dates.last7Days.lastOrNull { it.dateKey in availableDateKeys }?.dateKey
        }
        val dayDetails = dates.last7Days
            .filter { it.dateKey in availableDateKeys }
            .associate { day ->
                val dayRows = last7Records.filter { it.dateKey == day.dateKey }
                val total = dayRows.sumOf(AnalyticsUsageRecord::durationMillis)
                val apps = aggregateApps(dayRows)
                val categories = aggregateCategories(dayRows)
                val productivityEligibleRows = dayRows.filterNot {
                    it.packageName in ProductivityScorePolicy.EXCLUDED_PACKAGES
                }
                val productivityEligibleTotal = productivityEligibleRows
                    .sumOf(AnalyticsUsageRecord::durationMillis)
                val classifiedDuration = productivityEligibleRows
                    .filter { it.category in ProductivityScorePolicy.CATEGORY_SCORES }
                    .sumOf(AnalyticsUsageRecord::durationMillis)
                val productive = productivityEligibleRows
                    .filter {
                        it.category == AppCategory.Education || it.category == AppCategory.Productivity
                    }
                    .sumOf(AnalyticsUsageRecord::durationMillis)
                day.dateKey to AnalyticsDayDetail(
                    day = day,
                    totalDurationMillis = total,
                    comparison = comparisonForDay(
                        day = day,
                        selectedDuration = total,
                        knownDays = knownDays,
                        availableDateKeys = availableDateKeys,
                        durationForDate = { dateKey ->
                            records.filter { it.dateKey == dateKey }
                                .sumOf(AnalyticsUsageRecord::durationMillis)
                        }
                    ),
                    topApp = apps.firstOrNull(),
                    topCategory = categories.firstOrNull(),
                    activeAppCount = apps.size,
                    topApps = apps.take(3),
                    productiveDurationMillis = productive,
                    classifiedCoverage = if (productivityEligibleTotal > 0L) {
                        (classifiedDuration.toDouble() / productivityEligibleTotal).coerceIn(0.0, 1.0)
                    } else {
                        0.0
                    }
                )
            }
        val availableLast7Count = dates.last7Days.count { it.dateKey in availableDateKeys }
        val appTrends = last7Records.groupBy(AnalyticsUsageRecord::packageName).mapValues { (_, rows) ->
            val representative = rows.maxByOrNull(AnalyticsUsageRecord::durationMillis)!!
            val allRowsForApp = records.filter { it.packageName == representative.packageName }
            val points = dates.last7Days.map { day ->
                AnalyticsTrendPoint(
                    day,
                    if (day.dateKey in availableDateKeys) {
                        rows.filter { it.dateKey == day.dateKey }.sumOf(AnalyticsUsageRecord::durationMillis)
                    } else {
                        null
                    }
                )
            }
            val total = points.sumOf { it.durationMillis ?: 0L }
            AnalyticsAppTrend(
                packageName = representative.packageName,
                appName = representative.appName,
                category = representative.category,
                points = points,
                totalDurationMillis = total,
                dailyAverageMillis = total.takeIf { availableLast7Count > 0 }?.div(availableLast7Count),
                availableDayCount = availableLast7Count,
                comparison = trendComparison(
                    dates,
                    knownDays,
                    availableDateKeys,
                    allRowsForApp.groupBy(AnalyticsUsageRecord::dateKey)
                        .mapValues { (_, values) -> values.sumOf(AnalyticsUsageRecord::durationMillis) }
                )
            )
        }
        val categoryTrends = AppCategory.entries.associateWith { category ->
            val rows = last7Records.filter { it.category == category }
            val points = dates.last7Days.map { day ->
                AnalyticsTrendPoint(
                    day,
                    if (day.dateKey in availableDateKeys) {
                        rows.filter { it.dateKey == day.dateKey }.sumOf(AnalyticsUsageRecord::durationMillis)
                    } else {
                        null
                    }
                )
            }
            val total = points.sumOf { it.durationMillis ?: 0L }
            AnalyticsCategoryTrend(
                category = category,
                points = points,
                totalDurationMillis = total,
                dailyAverageMillis = total.takeIf { availableLast7Count > 0 }?.div(availableLast7Count),
                availableDayCount = availableLast7Count,
                comparison = trendComparison(
                    dates,
                    knownDays,
                    availableDateKeys,
                    records.filter { it.category == category }
                        .groupBy(AnalyticsUsageRecord::dateKey)
                        .mapValues { (_, values) -> values.sumOf(AnalyticsUsageRecord::durationMillis) }
                )
            )
        }.filterValues { trend -> trend.totalDurationMillis > 0L }
        val weeklyComparison = weeklyComparison(dates, records, availableDateKeys)
        val productivityTrend = dates.last7Days.map { day ->
            if (day.dateKey !in availableDateKeys) {
                AnalyticsProductivityPoint(day, null, null, null)
            } else {
                val dayRows = last7Records.filter { it.dateKey == day.dateKey }
                val productivityRows = dayRows.filterNot {
                    it.packageName in ProductivityScorePolicy.EXCLUDED_PACKAGES
                }
                val productivityTotal = productivityRows.sumOf(AnalyticsUsageRecord::durationMillis)
                val result = productivityScoreEngine.calculate(
                    ProductivityScoreInput(
                        totalForegroundDurationMillis = productivityTotal,
                        apps = productivityRows.map { row ->
                            ProductivityAppUsage(
                                packageName = row.packageName,
                                appName = row.appName,
                                durationMillis = row.durationMillis,
                                openCount = row.openCount,
                                category = row.category
                            )
                        }
                    )
                )
                AnalyticsProductivityPoint(
                    day = day,
                    score = result.score,
                    confidence = result.coverage.confidenceLevel,
                    coverage = result.coverage.classifiedCoverage
                )
            }
        }
        return AnalyticsInteractiveData(
            defaultSelectedDateKey = defaultSelected,
            dayDetails = dayDetails,
            appTrends = appTrends,
            categoryTrends = categoryTrends,
            weeklyComparison = weeklyComparison,
            productivityTrend = productivityTrend
        )
    }

    private fun aggregateApps(rows: List<AnalyticsUsageRecord>): List<AnalyticsAppTotal> = rows
        .groupBy(AnalyticsUsageRecord::packageName)
        .map { (packageName, values) ->
            val representative = values.maxByOrNull(AnalyticsUsageRecord::durationMillis)!!
            AnalyticsAppTotal(
                packageName,
                representative.appName,
                values.sumOf(AnalyticsUsageRecord::durationMillis),
                values.sumOf(AnalyticsUsageRecord::openCount),
                representative.category
            )
        }
        .filter { it.durationMillis > 0L }
        .sortedByDescending(AnalyticsAppTotal::durationMillis)

    private fun aggregateCategories(rows: List<AnalyticsUsageRecord>): List<AnalyticsCategoryTotal> = rows
        .groupBy(AnalyticsUsageRecord::category)
        .map { (category, values) ->
            AnalyticsCategoryTotal(category, values.sumOf(AnalyticsUsageRecord::durationMillis))
        }
        .filter { it.durationMillis > 0L }
        .sortedByDescending(AnalyticsCategoryTotal::durationMillis)

    private fun comparisonForDay(
        day: AnalyticsDay,
        selectedDuration: Long,
        knownDays: List<AnalyticsDay>,
        availableDateKeys: Set<String>,
        durationForDate: (String) -> Long
    ): AnalyticsComparison? {
        val previous = knownDays.lastOrNull {
            it.dateKey < day.dateKey && it.dateKey in availableDateKeys
        } ?: return null
        val previousDuration = durationForDate(previous.dateKey)
        return comparison(previous, selectedDuration, previousDuration)
    }

    private fun trendComparison(
        dates: AnalyticsDateContext,
        knownDays: List<AnalyticsDay>,
        availableDateKeys: Set<String>,
        durations: Map<String, Long>
    ): AnalyticsComparison? {
        if (dates.today.dateKey !in availableDateKeys) return null
        val previous = knownDays.lastOrNull {
            it.dateKey < dates.today.dateKey && it.dateKey in availableDateKeys
        } ?: return null
        return comparison(
            previous,
            durations[dates.today.dateKey] ?: 0L,
            durations[previous.dateKey] ?: 0L
        )
    }

    private fun comparison(
        previous: AnalyticsDay,
        currentDuration: Long,
        previousDuration: Long
    ) = AnalyticsComparison(
        baselineDateKey = previous.dateKey,
        differenceMillis = currentDuration - previousDuration,
        percentDifference = if (previousDuration > 0L) {
            (((currentDuration - previousDuration).toDouble() / previousDuration) * 100.0).roundToInt()
        } else {
            null
        },
        baselineLabel = previous.longLabel
    )

    private fun weeklyComparison(
        dates: AnalyticsDateContext,
        records: List<AnalyticsUsageRecord>,
        availableDateKeys: Set<String>
    ): AnalyticsWeeklyComparison? {
        val recent = dates.recentCompleted7Days
        val previous = dates.previousCompleted7Days
        if (
            recent.size != 7 || previous.size != 7 ||
            recent.any { it.dateKey !in availableDateKeys } ||
            previous.any { it.dateKey !in availableDateKeys }
        ) return null
        val recentKeys = recent.mapTo(hashSetOf(), AnalyticsDay::dateKey)
        val previousKeys = previous.mapTo(hashSetOf(), AnalyticsDay::dateKey)
        val recentTotal = records.filter { it.dateKey in recentKeys }
            .sumOf(AnalyticsUsageRecord::durationMillis)
        val previousTotal = records.filter { it.dateKey in previousKeys }
            .sumOf(AnalyticsUsageRecord::durationMillis)
        return AnalyticsWeeklyComparison(
            recentTotalMillis = recentTotal,
            previousTotalMillis = previousTotal,
            differenceMillis = recentTotal - previousTotal,
            percentDifference = if (previousTotal > 0L) {
                (((recentTotal - previousTotal).toDouble() / previousTotal) * 100.0).roundToInt()
            } else {
                null
            }
        )
    }
}
