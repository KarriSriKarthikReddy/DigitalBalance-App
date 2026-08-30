package com.digitalbalance.app.data.local

import com.digitalbalance.app.data.usage.AppUsage
import com.digitalbalance.app.domain.category.AppCategory
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class UsageMappingsTest {
    @Test
    fun `daily entity preserves durable usage fields and omits icon`() {
        val entity = AppUsage(
            packageName = "example.app",
            appName = "Example",
            foregroundDurationMillis = 42_000L,
            openCount = 3,
            icon = null,
            category = AppCategory.Utility
        ).toDailyUsageEntity(dateKey = "2026-08-28", updatedAtMillis = 100L)

        assertEquals("2026-08-28", entity.dateKey)
        assertEquals("example.app", entity.packageName)
        assertEquals("Example", entity.appName)
        assertEquals(42_000L, entity.foregroundDurationMillis)
        assertEquals(3, entity.openCount)
        assertEquals(100L, entity.updatedAtMillis)

        val record = entity.toDomain()
        assertEquals(entity.dateKey, record.dateKey)
        assertEquals(entity.packageName, record.packageName)
        assertEquals(entity.foregroundDurationMillis, record.foregroundDurationMillis)
    }

    @Test
    fun `category storage keys round trip`() {
        AppCategory.entries.forEach { category ->
            assertEquals(category, AppCategory.fromStorageKey(category.storageKey))
        }
        assertNull(AppCategory.fromStorageKey("not_a_category"))
    }

    @Test
    fun `digital balance usage maps to durable daily history unchanged`() {
        val entity = AppUsage(
            packageName = "com.digitalbalance.app",
            appName = "DigitalBalance",
            foregroundDurationMillis = 20L * 60L * 1_000L,
            openCount = 4,
            icon = null,
            category = AppCategory.Utility
        ).toDailyUsageEntity("2026-08-30", 200L)

        assertEquals("com.digitalbalance.app", entity.packageName)
        assertEquals(20L * 60L * 1_000L, entity.foregroundDurationMillis)
        assertEquals("com.digitalbalance.app", entity.toDomain().packageName)
        assertEquals(20L * 60L * 1_000L, entity.toDomain().foregroundDurationMillis)
    }
}
