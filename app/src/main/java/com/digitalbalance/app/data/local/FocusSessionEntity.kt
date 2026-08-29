package com.digitalbalance.app.data.local

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "focus_sessions",
    indices = [Index(value = ["statusKey"])]
)
data class FocusSessionEntity(
    @PrimaryKey val id: String,
    val presetKey: String,
    val plannedDurationMillis: Long,
    val accumulatedFocusedMillis: Long,
    val startedAtEpochMillis: Long,
    val segmentStartedAtEpochMillis: Long?,
    val segmentStartedElapsedRealtimeMillis: Long?,
    val segmentBootEpochOffsetMillis: Long?,
    val endedAtEpochMillis: Long?,
    val statusKey: String,
    val reflectionKey: String?,
    val updatedAtEpochMillis: Long
)
