package com.digitalbalance.app.data.local

import androidx.sqlite.db.SupportSQLiteDatabase
import java.lang.reflect.Proxy
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class FocusMigrationTest {
    @Test
    fun migrationTwoToThreeCreatesFocusTableAndStatusIndex() {
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

        DigitalBalanceDatabase.MIGRATION_2_3.migrate(database)

        assertEquals(2, statements.size)
        assertTrue(statements[0].contains("CREATE TABLE IF NOT EXISTS `focus_sessions`"))
        assertTrue(statements[0].contains("`segmentStartedElapsedRealtimeMillis` INTEGER"))
        assertTrue(statements[0].contains("`reflectionKey` TEXT"))
        assertTrue(statements[1].contains("index_focus_sessions_statusKey"))
    }
}
