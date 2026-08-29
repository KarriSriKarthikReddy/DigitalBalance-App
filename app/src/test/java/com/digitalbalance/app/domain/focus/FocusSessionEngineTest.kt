package com.digitalbalance.app.domain.focus

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class FocusSessionEngineTest {
    private val engine = FocusSessionEngine()
    private val start = FocusTimeSnapshot(epochMillis = 1_000_000L, elapsedRealtimeMillis = 100_000L)

    @Test
    fun startCreatesRunningSession() {
        val session = newSession()
        assertEquals(FocusSessionStatus.Running, session.status)
        assertEquals(FocusPreset.Study, session.preset)
        assertEquals(0L, session.accumulatedFocusedMillis)
        assertNotNull(session.segmentStartedElapsedRealtimeMillis)
    }

    @Test
    fun elapsedTimeUsesMonotonicClockAndCannotBecomeNegative() {
        val session = newSession()
        assertEquals(10_000L, engine.progress(session, after(seconds = 10)).actualFocusedMillis)
        assertEquals(0L, engine.progress(session, start.copy(elapsedRealtimeMillis = 90_000L)).actualFocusedMillis)
    }

    @Test
    fun pauseFreezesFocusedTime() {
        val paused = engine.pause(newSession(), after(seconds = 20))
        assertEquals(FocusSessionStatus.Paused, paused.status)
        assertEquals(20_000L, paused.accumulatedFocusedMillis)
        assertEquals(20_000L, engine.progress(paused, after(seconds = 80)).actualFocusedMillis)
    }

    @Test
    fun resumeStartsNewSegmentAndExcludesPausedTime() {
        val paused = engine.pause(newSession(), after(seconds = 20))
        val resumedAt = after(seconds = 70)
        val resumed = engine.resume(paused, resumedAt)
        val progress = engine.progress(resumed, after(seconds = 80))
        assertEquals(FocusSessionStatus.Running, resumed.status)
        assertEquals(30_000L, progress.actualFocusedMillis)
    }

    @Test
    fun reconcileCompletesAtPlannedDuration() {
        val completed = engine.reconcile(newSession(durationMillis = 60_000L), after(seconds = 75))
        assertEquals(FocusSessionStatus.Completed, completed.status)
        assertEquals(60_000L, completed.accumulatedFocusedMillis)
        assertEquals(0L, engine.progress(completed, after(seconds = 90)).remainingMillis)
    }

    @Test
    fun earlyStopRecordsActualTime() {
        val stopped = engine.stopEarly(newSession(), after(seconds = 25))
        assertEquals(FocusSessionStatus.StoppedEarly, stopped.status)
        assertEquals(25_000L, stopped.accumulatedFocusedMillis)
        assertEquals(after(seconds = 25).epochMillis, stopped.endedAtEpochMillis)
    }

    @Test
    fun restorationFallsBackToWallClockAfterBootChange() {
        val session = newSession()
        val restoredNow = FocusTimeSnapshot(
            epochMillis = start.epochMillis + 40_000L,
            elapsedRealtimeMillis = 5_000L
        )
        assertEquals(40_000L, engine.progress(session, restoredNow).actualFocusedMillis)
    }

    @Test
    fun timerNeverReturnsNegativeRemaining() {
        val progress = engine.progress(newSession(durationMillis = 30_000L), after(seconds = 90))
        assertEquals(0L, progress.remainingMillis)
        assertEquals(1f, progress.progressFraction)
    }

    @Test
    fun completedSessionCannotResume() {
        val completed = engine.reconcile(newSession(durationMillis = 10_000L), after(seconds = 10))
        assertEquals(completed, engine.resume(completed, after(seconds = 20)))
    }

    @Test
    fun reflectionIsOptionalAndOnlyAppliedToFinishedSession() {
        val running = newSession()
        assertNull(engine.withReflection(running, FocusReflection.Focused).reflection)
        val stopped = engine.stopEarly(running, after(seconds = 10))
        assertEquals(
            FocusReflection.Okay,
            engine.withReflection(stopped, FocusReflection.Okay).reflection
        )
    }

    @Test
    fun presetStorageMappingIsStable() {
        FocusPreset.entries.forEach { preset ->
            assertEquals(preset, FocusPreset.fromStorageKey(preset.storageKey))
        }
        assertNull(FocusPreset.fromStorageKey("unknown"))
    }

    @Test
    fun customDurationValidationUsesPolicyBounds() {
        assertTrue(FocusDurationPolicy.isValidCustomMinutes(1))
        assertTrue(FocusDurationPolicy.isValidCustomMinutes(480))
        assertFalse(FocusDurationPolicy.isValidCustomMinutes(0))
        assertFalse(FocusDurationPolicy.isValidCustomMinutes(481))
        assertFalse(FocusDurationPolicy.isValidCustomMinutes(null))
    }

    private fun newSession(durationMillis: Long = 120_000L) = engine.start(
        id = "session-1",
        preset = FocusPreset.Study,
        plannedDurationMillis = durationMillis,
        now = start
    )

    private fun after(seconds: Long) = FocusTimeSnapshot(
        epochMillis = start.epochMillis + seconds * 1_000L,
        elapsedRealtimeMillis = start.elapsedRealtimeMillis + seconds * 1_000L
    )
}
