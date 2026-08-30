package com.digitalbalance.app.data.repository

import com.digitalbalance.app.data.local.CategoryOverrideEntity
import com.digitalbalance.app.data.local.DailyUsageRecord
import com.digitalbalance.app.data.local.UsageDao
import com.digitalbalance.app.data.local.toDailyUsageEntity
import com.digitalbalance.app.data.local.toDomain
import com.digitalbalance.app.data.usage.TodayUsage
import com.digitalbalance.app.data.usage.UsageStatsDataSource
import com.digitalbalance.app.domain.category.AppCategory
import java.util.Calendar
import java.util.Locale
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map

class UsageRepository(
    private val systemUsage: UsageStatsDataSource,
    private val usageDao: UsageDao
) {
    private val mutableCurrentTodayUsage = MutableStateFlow<CurrentDayUsage?>(null)
    val currentTodayUsage: StateFlow<CurrentDayUsage?> = mutableCurrentTodayUsage.asStateFlow()

    fun hasUsageAccess(): Boolean = systemUsage.hasUsageAccess()

    suspend fun loadAndStoreToday(nowMillis: Long = System.currentTimeMillis()): TodayUsage {
        val usage = systemUsage.loadTodayUsage(nowMillis)
        val dateKey = localDateKey(nowMillis)
        usageDao.replaceDailyUsage(
            dateKey = dateKey,
            records = usage.apps.map { it.toDailyUsageEntity(dateKey, nowMillis) }
        )
        mutableCurrentTodayUsage.value = CurrentDayUsage(dateKey, usage)
        return usage
    }

    fun observeCategoryOverrides(): Flow<Map<String, AppCategory>> =
        usageDao.observeCategoryOverrides().map { rows ->
            rows.mapNotNull { row ->
                AppCategory.fromStorageKey(row.categoryKey)?.let { row.packageName to it }
            }.toMap()
        }

    suspend fun setCategoryOverride(packageName: String, category: AppCategory) {
        usageDao.upsertCategoryOverride(
            CategoryOverrideEntity(
                packageName = packageName,
                categoryKey = category.storageKey,
                updatedAtMillis = System.currentTimeMillis()
            )
        )
    }

    fun observeHistory(
        startDateKey: String,
        endDateKey: String
    ): Flow<List<DailyUsageRecord>> =
        usageDao.observeDailyUsage(startDateKey, endDateKey).map { rows -> rows.map { it.toDomain() } }

    fun resolveDefaultCategory(packageName: String): AppCategory =
        systemUsage.resolveDefaultCategory(packageName)

    fun loadIcon(packageName: String) = systemUsage.loadIcon(packageName)
}

data class CurrentDayUsage(
    val dateKey: String,
    val usage: TodayUsage
)

internal fun localDateKey(timestampMillis: Long): String {
    val calendar = Calendar.getInstance().apply { timeInMillis = timestampMillis }
    return String.format(
        Locale.ROOT,
        "%04d-%02d-%02d",
        calendar.get(Calendar.YEAR),
        calendar.get(Calendar.MONTH) + 1,
        calendar.get(Calendar.DAY_OF_MONTH)
    )
}
