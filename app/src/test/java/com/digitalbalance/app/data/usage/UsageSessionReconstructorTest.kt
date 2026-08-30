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
            UsageEventRecord(null, 450, UsageEventKind.ScreenNonInteractive),
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
    fun foregroundOemSurfaceIsCountedEvenWhileKeyguardStateIsShown() {
        val result = reconstruct(
            UsageEventRecord(null, 100, UsageEventKind.ScreenInteractive),
            UsageEventRecord(null, 110, UsageEventKind.KeyguardShown),
            resumed("oem.lock.surface", 120),
            paused("oem.lock.surface", 600),
            UsageEventRecord(null, 610, UsageEventKind.KeyguardHidden),
            end = 1_000
        )

        assertSession(
            result.sessions.single(),
            "oem.lock.surface",
            120,
            600,
            SessionEndReason.Paused
        )
    }

    @Test
    fun keyguardDoesNotPrematurelyCloseSessionAndOemResumeCreatesTransition() {
        val result = reconstruct(
            resumed("app.user", 100),
            UsageEventRecord(null, 400, UsageEventKind.KeyguardShown),
            resumed("oem.lock.surface", 410),
            paused("oem.lock.surface", 700),
            UsageEventRecord(null, 800, UsageEventKind.KeyguardHidden),
            end = 1_000
        )

        assertSession(
            result.sessions[0],
            "app.user",
            100,
            410,
            SessionEndReason.AppTransition
        )
        assertSession(result.sessions[1], "oem.lock.surface", 410, 700, SessionEndReason.Paused)
    }

    @Test
    fun normalForegroundSessionAfterUnlockIsStillCounted() {
        val result = reconstruct(
            UsageEventRecord(null, 100, UsageEventKind.ScreenInteractive),
            UsageEventRecord(null, 110, UsageEventKind.KeyguardShown),
            resumed("oem.lock.surface", 120),
            UsageEventRecord(null, 600, UsageEventKind.KeyguardHidden),
            resumed("app.user", 610),
            paused("app.user", 900),
            end = 1_000
        )

        assertSession(
            result.sessions[0],
            "oem.lock.surface",
            120,
            610,
            SessionEndReason.AppTransition
        )
        assertSession(result.sessions[1], "app.user", 610, 900, SessionEndReason.Paused)
    }

    @Test
    fun resumeAfterScreenOffIsAcceptedWithoutWaitingForInteractiveState() {
        val result = reconstruct(
            UsageEventRecord(null, 100, UsageEventKind.ScreenNonInteractive),
            resumed("background.surface", 120),
            UsageEventRecord(null, 500, UsageEventKind.ScreenInteractive),
            resumed("app.user", 510),
            paused("app.user", 800),
            end = 1_000
        )

        assertSession(
            result.sessions[0],
            "background.surface",
            120,
            510,
            SessionEndReason.AppTransition
        )
        assertSession(result.sessions[1], "app.user", 510, 800, SessionEndReason.Paused)
    }

    @Test
    fun stateEventsAtSameTimestampDoNotSuppressResume() {
        val result = reconstruct(
            resumed("oem.lock.surface", 100),
            UsageEventRecord(null, 100, UsageEventKind.ScreenInteractive),
            UsageEventRecord(null, 100, UsageEventKind.KeyguardShown),
            end = 1_000
        )

        assertSession(
            result.sessions.single(),
            "oem.lock.surface",
            100,
            1_000,
            SessionEndReason.EndOfRange
        )
    }

    @Test
    fun staleKeyguardStateDoesNotDiscardLaterInstagramUsage() {
        val result = reconstruct(
            UsageEventRecord(null, 100, UsageEventKind.ScreenNonInteractive),
            UsageEventRecord(null, 110, UsageEventKind.KeyguardShown),
            UsageEventRecord(null, 400, UsageEventKind.ScreenInteractive),
            resumed("com.instagram.android", 420),
            paused("com.instagram.android", 920),
            end = 1_000
        )

        assertSession(
            result.sessions.single(),
            "com.instagram.android",
            420,
            920,
            SessionEndReason.Paused
        )
    }

    @Test
    fun screenOffClosesThenLaterResumeOfSameAppStartsANewSession() {
        val result = reconstruct(
            resumed("app.a", 100),
            UsageEventRecord(null, 400, UsageEventKind.ScreenNonInteractive),
            resumed("app.a", 600),
            paused("app.a", 900),
            end = 1_000
        )

        assertSession(result.sessions[0], "app.a", 100, 400, SessionEndReason.ScreenInactive)
        assertSession(result.sessions[1], "app.a", 600, 900, SessionEndReason.Paused)
        assertEquals(600L, result.sessions.sumOf(ForegroundSession::durationMillis))
    }

    @Test
    fun unlockIntoAnotherAppSerializesSessionsWithoutOverlap() {
        val result = reconstruct(
            resumed("oem.lock.surface", 100),
            UsageEventRecord(null, 300, UsageEventKind.KeyguardHidden),
            resumed("app.user", 320),
            paused("oem.lock.surface", 330),
            paused("app.user", 700),
            end = 1_000
        )

        assertSession(
            result.sessions[0],
            "oem.lock.surface",
            100,
            320,
            SessionEndReason.AppTransition
        )
        assertSession(result.sessions[1], "app.user", 320, 700, SessionEndReason.Paused)
        assertEquals(600L, result.sessions.sumOf(ForegroundSession::durationMillis))
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

    @Test
    fun digitalBalanceForegroundPairReconstructsLikeAnyOtherAppWithoutOverlap() {
        val result = reconstruct(
            resumed("com.instagram.android", 100),
            resumed("com.digitalbalance.app", 400),
            paused("com.digitalbalance.app", 700),
            end = 1_000
        )

        assertSession(
            result.sessions[0],
            "com.instagram.android",
            100,
            400,
            SessionEndReason.AppTransition
        )
        assertSession(
            result.sessions[1],
            "com.digitalbalance.app",
            400,
            700,
            SessionEndReason.Paused
        )
        assertEquals(600L, result.sessions.sumOf(ForegroundSession::durationMillis))
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
