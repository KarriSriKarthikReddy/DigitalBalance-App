package com.digitalbalance.app.domain.score

enum class ProductivityScoreSummary {
    WithinAllLimits,
    GoalsProgressing,
    LimitsExceeded,
    ProductiveTargetInProgress
}

fun ProductivityScoreResult.overallSummary(): ProductivityScoreSummary {
    val limitComponents = components.filter { component ->
        component.kind != ScoreComponentKind.ProductiveGoal
    }
    val productiveComponent = components.firstOrNull { component ->
        component.kind == ScoreComponentKind.ProductiveGoal
    }
    return when {
        limitComponents.any { it.actualDurationMillis > it.targetDurationMillis } ->
            ProductivityScoreSummary.LimitsExceeded
        productiveComponent != null &&
            productiveComponent.actualDurationMillis < productiveComponent.targetDurationMillis ->
            ProductivityScoreSummary.ProductiveTargetInProgress
        productiveComponent != null -> ProductivityScoreSummary.GoalsProgressing
        else -> ProductivityScoreSummary.WithinAllLimits
    }
}
