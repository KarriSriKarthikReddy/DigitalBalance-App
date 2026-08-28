package com.digitalbalance.app.data.usage

import org.junit.Assert.assertEquals
import org.junit.Test

class UsageSessionReconstructorTest {
    private val reconstructor = UsageSessionReconstructor()

    @Test
    fun normalForegroundBackgroundPairCreatesOneSession() {
        val result = reconstruct(
            resumed("app.a", 100),
            paused("app.a", 600),
            end = 1_000
        )

        assertEquals(1, result.sessions.size)
        assertSession(result.sessions.single(), "app.a", 100, 600, SessionEndReason.Paused)
    }

    @Test
    fun currentlyForegroundAppIsClosedAtNow() {
        val result = reconstruct(resumed("app.a", 250), end = 1_000)

        assertSession(result.sessions.single(), "app.a", 250, 1_000, SessionEndReason.EndOfRange)
    }

    @Test
    fun repeatedResumeIsIgnoredAndSeparatePairsRemainSeparateSessions() {
        val result = reconstruct(
            resumed("app.a", 100),
            resumed("app.a", 150),
            paused("app.a", 300),
            paused("app.a", 350),
            resumed("app.a", 500),
            paused("app.a", 700),
            end = 1_000
        )

        assertEquals(2, result.sessions.size)
        assertEquals(400L, result.sessions.sumOf(ForegroundSession::durationMillis))
        assertEquals(1, result.unmatchedPauseCount)
    }

    @Test
    fun missingStartIsIgnoredAndUnrelatedPauseDoesNotCloseActiveApp() {
        val result = reconstruct(
            paused("app.a", 100),
            resumed("app.b", 200),
            paused("app.a", 400),
            end = 800
        )

        assertEquals(2, result.unmatchedPauseCount)
        assertSession(result.sessions.single(), "app.b", 200, 800, SessionEndReason.EndOfRange)
    }

    @Test
    fun overlappingResumesAreSerializedToAvoidDoubleCounting() {
        val result = reconstruct(
            resumed("app.a", 100),
            resumed("app.b", 400),
            paused("app.b", 700),
            paused("app.a", 900),
            end = 1_000
        )

        assertEquals(2, result.sessions.size)
        assertSession(result.sessions[0], "app.a", 100, 400, SessionEndReason.AppTransition)
        assertSession(result.sessions[1], "app.b", 400, 700, SessionEndReason.Paused)
        assertEquals(600L, result.sessions.sumOf(ForegroundSession::durationMillis))
    }

    @Test
    fun transitionClosesPreviousAppEvenWhenItsPauseArrivesLate() {
        val result = reconstruct(
            resumed("app.a", 50),
            resumed("app.b", 250),
            paused("app.a", 260),
            paused("app.b", 650),
            end = 1_000
        )

        assertSession(result.sessions[0], "app.a", 50, 250, SessionEndReason.AppTransition)
        assertSession(result.sessions[1], "app.b", 250, 650, SessionEndReason.Paused)
        assertEquals(1, result.unmatchedPauseCount)
    }

    @Test
    fun screenInactiveClosesCurrentSession() {
        val result = reconstruct(
            resumed("app.a", 100),
            UsageEventRecord(null, 450, UsageEventKind.StopAll),
            end = 1_000
        )

        assertSession(
            result.sessions.single(),
            "app.a",
            100,
            450,
            SessionEndReason.ScreenInactive
        )
    }

    @Test
    fun pauseFromOldActivityDoesNotEndAnotherActivityInSameApp() {
        val result = reconstruct(
            resumed("app.a", 100, "First#1"),
            resumed("app.a", 200, "Second#2"),
            paused("app.a", 250, "First#1"),
            paused("app.a", 600, "Second#2"),
            end = 1_000
        )

        assertSession(result.sessions.single(), "app.a", 100, 600, SessionEndReason.Paused)
    }

    @Test
    fun outOfRangeAndZeroLengthEventsNeverCreateInvalidDurations() {
        val result = reconstructor.reconstruct(
            events = listOf(
                resumed("app.a", -10),
                resumed("app.b", 500),
                paused("app.b", 500),
                resumed("app.c", 1_100)
            ),
            rangeStartMillis = 0,
            rangeEndMillis = 1_000
        )

        assertEquals(emptyList<ForegroundSession>(), result.sessions)
        assertEquals(3, result.ignoredEventCount)
    }

    private fun reconstruct(
        vararg events: UsageEventRecord,
        end: Long
    ): SessionReconstruction = reconstructor.reconstruct(
        events = events.toList(),
        rangeStartMillis = 0,
        rangeEndMillis = end
    )

    private fun resumed(
        packageName: String,
        timestamp: Long,
        activityId: String? = null
    ) = UsageEventRecord(
        packageName = packageName,
        timestampMillis = timestamp,
        kind = UsageEventKind.Resumed,
        activityId = activityId
    )

    private fun paused(
        packageName: String,
        timestamp: Long,
        activityId: String? = null
    ) = UsageEventRecord(
        packageName = packageName,
        timestampMillis = timestamp,
        kind = UsageEventKind.Paused,
        activityId = activityId
    )

    private fun assertSession(
        session: ForegroundSession,
        packageName: String,
        start: Long,
        end: Long,
        reason: SessionEndReason
    ) {
        assertEquals(packageName, session.packageName)
        assertEquals(start, session.startMillis)
        assertEquals(end, session.endMillis)
        assertEquals(reason, session.endReason)
        assertEquals(end - start, session.durationMillis)
    }
}
