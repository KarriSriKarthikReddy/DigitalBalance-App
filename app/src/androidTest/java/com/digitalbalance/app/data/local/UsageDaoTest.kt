package com.digitalbalance.app.data.local

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class UsageDaoTest {
    private lateinit var database: DigitalBalanceDatabase
    private lateinit var dao: UsageDao

    @Before
    fun createDatabase() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        database = Room.inMemoryDatabaseBuilder(
            context,
            DigitalBalanceDatabase::class.java
        ).build()
        dao = database.usageDao()
    }

    @After
    fun closeDatabase() {
        database.close()
    }

    @Test
    fun upsertReplacesSameAppAndDayWithoutDuplicate() = runBlocking {
        val original = DailyUsageEntity(
            dateKey = "2026-08-28",
            packageName = "example.app",
            appName = "Example",
            foregroundDurationMillis = 10L,
            openCount = 1,
            updatedAtMillis = 10L
        )
        dao.upsertDailyUsage(listOf(original))
        dao.upsertDailyUsage(
            listOf(
                original.copy(
                    foregroundDurationMillis = 25L,
                    openCount = 2,
                    updatedAtMillis = 20L
                )
            )
        )

        val records = dao.dailyUsage("2026-08-28")
        assertEquals(1, records.size)
        assertEquals(25L, records.single().foregroundDurationMillis)
        assertEquals(2, records.single().openCount)
    }
}
