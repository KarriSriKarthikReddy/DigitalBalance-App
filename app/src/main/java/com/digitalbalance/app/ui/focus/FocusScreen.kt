package com.digitalbalance.app.ui.focus

import android.content.Context
import android.content.ContextWrapper
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.digitalbalance.app.R
import com.digitalbalance.app.domain.focus.FocusDurationPolicy
import com.digitalbalance.app.domain.focus.FocusPreset
import com.digitalbalance.app.domain.focus.FocusProgress
import com.digitalbalance.app.domain.focus.FocusReflection
import com.digitalbalance.app.domain.focus.FocusSession
import com.digitalbalance.app.domain.focus.FocusSessionStatus
import com.digitalbalance.app.ui.components.LoadingContent
import com.digitalbalance.app.ui.components.PremiumCard
import com.digitalbalance.app.ui.components.ScreenHeader
import com.digitalbalance.app.ui.components.StatusPill
import com.digitalbalance.app.ui.components.CompanionPose
import com.digitalbalance.app.ui.components.WellbeingCompanion
import com.digitalbalance.app.ui.theme.DigitalBalanceSpacing
import com.digitalbalance.app.ui.theme.digitalBalanceColors
import java.text.DateFormat
import java.util.Date
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.LifecycleOwner
import kotlinx.coroutines.delay

@Composable
fun FocusScreen(
    state: FocusUiState,
    onSelectPreset: (FocusPreset) -> Unit,
    onSelectDuration: (Int) -> Unit,
    onSelectCustomDuration: () -> Unit,
    onCustomDurationChanged: (String) -> Unit,
    onStart: () -> Unit,
    onPause: () -> Unit,
    onResume: () -> Unit,
    onStopEarly: () -> Unit,
    onReflectionChanged: (FocusReflection?) -> Unit,
    onDone: () -> Unit,
    onStartAnother: () -> Unit,
    onTick: () -> Unit,
    modifier: Modifier = Modifier
) {
    when (state) {
        FocusUiState.Loading -> Box(modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            LoadingContent()
        }
        is FocusUiState.Idle -> IdleFocusScreen(
            state, onSelectPreset, onSelectDuration, onSelectCustomDuration,
            onCustomDurationChanged, onStart, modifier
        )
        is FocusUiState.Active -> ActiveFocusScreen(
            state, onPause, onResume, onStopEarly, onTick, modifier
        )
        is FocusUiState.Summary -> FocusSummaryScreen(
            state, onReflectionChanged, onDone, onStartAnother, modifier
        )
    }
}

@Composable
private fun IdleFocusScreen(
    state: FocusUiState.Idle,
    onSelectPreset: (FocusPreset) -> Unit,
    onSelectDuration: (Int) -> Unit,
    onSelectCustomDuration: () -> Unit,
    onCustomDurationChanged: (String) -> Unit,
    onStart: () -> Unit,
    modifier: Modifier
) {
    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(
            horizontal = DigitalBalanceSpacing.screen,
            vertical = DigitalBalanceSpacing.section
        ),
        verticalArrangement = Arrangement.spacedBy(18.dp)
    ) {
        item {
            ScreenHeader(
                title = stringResource(R.string.focus_idle_title),
                subtitle = stringResource(R.string.focus_idle_description)
            )
        }
        item {
            Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                WellbeingCompanion(
                    pose = CompanionPose.Meditation,
                    modifier = Modifier.size(156.dp)
                )
            }
        }
        item {
            Text(stringResource(R.string.focus_choose_intention), style = MaterialTheme.typography.titleLarge)
            Spacer(Modifier.height(10.dp))
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                FocusPreset.entries.chunked(2).forEach { pair ->
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        pair.forEach { preset ->
                            PresetCard(
                                preset = preset,
                                selected = state.setup.preset == preset,
                                onClick = { onSelectPreset(preset) },
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                }
            }
        }
        item {
            Text(stringResource(R.string.focus_choose_duration), style = MaterialTheme.typography.titleLarge)
            LazyRow(
                contentPadding = PaddingValues(top = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(FocusDurationPolicy.suggestedMinutes) { minutes ->
                    FilterChip(
                        selected = !state.setup.customSelected && state.setup.selectedMinutes == minutes,
                        onClick = { onSelectDuration(minutes) },
                        label = { Text(stringResource(R.string.focus_minutes, minutes)) }
                    )
                }
                item {
                    FilterChip(
                        selected = state.setup.customSelected,
                        onClick = onSelectCustomDuration,
                        label = { Text(stringResource(R.string.focus_custom)) }
                    )
                }
            }
            if (state.setup.customSelected) {
                OutlinedTextField(
                    value = state.setup.customMinutesText,
                    onValueChange = onCustomDurationChanged,
                    modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                    label = { Text(stringResource(R.string.focus_custom_minutes)) },
                    supportingText = { Text(stringResource(R.string.focus_custom_range)) },
                    isError = state.setup.customMinutesText.isNotEmpty() && !state.setup.canStart,
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                )
            }
        }
        item {
            Button(
                onClick = onStart,
                enabled = state.setup.canStart,
                modifier = Modifier.fillMaxWidth().height(52.dp)
            ) {
                Text(
                    state.setup.durationMinutes?.let {
                        stringResource(R.string.focus_start_minutes, it)
                    } ?: stringResource(R.string.focus_start)
                )
            }
        }
        if (state.history.isNotEmpty()) item { FocusHistory(state.history) }
    }
}

@Composable
private fun PresetCard(
    preset: FocusPreset,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    androidx.compose.material3.Card(
        onClick = onClick,
        modifier = modifier,
        colors = androidx.compose.material3.CardDefaults.cardColors(
            containerColor = if (selected) {
                MaterialTheme.colorScheme.primaryContainer
            } else {
                MaterialTheme.colorScheme.surfaceContainerLow
            }
        )
    ) {
        Column(Modifier.padding(15.dp), verticalArrangement = Arrangement.spacedBy(3.dp)) {
            Text(stringResource(preset.labelRes), style = MaterialTheme.typography.titleMedium)
            Text(
                stringResource(preset.descriptionRes),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                minLines = 2
            )
        }
    }
}

@Composable
private fun ActiveFocusScreen(
    state: FocusUiState.Active,
    onPause: () -> Unit,
    onResume: () -> Unit,
    onStopEarly: () -> Unit,
    onTick: () -> Unit,
    modifier: Modifier
) {
    var confirmStop by remember { mutableStateOf(false) }
    val running = state.session.status == FocusSessionStatus.Running
    val context = LocalContext.current
    val lifecycleOwner = remember(context) { context.findLifecycleOwner() }
    var isResumed by remember(lifecycleOwner) {
        mutableStateOf(
            lifecycleOwner?.lifecycle?.currentState?.isAtLeast(Lifecycle.State.RESUMED) != false
        )
    }
    DisposableEffect(lifecycleOwner) {
        if (lifecycleOwner == null) return@DisposableEffect onDispose { }
        val observer = LifecycleEventObserver { _, _ ->
            isResumed = lifecycleOwner.lifecycle.currentState.isAtLeast(Lifecycle.State.RESUMED)
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }
    LaunchedEffect(state.session.id, running, isResumed) {
        while (running && isResumed) {
            onTick()
            delay(TICK_INTERVAL_MILLIS)
        }
    }
    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(horizontal = 24.dp, vertical = 26.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(18.dp)
    ) {
        item {
            StatusPill(
                text = stringResource(if (running) R.string.focus_running else R.string.focus_paused),
                color = if (running) MaterialTheme.colorScheme.primary else MaterialTheme.digitalBalanceColors.warning
            )
        }
        item {
            Text(
                text = stringResource(state.session.preset.labelRes),
                style = MaterialTheme.typography.headlineMedium,
                textAlign = TextAlign.Center
            )
            Text(
                text = stringResource(R.string.focus_selected_intention),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        item { FocusCountdownRing(state.progress) }
        item {
            Text(
                stringResource(
                    R.string.focus_elapsed_of_planned,
                    focusDuration(state.progress.actualFocusedMillis),
                    focusDuration(state.session.plannedDurationMillis)
                ),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        item {
            Button(
                onClick = if (running) onPause else onResume,
                modifier = Modifier.fillMaxWidth().height(52.dp)
            ) {
                Text(stringResource(if (running) R.string.focus_pause else R.string.focus_resume))
            }
            OutlinedButton(
                onClick = { confirmStop = true },
                modifier = Modifier.fillMaxWidth().padding(top = 10.dp)
            ) { Text(stringResource(R.string.focus_stop)) }
        }
        if (state.history.isNotEmpty()) item { FocusHistory(state.history) }
    }
    if (confirmStop) {
        AlertDialog(
            onDismissRequest = { confirmStop = false },
            title = { Text(stringResource(R.string.focus_stop_confirm_title)) },
            text = { Text(stringResource(R.string.focus_stop_confirm_description)) },
            confirmButton = {
                TextButton(onClick = {
                    confirmStop = false
                    onStopEarly()
                }) { Text(stringResource(R.string.focus_end_session)) }
            },
            dismissButton = {
                TextButton(onClick = { confirmStop = false }) { Text(stringResource(R.string.cancel)) }
            }
        )
    }
}

@Composable
private fun FocusCountdownRing(progress: FocusProgress) {
    val animated by animateFloatAsState(1f - progress.progressFraction, tween(450), label = "focusCountdown")
    val primary = MaterialTheme.colorScheme.primary
    val track = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.55f)
    val remaining = countdownDuration(progress.remainingMillis)
    val accessibilityLabel = stringResource(R.string.focus_remaining_accessibility, remaining)
    Box(
        modifier = Modifier.size(250.dp).semantics {
            contentDescription = accessibilityLabel
        },
        contentAlignment = Alignment.Center
    ) {
        Canvas(Modifier.matchParentSize().padding(12.dp)) {
            val width = 16.dp.toPx()
            drawCircle(track, style = Stroke(width))
            drawArc(
                color = primary,
                startAngle = -90f,
                sweepAngle = 360f * animated,
                useCenter = false,
                style = Stroke(width, cap = StrokeCap.Round)
            )
        }
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(remaining, style = MaterialTheme.typography.displayMedium, fontWeight = FontWeight.SemiBold)
            Text(
                stringResource(R.string.focus_remaining),
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun FocusSummaryScreen(
    state: FocusUiState.Summary,
    onReflectionChanged: (FocusReflection?) -> Unit,
    onDone: () -> Unit,
    onStartAnother: () -> Unit,
    modifier: Modifier
) {
    val completed = state.session.status == FocusSessionStatus.Completed
    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(
            horizontal = DigitalBalanceSpacing.screen,
            vertical = DigitalBalanceSpacing.section
        ),
        verticalArrangement = Arrangement.spacedBy(18.dp)
    ) {
        item {
            ScreenHeader(
                title = if (completed) {
                    stringResource(
                        R.string.focus_complete_title,
                        state.session.plannedDurationMillis / MILLIS_PER_MINUTE,
                        stringResource(state.session.preset.labelRes)
                    )
                } else {
                    stringResource(R.string.focus_ended_title)
                },
                subtitle = stringResource(
                    if (completed) R.string.focus_complete_description else R.string.focus_ended_description
                )
            )
        }
        item {
            PremiumCard(Modifier.fillMaxWidth()) {
                Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    SummaryMetric(stringResource(R.string.focus_intention), stringResource(state.session.preset.labelRes))
                    SummaryMetric(stringResource(R.string.focus_planned), focusDuration(state.session.plannedDurationMillis))
                    SummaryMetric(stringResource(R.string.focus_actual), focusDuration(state.session.accumulatedFocusedMillis))
                    StatusPill(
                        text = stringResource(if (completed) R.string.focus_completed else R.string.focus_stopped_early),
                        color = if (completed) MaterialTheme.digitalBalanceColors.positive else MaterialTheme.digitalBalanceColors.informational
                    )
                }
            }
        }
        item {
            Text(stringResource(R.string.focus_reflection_title), style = MaterialTheme.typography.titleLarge)
            Text(
                stringResource(R.string.focus_reflection_description),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                items(FocusReflection.entries) { reflection ->
                    FilterChip(
                        selected = state.session.reflection == reflection,
                        onClick = {
                            onReflectionChanged(
                                reflection.takeUnless { state.session.reflection == reflection }
                            )
                        },
                        label = { Text(stringResource(reflection.labelRes)) }
                    )
                }
            }
        }
        item {
            Button(onClick = onDone, modifier = Modifier.fillMaxWidth()) {
                Text(stringResource(R.string.done))
            }
            TextButton(onClick = onStartAnother, modifier = Modifier.fillMaxWidth()) {
                Text(stringResource(R.string.focus_start_another))
            }
        }
        if (state.history.isNotEmpty()) item { FocusHistory(state.history) }
    }
}

@Composable
private fun SummaryMetric(label: String, value: String) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        Text(label, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(value, fontWeight = FontWeight.SemiBold)
    }
}

@Composable
private fun FocusHistory(sessions: List<FocusSession>) {
    val dateFormatter = remember {
        DateFormat.getDateTimeInstance(DateFormat.MEDIUM, DateFormat.SHORT)
    }
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Text(stringResource(R.string.focus_recent_sessions), style = MaterialTheme.typography.titleLarge)
        PremiumCard(Modifier.fillMaxWidth()) {
            Column(Modifier.padding(horizontal = 18.dp, vertical = 8.dp)) {
                sessions.take(5).forEachIndexed { index, session ->
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(vertical = 11.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(Modifier.weight(1f)) {
                            Text(stringResource(session.preset.labelRes), style = MaterialTheme.typography.titleSmall)
                            Text(
                                dateFormatter.format(Date(session.startedAtEpochMillis)),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Column(horizontalAlignment = Alignment.End) {
                            Text(focusDuration(session.accumulatedFocusedMillis), fontWeight = FontWeight.SemiBold)
                            Text(
                                stringResource(
                                    if (session.status == FocusSessionStatus.Completed) {
                                        R.string.focus_completed
                                    } else {
                                        R.string.focus_stopped_early
                                    }
                                ),
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                    if (index < minOf(4, sessions.lastIndex)) {
                        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                    }
                }
            }
        }
    }
}

private val FocusPreset.labelRes: Int get() = when (this) {
    FocusPreset.Focus -> R.string.focus_preset_focus
    FocusPreset.Study -> R.string.focus_preset_study
    FocusPreset.Work -> R.string.focus_preset_work
    FocusPreset.Mindfulness -> R.string.focus_preset_mindfulness
}

private val FocusPreset.descriptionRes: Int get() = when (this) {
    FocusPreset.Focus -> R.string.focus_preset_focus_description
    FocusPreset.Study -> R.string.focus_preset_study_description
    FocusPreset.Work -> R.string.focus_preset_work_description
    FocusPreset.Mindfulness -> R.string.focus_preset_mindfulness_description
}

private val FocusReflection.labelRes: Int get() = when (this) {
    FocusReflection.Focused -> R.string.focus_reflection_focused
    FocusReflection.Okay -> R.string.focus_reflection_okay
    FocusReflection.Distracted -> R.string.focus_reflection_distracted
}

private fun countdownDuration(millis: Long): String {
    val totalSeconds = ((millis.coerceAtLeast(0L) + 999L) / 1_000L)
    val hours = totalSeconds / 3_600L
    val minutes = (totalSeconds % 3_600L) / 60L
    val seconds = totalSeconds % 60L
    return if (hours > 0L) "%d:%02d:%02d".format(hours, minutes, seconds)
    else "%02d:%02d".format(minutes, seconds)
}

private fun focusDuration(millis: Long): String {
    val totalMinutes = millis.coerceAtLeast(0L) / MILLIS_PER_MINUTE
    return when {
        totalMinutes < 1L -> "<1 min"
        totalMinutes < 60L -> "$totalMinutes min"
        totalMinutes % 60L == 0L -> "${totalMinutes / 60L}h"
        else -> "${totalMinutes / 60L}h ${totalMinutes % 60L}m"
    }
}

private fun Context.findLifecycleOwner(): LifecycleOwner? {
    var current: Context? = this
    while (current is ContextWrapper) {
        if (current is LifecycleOwner) return current
        val base = current.baseContext
        if (base === current) break
        current = base
    }
    return current as? LifecycleOwner
}

private const val TICK_INTERVAL_MILLIS = 1_000L
private const val MILLIS_PER_MINUTE = 60_000L
