package com.digitalbalance.app.domain.analytics

import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale
import java.util.TimeZone

class AnalyticsCalendar(
    private val timeZone: TimeZone = TimeZone.getDefault(),
    private val locale: Locale = Locale.getDefault()
) {
    fun context(nowMillis: Long): AnalyticsDateContext {
        val start = startOfLocalDay(nowMillis)
        val allDays = (-14..0).associateWith { offset -> dayAtOffset(start, offset) }
        val last7 = (-6..0).map(allDays::getValue)
        return AnalyticsDateContext(
            today = last7.last(),
            yesterday = allDays.getValue(-1),
            last7Days = last7,
            recentCompleted7Days = (-7..-1).map(allDays::getValue),
            previousCompleted7Days = (-14..-8).map(allDays::getValue)
        )
    }

    fun dateKey(timestampMillis: Long): String = formatter(DATE_KEY_PATTERN).format(timestampMillis)

    fun startOfLocalDay(timestampMillis: Long): Long = Calendar.getInstance(timeZone).run {
        timeInMillis = timestampMillis
        set(Calendar.HOUR_OF_DAY, 0)
        set(Calendar.MINUTE, 0)
        set(Calendar.SECOND, 0)
        set(Calendar.MILLISECOND, 0)
        timeInMillis
    }

    private fun dayAtOffset(todayStartMillis: Long, offsetDays: Int): AnalyticsDay {
        val timestamp = Calendar.getInstance(timeZone).run {
            timeInMillis = todayStartMillis
            add(Calendar.DAY_OF_MONTH, offsetDays)
            timeInMillis
        }
        return AnalyticsDay(
            dateKey = dateKey(timestamp),
            shortLabel = formatter(DAY_LABEL_PATTERN).format(timestamp),
            isToday = offsetDays == 0,
            longLabel = formatter(FULL_DAY_LABEL_PATTERN).format(timestamp)
        )
    }

    private fun formatter(pattern: String) = SimpleDateFormat(pattern, locale).apply {
        timeZone = this@AnalyticsCalendar.timeZone
    }

    private companion object {
        const val DATE_KEY_PATTERN = "yyyy-MM-dd"
        const val DAY_LABEL_PATTERN = "EEE"
        const val FULL_DAY_LABEL_PATTERN = "EEEE"
    }
}
