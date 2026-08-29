package com.digitalbalance.app.data.repository

import com.digitalbalance.app.data.local.FocusSessionDao
import com.digitalbalance.app.data.local.toDomain
import com.digitalbalance.app.data.local.toEntity
import com.digitalbalance.app.domain.focus.FocusPreset
import com.digitalbalance.app.domain.focus.FocusReflection
import com.digitalbalance.app.domain.focus.FocusSession
import com.digitalbalance.app.domain.focus.FocusSessionEngine
import com.digitalbalance.app.domain.focus.FocusTimeSnapshot
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

class FocusRepository(
    private val dao: FocusSessionDao,
    private val engine: FocusSessionEngine = FocusSessionEngine()
) {
    private val mutationMutex = Mutex()

    fun observeSessions(): Flow<List<FocusSession>> = dao.observeSessions().map { rows ->
        rows.mapNotNull { it.toDomain() }
    }

    suspend fun start(
        id: String,
        preset: FocusPreset,
        durationMillis: Long,
        now: FocusTimeSnapshot
    ): FocusSession? = mutationMutex.withLock {
        if (dao.activeSession()?.toDomain() != null) return@withLock null
        engine.start(id, preset, durationMillis, now).also { save(it, now) }
    }

    suspend fun pause(now: FocusTimeSnapshot): FocusSession? = mutateActive(now, engine::pause)

    suspend fun resume(now: FocusTimeSnapshot): FocusSession? = mutateActive(now, engine::resume)

    suspend fun stopEarly(now: FocusTimeSnapshot): FocusSession? =
        mutateActive(now, engine::stopEarly)

    suspend fun reconcileActive(now: FocusTimeSnapshot): FocusSession? = mutationMutex.withLock {
        val current = dao.activeSession()?.toDomain() ?: return@withLock null
        val reconciled = engine.reconcile(current, now)
        if (reconciled != current) save(reconciled, now)
        reconciled
    }

    suspend fun setReflection(
        session: FocusSession,
        reflection: FocusReflection?,
        now: FocusTimeSnapshot
    ): FocusSession = mutationMutex.withLock {
        engine.withReflection(session, reflection).also { save(it, now) }
    }

    private suspend fun mutateActive(
        now: FocusTimeSnapshot,
        operation: (FocusSession, FocusTimeSnapshot) -> FocusSession
    ): FocusSession? = mutationMutex.withLock {
        val current = dao.activeSession()?.toDomain() ?: return@withLock null
        operation(current, now).also { save(it, now) }
    }

    private suspend fun save(session: FocusSession, now: FocusTimeSnapshot) {
        dao.upsert(session.toEntity(now.epochMillis))
    }
}
