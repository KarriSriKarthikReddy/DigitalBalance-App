package com.digitalbalance.app.domain.focus

enum class FocusPreset(val storageKey: String) {
    Focus("focus"),
    Study("study"),
    Work("work"),
    Mindfulness("mindfulness");

    companion object {
        fun fromStorageKey(value: String): FocusPreset? = entries.firstOrNull { it.storageKey == value }
    }
}

enum class FocusSessionStatus(val storageKey: String) {
    Running("running"),
    Paused("paused"),
    Completed("completed"),
    StoppedEarly("stopped_early");

    val isFinished: Boolean get() = this == Completed || this == StoppedEarly

    companion object {
        fun fromStorageKey(value: String): FocusSessionStatus? = entries.firstOrNull { it.storageKey == value }
    }
}

enum class FocusReflection(val storageKey: String) {
    Focused("focused"),
    Okay("okay"),
    Distracted("distracted");

    companion object {
        fun fromStorageKey(value: String): FocusReflection? = entries.firstOrNull { it.storageKey == value }
    }
}

data class FocusTimeSnapshot(
    val epochMillis: Long,
    val elapsedRealtimeMillis: Long
) {
    val bootEpochOffsetMillis: Long get() = epochMillis - elapsedRealtimeMillis
}

data class FocusSession(
    val id: String,
    val preset: FocusPreset,
    val plannedDurationMillis: Long,
    val accumulatedFocusedMillis: Long,
    val startedAtEpochMillis: Long,
    val segmentStartedAtEpochMillis: Long?,
    val segmentStartedElapsedRealtimeMillis: Long?,
    val segmentBootEpochOffsetMillis: Long?,
    val endedAtEpochMillis: Long?,
    val status: FocusSessionStatus,
    val reflection: FocusReflection? = null
)

data class FocusProgress(
    val actualFocusedMillis: Long,
    val remainingMillis: Long,
    val progressFraction: Float
)

object FocusDurationPolicy {
    const val MIN_CUSTOM_MINUTES = 1
    const val MAX_CUSTOM_MINUTES = 480
    val suggestedMinutes = listOf(15, 25, 45, 60)

    fun isValidCustomMinutes(minutes: Int?): Boolean =
        minutes != null && minutes in MIN_CUSTOM_MINUTES..MAX_CUSTOM_MINUTES
}
