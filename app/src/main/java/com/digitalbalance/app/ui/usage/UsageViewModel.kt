package com.digitalbalance.app.ui.usage

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.digitalbalance.app.data.usage.AppUsage
import com.digitalbalance.app.data.repository.UsageRepository
import com.digitalbalance.app.data.repository.GoalRepository
import com.digitalbalance.app.domain.category.AppCategory
import com.digitalbalance.app.domain.category.categoryWithOverride
import com.digitalbalance.app.domain.goal.DigitalGoal
import com.digitalbalance.app.domain.goal.GoalProgress
import com.digitalbalance.app.domain.goal.GoalProgressCalculator
import com.digitalbalance.app.domain.goal.GoalType
import com.digitalbalance.app.domain.score.ProductivityScoreEngine
import com.digitalbalance.app.domain.score.ProductivityScoreInput
import com.digitalbalance.app.domain.score.ProductivityScoreResult
import com.digitalbalance.app.domain.score.ScoredAppUsage
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

sealed interface UsageUiState {
    data object Loading : UsageUiState
    data object PermissionRequired : UsageUiState
    data object Empty : UsageUiState
    data class Content(
        val apps: List<AppUsage>,
        val totalDurationMillis: Long
    ) : UsageUiState
    data object Error : UsageUiState
}

sealed interface GoalUiState {
    data object Loading : GoalUiState
    data object Empty : GoalUiState
    data class Content(val progress: List<GoalProgress>) : GoalUiState
}

sealed interface ScoreUiState {
    data object Loading : ScoreUiState
    data class Result(val score: ProductivityScoreResult) : ScoreUiState
}

class UsageViewModel(
    private val repository: UsageRepository,
    private val goalRepository: GoalRepository
) : ViewModel() {
    private val goalCalculator = GoalProgressCalculator()
    private val scoreEngine = ProductivityScoreEngine()
    private val baseState = MutableStateFlow<UsageUiState>(UsageUiState.Loading)
    private val goals: StateFlow<List<DigitalGoal>?> = goalRepository.observeGoals()
        .map<List<DigitalGoal>, List<DigitalGoal>?> { it }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.Eagerly,
            initialValue = null
        )
    val uiState: StateFlow<UsageUiState> = combine(
        baseState,
        repository.observeCategoryOverrides()
    ) { state, overrides ->
        if (state !is UsageUiState.Content) return@combine state
        state.copy(
            apps = state.apps.map { usage ->
                usage.copy(
                    category = categoryWithOverride(
                        defaultCategory = usage.category,
                        userOverride = overrides[usage.packageName]
                    )
                )
            }
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.Eagerly,
        initialValue = UsageUiState.Loading
    )
    val goalUiState: StateFlow<GoalUiState> = combine(
        uiState,
        goals
    ) { usageState, goals ->
        if (goals == null) return@combine GoalUiState.Loading
        if (goalCalculator.noGoals(goals)) return@combine GoalUiState.Empty
        val apps = when (usageState) {
            is UsageUiState.Content -> usageState.apps
            UsageUiState.Empty -> emptyList()
            else -> null
        }
        val total = when (usageState) {
            is UsageUiState.Content -> usageState.totalDurationMillis
            UsageUiState.Empty -> 0L
            else -> null
        }
        GoalUiState.Content(goalCalculator.calculate(goals, apps, total))
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.Eagerly,
        initialValue = GoalUiState.Loading
    )
    val scoreUiState: StateFlow<ScoreUiState> = combine(uiState, goals) { usageState, goals ->
        if (goals == null || usageState == UsageUiState.Loading) {
            return@combine ScoreUiState.Loading
        }
        val apps = (usageState as? UsageUiState.Content)?.apps.orEmpty()
        val totalDuration = when (usageState) {
            is UsageUiState.Content -> usageState.totalDurationMillis
            else -> 0L
        }
        ScoreUiState.Result(
            scoreEngine.calculate(
                ProductivityScoreInput(
                    totalForegroundDurationMillis = totalDuration,
                    apps = apps.map { app ->
                        ScoredAppUsage(
                            packageName = app.packageName,
                            appName = app.appName,
                            durationMillis = app.foregroundDurationMillis,
                            category = app.category
                        )
                    },
                    goals = goals
                )
            )
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.Eagerly,
        initialValue = ScoreUiState.Loading
    )

    private var refreshJob: Job? = null

    fun refresh() {
        refreshJob?.cancel()
        refreshJob = viewModelScope.launch {
            baseState.value = UsageUiState.Loading
            baseState.value = withContext(Dispatchers.IO) { loadState() }
        }
    }

    fun setCategory(packageName: String, category: AppCategory) {
        viewModelScope.launch(Dispatchers.IO) {
            repository.setCategoryOverride(packageName, category)
        }
    }

    fun saveGoal(
        type: GoalType,
        targetDurationMillis: Long,
        packageName: String?,
        appName: String?
    ) {
        viewModelScope.launch(Dispatchers.IO) {
            val normalizedPackage = packageName?.takeIf(String::isNotBlank)
            goalRepository.saveGoal(
                DigitalGoal(
                    id = DigitalGoal.idFor(type, normalizedPackage),
                    type = type,
                    targetDurationMillis = targetDurationMillis,
                    packageName = normalizedPackage,
                    appName = appName?.takeIf(String::isNotBlank)
                )
            )
        }
    }

    fun deleteGoal(goalId: String) {
        viewModelScope.launch(Dispatchers.IO) {
            goalRepository.deleteGoal(goalId)
        }
    }

    private suspend fun loadState(): UsageUiState {
        if (!repository.hasUsageAccess()) return UsageUiState.PermissionRequired

        return try {
            val usage = repository.loadAndStoreToday()
            if (usage.apps.isEmpty()) {
                UsageUiState.Empty
            } else {
                UsageUiState.Content(
                    apps = usage.apps,
                    totalDurationMillis = usage.totalForegroundDurationMillis
                )
            }
        } catch (_: SecurityException) {
            UsageUiState.PermissionRequired
        } catch (_: RuntimeException) {
            UsageUiState.Error
        }
    }

    companion object {
        fun factory(
            repository: UsageRepository,
            goalRepository: GoalRepository
        ): ViewModelProvider.Factory =
            object : ViewModelProvider.Factory {
                @Suppress("UNCHECKED_CAST")
                override fun <T : ViewModel> create(modelClass: Class<T>): T {
                    require(modelClass.isAssignableFrom(UsageViewModel::class.java))
                    return UsageViewModel(repository, goalRepository) as T
                }
            }
    }
}
