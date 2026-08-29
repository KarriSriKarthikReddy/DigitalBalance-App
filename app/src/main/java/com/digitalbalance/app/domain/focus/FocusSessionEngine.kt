package com.digitalbalance.app.domain.focus

import kotlin.math.abs

class FocusSessionEngine {
    fun start(
        id: String,
        preset: FocusPreset,
        plannedDurationMillis: Long,
        now: FocusTimeSnapshot
    ): FocusSession {
        require(id.isNotBlank())
        require(plannedDurationMillis > 0L)
        return FocusSession(
            id = id,
            preset = preset,
            plannedDurationMillis = plannedDurationMillis,
            accumulatedFocusedMillis = 0L,
            startedAtEpochMillis = now.epochMillis,
            segmentStartedAtEpochMillis = now.epochMillis,
            segmentStartedElapsedRealtimeMillis = now.elapsedRealtimeMillis,
            segmentBootEpochOffsetMillis = now.bootEpochOffsetMillis,
            endedAtEpochMillis = null,
            status = FocusSessionStatus.Running
        )
    }

    fun progress(session: FocusSession, now: FocusTimeSnapshot): FocusProgress {
        val actual = actualFocusedMillis(session, now)
            .coerceIn(0L, session.plannedDurationMillis.coerceAtLeast(0L))
        val remaining = (session.plannedDurationMillis - actual).coerceAtLeast(0L)
        val fraction = if (session.plannedDurationMillis > 0L) {
            (actual.toDouble() / session.plannedDurationMillis).toFloat().coerceIn(0f, 1f)
        } else {
            0f
        }
        return FocusProgress(actual, remaining, fraction)
    }

    fun pause(session: FocusSession, now: FocusTimeSnapshot): FocusSession {
        val reconciled = reconcile(session, now)
        if (reconciled.status != FocusSessionStatus.Running) return reconciled
        return reconciled.copy(
            accumulatedFocusedMillis = progress(reconciled, now).actualFocusedMillis,
            segmentStartedAtEpochMillis = null,
            segmentStartedElapsedRealtimeMillis = null,
            segmentBootEpochOffsetMillis = null,
            status = FocusSessionStatus.Paused
        )
    }

    fun resume(session: FocusSession, now: FocusTimeSnapshot): FocusSession {
        if (session.status != FocusSessionStatus.Paused) return session
        if (session.accumulatedFocusedMillis >= session.plannedDurationMillis) {
            return complete(session, now)
        }
        return session.copy(
            segmentStartedAtEpochMillis = now.epochMillis,
            segmentStartedElapsedRealtimeMillis = now.elapsedRealtimeMillis,
            segmentBootEpochOffsetMillis = now.bootEpochOffsetMillis,
            status = FocusSessionStatus.Running
        )
    }

    fun stopEarly(session: FocusSession, now: FocusTimeSnapshot): FocusSession {
        val reconciled = reconcile(session, now)
        if (reconciled.status.isFinished) return reconciled
        return reconciled.copy(
            accumulatedFocusedMillis = progress(reconciled, now).actualFocusedMillis,
            segmentStartedAtEpochMillis = null,
            segmentStartedElapsedRealtimeMillis = null,
            segmentBootEpochOffsetMillis = null,
            endedAtEpochMillis = now.epochMillis,
            status = FocusSessionStatus.StoppedEarly
        )
    }

    fun reconcile(session: FocusSession, now: FocusTimeSnapshot): FocusSession {
        if (session.status.isFinished) return session
        return if (progress(session, now).remainingMillis == 0L) complete(session, now) else session
    }

    fun withReflection(session: FocusSession, reflection: FocusReflection?): FocusSession =
        if (session.status.isFinished) session.copy(reflection = reflection) else session

    private fun complete(session: FocusSession, now: FocusTimeSnapshot): FocusSession {
        val completionEpoch = if (session.status == FocusSessionStatus.Running) {
            val segmentStart = session.segmentStartedAtEpochMillis ?: now.epochMillis
            segmentStart + (session.plannedDurationMillis - session.accumulatedFocusedMillis).coerceAtLeast(0L)
        } else {
            now.epochMillis
        }
        return session.copy(
            accumulatedFocusedMillis = session.plannedDurationMillis.coerceAtLeast(0L),
            segmentStartedAtEpochMillis = null,
            segmentStartedElapsedRealtimeMillis = null,
            segmentBootEpochOffsetMillis = null,
            endedAtEpochMillis = completionEpoch.coerceAtMost(now.epochMillis),
            status = FocusSessionStatus.Completed
        )
    }

    private fun actualFocusedMillis(session: FocusSession, now: FocusTimeSnapshot): Long {
        val accumulated = session.accumulatedFocusedMillis.coerceAtLeast(0L)
        if (session.status != FocusSessionStatus.Running) return accumulated
        val wallStart = session.segmentStartedAtEpochMillis ?: return accumulated
        val elapsedStart = session.segmentStartedElapsedRealtimeMillis
        val storedOffset = session.segmentBootEpochOffsetMillis
        val sameBoot = elapsedStart != null && storedOffset != null &&
            now.elapsedRealtimeMillis >= elapsedStart &&
            abs(now.bootEpochOffsetMillis - storedOffset) <= BOOT_OFFSET_TOLERANCE_MILLIS
        val segmentElapsed = if (sameBoot) {
            now.elapsedRealtimeMillis - requireNotNull(elapsedStart)
        } else {
            now.epochMillis - wallStart
        }
        return accumulated + segmentElapsed.coerceAtLeast(0L)
    }

    private companion object {
        const val BOOT_OFFSET_TOLERANCE_MILLIS = 60_000L
    }
}
