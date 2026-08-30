package com.digitalbalance.app.domain.analytics

import java.util.Calendar
import java.util.Locale
import java.util.TimeZone
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class AnalyticsCalendarTest {
    private val timeZone = TimeZone.getTimeZone("Asia/Kolkata")
    private val analyticsCalendar = AnalyticsCalendar(timeZone, Locale.US)
    private val now = timestamp(2026, Calendar.AUGUST, 30, 14, 30)

    @Test
    fun todayRangeContainsOnlyLocalToday() {
        val context = analyticsCalendar.context(now)

        assertEquals(listOf("2026-08-30"), context.daysFor(AnalyticsPeriod.Today).map(AnalyticsDay::dateKey))
        assertTrue(context.today.isToday)
    }

    @Test
    fun yesterdayRangeUsesPreviousCalendarDate() {
        val context = analyticsCalendar.context(now)

        assertEquals(listOf("2026-08-29"), context.daysFor(AnalyticsPeriod.Yesterday).map(AnalyticsDay::dateKey))
        assertFalse(context.yesterday.isToday)
    }

    @Test
    fun lastSevenUsesSevenCalendarDaysEndingToday() {
        val keys = analyticsCalendar.context(now).last7Days.map(AnalyticsDay::dateKey)

        assertEquals(
            listOf(
                "2026-08-24", "2026-08-25", "2026-08-26", "2026-08-27",
                "2026-08-28", "2026-08-29", "2026-08-30"
            ),
            keys
        )
    }

    @Test
    fun localMidnightSeparatesAdjacentLocalDates() {
        val beforeMidnight = timestamp(2026, Calendar.AUGUST, 29, 23, 59)
        val afterMidnight = timestamp(2026, Calendar.AUGUST, 30, 0, 1)

        assertEquals("2026-08-29", analyticsCalendar.dateKey(beforeMidnight))
        assertEquals("2026-08-30", analyticsCalendar.dateKey(afterMidnight))
        assertEquals(timestamp(2026, Calendar.AUGUST, 30, 0, 0), analyticsCalendar.startOfLocalDay(afterMidnight))
    }

    @Test
    fun lastSevenRemainsCalendarBasedAcrossDaylightSavingChange() {
        val eastern = TimeZone.getTimeZone("America/New_York")
        val calendar = AnalyticsCalendar(eastern, Locale.US)
        val afterSpringForward = Calendar.getInstance(eastern).run {
            clear()
            set(2026, Calendar.MARCH, 10, 12, 0, 0)
            timeInMillis
        }

        assertEquals(
            listOf(
                "2026-03-04", "2026-03-05", "2026-03-06", "2026-03-07",
                "2026-03-08", "2026-03-09", "2026-03-10"
            ),
            calendar.context(afterSpringForward).last7Days.map(AnalyticsDay::dateKey)
        )
    }

    private fun timestamp(year: Int, month: Int, day: Int, hour: Int, minute: Int): Long =
        Calendar.getInstance(timeZone).run {
            clear()
            set(year, month, day, hour, minute, 0)
            timeInMillis
        }
}
