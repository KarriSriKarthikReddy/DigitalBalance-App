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
import com.digitalbalance.app.domain.insight.InsightAppUsage
import com.digitalbalance.app.domain.insight.InsightInput
import com.digitalbalance.app.domain.insight.PersonalInsight
import com.digitalbalance.app.domain.insight.SmartInsightEngine
import com.digitalbalance.app.domain.productivity.ProductivityAppUsage
import com.digitalbalance.app.domain.productivity.ProductivityScoreEngine
import com.digitalbalance.app.domain.productivity.ProductivityScoreInput
import com.digitalbalance.app.domain.productivity.ProductivityScoreResult
import com.digitalbalance.app.domain.score.GoalAlignmentEngine
import com.digitalbalance.app.domain.score.GoalAlignmentInput
import com.digitalbalance.app.domain.score.GoalAlignmentResult
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

sealed interface GoalAlignmentUiState {
    data object Loading : GoalAlignmentUiState
    data class Result(val alignment: GoalAlignmentResult) : GoalAlignmentUiState
}

sealed interface ProductivityUiState {
    data object Loading : ProductivityUiState
    data class Result(val productivity: ProductivityScoreResult) : ProductivityUiState
}

sealed interface InsightUiState {
    data object Loading : InsightUiState
    data class Content(val insights: List<PersonalInsight>) : InsightUiState
}

class UsageViewModel(
    private val repository: UsageRepository,
    private val goalRepository: GoalRepository
) : ViewModel() {
    private val goalCalculator = GoalProgressCalculator()
    private val goalAlignmentEngine = GoalAlignmentEngine()
    private val productivityScoreEngine = ProductivityScoreEngine()
    private val insightEngine = SmartInsightEngine()
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
        val apps = state.apps.map { usage ->
                usage.copy(
                    category = categoryWithOverride(
                        defaultCategory = usage.category,
                        userOverride = overrides[usage.packageName]
                    )
                )
            }
        state.copy(apps = apps)
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
    val goalAlignmentUiState: StateFlow<GoalAlignmentUiState> = combine(
        uiState,
        goals
    ) { usageState, goals ->
        if (goals == null || usageState == UsageUiState.Loading) {
            return@combine GoalAlignmentUiState.Loading
        }
        val apps = (usageState as? UsageUiState.Content)?.apps.orEmpty()
        val totalDuration = when (usageState) {
            is UsageUiState.Content -> usageState.totalDurationMillis
            else -> 0L
        }
        GoalAlignmentUiState.Result(
            goalAlignmentEngine.calculate(
                GoalAlignmentInput(
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
        initialValue = GoalAlignmentUiState.Loading
    )
    val productivityUiState: StateFlow<ProductivityUiState> = uiState.map { usageState ->
        if (usageState == UsageUiState.Loading) return@map ProductivityUiState.Loading
        val usage = usageState as? UsageUiState.Content
        ProductivityUiState.Result(
            productivityScoreEngine.calculate(
                ProductivityScoreInput(
                    totalForegroundDurationMillis = usage?.totalDurationMillis ?: 0L,
                    apps = usage?.apps.orEmpty().map { app ->
                        ProductivityAppUsage(
                            packageName = app.packageName,
                            appName = app.appName,
                            durationMillis = app.foregroundDurationMillis,
                            openCount = app.openCount,
                            category = app.category
                        )
                    }
                )
            )
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.Eagerly,
        initialValue = ProductivityUiState.Loading
    )
    val insightUiState: StateFlow<InsightUiState> = combine(
        uiState,
        goalUiState,
        productivityUiState
    ) { usageState, goalState, productivityState ->
        if (
            usageState == UsageUiState.Loading ||
            goalState == GoalUiState.Loading ||
            productivityState == ProductivityUiState.Loading
        ) {
            return@combine InsightUiState.Loading
        }
        val usage = usageState as? UsageUiState.Content
        val progress = (goalState as? GoalUiState.Content)?.progress.orEmpty()
        val productivity = (productivityState as? ProductivityUiState.Result)?.productivity
        InsightUiState.Content(
            insightEngine.generate(
                InsightInput(
                    totalForegroundDurationMillis = usage?.totalDurationMillis ?: 0L,
                    apps = usage?.apps.orEmpty().map { app ->
                        InsightAppUsage(
                            packageName = app.packageName,
                            appName = app.appName,
                            durationMillis = app.foregroundDurationMillis,
                            openCount = app.openCount,
                            category = app.category
                        )
                    },
                    goalProgress = progress,
                    productivityScoreResult = productivity
                )
            )
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.Eagerly,
        initialValue = InsightUiState.Loading
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
