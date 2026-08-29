package com.digitalbalance.app.domain.productivity

import com.digitalbalance.app.domain.category.AppCategory

data class ProductivityAppUsage(
    val packageName: String,
    val appName: String,
    val durationMillis: Long,
    val openCount: Int,
    val category: AppCategory
)

data class ProductivityScoreInput(
    val totalForegroundDurationMillis: Long,
    val apps: List<ProductivityAppUsage>
)

enum class ProductivityScoreStatus {
    Ready,
    NotEnoughData
}

enum class ProductivityConfidence {
    Low,
    Medium,
    High
}

enum class ProductivityComponentKind {
    CategoryBalance,
    UsageIntensity,
    UsagePattern
}

data class ProductivityComponent(
    val kind: ProductivityComponentKind,
    val name: String,
    val score: Int,
    val activeWeight: Double,
    val explanation: String
)

data class ProductivityCoverage(
    val classifiedCoverage: Double,
    val confidence: Double,
    val confidenceLevel: ProductivityConfidence,
    val classifiedDurationMillis: Long,
    val mixedDurationMillis: Long,
    val otherOrUnknownDurationMillis: Long,
    val totalForegroundDurationMillis: Long
)

data class ProductivityScoreResult(
    val status: ProductivityScoreStatus,
    val score: Int?,
    val components: List<ProductivityComponent>,
    val reasons: List<String>,
    val coverage: ProductivityCoverage,
    val totalActiveWeight: Double
)
