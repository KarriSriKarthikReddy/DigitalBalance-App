package com.digitalbalance.app.domain.reminder

import com.digitalbalance.app.domain.category.AppCategory
import com.digitalbalance.app.domain.goal.DigitalGoal
import com.digitalbalance.app.domain.goal.GoalProgress
import com.digitalbalance.app.domain.goal.GoalType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ReminderEngineTest {
    private val engine = ReminderEngine()

    @Test
    fun approachingReminderTriggersAtEightyPercent() {
        val result = engine.evaluate(input(progress = limitProgress(48, 60)))

        assertEquals(ReminderType.LimitApproaching, result.single().deliveryKey.type)
        assertTrue(result.single().message.contains("80%"))
    }

    @Test
    fun exceededReminderTriggersAtConfiguredLimit() {
        val result = engine.evaluate(input(progress = limitProgress(60, 60)))

        assertEquals(ReminderType.LimitExceeded, result.single().deliveryKey.type)
    }

    @Test
    fun noReminderBelowApproachingThreshold() {
        assertTrue(engine.evaluate(input(progress = limitProgress(47, 60))).isEmpty())
    }

    @Test
    fun deliveredThresholdIsSuppressed() {
        val candidate = engine.evaluate(input(progress = limitProgress(48, 60))).single()

        val repeated = engine.evaluate(
            input(
                progress = limitProgress(48, 60),
                delivered = setOf(candidate.deliveryKey.storageKey)
            )
        )

        assertTrue(repeated.isEmpty())
    }

    @Test
    fun approachingAndExceededThresholdsCanEachTriggerOnce() {
        val approaching = engine.evaluate(input(progress = limitProgress(48, 60))).single()
        val exceeded = engine.evaluate(
            input(
                progress = limitProgress(62, 60),
                delivered = setOf(approaching.deliveryKey.storageKey)
            )
        ).single()

        assertEquals(ReminderType.LimitExceeded, exceeded.deliveryKey.type)
        val delivered = setOf(approaching.deliveryKey.storageKey, exceeded.deliveryKey.storageKey)
        assertTrue(engine.evaluate(input(limitProgress(70, 60), delivered = delivered)).isEmpty())
    }

    @Test
    fun nextCalendarDayCanNotifyAgain() {
        val first = engine.evaluate(input(progress = limitProgress(48, 60))).single()

        val nextDay = engine.evaluate(
            input(
                progress = limitProgress(48, 60),
                dateKey = "2026-08-31",
                delivered = setOf(first.deliveryKey.storageKey)
            )
        )

        assertEquals(1, nextDay.size)
        assertEquals("2026-08-31", nextDay.single().deliveryKey.dateKey)
    }

    @Test
    fun remindersDisabledProducesNoCandidate() {
        val settings = ReminderSettings(notificationsEnabled = false)
        assertTrue(engine.evaluate(input(limitProgress(60, 60), settings = settings)).isEmpty())
    }

    @Test
    fun deniedNotificationPermissionProducesNoCandidate() {
        assertTrue(
            engine.evaluate(
                input(limitProgress(60, 60), notificationPermissionGranted = false)
            ).isEmpty()
        )
    }

    @Test
    fun productiveTargetReportsMeaningfulRemainingTimeLaterInDay() {
        val productiveGoal = DigitalGoal(
            id = "productive_time",
            type = GoalType.ProductiveTime,
            targetDurationMillis = minutes(120)
        )
        val progress = GoalProgress(
            productiveGoal,
            currentDurationMillis = minutes(88),
            progressFraction = 88f / 120f
        )

        val result = engine.evaluate(
            input(
                progress = progress,
                localHour = 19,
                apps = listOf(app("study", minutes(88), AppCategory.Education))
            )
        )

        assertEquals(ReminderType.ProductiveTarget, result.single().deliveryKey.type)
        assertTrue(result.single().message.contains("32 min remain"))
    }

    @Test
    fun focusSuggestionMapsToFocusWithoutAutoStarting() {
        val settings = ReminderSettings(
            notificationsEnabled = true,
            goalAndLimitRemindersEnabled = false,
            focusSuggestionsEnabled = true
        )

        val result = engine.evaluate(
            input(
                progress = null,
                settings = settings,
                localHour = 14,
                apps = listOf(app("reading", minutes(121), AppCategory.Education))
            )
        )

        assertEquals(ReminderDestination.Focus, result.single().destination)
        assertTrue(result.single().message.contains("25 min Focus"))
    }

    @Test
    fun digitalBalanceSelfUsageCannotTriggerAppLimitOrFocusSuggestion() {
        val selfLimit = GoalProgress(
            DigitalGoal(
                id = "self-limit",
                type = GoalType.AppDailyLimit,
                targetDurationMillis = minutes(30),
                packageName = "com.digitalbalance.app",
                appName = "DigitalBalance"
            ),
            currentDurationMillis = minutes(180),
            progressFraction = 6f
        )
        val settings = ReminderSettings(
            notificationsEnabled = true,
            goalAndLimitRemindersEnabled = true,
            focusSuggestionsEnabled = true
        )

        val result = engine.evaluate(
            input(
                progress = selfLimit,
                settings = settings,
                localHour = 14,
                apps = listOf(
                    app("com.digitalbalance.app", minutes(180), AppCategory.Productivity)
                )
            )
        )

        assertTrue(result.isEmpty())
    }

    @Test
    fun deliveryKeyEligibilityResetsByLocalDateKey() {
        val key = ReminderDeliveryKey(
            "2026-08-30",
            ReminderType.LimitApproaching,
            "social",
            "80"
        )

        assertTrue(ReminderDeliveryKey.belongsToDate(key.storageKey, "2026-08-30"))
        assertTrue(!ReminderDeliveryKey.belongsToDate(key.storageKey, "2026-08-31"))
    }

    private fun input(
        progress: GoalProgress?,
        dateKey: String = "2026-08-30",
        settings: ReminderSettings = ReminderSettings(notificationsEnabled = true),
        notificationPermissionGranted: Boolean = true,
        localHour: Int = 12,
        apps: List<ReminderAppUsage> = emptyList(),
        delivered: Set<String> = emptySet()
    ) = ReminderEvaluationInput(
        dateKey = dateKey,
        localHour = localHour,
        settings = settings,
        notificationPermissionGranted = notificationPermissionGranted,
        apps = apps,
        goalProgress = listOfNotNull(progress),
        completedFocusSessionsToday = 0,
        focusSessionActive = false,
        deliveredKeys = delivered
    )

    private fun limitProgress(currentMinutes: Int, targetMinutes: Int): GoalProgress = GoalProgress(
        goal = DigitalGoal(
            id = "social_media_limit",
            type = GoalType.SocialMediaLimit,
            targetDurationMillis = minutes(targetMinutes)
        ),
        currentDurationMillis = minutes(currentMinutes),
        progressFraction = currentMinutes.toFloat() / targetMinutes
    )

    private fun app(packageName: String, duration: Long, category: AppCategory) = ReminderAppUsage(
        packageName = packageName,
        appName = packageName,
        durationMillis = duration,
        category = category
    )

    private fun minutes(value: Int) = value * 60_000L
}
