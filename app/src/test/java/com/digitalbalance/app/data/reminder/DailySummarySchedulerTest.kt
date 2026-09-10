package com.digitalbalance.app.data.reminder

import java.util.Calendar
import java.util.TimeZone
import org.junit.Assert.assertEquals
import org.junit.Test

class DailySummarySchedulerTest {
    private val timeZone = TimeZone.getTimeZone("Asia/Kolkata")

    @Test
    fun nextRunUsesLocalSummaryTimeTodayWhenStillUpcoming() {
        val now = localTime(2026, Calendar.AUGUST, 30, 12, 0)
        val next = DailySummaryScheduler.nextRunMillis(now, timeZone)
        val calendar = Calendar.getInstance(timeZone).apply { timeInMillis = next }

        assertEquals(30, calendar.get(Calendar.DAY_OF_MONTH))
        assertEquals(20, calendar.get(Calendar.HOUR_OF_DAY))
        assertEquals(30, calendar.get(Calendar.MINUTE))
    }

    @Test
    fun nextRunMovesToNextLocalDayAfterSummaryTime() {
        val now = localTime(2026, Calendar.AUGUST, 30, 21, 0)
        val next = DailySummaryScheduler.nextRunMillis(now, timeZone)
        val calendar = Calendar.getInstance(timeZone).apply { timeInMillis = next }

        assertEquals(31, calendar.get(Calendar.DAY_OF_MONTH))
        assertEquals(20, calendar.get(Calendar.HOUR_OF_DAY))
        assertEquals(30, calendar.get(Calendar.MINUTE))
    }

    private fun localTime(year: Int, month: Int, day: Int, hour: Int, minute: Int): Long =
        Calendar.getInstance(timeZone).apply {
            clear()
            set(year, month, day, hour, minute)
        }.timeInMillis
}
