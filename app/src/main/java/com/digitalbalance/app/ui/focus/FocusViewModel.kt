package com.digitalbalance.app.ui.focus

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.digitalbalance.app.data.focus.AndroidFocusClock
import com.digitalbalance.app.data.focus.FocusClock
import com.digitalbalance.app.data.repository.FocusRepository
import com.digitalbalance.app.domain.focus.FocusDurationPolicy
import com.digitalbalance.app.domain.focus.FocusLaunchSuggestion
import com.digitalbalance.app.domain.focus.FocusPreset
import com.digitalbalance.app.domain.focus.FocusProgress
import com.digitalbalance.app.domain.focus.FocusReflection
import com.digitalbalance.app.domain.focus.FocusSession
import com.digitalbalance.app.domain.focus.FocusSessionEngine
import com.digitalbalance.app.domain.focus.FocusSessionStatus
import com.digitalbalance.app.domain.focus.FocusTimeSnapshot
import java.util.UUID
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class FocusSetupState(
    val preset: FocusPreset = FocusPreset.Focus,
    val selectedMinutes: Int? = 25,
    val customMinutesText: String = "",
    val customSelected: Boolean = false
) {
    val durationMinutes: Int?
        get() = if (customSelected) customMinutesText.toIntOrNull() else selectedMinutes
    val canStart: Boolean
        get() = if (customSelected) {
            FocusDurationPolicy.isValidCustomMinutes(durationMinutes)
        } else {
            durationMinutes in FocusDurationPolicy.suggestedMinutes
        }
}

sealed interface FocusUiState {
    data object Loading : FocusUiState
    data class Idle(
        val setup: FocusSetupState,
        val history: List<FocusSession>
    ) : FocusUiState
    data class Active(
        val session: FocusSession,
        val progress: FocusProgress,
        val history: List<FocusSession>
    ) : FocusUiState
    data class Summary(
        val session: FocusSession,
        val history: List<FocusSession>
    ) : FocusUiState
}

class FocusViewModel(
    private val repository: FocusRepository,
    private val clock: FocusClock = AndroidFocusClock(),
    private val engine: FocusSessionEngine = FocusSessionEngine(),
    private val idFactory: () -> String = { UUID.randomUUID().toString() }
) : ViewModel() {
    private val setup = MutableStateFlow(FocusSetupState())
    private val clockTick = MutableStateFlow(clock.snapshot())
    private val summarySessionId = MutableStateFlow<String?>(null)
    private val restoring = MutableStateFlow(true)
    private var completionInFlight = false
    private var startInFlight = false

    private val sessions = repository.observeSessions().stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5_000),
        emptyList()
    )

    val uiState: StateFlow<FocusUiState> = combine(
        sessions,
        setup,
        clockTick,
        summarySessionId,
        restoring
    ) { allSessions, currentSetup, now, summaryId, isRestoring ->
        if (isRestoring) return@combine FocusUiState.Loading
        val history = allSessions.filter { it.status.isFinished }.take(HISTORY_LIMIT)
        val active = allSessions.firstOrNull {
            it.status == FocusSessionStatus.Running || it.status == FocusSessionStatus.Paused
        }
        when {
            active != null -> FocusUiState.Active(active, engine.progress(active, now), history)
            summaryId != null -> allSessions.firstOrNull { it.id == summaryId }?.let {
                FocusUiState.Summary(it, history)
            } ?: FocusUiState.Idle(currentSetup, history)
            else -> FocusUiState.Idle(currentSetup, history)
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), FocusUiState.Loading)

    init {
        viewModelScope.launch {
            val restored = repository.reconcileActive(clock.snapshot())
            if (restored?.status?.isFinished == true) summarySessionId.value = restored.id
            restoring.value = false
            tick()
        }
    }

    fun selectPreset(preset: FocusPreset) {
        setup.value = setup.value.copy(preset = preset)
    }

    fun selectSuggestedDuration(minutes: Int) {
        if (minutes !in FocusDurationPolicy.suggestedMinutes) return
        setup.value = setup.value.copy(
            selectedMinutes = minutes,
            customSelected = false,
            customMinutesText = ""
        )
    }

    fun selectCustomDuration() {
        setup.value = setup.value.copy(customSelected = true, selectedMinutes = null)
    }

    fun setCustomDuration(value: String) {
        if (value.length > 3 || !value.all(Char::isDigit)) return
        setup.value = setup.value.copy(customSelected = true, customMinutesText = value)
    }

    fun applySuggestion(suggestion: FocusLaunchSuggestion) {
        val hasActive = sessions.value.any {
            it.status == FocusSessionStatus.Running || it.status == FocusSessionStatus.Paused
        }
        if (hasActive) return
        setup.value = setup.value.copy(
            preset = suggestion.preset,
            selectedMinutes = suggestion.durationMinutes,
            customSelected = false,
            customMinutesText = ""
        )
        summarySessionId.value = null
    }

    fun start() {
        val current = setup.value
        val minutes = current.durationMinutes?.takeIf { current.canStart } ?: return
        if (startInFlight) return
        startInFlight = true
        summarySessionId.value = null
        viewModelScope.launch {
            try {
                repository.start(
                    id = idFactory(),
                    preset = current.preset,
                    durationMillis = minutes * MILLIS_PER_MINUTE,
                    now = clock.snapshot()
                )
                tick()
            } finally {
                startInFlight = false
            }
        }
    }

    fun pause() = mutate { repository.pause(it) }

    fun resume() = mutate { repository.resume(it) }

    fun stopEarly() {
        viewModelScope.launch {
            repository.stopEarly(clock.snapshot())?.let { summarySessionId.value = it.id }
            tick()
        }
    }

    fun setReflection(reflection: FocusReflection?) {
        val summary = (uiState.value as? FocusUiState.Summary)?.session ?: return
        viewModelScope.launch {
            repository.setReflection(summary, reflection, clock.snapshot())
        }
    }

    fun done() {
        summarySessionId.value = null
    }

    fun startAnother() {
        summarySessionId.value = null
    }

    fun tick() {
        val now = clock.snapshot()
        clockTick.value = now
        val active = sessions.value.firstOrNull { it.status == FocusSessionStatus.Running } ?: return
        if (engine.progress(active, now).remainingMillis > 0L || completionInFlight) return
        completionInFlight = true
        viewModelScope.launch {
            try {
                repository.reconcileActive(now)?.let { completed ->
                    if (completed.status == FocusSessionStatus.Completed) {
                        summarySessionId.value = completed.id
                    }
                }
            } finally {
                completionInFlight = false
                clockTick.value = clock.snapshot()
            }
        }
    }

    private fun mutate(operation: suspend (FocusTimeSnapshot) -> FocusSession?) {
        viewModelScope.launch {
            operation(clock.snapshot())?.let { updated ->
                if (updated.status.isFinished) summarySessionId.value = updated.id
            }
            tick()
        }
    }

    companion object {
        private const val MILLIS_PER_MINUTE = 60_000L
        private const val HISTORY_LIMIT = 10

        fun factory(repository: FocusRepository): ViewModelProvider.Factory =
            object : ViewModelProvider.Factory {
                @Suppress("UNCHECKED_CAST")
                override fun <T : ViewModel> create(modelClass: Class<T>): T =
                    FocusViewModel(repository) as T
            }
    }
}
