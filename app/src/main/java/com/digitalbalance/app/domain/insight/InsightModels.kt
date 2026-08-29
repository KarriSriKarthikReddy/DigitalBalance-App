package com.digitalbalance.app.domain.insight

import com.digitalbalance.app.domain.category.AppCategory
import com.digitalbalance.app.domain.goal.GoalProgress
import com.digitalbalance.app.domain.productivity.ProductivityScoreResult

data class InsightAppUsage(
    val packageName: String,
    val appName: String,
    val durationMillis: Long,
    val openCount: Int,
    val category: AppCategory
)

data class InsightInput(
    val totalForegroundDurationMillis: Long,
    val apps: List<InsightAppUsage>,
    val goalProgress: List<GoalProgress>,
    val productivityScoreResult: ProductivityScoreResult?
)

enum class InsightType {
    GoalProgress,
    GoalExceeded,
    GoalSuccess,
    DominantApp,
    DominantCategory,
    ClassificationCoverage,
    FrequentOpens
}

enum class InsightSeverity {
    Attention,
    Progress,
    Positive,
    Informational
}

enum class InsightActionType {
    OpenGoals,
    OpenApps,
    ReviewCategories,
    OpenFocus
}

data class PersonalInsight(
    val id: String,
    val type: InsightType,
    val priority: Int,
    val severity: InsightSeverity,
    val title: String,
    val description: String,
    val supportingMetric: String? = null,
    val relatedPackageName: String? = null,
    val relatedCategory: AppCategory? = null,
    val relatedGoalId: String? = null,
    val recommendation: String? = null,
    val actionType: InsightActionType? = null,
    val deduplicationKey: String = id
)
