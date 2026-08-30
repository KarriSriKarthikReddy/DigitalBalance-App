package com.digitalbalance.app.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

@Database(
    entities = [
        DailyUsageEntity::class,
        CategoryOverrideEntity::class,
        GoalEntity::class,
        FocusSessionEntity::class
    ],
    version = 5,
    exportSchema = true
)
abstract class DigitalBalanceDatabase : RoomDatabase() {
    abstract fun usageDao(): UsageDao
    abstract fun goalDao(): GoalDao
    abstract fun focusSessionDao(): FocusSessionDao

    companion object {
        @Volatile
        private var instance: DigitalBalanceDatabase? = null

        fun getInstance(context: Context): DigitalBalanceDatabase =
            instance ?: synchronized(this) {
                instance ?: Room.databaseBuilder(
                    context.applicationContext,
                    DigitalBalanceDatabase::class.java,
                    "digital_balance.db"
                ).addMigrations(MIGRATION_1_2, MIGRATION_2_3, MIGRATION_3_4, MIGRATION_4_5).build()
                    .also { instance = it }
            }

        val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    "CREATE TABLE IF NOT EXISTS `goals` (" +
                        "`id` TEXT NOT NULL, " +
                        "`typeKey` TEXT NOT NULL, " +
                        "`targetDurationMillis` INTEGER NOT NULL, " +
                        "`packageName` TEXT, " +
                        "`appName` TEXT, " +
                        "`updatedAtMillis` INTEGER NOT NULL, " +
                        "PRIMARY KEY(`id`))"
                )
            }
        }

        val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    "CREATE TABLE IF NOT EXISTS `focus_sessions` (" +
                        "`id` TEXT NOT NULL, " +
                        "`presetKey` TEXT NOT NULL, " +
                        "`plannedDurationMillis` INTEGER NOT NULL, " +
                        "`accumulatedFocusedMillis` INTEGER NOT NULL, " +
                        "`startedAtEpochMillis` INTEGER NOT NULL, " +
                        "`segmentStartedAtEpochMillis` INTEGER, " +
                        "`segmentStartedElapsedRealtimeMillis` INTEGER, " +
                        "`segmentBootEpochOffsetMillis` INTEGER, " +
                        "`endedAtEpochMillis` INTEGER, " +
                        "`statusKey` TEXT NOT NULL, " +
                        "`reflectionKey` TEXT, " +
                        "`updatedAtEpochMillis` INTEGER NOT NULL, " +
                        "PRIMARY KEY(`id`))"
                )
                db.execSQL(
                    "CREATE INDEX IF NOT EXISTS `index_focus_sessions_statusKey` " +
                        "ON `focus_sessions` (`statusKey`)"
                )
            }
        }

        val MIGRATION_3_4 = object : Migration(3, 4) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    "CREATE TABLE IF NOT EXISTS `purpose_attributions` (" +
                        "`id` TEXT NOT NULL, `packageName` TEXT NOT NULL, `dateKey` TEXT NOT NULL, " +
                        "`scopeKey` TEXT NOT NULL, `sessionStartMillis` INTEGER, " +
                        "`sessionEndMillis` INTEGER, `purposeKey` TEXT NOT NULL, " +
                        "`provenanceKey` TEXT NOT NULL, `updatedAtMillis` INTEGER NOT NULL, " +
                        "PRIMARY KEY(`id`))"
                )
                db.execSQL(
                    "CREATE INDEX IF NOT EXISTS `index_purpose_attributions_dateKey` " +
                        "ON `purpose_attributions` (`dateKey`)"
                )
                db.execSQL(
                    "CREATE INDEX IF NOT EXISTS `index_purpose_attributions_packageName` " +
                        "ON `purpose_attributions` (`packageName`)"
                )
                db.execSQL(
                    "CREATE TABLE IF NOT EXISTS `purpose_defaults` (" +
                        "`packageName` TEXT NOT NULL, `purposeKey` TEXT NOT NULL, " +
                        "`effectiveFromMillis` INTEGER NOT NULL, `updatedAtMillis` INTEGER NOT NULL, " +
                        "PRIMARY KEY(`packageName`))"
                )
            }
        }

        val MIGRATION_4_5 = object : Migration(4, 5) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("DROP TABLE IF EXISTS `purpose_attributions`")
                db.execSQL("DROP TABLE IF EXISTS `purpose_defaults`")
            }
        }
    }
}
