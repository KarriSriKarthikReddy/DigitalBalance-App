package com.digitalbalance.app.data.local

import androidx.room.Entity

@Entity(
    tableName = "daily_usage",
    primaryKeys = ["dateKey", "packageName"]
)
data class DailyUsageEntity(
    val dateKey: String,
    val packageName: String,
    val appName: String,
    val foregroundDurationMillis: Long,
    val openCount: Int,
    val updatedAtMillis: Long
)
