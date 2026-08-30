package com.digitalbalance.app.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.semantics.clearAndSetSemantics
import com.digitalbalance.app.ui.theme.digitalBalanceColors

enum class CompanionPose {
    Meditation,
    Perched
}

@Composable
fun WellbeingCompanion(
    pose: CompanionPose,
    modifier: Modifier = Modifier
) {
    val scheme = MaterialTheme.colorScheme
    val semantic = MaterialTheme.digitalBalanceColors
    Canvas(modifier.clearAndSetSemantics { }) {
        val palette = CompanionPalette(
            shell = scheme.primaryContainer,
            shellStroke = scheme.primary,
            face = scheme.surfaceContainerLowest,
            feature = scheme.onSurface,
            accent = semantic.productive,
            secondary = scheme.secondary,
            shadow = scheme.onSurface.copy(alpha = 0.10f),
            halo = scheme.primary.copy(alpha = 0.08f)
        )
        when (pose) {
            CompanionPose.Meditation -> drawMeditatingCompanion(palette)
            CompanionPose.Perched -> drawPerchedCompanion(palette)
        }
    }
}

private data class CompanionPalette(
    val shell: androidx.compose.ui.graphics.Color,
    val shellStroke: androidx.compose.ui.graphics.Color,
    val face: androidx.compose.ui.graphics.Color,
    val feature: androidx.compose.ui.graphics.Color,
    val accent: androidx.compose.ui.graphics.Color,
    val secondary: androidx.compose.ui.graphics.Color,
    val shadow: androidx.compose.ui.graphics.Color,
    val halo: androidx.compose.ui.graphics.Color
)

private fun DrawScope.drawMeditatingCompanion(palette: CompanionPalette) {
    val grid = CompanionGrid(size)
    drawCircle(palette.halo, radius = grid.u(46f), center = grid.p(50f, 50f))
    drawOval(
        color = palette.shadow,
        topLeft = grid.p(19f, 82f),
        size = grid.s(62f, 9f)
    )

    drawLine(
        color = palette.shellStroke,
        start = grid.p(50f, 20f),
        end = grid.p(50f, 13f),
        strokeWidth = grid.u(2.2f),
        cap = StrokeCap.Round
    )
    drawCircle(palette.accent, radius = grid.u(3f), center = grid.p(50f, 10f))

    drawRoundRect(
        color = palette.shell,
        topLeft = grid.p(28f, 20f),
        size = grid.s(44f, 31f),
        cornerRadius = CornerRadius(grid.u(11f))
    )
    drawRoundRect(
        color = palette.shellStroke.copy(alpha = 0.55f),
        topLeft = grid.p(28f, 20f),
        size = grid.s(44f, 31f),
        cornerRadius = CornerRadius(grid.u(11f)),
        style = Stroke(grid.u(1.6f))
    )
    drawRoundRect(
        color = palette.face,
        topLeft = grid.p(34f, 27f),
        size = grid.s(32f, 17f),
        cornerRadius = CornerRadius(grid.u(7f))
    )
    drawLine(palette.feature, grid.p(40f, 35f), grid.p(45f, 35f), grid.u(2f), StrokeCap.Round)
    drawLine(palette.feature, grid.p(55f, 35f), grid.p(60f, 35f), grid.u(2f), StrokeCap.Round)

    drawRoundRect(
        color = palette.shell,
        topLeft = grid.p(38f, 51f),
        size = grid.s(24f, 27f),
        cornerRadius = CornerRadius(grid.u(9f))
    )
    drawRoundRect(
        color = palette.shellStroke.copy(alpha = 0.5f),
        topLeft = grid.p(38f, 51f),
        size = grid.s(24f, 27f),
        cornerRadius = CornerRadius(grid.u(9f)),
        style = Stroke(grid.u(1.5f))
    )
    drawPulse(grid, palette)

    val arms = Path().apply {
        moveTo(grid.x(39f), grid.y(57f))
        cubicTo(grid.x(28f), grid.y(60f), grid.x(33f), grid.y(72f), grid.x(47f), grid.y(72f))
        moveTo(grid.x(61f), grid.y(57f))
        cubicTo(grid.x(72f), grid.y(60f), grid.x(67f), grid.y(72f), grid.x(53f), grid.y(72f))
    }
    drawPath(arms, palette.secondary, style = Stroke(grid.u(4f), cap = StrokeCap.Round))
    drawCircle(palette.accent, grid.u(2.5f), grid.p(47f, 72f))
    drawCircle(palette.accent, grid.u(2.5f), grid.p(53f, 72f))

    val legs = Path().apply {
        moveTo(grid.x(43f), grid.y(75f))
        cubicTo(grid.x(39f), grid.y(82f), grid.x(29f), grid.y(84f), grid.x(24f), grid.y(80f))
        moveTo(grid.x(57f), grid.y(75f))
        cubicTo(grid.x(61f), grid.y(82f), grid.x(71f), grid.y(84f), grid.x(76f), grid.y(80f))
        moveTo(grid.x(24f), grid.y(80f))
        cubicTo(grid.x(32f), grid.y(76f), grid.x(40f), grid.y(78f), grid.x(47f), grid.y(82f))
        moveTo(grid.x(76f), grid.y(80f))
        cubicTo(grid.x(68f), grid.y(76f), grid.x(60f), grid.y(78f), grid.x(53f), grid.y(82f))
    }
    drawPath(legs, palette.shellStroke, style = Stroke(grid.u(6f), cap = StrokeCap.Round))
}

private fun DrawScope.drawPerchedCompanion(palette: CompanionPalette) {
    val grid = CompanionGrid(size)
    drawCircle(palette.halo, radius = grid.u(39f), center = grid.p(50f, 48f))
    drawLine(palette.shellStroke, grid.p(50f, 18f), grid.p(50f, 11f), grid.u(2.4f), StrokeCap.Round)
    drawCircle(palette.accent, grid.u(3.2f), grid.p(50f, 8f))
    drawRoundRect(
        color = palette.shell,
        topLeft = grid.p(25f, 18f),
        size = grid.s(50f, 34f),
        cornerRadius = CornerRadius(grid.u(12f))
    )
    drawRoundRect(
        color = palette.shellStroke.copy(alpha = 0.55f),
        topLeft = grid.p(25f, 18f),
        size = grid.s(50f, 34f),
        cornerRadius = CornerRadius(grid.u(12f)),
        style = Stroke(grid.u(1.8f))
    )
    drawRoundRect(
        color = palette.face,
        topLeft = grid.p(32f, 26f),
        size = grid.s(36f, 18f),
        cornerRadius = CornerRadius(grid.u(7f))
    )
    drawCircle(palette.feature, grid.u(2.2f), grid.p(42f, 35f))
    drawLine(palette.feature, grid.p(56f, 35f), grid.p(61f, 35f), grid.u(2.1f), StrokeCap.Round)
    drawRoundRect(
        color = palette.shell,
        topLeft = grid.p(36f, 52f),
        size = grid.s(28f, 24f),
        cornerRadius = CornerRadius(grid.u(9f))
    )
    drawPulse(grid, palette, centerY = 63f)
    drawLine(palette.secondary, grid.p(36f, 58f), grid.p(27f, 67f), grid.u(4f), StrokeCap.Round)
    drawLine(palette.secondary, grid.p(64f, 58f), grid.p(71f, 66f), grid.u(4f), StrokeCap.Round)
    drawLine(palette.shellStroke, grid.p(43f, 75f), grid.p(39f, 91f), grid.u(5f), StrokeCap.Round)
    drawLine(palette.shellStroke, grid.p(57f, 75f), grid.p(62f, 91f), grid.u(5f), StrokeCap.Round)
    drawCircle(palette.accent, grid.u(3f), grid.p(38f, 92f))
    drawCircle(palette.accent, grid.u(3f), grid.p(63f, 92f))
}

private fun DrawScope.drawPulse(
    grid: CompanionGrid,
    palette: CompanionPalette,
    centerY: Float = 64f
) {
    val path = Path().apply {
        moveTo(grid.x(43f), grid.y(centerY))
        lineTo(grid.x(47f), grid.y(centerY))
        lineTo(grid.x(50f), grid.y(centerY - 4f))
        lineTo(grid.x(53f), grid.y(centerY + 4f))
        lineTo(grid.x(56f), grid.y(centerY))
        lineTo(grid.x(59f), grid.y(centerY))
    }
    drawPath(path, palette.accent, style = Stroke(grid.u(1.8f), cap = StrokeCap.Round))
}

private class CompanionGrid(size: Size) {
    private val unit = size.minDimension / 100f
    private val originX = (size.width - size.minDimension) / 2f
    private val originY = (size.height - size.minDimension) / 2f

    fun u(value: Float) = value * unit
    fun x(value: Float) = originX + u(value)
    fun y(value: Float) = originY + u(value)
    fun p(x: Float, y: Float) = Offset(this.x(x), this.y(y))
    fun s(width: Float, height: Float) = Size(u(width), u(height))
}
