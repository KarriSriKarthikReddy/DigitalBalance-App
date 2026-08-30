package com.digitalbalance.app.ui.insights

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.digitalbalance.app.data.local.DailyUsageRecord
import com.digitalbalance.app.data.repository.FocusRepository
import com.digitalbalance.app.data.repository.UsageRepository
import com.digitalbalance.app.data.repository.CurrentDayUsage
import com.digitalbalance.app.data.usage.AppUsage
import com.digitalbalance.app.data.usage.TodayUsage
import com.digitalbalance.app.domain.analytics.AnalyticsCalendar
import com.digitalbalance.app.domain.analytics.AnalyticsDateContext
import com.digitalbalance.app.domain.analytics.AnalyticsEngine
import com.digitalbalance.app.domain.analytics.AnalyticsFocusRecord
import com.digitalbalance.app.domain.analytics.AnalyticsPeriod
import com.digitalbalance.app.domain.analytics.AnalyticsSnapshot
import com.digitalbalance.app.domain.analytics.AnalyticsUsageRecord
import com.digitalbalance.app.domain.category.AppCategory
import com.digitalbalance.app.domain.category.categoryWithOverride
import com.digitalbalance.app.domain.focus.FocusSession
import com.digitalbalance.app.domain.focus.FocusSessionStatus
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn

sealed interface AnalyticsUiState {
    data object Loading : AnalyticsUiState
    data class Content(
        val snapshot: AnalyticsSnapshot,
        val rankedApps: List<AppUsage>
    ) : AnalyticsUiState
}

@OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
class AnalyticsViewModel(
    private val usageRepository: UsageRepository,
    focusRepository: FocusRepository,
    private val calendar: AnalyticsCalendar = AnalyticsCalendar(),
    private val engine: AnalyticsEngine = AnalyticsEngine(),
    private val clock: () -> Long = System::currentTimeMillis
) : ViewModel() {
    private val selectedPeriod = MutableStateFlow(AnalyticsPeriod.Today)
    private val dates = MutableStateFlow(calendar.context(clock()))
    private val defaultCategoryCache = mutableMapOf<String, AppCategory>()

    private val history: Flow<List<DailyUsageRecord>> = dates.flatMapLatest { context ->
        usageRepository.observeHistory(
            startDateKey = context.previousCompleted7Days.firstOrNull()?.dateKey
                ?: context.last7Days.first().dateKey,
            endDateKey = context.today.dateKey
        )
    }
    private val historySelection = combine(dates, selectedPeriod, history) { dateContext, period, rows ->
        HistorySelection(dateContext, period, rows)
    }
    private val liveInputs = combine(
        usageRepository.currentTodayUsage,
        usageRepository.observeCategoryOverrides(),
        focusRepository.observeSessions()
    ) { today, overrides, focusSessions ->
        LiveInputs(today, overrides, focusSessions)
    }

    val uiState: StateFlow<AnalyticsUiState> = combine(
        historySelection,
        liveInputs
    ) { selection, live ->
        buildContent(selection, live)
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.Eagerly,
        initialValue = AnalyticsUiState.Loading
    )

    fun selectPeriod(period: AnalyticsPeriod) {
        selectedPeriod.value = period
    }

    fun refreshDate(nowMillis: Long = clock()) {
        val refreshed = calendar.context(nowMillis)
        if (refreshed.today.dateKey != dates.value.today.dateKey) dates.value = refreshed
    }

    private fun buildContent(
        selection: HistorySelection,
        live: LiveInputs
    ): AnalyticsUiState.Content {
        val todayKey = selection.dates.today.dateKey
        val historicalRows = selection.rows.filter { it.dateKey != todayKey }
        val currentToday = live.today?.takeIf { it.dateKey == todayKey }?.usage
        val records = historicalRows.map { row -> row.toAnalyticsRecord(live.overrides) } +
            currentToday.toAnalyticsRecords(todayKey, live.overrides)
        val availableKeys = historicalRows.mapTo(mutableSetOf(), DailyUsageRecord::dateKey).apply {
            if (currentToday != null) add(todayKey)
        }
        val focusRecords = live.focusSessions.mapNotNull { session ->
            val endedAt = session.endedAtEpochMillis ?: return@mapNotNull null
            AnalyticsFocusRecord(
                endedDateKey = calendar.dateKey(endedAt),
                focusedDurationMillis = session.accumulatedFocusedMillis,
                completed = session.status == FocusSessionStatus.Completed
            )
        }
        val snapshot = engine.calculate(
            period = selection.period,
            dates = selection.dates,
            usageRecords = records,
            availableDateKeys = availableKeys,
            focusRecords = focusRecords
        )
        val rankedApps = snapshot.rankedApps.take(MAX_ICON_ROWS).map { total ->
            AppUsage(
                packageName = total.packageName,
                appName = total.appName,
                foregroundDurationMillis = total.durationMillis,
                openCount = total.openCount,
                icon = usageRepository.loadIcon(total.packageName),
                category = total.category
            )
        }
        return AnalyticsUiState.Content(snapshot, rankedApps)
    }

    private fun DailyUsageRecord.toAnalyticsRecord(
        overrides: Map<String, AppCategory>
    ) = AnalyticsUsageRecord(
        dateKey = dateKey,
        packageName = packageName,
        appName = appName,
        durationMillis = foregroundDurationMillis,
        openCount = openCount,
        category = categoryFor(packageName, overrides)
    )

    private fun TodayUsage?.toAnalyticsRecords(
        todayKey: String,
        overrides: Map<String, AppCategory>
    ): List<AnalyticsUsageRecord> = this?.apps.orEmpty().map { app ->
        AnalyticsUsageRecord(
            dateKey = todayKey,
            packageName = app.packageName,
            appName = app.appName,
            durationMillis = app.foregroundDurationMillis,
            openCount = app.openCount,
            category = categoryWithOverride(app.category, overrides[app.packageName])
        )
    }

    private fun categoryFor(
        packageName: String,
        overrides: Map<String, AppCategory>
    ): AppCategory = categoryWithOverride(
        defaultCategory = defaultCategoryCache.getOrPut(packageName) {
            usageRepository.resolveDefaultCategory(packageName)
        },
        userOverride = overrides[packageName]
    )

    private data class HistorySelection(
        val dates: AnalyticsDateContext,
        val period: AnalyticsPeriod,
        val rows: List<DailyUsageRecord>
    )

    private data class LiveInputs(
        val today: CurrentDayUsage?,
        val overrides: Map<String, AppCategory>,
        val focusSessions: List<FocusSession>
    )

    companion object {
        private const val MAX_ICON_ROWS = 8

        fun factory(
            usageRepository: UsageRepository,
            focusRepository: FocusRepository
        ): ViewModelProvider.Factory = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                require(modelClass.isAssignableFrom(AnalyticsViewModel::class.java))
                return AnalyticsViewModel(usageRepository, focusRepository) as T
            }
        }
    }
}
