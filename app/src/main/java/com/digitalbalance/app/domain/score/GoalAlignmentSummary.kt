package com.digitalbalance.app.domain.score

enum class GoalAlignmentSummary {
    WithinAllLimits,
    GoalsProgressing,
    LimitsExceeded,
    ProductiveTargetInProgress
}

fun GoalAlignmentResult.overallSummary(): GoalAlignmentSummary {
    val limitComponents = components.filter { component ->
        component.kind != ScoreComponentKind.ProductiveGoal
    }
    val productiveComponent = components.firstOrNull { component ->
        component.kind == ScoreComponentKind.ProductiveGoal
    }
    return when {
        limitComponents.any { it.actualDurationMillis > it.targetDurationMillis } ->
            GoalAlignmentSummary.LimitsExceeded
        productiveComponent != null &&
            productiveComponent.actualDurationMillis < productiveComponent.targetDurationMillis ->
            GoalAlignmentSummary.ProductiveTargetInProgress
        productiveComponent != null -> GoalAlignmentSummary.GoalsProgressing
        else -> GoalAlignmentSummary.WithinAllLimits
    }
}
