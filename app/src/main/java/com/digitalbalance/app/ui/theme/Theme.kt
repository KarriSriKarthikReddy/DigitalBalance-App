package com.digitalbalance.app.ui.theme

import android.app.Activity
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.unit.dp
import androidx.core.view.WindowCompat

private val DarkColorScheme = darkColorScheme(
    primary = Teal300, onPrimary = Navy950,
    primaryContainer = Color(0xFF124C4B), onPrimaryContainer = Color(0xFFB5F1E9),
    secondary = BlueLight, onSecondary = Navy950,
    secondaryContainer = Color(0xFF203E50), onSecondaryContainer = Color(0xFFD5E9F8),
    tertiary = AmberLight, onTertiary = Color(0xFF332300),
    tertiaryContainer = Color(0xFF4C3B16), onTertiaryContainer = Color(0xFFFFDEA0),
    error = ErrorLight, errorContainer = Color(0xFF73342F), onErrorContainer = Color(0xFFFFDAD6),
    background = Navy950, onBackground = Color(0xFFDCE6E7),
    surface = Navy950, onSurface = Color(0xFFDCE6E7),
    surfaceVariant = Slate800, onSurfaceVariant = Slate300,
    surfaceContainerLowest = Color(0xFF06141B), surfaceContainerLow = Navy900,
    surfaceContainer = Slate850, surfaceContainerHigh = Slate800,
    surfaceContainerHighest = Slate700, outline = Color(0xFF789098),
    outlineVariant = Color(0xFF29434B)
)

private val LightColorScheme = lightColorScheme(
    primary = Teal700, onPrimary = Color.White,
    primaryContainer = Color(0xFFC0F2EA), onPrimaryContainer = Color(0xFF002F2D),
    secondary = BlueDark, onSecondary = Color.White,
    secondaryContainer = Color(0xFFD6EAF8), onSecondaryContainer = Color(0xFF102F42),
    tertiary = AmberDark, onTertiary = Color.White,
    tertiaryContainer = Color(0xFFFFE6B5), onTertiaryContainer = Color(0xFF332300),
    error = ErrorDark, errorContainer = Color(0xFFFFDAD6), onErrorContainer = Color(0xFF410002),
    background = Neutral50, onBackground = Color(0xFF182124),
    surface = Neutral50, onSurface = Color(0xFF182124),
    surfaceVariant = Slate100, onSurfaceVariant = Color(0xFF46595F),
    surfaceContainerLowest = Color.White, surfaceContainerLow = Color(0xFFF1F5F3),
    surfaceContainer = Color(0xFFEBF0EE), surfaceContainerHigh = Color(0xFFE4EAE8),
    surfaceContainerHighest = Color(0xFFDDE5E3), outline = Color(0xFF697C82),
    outlineVariant = Color(0xFFC3CFD1)
)

private val AppShapes = Shapes(
    extraSmall = RoundedCornerShape(8.dp), small = RoundedCornerShape(12.dp),
    medium = RoundedCornerShape(16.dp), large = RoundedCornerShape(20.dp),
    extraLarge = RoundedCornerShape(28.dp)
)

val MaterialTheme.digitalBalanceColors: DigitalBalanceSemanticColors
    @Composable @ReadOnlyComposable get() = LocalDigitalBalanceColors.current

@Composable
fun DigitalBalanceTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme
    val semanticColors = if (darkTheme) {
        DigitalBalanceSemanticColors(
            GreenLight, VioletLight, AmberLight, PurpleLight, BlueLight, Teal300,
            Slate300, GreenLight, AmberLight, ErrorLight, BlueLight
        )
    } else {
        DigitalBalanceSemanticColors(
            GreenDark, VioletDark, AmberDark, PurpleDark, BlueDark, Teal700,
            Slate700, GreenDark, AmberDark, ErrorDark, BlueDark
        )
    }
    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            window.statusBarColor = Color.Transparent.toArgb()
            window.navigationBarColor = colorScheme.background.toArgb()
            WindowCompat.getInsetsController(window, view).apply {
                isAppearanceLightStatusBars = !darkTheme
                isAppearanceLightNavigationBars = !darkTheme
            }
        }
    }
    CompositionLocalProvider(LocalDigitalBalanceColors provides semanticColors) {
        MaterialTheme(
            colorScheme = colorScheme,
            shapes = AppShapes,
            typography = Typography,
            content = content
        )
    }
}
