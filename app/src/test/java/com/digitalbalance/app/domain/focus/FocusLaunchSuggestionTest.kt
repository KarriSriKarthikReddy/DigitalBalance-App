package com.digitalbalance.app.domain.focus

import com.digitalbalance.app.domain.insight.InsightActionType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Test

class FocusLaunchSuggestionTest {
    @Test
    fun focusInsightOpensStandardSetupWithoutAutoStart() {
        val suggestion = FocusLaunchSuggestion.forInsightAction(InsightActionType.OpenFocus)!!
        assertEquals(FocusPreset.Focus, suggestion.preset)
        assertEquals(25, suggestion.durationMinutes)
        assertFalse(suggestion.autoStart)
    }

    @Test
    fun unrelatedInsightActionsDoNotCreateFocusSuggestion() {
        assertNull(FocusLaunchSuggestion.forInsightAction(InsightActionType.OpenGoals))
    }
}
