package com.digitalbalance.app.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

private val DefaultFamily = FontFamily.Default

private fun appTextStyle(
    weight: FontWeight,
    size: Int,
    lineHeight: Int,
    letterSpacing: Float = 0f
) = TextStyle(
    fontFamily = DefaultFamily,
    fontWeight = weight,
    fontSize = size.sp,
    lineHeight = lineHeight.sp,
    letterSpacing = letterSpacing.sp
)

val Typography = Typography(
    displayMedium = appTextStyle(FontWeight.SemiBold, 44, 50, -0.6f),
    displaySmall = appTextStyle(FontWeight.SemiBold, 36, 42, -0.4f),
    headlineLarge = appTextStyle(FontWeight.SemiBold, 30, 36, -0.2f),
    headlineMedium = appTextStyle(FontWeight.SemiBold, 26, 32),
    headlineSmall = appTextStyle(FontWeight.SemiBold, 22, 28),
    titleLarge = appTextStyle(FontWeight.SemiBold, 20, 26),
    titleMedium = appTextStyle(FontWeight.Medium, 16, 22, 0.1f),
    titleSmall = appTextStyle(FontWeight.SemiBold, 14, 20, 0.1f),
    bodyLarge = appTextStyle(FontWeight.Normal, 16, 24, 0.15f),
    bodyMedium = appTextStyle(FontWeight.Normal, 14, 21, 0.15f),
    bodySmall = appTextStyle(FontWeight.Normal, 12, 18, 0.2f),
    labelLarge = appTextStyle(FontWeight.SemiBold, 14, 20, 0.1f),
    labelMedium = appTextStyle(FontWeight.Medium, 12, 16, 0.25f),
    labelSmall = appTextStyle(FontWeight.Medium, 11, 16, 0.3f)
)
