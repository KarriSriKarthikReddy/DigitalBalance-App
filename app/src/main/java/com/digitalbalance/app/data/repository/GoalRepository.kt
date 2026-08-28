package com.digitalbalance.app.data.repository

import com.digitalbalance.app.data.local.GoalDao
import com.digitalbalance.app.data.local.toDomain
import com.digitalbalance.app.data.local.toEntity
import com.digitalbalance.app.domain.goal.DigitalGoal
import com.digitalbalance.app.domain.goal.GoalType
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class GoalRepository(private val goalDao: GoalDao) {
    fun observeGoals(): Flow<List<DigitalGoal>> = goalDao.observeGoals().map { rows ->
        rows.mapNotNull { it.toDomain() }
    }

    suspend fun saveGoal(goal: DigitalGoal) {
        require(goal.targetDurationMillis > 0L)
        require(
            goal.type != GoalType.AppDailyLimit ||
                !goal.packageName.isNullOrBlank()
        )
        goalDao.upsertGoal(goal.toEntity(System.currentTimeMillis()))
    }

    suspend fun deleteGoal(goalId: String) {
        goalDao.deleteGoal(goalId)
    }
}
