package com.digitalbalance.app.data.usage

import android.graphics.Bitmap

data class AppUsage(
    val packageName: String,
    val appName: String,
    val foregroundDurationMillis: Long,
    val openCount: Int,
    val icon: Bitmap?
)

data class TodayUsage(
    val apps: List<AppUsage>,
    val totalForegroundDurationMillis: Long
)
