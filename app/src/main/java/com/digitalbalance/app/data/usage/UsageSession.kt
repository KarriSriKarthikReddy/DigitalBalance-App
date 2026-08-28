package com.digitalbalance.app.data.usage

enum class UsageEventKind {
    Resumed,
    Paused,
    StopAll
}

data class UsageEventRecord(
    val packageName: String?,
    val timestampMillis: Long,
    val kind: UsageEventKind,
    val activityId: String? = null
)

enum class SessionEndReason {
    Paused,
    AppTransition,
    ScreenInactive,
    EndOfRange
}

data class ForegroundSession(
    val packageName: String,
    val startMillis: Long,
    val endMillis: Long,
    val endReason: SessionEndReason
) {
    val durationMillis: Long
        get() = (endMillis - startMillis).coerceAtLeast(0L)
}

data class SessionReconstruction(
    val sessions: List<ForegroundSession>,
    val unmatchedPauseCount: Int,
    val ignoredEventCount: Int
)
