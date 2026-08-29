package com.digitalbalance.app.data.local

import com.digitalbalance.app.domain.focus.FocusPreset
import com.digitalbalance.app.domain.focus.FocusReflection
import com.digitalbalance.app.domain.focus.FocusSession
import com.digitalbalance.app.domain.focus.FocusSessionStatus
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class FocusSessionMappingsTest {
    @Test
    fun entityRoundTripPreservesPersistedSessionState() {
        val session = FocusSession(
            id = "focus-1",
            preset = FocusPreset.Mindfulness,
            plannedDurationMillis = 900_000L,
            accumulatedFocusedMillis = 600_000L,
            startedAtEpochMillis = 1_000L,
            segmentStartedAtEpochMillis = null,
            segmentStartedElapsedRealtimeMillis = null,
            segmentBootEpochOffsetMillis = null,
            endedAtEpochMillis = 601_000L,
            status = FocusSessionStatus.StoppedEarly,
            reflection = FocusReflection.Focused
        )
        assertEquals(session, session.toEntity(updatedAtEpochMillis = 700_000L).toDomain())
    }

    @Test
    fun invalidStoredValuesAreIgnored() {
        val invalid = FocusSessionEntity(
            id = "bad",
            presetKey = "unknown",
            plannedDurationMillis = 0L,
            accumulatedFocusedMillis = -1L,
            startedAtEpochMillis = 0L,
            segmentStartedAtEpochMillis = null,
            segmentStartedElapsedRealtimeMillis = null,
            segmentBootEpochOffsetMillis = null,
            endedAtEpochMillis = null,
            statusKey = "unknown",
            reflectionKey = null,
            updatedAtEpochMillis = 0L
        )
        assertNull(invalid.toDomain())
    }
}
