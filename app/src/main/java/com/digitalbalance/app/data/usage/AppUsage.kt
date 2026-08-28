package com.digitalbalance.app.data.usage

data class AppUsage(
    val packageName: String,
    val appName: String,
    val foregroundDurationMillis: Long,
    val openCount: Int
)

data class TodayUsage(
    val apps: List<AppUsage>,
    val totalForegroundDurationMillis: Long
)
