package com.digitalbalance.app.ui.theme

import android.app.Activity
import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

private val DarkColorScheme = darkColorScheme(
    primary = CalmGreen80,
    secondary = CalmSlate80,
    tertiary = CalmGold80,
    onPrimary = Color(0xFF00382B),
    primaryContainer = Color(0xFF145040),
    onPrimaryContainer = Color(0xFFC9F7E5),
    secondaryContainer = Color(0xFF354B43),
    onSecondaryContainer = Color(0xFFD7E8E0),
    tertiaryContainer = Color(0xFF504719),
    onTertiaryContainer = Color(0xFFF5E6A7),
    background = Color(0xFF101412),
    surface = Color(0xFF101412),
    surfaceContainerLow = Color(0xFF181D1A),
    surfaceContainer = Color(0xFF1C211E),
    surfaceContainerHigh = Color(0xFF262B28),
    onBackground = Color(0xFFE1E3DF),
    onSurface = Color(0xFFE1E3DF),
    onSurfaceVariant = Color(0xFFBEC9C2),
    outline = Color(0xFF89938D),
    outlineVariant = Color(0xFF3F4944)
)

private val LightColorScheme = lightColorScheme(
    primary = CalmGreen40,
    secondary = CalmSlate40,
    tertiary = CalmGold40,
    onPrimary = Color.White,
    primaryContainer = Color(0xFFC9F7E5),
    onPrimaryContainer = Color(0xFF002119),
    secondaryContainer = Color(0xFFD6E7DF),
    onSecondaryContainer = Color(0xFF10201A),
    tertiaryContainer = Color(0xFFF0E3A6),
    onTertiaryContainer = Color(0xFF211B00),
    background = Color(0xFFF7FAF7),
    surface = Color(0xFFF7FAF7),
    surfaceContainerLow = Color(0xFFF1F5F1),
    surfaceContainer = Color(0xFFEBEFEB),
    surfaceContainerHigh = Color(0xFFE5E9E5),
    onBackground = Color(0xFF191C1A),
    onSurface = Color(0xFF191C1A),
    onSurfaceVariant = Color(0xFF404943),
    outline = Color(0xFF707973),
    outlineVariant = Color(0xFFC0C9C2)
)

@Composable
fun DigitalBalanceTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    // Dynamic color is available on Android 12+
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit
) {
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }

        darkTheme -> DarkColorScheme
        else -> LightColorScheme
    }
    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            WindowCompat.getInsetsController(window, view).apply {
                isAppearanceLightStatusBars = !darkTheme
                isAppearanceLightNavigationBars = !darkTheme
            }
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
