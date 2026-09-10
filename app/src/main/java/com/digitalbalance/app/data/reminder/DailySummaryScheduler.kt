package com.digitalbalance.app.data.reminder

import android.content.Context
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import com.digitalbalance.app.data.repository.localDateKey
import com.digitalbalance.app.domain.reminder.DailySummaryPolicy
import com.digitalbalance.app.domain.reminder.ReminderSettings
import java.util.Calendar
import java.util.TimeZone
import java.util.concurrent.TimeUnit

class DailySummaryScheduler(context: Context) {
    private val appContext = context.applicationContext
    private val workManager = WorkManager.getInstance(appContext)

    fun sync(settings: ReminderSettings, nowMillis: Long = System.currentTimeMillis()) {
        workManager.cancelAllWorkByTag(WORK_TAG)
        if (settings.notificationsEnabled && settings.dailySummaryEnabled) {
            scheduleNext(nowMillis)
        }
    }

    fun scheduleNext(nowMillis: Long = System.currentTimeMillis()) {
        val runAt = nextRunMillis(nowMillis)
        val request = OneTimeWorkRequestBuilder<DailySummaryWorker>()
            .setInitialDelay((runAt - nowMillis).coerceAtLeast(0L), TimeUnit.MILLISECONDS)
            .addTag(WORK_TAG)
            .build()
        workManager.enqueueUniqueWork(
            "$UNIQUE_WORK_PREFIX:${localDateKey(runAt)}",
            ExistingWorkPolicy.REPLACE,
            request
        )
    }

    companion object {
        private const val WORK_TAG = "digital_balance_daily_summary"
        private const val UNIQUE_WORK_PREFIX = "digital_balance_daily_summary"

        fun nextRunMillis(
            nowMillis: Long,
            timeZone: TimeZone = TimeZone.getDefault()
        ): Long {
            val now = Calendar.getInstance(timeZone).apply { timeInMillis = nowMillis }
            val next = Calendar.getInstance(timeZone).apply {
                timeInMillis = nowMillis
                set(Calendar.HOUR_OF_DAY, DailySummaryPolicy.SUMMARY_HOUR)
                set(Calendar.MINUTE, DailySummaryPolicy.SUMMARY_MINUTE)
                set(Calendar.SECOND, 0)
                set(Calendar.MILLISECOND, 0)
                if (!after(now)) add(Calendar.DAY_OF_YEAR, 1)
            }
            return next.timeInMillis
        }
    }
}
