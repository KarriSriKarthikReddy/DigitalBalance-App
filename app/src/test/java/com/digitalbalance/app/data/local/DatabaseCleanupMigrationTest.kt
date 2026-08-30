package com.digitalbalance.app.data.local

import androidx.sqlite.db.SupportSQLiteDatabase
import java.lang.reflect.Proxy
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class DatabaseCleanupMigrationTest {
    @Test
    fun migrationFourToFiveDropsOnlyDeferredFeatureTables() {
        val statements = mutableListOf<String>()
        val database = Proxy.newProxyInstance(
            SupportSQLiteDatabase::class.java.classLoader,
            arrayOf(SupportSQLiteDatabase::class.java)
        ) { _, method, args ->
            if (method.name == "execSQL" && args?.firstOrNull() is String) {
                statements += args.first() as String
            }
            when (method.returnType) {
                Boolean::class.javaPrimitiveType -> false
                Int::class.javaPrimitiveType -> 0
                Long::class.javaPrimitiveType -> 0L
                else -> null
            }
        } as SupportSQLiteDatabase

        DigitalBalanceDatabase.MIGRATION_4_5.migrate(database)

        assertEquals(
            listOf(
                "DROP TABLE IF EXISTS `purpose_attributions`",
                "DROP TABLE IF EXISTS `purpose_defaults`"
            ),
            statements
        )
        listOf("daily_usage", "category_overrides", "goals", "focus_sessions").forEach { table ->
            assertTrue(statements.none { it.contains(table) })
        }
    }

    @Test
    fun migrationPathKeepsExistingFeatureMigrationsAvailable() {
        assertEquals(1, DigitalBalanceDatabase.MIGRATION_1_2.startVersion)
        assertEquals(2, DigitalBalanceDatabase.MIGRATION_2_3.startVersion)
        assertEquals(3, DigitalBalanceDatabase.MIGRATION_3_4.startVersion)
        assertEquals(4, DigitalBalanceDatabase.MIGRATION_4_5.startVersion)
        assertEquals(5, DigitalBalanceDatabase.MIGRATION_4_5.endVersion)
    }

    @Test
    fun currentDatabaseApiDoesNotExposeRemovedFeatureDao() {
        assertTrue(
            DigitalBalanceDatabase::class.java.declaredMethods.none { method ->
                method.name.contains("purpose", ignoreCase = true)
            }
        )
    }
}
