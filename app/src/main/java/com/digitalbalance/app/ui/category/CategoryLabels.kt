package com.digitalbalance.app.ui.category

import androidx.annotation.StringRes
import com.digitalbalance.app.R
import com.digitalbalance.app.domain.category.AppCategory

@StringRes
fun AppCategory.labelRes(): Int = when (this) {
    AppCategory.Education -> R.string.category_education
    AppCategory.Productivity -> R.string.category_productivity
    AppCategory.Communication -> R.string.category_communication
    AppCategory.Social -> R.string.category_social
    AppCategory.Entertainment -> R.string.category_entertainment
    AppCategory.Gaming -> R.string.category_gaming
    AppCategory.Utility -> R.string.category_utility
    AppCategory.Other -> R.string.category_other
    AppCategory.MixedContextDependent -> R.string.category_mixed
}
