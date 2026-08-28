package com.digitalbalance.app.domain.goal

enum class GoalType(val storageKey: String, val isMinimumTarget: Boolean) {
    OverallForegroundUsage("overall_foreground_usage", false),
    ProductiveTime("productive_time", true),
    SocialMediaLimit("social_media_limit", false),
    EntertainmentLimit("entertainment_limit", false),
    AppDailyLimit("app_daily_limit", false);

    companion object {
        fun fromStorageKey(value: String): GoalType? =
            entries.firstOrNull { it.storageKey == value }
    }
}

data class DigitalGoal(
    val id: String,
    val type: GoalType,
    val targetDurationMillis: Long,
    val packageName: String? = null,
    val appName: String? = null
) {
    companion object {
        fun idFor(type: GoalType, packageName: String? = null): String =
            if (type == GoalType.AppDailyLimit) {
                "${type.storageKey}:${requireNotNull(packageName)}"
            } else {
                type.storageKey
            }
    }
}

data class GoalProgress(
    val goal: DigitalGoal,
    val currentDurationMillis: Long?,
    val progressFraction: Float?
)
