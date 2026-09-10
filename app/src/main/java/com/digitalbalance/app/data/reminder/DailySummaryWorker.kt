package com.digitalbalance.app.data.reminder

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.digitalbalance.app.data.local.DigitalBalanceDatabase
import com.digitalbalance.app.data.repository.FocusRepository
import com.digitalbalance.app.data.repository.GoalRepository
import com.digitalbalance.app.data.repository.UsageRepository
import com.digitalbalance.app.data.repository.localDateKey
import com.digitalbalance.app.data.usage.UsageStatsDataSource
import com.digitalbalance.app.domain.category.categoryWithOverride
import com.digitalbalance.app.domain.reminder.DailySummaryFormatter
import com.digitalbalance.app.domain.reminder.ReminderCandidate
import com.digitalbalance.app.domain.reminder.ReminderDeliveryKey
import com.digitalbalance.app.domain.reminder.ReminderDestination
import com.digitalbalance.app.domain.reminder.ReminderType
import kotlinx.coroutines.flow.first

class DailySummaryWorker(
    appContext: Context,
    parameters: WorkerParameters
) : CoroutineWorker(appContext, parameters) {
    override suspend fun doWork(): Result {
        val preferences = ReminderPreferences(applicationContext)
        val settings = preferences.currentSettings()
        if (!settings.notificationsEnabled || !settings.dailySummaryEnabled) return Result.success()
        val notifier = ReminderNotificationManager(applicationContext)
        if (!notifier.canNotify()) {
            DailySummaryScheduler(applicationContext).scheduleNext()
            return Result.success()
        }
        var scheduleNext = true
        return try {
            val database = DigitalBalanceDatabase.getInstance(applicationContext)
            val usageRepository = UsageRepository(
                UsageStatsDataSource(applicationContext),
                database.usageDao()
            )
            if (!usageRepository.hasUsageAccess()) return Result.success()
            val now = System.currentTimeMillis()
            val usage = usageRepository.loadAndStoreToday(now)
            if (usage.apps.isEmpty()) return Result.success()
            val overrides = usageRepository.observeCategoryOverrides().first()
            val apps = usage.apps.map { app ->
                app.copy(category = categoryWithOverride(app.category, overrides[app.packageName]))
            }
            val snapshot = ReminderSnapshotFactory().create(
                dateKey = localDateKey(now),
                apps = apps,
                totalForegroundDurationMillis = usage.totalForegroundDurationMillis,
                goals = GoalRepository(database.goalDao()).observeGoals().first(),
                focusSessions = FocusRepository(database.focusSessionDao()).observeSessions().first()
            )
            val deliveryKey = ReminderDeliveryKey(
                snapshot.dateKey,
                ReminderType.DailySummary,
                "daily",
                "scheduled"
            )
            if (deliveryKey.storageKey !in preferences.deliveredKeys(snapshot.dateKey)) {
                val candidate = ReminderCandidate(
                    deliveryKey = deliveryKey,
                    title = "Your DigitalBalance summary",
                    message = DailySummaryFormatter.notificationBody(snapshot.dailySummary),
                    destination = ReminderDestination.Insights
                )
                if (notifier.notify(candidate, DailySummaryFormatter.detailLines(snapshot.dailySummary))) {
                    preferences.markDelivered(deliveryKey)
                }
            }
            Result.success()
        } catch (_: SecurityException) {
            Result.success()
        } catch (_: RuntimeException) {
            scheduleNext = false
            Result.retry()
        } finally {
            val latest = preferences.currentSettings()
            if (scheduleNext && latest.notificationsEnabled && latest.dailySummaryEnabled) {
                DailySummaryScheduler(applicationContext).scheduleNext()
            }
        }
    }
}
