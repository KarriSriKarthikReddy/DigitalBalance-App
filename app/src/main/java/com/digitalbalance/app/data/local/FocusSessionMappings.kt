package com.digitalbalance.app.data.local

import com.digitalbalance.app.domain.focus.FocusPreset
import com.digitalbalance.app.domain.focus.FocusReflection
import com.digitalbalance.app.domain.focus.FocusSession
import com.digitalbalance.app.domain.focus.FocusSessionStatus

fun FocusSession.toEntity(updatedAtEpochMillis: Long) = FocusSessionEntity(
    id = id,
    presetKey = preset.storageKey,
    plannedDurationMillis = plannedDurationMillis,
    accumulatedFocusedMillis = accumulatedFocusedMillis,
    startedAtEpochMillis = startedAtEpochMillis,
    segmentStartedAtEpochMillis = segmentStartedAtEpochMillis,
    segmentStartedElapsedRealtimeMillis = segmentStartedElapsedRealtimeMillis,
    segmentBootEpochOffsetMillis = segmentBootEpochOffsetMillis,
    endedAtEpochMillis = endedAtEpochMillis,
    statusKey = status.storageKey,
    reflectionKey = reflection?.storageKey,
    updatedAtEpochMillis = updatedAtEpochMillis
)

fun FocusSessionEntity.toDomain(): FocusSession? {
    val preset = FocusPreset.fromStorageKey(presetKey) ?: return null
    val status = FocusSessionStatus.fromStorageKey(statusKey) ?: return null
    if (plannedDurationMillis <= 0L || accumulatedFocusedMillis < 0L) return null
    return FocusSession(
        id = id,
        preset = preset,
        plannedDurationMillis = plannedDurationMillis,
        accumulatedFocusedMillis = accumulatedFocusedMillis,
        startedAtEpochMillis = startedAtEpochMillis,
        segmentStartedAtEpochMillis = segmentStartedAtEpochMillis,
        segmentStartedElapsedRealtimeMillis = segmentStartedElapsedRealtimeMillis,
        segmentBootEpochOffsetMillis = segmentBootEpochOffsetMillis,
        endedAtEpochMillis = endedAtEpochMillis,
        status = status,
        reflection = reflectionKey?.let(FocusReflection::fromStorageKey)
    )
}
