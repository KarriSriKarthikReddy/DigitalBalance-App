package com.digitalbalance.app.domain.score

import org.junit.Assert.assertEquals
import org.junit.Test

class ProductivityScoreSummaryTest {
    @Test
    fun exceededLimitTakesPriority() {
        assertEquals(
            ProductivityScoreSummary.LimitsExceeded,
            result(productiveActual = 30, productiveTarget = 60, limitActual = 61).overallSummary()
        )
    }

    @Test
    fun incompleteProductiveTargetIsReportedWhenLimitsAreWithinRange() {
        assertEquals(
            ProductivityScoreSummary.ProductiveTargetInProgress,
            result(productiveActual = 30, productiveTarget = 60, limitActual = 30).overallSummary()
        )
    }

    @Test
    fun completedProductiveTargetReportsOverallProgress() {
        assertEquals(
            ProductivityScoreSummary.GoalsProgressing,
            result(productiveActual = 60, productiveTarget = 60, limitActual = 30).overallSummary()
        )
    }

    @Test
    fun limitOnlyResultReportsAllLimitsWithinRange() {
        assertEquals(
            ProductivityScoreSummary.WithinAllLimits,
            result(productiveActual = null, productiveTarget = null, limitActual = 30).overallSummary()
        )
    }

    private fun result(
        productiveActual: Long?,
        productiveTarget: Long?,
        limitActual: Long
    ): ProductivityScoreResult {
        val components = buildList {
            if (productiveActual != null && productiveTarget != null) {
                add(component(ScoreComponentKind.ProductiveGoal, productiveActual, productiveTarget))
            }
            add(component(ScoreComponentKind.OverallLimit, limitActual, 60))
        }
        return ProductivityScoreResult(
            status = ProductivityScoreStatus.Ready,
            score = 100,
            components = components,
            reasons = emptyList(),
            coverage = ScoreCoverage(1.0, 1.0, ScoreConfidence.High, 0, 0, 0, 0),
            totalActiveWeight = components.sumOf(ScoreComponent::activeWeight)
        )
    }

    private fun component(kind: ScoreComponentKind, actual: Long, target: Long) = ScoreComponent(
        id = kind.name,
        kind = kind,
        name = kind.name,
        score = 100,
        activeWeight = 1.0,
        actualDurationMillis = actual,
        targetDurationMillis = target,
        explanation = "component detail",
        requiresClassification = false
    )
}
