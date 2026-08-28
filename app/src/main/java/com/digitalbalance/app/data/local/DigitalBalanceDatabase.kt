package com.digitalbalance.app.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

@Database(
    entities = [DailyUsageEntity::class, CategoryOverrideEntity::class, GoalEntity::class],
    version = 2,
    exportSchema = true
)
abstract class DigitalBalanceDatabase : RoomDatabase() {
    abstract fun usageDao(): UsageDao
    abstract fun goalDao(): GoalDao

    companion object {
        @Volatile
        private var instance: DigitalBalanceDatabase? = null

        fun getInstance(context: Context): DigitalBalanceDatabase =
            instance ?: synchronized(this) {
                instance ?: Room.databaseBuilder(
                    context.applicationContext,
                    DigitalBalanceDatabase::class.java,
                    "digital_balance.db"
                ).addMigrations(MIGRATION_1_2).build().also { instance = it }
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
    }
}
