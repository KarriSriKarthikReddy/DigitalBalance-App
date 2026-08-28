package com.digitalbalance.app.ui.usage

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.digitalbalance.app.data.usage.AppUsage
import com.digitalbalance.app.data.usage.UsageStatsDataSource
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
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
    private val dataSource: UsageStatsDataSource
) : ViewModel() {
    private val _uiState = MutableStateFlow<UsageUiState>(UsageUiState.Loading)
    val uiState: StateFlow<UsageUiState> = _uiState.asStateFlow()

    private var refreshJob: Job? = null

    fun refresh() {
        refreshJob?.cancel()
        refreshJob = viewModelScope.launch {
            _uiState.value = UsageUiState.Loading
            _uiState.value = withContext(Dispatchers.IO) { loadState() }
        }
    }

    private fun loadState(): UsageUiState {
        if (!dataSource.hasUsageAccess()) return UsageUiState.PermissionRequired

        return try {
            val usage = dataSource.loadTodayUsage()
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
        fun factory(dataSource: UsageStatsDataSource): ViewModelProvider.Factory =
            object : ViewModelProvider.Factory {
                @Suppress("UNCHECKED_CAST")
                override fun <T : ViewModel> create(modelClass: Class<T>): T {
                    require(modelClass.isAssignableFrom(UsageViewModel::class.java))
                    return UsageViewModel(dataSource) as T
                }
            }
    }
}
