package com.digitalbalance.app.data.local

import com.digitalbalance.app.data.usage.AppUsage

data class DailyUsageRecord(
    val dateKey: String,
    val packageName: String,
    val appName: String,
    val foregroundDurationMillis: Long,
    val openCount: Int,
    val updatedAtMillis: Long
)

fun AppUsage.toDailyUsageEntity(
    dateKey: String,
    updatedAtMillis: Long
): DailyUsageEntity = DailyUsageEntity(
    dateKey = dateKey,
    packageName = packageName,
    appName = appName,
    foregroundDurationMillis = foregroundDurationMillis,
    openCount = openCount,
    updatedAtMillis = updatedAtMillis
)

fun DailyUsageEntity.toDomain(): DailyUsageRecord = DailyUsageRecord(
    dateKey = dateKey,
    packageName = packageName,
    appName = appName,
    foregroundDurationMillis = foregroundDurationMillis,
    openCount = openCount,
    updatedAtMillis = updatedAtMillis
)
