package com.digitalbalance.app.data.usage

import android.graphics.Bitmap
import com.digitalbalance.app.domain.category.AppCategory

data class AppUsage(
    val packageName: String,
    val appName: String,
    val foregroundDurationMillis: Long,
    val openCount: Int,
    val icon: Bitmap?,
    val category: AppCategory
)

data class TodayUsage(
    val apps: List<AppUsage>,
    val totalForegroundDurationMillis: Long
)
