package com.digitalbalance.app.ui.usage

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.digitalbalance.app.data.usage.AppUsage
import com.digitalbalance.app.data.repository.UsageRepository
import com.digitalbalance.app.domain.category.AppCategory
import com.digitalbalance.app.domain.category.categoryWithOverride
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
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

class UsageViewModel(
    private val repository: UsageRepository
) : ViewModel() {
    private val baseState = MutableStateFlow<UsageUiState>(UsageUiState.Loading)
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
        fun factory(repository: UsageRepository): ViewModelProvider.Factory =
            object : ViewModelProvider.Factory {
                @Suppress("UNCHECKED_CAST")
                override fun <T : ViewModel> create(modelClass: Class<T>): T {
                    require(modelClass.isAssignableFrom(UsageViewModel::class.java))
                    return UsageViewModel(repository) as T
                }
            }
    }
}
