package com.digitalbalance.app.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import com.digitalbalance.app.domain.category.AppCategory

@Composable
fun AppCategory.accentColor(): Color = when (this) {
    AppCategory.Education, AppCategory.Productivity -> MaterialTheme.digitalBalanceColors.productive
    AppCategory.Social -> MaterialTheme.digitalBalanceColors.social
    AppCategory.Entertainment -> MaterialTheme.digitalBalanceColors.entertainment
    AppCategory.Gaming -> MaterialTheme.digitalBalanceColors.gaming
    AppCategory.Communication -> MaterialTheme.digitalBalanceColors.communication
    AppCategory.Utility -> MaterialTheme.digitalBalanceColors.utility
    AppCategory.Other, AppCategory.MixedContextDependent -> MaterialTheme.digitalBalanceColors.mixed
}
