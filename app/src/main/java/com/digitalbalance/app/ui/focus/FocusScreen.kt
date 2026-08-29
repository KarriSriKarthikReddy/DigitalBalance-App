package com.digitalbalance.app.ui.focus

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.digitalbalance.app.R
import com.digitalbalance.app.ui.components.PremiumCard
import com.digitalbalance.app.ui.components.StatusPill
import com.digitalbalance.app.ui.theme.digitalBalanceColors

@Composable
fun FocusScreen(modifier: Modifier = Modifier) {
    val primary = MaterialTheme.colorScheme.primary
    val secondary = MaterialTheme.colorScheme.secondary
    Box(
        modifier = modifier.fillMaxSize().padding(24.dp),
        contentAlignment = Alignment.Center
    ) {
        PremiumCard(Modifier.widthIn(max = 480.dp)) {
            Column(
                modifier = Modifier.padding(horizontal = 28.dp, vertical = 36.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Canvas(Modifier.size(150.dp)) {
                    val stroke = 7.dp.toPx()
                    drawCircle(primary.copy(alpha = 0.12f), style = Stroke(stroke))
                    drawArc(primary, -90f, 225f, false, style = Stroke(stroke, cap = StrokeCap.Round))
                    drawArc(secondary, 148f, 112f, false, style = Stroke(stroke, cap = StrokeCap.Round))
                    val y = size.height / 2f
                    val path = androidx.compose.ui.graphics.Path().apply {
                        moveTo(size.width * .25f, y)
                        lineTo(size.width * .38f, y)
                        lineTo(size.width * .45f, y - size.height * .12f)
                        lineTo(size.width * .54f, y + size.height * .14f)
                        lineTo(size.width * .62f, y)
                        lineTo(size.width * .75f, y)
                    }
                    drawPath(path, primary, style = Stroke(4.dp.toPx(), cap = StrokeCap.Round))
                }
                Spacer(Modifier.height(20.dp))
                StatusPill(stringResource(R.string.focus_status), MaterialTheme.digitalBalanceColors.informational)
                Text(
                    text = stringResource(R.string.focus_title),
                    modifier = Modifier.padding(top = 16.dp),
                    style = MaterialTheme.typography.headlineMedium,
                    textAlign = TextAlign.Center
                )
                Text(
                    text = stringResource(R.string.focus_description),
                    modifier = Modifier.padding(top = 10.dp),
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center
                )
            }
        }
    }
}
