package com.digitalbalance.app.domain.analytics

import com.digitalbalance.app.domain.category.AppCategory
import com.digitalbalance.app.domain.productivity.ProductivityConfidence

enum class AnalyticsPeriod {
    Today,
    Yesterday,
    Last7Days
}

data class AnalyticsDay(
    val dateKey: String,
    val shortLabel: String,
    val isToday: Boolean,
    val longLabel: String = shortLabel
)

data class AnalyticsDateContext(
    val today: AnalyticsDay,
    val yesterday: AnalyticsDay,
    val last7Days: List<AnalyticsDay>,
    val recentCompleted7Days: List<AnalyticsDay> = emptyList(),
    val previousCompleted7Days: List<AnalyticsDay> = emptyList()
) {
    fun daysFor(period: AnalyticsPeriod): List<AnalyticsDay> = when (period) {
        AnalyticsPeriod.Today -> listOf(today)
        AnalyticsPeriod.Yesterday -> listOf(yesterday)
        AnalyticsPeriod.Last7Days -> last7Days
    }
}

data class AnalyticsUsageRecord(
    val dateKey: String,
    val packageName: String,
    val appName: String,
    val durationMillis: Long,
    val openCount: Int,
    val category: AppCategory
)

data class AnalyticsAppTotal(
    val packageName: String,
    val appName: String,
    val durationMillis: Long,
    val openCount: Int,
    val category: AppCategory
)

data class AnalyticsCategoryTotal(
    val category: AppCategory,
    val durationMillis: Long
)

data class AnalyticsDayTotal(
    val day: AnalyticsDay,
    val durationMillis: Long?
)

data class AnalyticsComparison(
    val baselineDateKey: String,
    val differenceMillis: Long,
    val percentDifference: Int?,
    val baselineLabel: String? = null
)

data class AnalyticsFocusRecord(
    val endedDateKey: String,
    val focusedDurationMillis: Long,
    val completed: Boolean
)

data class AnalyticsFocusSummary(
    val completedSessions: Int,
    val focusedDurationMillis: Long
)

data class AnalyticsDayDetail(
    val day: AnalyticsDay,
    val totalDurationMillis: Long,
    val comparison: AnalyticsComparison?,
    val topApp: AnalyticsAppTotal?,
    val topCategory: AnalyticsCategoryTotal?,
    val activeAppCount: Int,
    val topApps: List<AnalyticsAppTotal>,
    val productiveDurationMillis: Long,
    val classifiedCoverage: Double
)

data class AnalyticsTrendPoint(
    val day: AnalyticsDay,
    val durationMillis: Long?
)

data class AnalyticsAppTrend(
    val packageName: String,
    val appName: String,
    val category: AppCategory,
    val points: List<AnalyticsTrendPoint>,
    val totalDurationMillis: Long,
    val dailyAverageMillis: Long?,
    val availableDayCount: Int,
    val comparison: AnalyticsComparison?
)

data class AnalyticsCategoryTrend(
    val category: AppCategory,
    val points: List<AnalyticsTrendPoint>,
    val totalDurationMillis: Long,
    val dailyAverageMillis: Long?,
    val availableDayCount: Int,
    val comparison: AnalyticsComparison?
)

data class AnalyticsWeeklyComparison(
    val recentTotalMillis: Long,
    val previousTotalMillis: Long,
    val differenceMillis: Long,
    val percentDifference: Int?
)

data class AnalyticsProductivityPoint(
    val day: AnalyticsDay,
    val score: Int?,
    val confidence: ProductivityConfidence?,
    val coverage: Double?
)

data class AnalyticsInteractiveData(
    val defaultSelectedDateKey: String?,
    val dayDetails: Map<String, AnalyticsDayDetail>,
    val appTrends: Map<String, AnalyticsAppTrend>,
    val categoryTrends: Map<AppCategory, AnalyticsCategoryTrend>,
    val weeklyComparison: AnalyticsWeeklyComparison?,
    val productivityTrend: List<AnalyticsProductivityPoint>
)

data class AnalyticsSnapshot(
    val period: AnalyticsPeriod,
    val dayTotals: List<AnalyticsDayTotal>,
    val availableDayCount: Int,
    val expectedDayCount: Int,
    val totalDurationMillis: Long,
    val dailyAverageMillis: Long?,
    val rankedApps: List<AnalyticsAppTotal>,
    val categoryTotals: List<AnalyticsCategoryTotal>,
    val activeAppCount: Int,
    val comparison: AnalyticsComparison?,
    val focusSummary: AnalyticsFocusSummary?,
    val interactive: AnalyticsInteractiveData
) {
    val hasAvailableData: Boolean get() = availableDayCount > 0
    val mostUsedApp: AnalyticsAppTotal? get() = rankedApps.firstOrNull()
    val mostUsedCategory: AnalyticsCategoryTotal? get() = categoryTotals.firstOrNull()
}
