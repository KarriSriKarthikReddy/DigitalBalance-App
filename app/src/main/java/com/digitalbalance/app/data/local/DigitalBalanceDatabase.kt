package com.digitalbalance.app.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(
    entities = [DailyUsageEntity::class, CategoryOverrideEntity::class],
    version = 1,
    exportSchema = true
)
abstract class DigitalBalanceDatabase : RoomDatabase() {
    abstract fun usageDao(): UsageDao

    companion object {
        @Volatile
        private var instance: DigitalBalanceDatabase? = null

        fun getInstance(context: Context): DigitalBalanceDatabase =
            instance ?: synchronized(this) {
                instance ?: Room.databaseBuilder(
                    context.applicationContext,
                    DigitalBalanceDatabase::class.java,
                    "digital_balance.db"
                ).build().also { instance = it }
            }
    }
}
