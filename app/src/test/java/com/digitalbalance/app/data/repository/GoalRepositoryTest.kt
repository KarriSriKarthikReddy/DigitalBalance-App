package com.digitalbalance.app.data.repository

import com.digitalbalance.app.data.local.GoalDao
import com.digitalbalance.app.data.local.GoalEntity
import com.digitalbalance.app.domain.goal.DigitalGoal
import com.digitalbalance.app.domain.goal.GoalType
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class GoalRepositoryTest {
    @Test
    fun `save edits stable goal and delete removes it`() = runBlocking {
        val dao = FakeGoalDao()
        val repository = GoalRepository(dao)
        val goal = DigitalGoal(
            id = DigitalGoal.idFor(GoalType.SocialMediaLimit),
            type = GoalType.SocialMediaLimit,
            targetDurationMillis = 30L
        )

        repository.saveGoal(goal)
        repository.saveGoal(goal.copy(targetDurationMillis = 45L))

        val saved = repository.observeGoals().first()
        assertEquals(1, saved.size)
        assertEquals(45L, saved.single().targetDurationMillis)

        repository.deleteGoal(goal.id)
        assertTrue(repository.observeGoals().first().isEmpty())
    }

    private class FakeGoalDao : GoalDao {
        private val rows = MutableStateFlow<List<GoalEntity>>(emptyList())

        override fun observeGoals(): Flow<List<GoalEntity>> = rows

        override suspend fun upsertGoal(goal: GoalEntity) {
            rows.value = rows.value.filterNot { it.id == goal.id } + goal
        }

        override suspend fun deleteGoal(goalId: String) {
            rows.value = rows.value.filterNot { it.id == goalId }
        }
    }
}
