package com.digitalbalance.app.domain.focus

import com.digitalbalance.app.domain.insight.InsightActionType

data class FocusLaunchSuggestion(
    val preset: FocusPreset,
    val durationMinutes: Int,
    val autoStart: Boolean = false
) {
    companion object {
        fun forInsightAction(action: InsightActionType): FocusLaunchSuggestion? =
            if (action == InsightActionType.OpenFocus) {
                FocusLaunchSuggestion(FocusPreset.Focus, 25, autoStart = false)
            } else {
                null
            }
    }
}
