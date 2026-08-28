package com.digitalbalance.app.ui.goals

import androidx.annotation.StringRes
import com.digitalbalance.app.R
import com.digitalbalance.app.domain.goal.GoalType

@StringRes
fun GoalType.labelRes(): Int = when (this) {
    GoalType.OverallForegroundUsage -> R.string.goal_overall
    GoalType.ProductiveTime -> R.string.goal_productive
    GoalType.SocialMediaLimit -> R.string.goal_social
    GoalType.EntertainmentLimit -> R.string.goal_entertainment
    GoalType.AppDailyLimit -> R.string.goal_per_app
}

@StringRes
fun GoalType.descriptionRes(): Int = when (this) {
    GoalType.OverallForegroundUsage -> R.string.goal_overall_description
    GoalType.ProductiveTime -> R.string.goal_productive_description
    GoalType.SocialMediaLimit -> R.string.goal_social_description
    GoalType.EntertainmentLimit -> R.string.goal_entertainment_description
    GoalType.AppDailyLimit -> R.string.goal_per_app_description
}
