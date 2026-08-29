package com.digitalbalance.app.ui.theme

import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

object DigitalBalanceSpacing {
    val screen = 20.dp
    val section = 22.dp
    val card = 18.dp
    val item = 12.dp
    val compact = 8.dp
}

@Immutable
data class DigitalBalanceSemanticColors(
    val productive: Color,
    val social: Color,
    val entertainment: Color,
    val gaming: Color,
    val communication: Color,
    val utility: Color,
    val mixed: Color,
    val positive: Color,
    val warning: Color,
    val exceeded: Color,
    val informational: Color
)

internal val LocalDigitalBalanceColors = staticCompositionLocalOf {
    DigitalBalanceSemanticColors(
        GreenDark, VioletDark, AmberDark, PurpleDark, BlueDark, Teal700,
        Slate700, GreenDark, AmberDark, ErrorDark, BlueDark
    )
}
