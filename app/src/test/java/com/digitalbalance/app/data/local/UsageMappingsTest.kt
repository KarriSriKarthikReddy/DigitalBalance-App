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
}
