package com.digitalbalance.app.data.repository

import com.digitalbalance.app.data.local.FocusSessionDao
import com.digitalbalance.app.data.local.FocusSessionEntity
import com.digitalbalance.app.domain.focus.FocusPreset
import com.digitalbalance.app.domain.focus.FocusSessionStatus
import com.digitalbalance.app.domain.focus.FocusTimeSnapshot
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test

class FocusRepositoryTest {
    @Test
    fun activeSessionPersistsAndPreventsDuplicateStart() = runBlocking {
        val dao = FakeFocusSessionDao()
        val repository = FocusRepository(dao)
        val now = FocusTimeSnapshot(1_000_000L, 100_000L)

        assertNotNull(repository.start("one", FocusPreset.Work, 60_000L, now))
        assertNull(repository.start("two", FocusPreset.Study, 60_000L, now))
        assertEquals("one", dao.activeSession()?.id)
    }

    @Test
    fun pauseAndCompletionAreDurablyUpserted() = runBlocking {
        val dao = FakeFocusSessionDao()
        val repository = FocusRepository(dao)
        val start = FocusTimeSnapshot(1_000_000L, 100_000L)
        repository.start("one", FocusPreset.Focus, 60_000L, start)

        val paused = repository.pause(FocusTimeSnapshot(1_020_000L, 120_000L))!!
        assertEquals(FocusSessionStatus.Paused, paused.status)
        assertEquals(20_000L, dao.activeSession()?.accumulatedFocusedMillis)

        repository.resume(FocusTimeSnapshot(1_030_000L, 130_000L))
        val completed = repository.reconcileActive(FocusTimeSnapshot(1_070_000L, 170_000L))!!
        assertEquals(FocusSessionStatus.Completed, completed.status)
        assertNull(dao.activeSession())
        assertEquals(FocusSessionStatus.Completed.storageKey, dao.rows.value.single().statusKey)
    }

    private class FakeFocusSessionDao : FocusSessionDao {
        val rows = MutableStateFlow<List<FocusSessionEntity>>(emptyList())

        override fun observeSessions(): Flow<List<FocusSessionEntity>> = rows

        override suspend fun activeSession(): FocusSessionEntity? = rows.value
            .filter { it.statusKey == "running" || it.statusKey == "paused" }
            .maxByOrNull(FocusSessionEntity::updatedAtEpochMillis)

        override suspend fun upsert(session: FocusSessionEntity) {
            rows.value = rows.value.filterNot { it.id == session.id } + session
        }
    }
}
