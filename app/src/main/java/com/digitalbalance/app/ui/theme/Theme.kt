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
import androidx.compose.ui.platform.LocalContext

private val DarkColorScheme = darkColorScheme(
    primary = CalmGreen80,
    secondary = CalmSlate80,
    tertiary = CalmGold80,
    background = androidx.compose.ui.graphics.Color(0xFF101412),
    surface = androidx.compose.ui.graphics.Color(0xFF101412),
    onBackground = androidx.compose.ui.graphics.Color(0xFFE1E3DF),
    onSurface = androidx.compose.ui.graphics.Color(0xFFE1E3DF)
)

private val LightColorScheme = lightColorScheme(
    primary = CalmGreen40,
    secondary = CalmSlate40,
    tertiary = CalmGold40,
    background = androidx.compose.ui.graphics.Color(0xFFF7FAF7),
    surface = androidx.compose.ui.graphics.Color(0xFFF7FAF7),
    onBackground = androidx.compose.ui.graphics.Color(0xFF191C1A),
    onSurface = androidx.compose.ui.graphics.Color(0xFF191C1A)

    /* Other default colors to override
    background = Color(0xFFFFFBFE),
    surface = Color(0xFFFFFBFE),
    onPrimary = Color.White,
    onSecondary = Color.White,
    onTertiary = Color.White,
    onBackground = Color(0xFF1C1B1F),
    onSurface = Color(0xFF1C1B1F),
    */
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

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
