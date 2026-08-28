package com.digitalbalance.app.data.local

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import kotlinx.coroutines.flow.Flow

@Dao
interface UsageDao {
    @Upsert
    suspend fun upsertDailyUsage(records: List<DailyUsageEntity>)

    @Query(
        "SELECT * FROM daily_usage WHERE dateKey BETWEEN :startDateKey AND :endDateKey " +
            "ORDER BY dateKey DESC, foregroundDurationMillis DESC"
    )
    fun observeDailyUsage(
        startDateKey: String,
        endDateKey: String
    ): Flow<List<DailyUsageEntity>>

    @Query("SELECT * FROM daily_usage WHERE dateKey = :dateKey")
    suspend fun dailyUsage(dateKey: String): List<DailyUsageEntity>

    @Upsert
    suspend fun upsertCategoryOverride(override: CategoryOverrideEntity)

    @Query("SELECT * FROM category_overrides")
    fun observeCategoryOverrides(): Flow<List<CategoryOverrideEntity>>
}
