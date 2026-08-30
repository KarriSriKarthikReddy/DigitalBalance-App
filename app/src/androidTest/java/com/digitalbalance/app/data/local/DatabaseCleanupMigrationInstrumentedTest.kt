package com.digitalbalance.app.data.local

import android.content.Context
import androidx.sqlite.db.SupportSQLiteDatabase
import androidx.sqlite.db.SupportSQLiteOpenHelper
import androidx.sqlite.db.framework.FrameworkSQLiteOpenHelperFactory
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class DatabaseCleanupMigrationInstrumentedTest {
    private lateinit var context: Context
    private lateinit var helper: SupportSQLiteOpenHelper

    @Before
    fun createVersionFourDatabase() {
        context = ApplicationProvider.getApplicationContext()
        context.deleteDatabase(DATABASE_NAME)
        helper = FrameworkSQLiteOpenHelperFactory().create(
            SupportSQLiteOpenHelper.Configuration.builder(context)
                .name(DATABASE_NAME)
                .callback(
                    object : SupportSQLiteOpenHelper.Callback(4) {
                        override fun onCreate(db: SupportSQLiteDatabase) {
                            createVersionFourTables(db)
                        }

                        override fun onUpgrade(
                            db: SupportSQLiteDatabase,
                            oldVersion: Int,
                            newVersion: Int
                        ) = Unit
                    }
                )
                .build()
        )
    }

    @After
    fun closeDatabase() {
        helper.close()
        context.deleteDatabase(DATABASE_NAME)
    }

    @Test
    fun migrationFourToFivePreservesAllEarlierFeatureRowsAndDropsOnlyRemovedTables() {
        val db = helper.writableDatabase
        insertSampleRows(db)

        DigitalBalanceDatabase.MIGRATION_4_5.migrate(db)

        assertEquals("Example", queryString(db, "SELECT appName FROM daily_usage"))
        assertEquals("productivity", queryString(db, "SELECT categoryKey FROM category_overrides"))
        assertEquals(3_600_000L, queryLong(db, "SELECT targetDurationMillis FROM goals"))
        assertEquals("completed", queryString(db, "SELECT statusKey FROM focus_sessions"))
        assertFalse(tableExists(db, "purpose_attributions"))
        assertFalse(tableExists(db, "purpose_defaults"))
        listOf("daily_usage", "category_overrides", "goals", "focus_sessions").forEach { table ->
            assertTrue(tableExists(db, table))
        }
    }

    private fun createVersionFourTables(db: SupportSQLiteDatabase) {
        db.execSQL(
            "CREATE TABLE daily_usage (dateKey TEXT NOT NULL, packageName TEXT NOT NULL, " +
                "appName TEXT NOT NULL, foregroundDurationMillis INTEGER NOT NULL, " +
                "openCount INTEGER NOT NULL, updatedAtMillis INTEGER NOT NULL, " +
                "PRIMARY KEY(dateKey, packageName))"
        )
        db.execSQL(
            "CREATE TABLE category_overrides (packageName TEXT NOT NULL PRIMARY KEY, " +
                "categoryKey TEXT NOT NULL, updatedAtMillis INTEGER NOT NULL)"
        )
        db.execSQL(
            "CREATE TABLE goals (id TEXT NOT NULL PRIMARY KEY, typeKey TEXT NOT NULL, " +
                "targetDurationMillis INTEGER NOT NULL, packageName TEXT, appName TEXT, " +
                "updatedAtMillis INTEGER NOT NULL)"
        )
        db.execSQL(
            "CREATE TABLE focus_sessions (id TEXT NOT NULL PRIMARY KEY, presetKey TEXT NOT NULL, " +
                "plannedDurationMillis INTEGER NOT NULL, accumulatedFocusedMillis INTEGER NOT NULL, " +
                "startedAtEpochMillis INTEGER NOT NULL, segmentStartedAtEpochMillis INTEGER, " +
                "segmentStartedElapsedRealtimeMillis INTEGER, segmentBootEpochOffsetMillis INTEGER, " +
                "endedAtEpochMillis INTEGER, statusKey TEXT NOT NULL, reflectionKey TEXT, " +
                "updatedAtEpochMillis INTEGER NOT NULL)"
        )
        db.execSQL("CREATE TABLE purpose_attributions (id TEXT NOT NULL PRIMARY KEY)")
        db.execSQL("CREATE TABLE purpose_defaults (packageName TEXT NOT NULL PRIMARY KEY)")
    }

    private fun insertSampleRows(db: SupportSQLiteDatabase) {
        db.execSQL(
            "INSERT INTO daily_usage VALUES (?, ?, ?, ?, ?, ?)",
            arrayOf<Any?>("2026-08-30", "example.app", "Example", 120_000L, 2, 1L)
        )
        db.execSQL(
            "INSERT INTO category_overrides VALUES (?, ?, ?)",
            arrayOf<Any?>("example.app", "productivity", 1L)
        )
        db.execSQL(
            "INSERT INTO goals VALUES (?, ?, ?, ?, ?, ?)",
            arrayOf<Any?>("overall", "overall_usage_target", 3_600_000L, null, null, 1L)
        )
        db.execSQL(
            "INSERT INTO focus_sessions VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)",
            arrayOf<Any?>(
                "focus-1", "work", 1_500_000L, 1_500_000L, 1L,
                null, null, null, 1_500_001L, "completed", null, 1_500_001L
            )
        )
    }

    private fun tableExists(db: SupportSQLiteDatabase, table: String): Boolean =
        db.query(
            "SELECT name FROM sqlite_master WHERE type = 'table' AND name = ?",
            arrayOf(table)
        ).use { it.moveToFirst() }

    private fun queryString(db: SupportSQLiteDatabase, sql: String): String =
        db.query(sql).use { cursor ->
            check(cursor.moveToFirst())
            cursor.getString(0)
        }

    private fun queryLong(db: SupportSQLiteDatabase, sql: String): Long =
        db.query(sql).use { cursor ->
            check(cursor.moveToFirst())
            cursor.getLong(0)
        }

    private companion object {
        const val DATABASE_NAME = "purpose_cleanup_migration_test.db"
    }
}
