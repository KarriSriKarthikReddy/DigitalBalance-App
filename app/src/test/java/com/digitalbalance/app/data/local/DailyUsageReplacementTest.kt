package com.digitalbalance.app.data.local

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Test

class DailyUsageReplacementTest {
    @Test
    fun repeatedRefreshReplacesDayWithoutDoubleCountingOrLeavingStaleApps() = runBlocking {
        val dao = FakeUsageDao()
        dao.replaceDailyUsage("2026-08-30", listOf(row("app.a", 100), row("app.b", 200)))
        dao.replaceDailyUsage("2026-08-30", listOf(row("app.a", 150)))

        val stored = dao.dailyUsage("2026-08-30")
        assertEquals(1, stored.size)
        assertEquals("app.a", stored.single().packageName)
        assertEquals(150L, stored.single().foregroundDurationMillis)
    }

    @Test
    fun digitalBalanceRowPersistsAndStillUsesTheDayPackagePrimaryKey() = runBlocking {
        val dao = FakeUsageDao()
        dao.replaceDailyUsage(
            "2026-08-30",
            listOf(row("com.instagram.android", 60), row("com.digitalbalance.app", 20))
        )
        dao.replaceDailyUsage(
            "2026-08-30",
            listOf(row("com.instagram.android", 65), row("com.digitalbalance.app", 25))
        )

        val stored = dao.dailyUsage("2026-08-30")
        assertEquals(2, stored.size)
        assertEquals(
            25L,
            stored.single { it.packageName == "com.digitalbalance.app" }.foregroundDurationMillis
        )
        assertEquals(90L, stored.sumOf(DailyUsageEntity::foregroundDurationMillis))
    }

    private fun row(packageName: String, duration: Long) = DailyUsageEntity(
        dateKey = "2026-08-30",
        packageName = packageName,
        appName = packageName,
        foregroundDurationMillis = duration,
        openCount = 1,
        updatedAtMillis = 1
    )

    private class FakeUsageDao : UsageDao {
        private val dailyRows = mutableListOf<DailyUsageEntity>()
        private val overrides = MutableStateFlow<List<CategoryOverrideEntity>>(emptyList())

        override suspend fun upsertDailyUsage(records: List<DailyUsageEntity>) {
            records.forEach { record ->
                dailyRows.removeAll {
                    it.dateKey == record.dateKey && it.packageName == record.packageName
                }
                dailyRows += record
            }
        }

        override suspend fun deleteDailyUsage(dateKey: String) {
            dailyRows.removeAll { it.dateKey == dateKey }
        }

        override fun observeDailyUsage(startDateKey: String, endDateKey: String): Flow<List<DailyUsageEntity>> =
            MutableStateFlow(
                dailyRows.filter { it.dateKey >= startDateKey && it.dateKey <= endDateKey }
            )

        override suspend fun dailyUsage(dateKey: String): List<DailyUsageEntity> =
            dailyRows.filter { it.dateKey == dateKey }

        override suspend fun upsertCategoryOverride(override: CategoryOverrideEntity) {
            overrides.value = overrides.value.filterNot { it.packageName == override.packageName } + override
        }

        override fun observeCategoryOverrides(): Flow<List<CategoryOverrideEntity>> = overrides
    }
}
