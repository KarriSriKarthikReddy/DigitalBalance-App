package com.digitalbalance.app.domain.score

import com.digitalbalance.app.domain.category.AppCategory
import com.digitalbalance.app.domain.goal.DigitalGoal

data class ScoredAppUsage(
    val packageName: String,
    val appName: String,
    val durationMillis: Long,
    val category: AppCategory
)

data class GoalAlignmentInput(
    val totalForegroundDurationMillis: Long,
    val apps: List<ScoredAppUsage>,
    val goals: List<DigitalGoal>
)

enum class GoalAlignmentStatus {
    Ready,
    NotEnoughData
}

enum class ScoreConfidence {
    Low,
    Medium,
    High
}

enum class ScoreComponentKind {
    ProductiveGoal,
    OverallLimit,
    SocialLimit,
    EntertainmentLimit,
    PerAppLimit
}

data class ScoreComponent(
    val id: String,
    val kind: ScoreComponentKind,
    val name: String,
    val score: Int,
    val activeWeight: Double,
    val actualDurationMillis: Long,
    val targetDurationMillis: Long,
    val explanation: String,
    val requiresClassification: Boolean
)

data class ScoreCoverage(
    val classificationCoverage: Double,
    val confidence: Double,
    val confidenceLevel: ScoreConfidence,
    val classifiedDurationMillis: Long,
    val mixedDurationMillis: Long,
    val otherOrUnknownDurationMillis: Long,
    val totalForegroundDurationMillis: Long
)

data class GoalAlignmentResult(
    val status: GoalAlignmentStatus,
    val score: Int?,
    val components: List<ScoreComponent>,
    val reasons: List<String>,
    val coverage: ScoreCoverage,
    val totalActiveWeight: Double
)
