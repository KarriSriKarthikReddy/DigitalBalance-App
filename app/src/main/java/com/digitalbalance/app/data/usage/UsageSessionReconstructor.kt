package com.digitalbalance.app.data.usage

class UsageSessionReconstructor {
    fun reconstruct(
        events: List<UsageEventRecord>,
        rangeStartMillis: Long,
        rangeEndMillis: Long
    ): SessionReconstruction {
        require(rangeEndMillis >= rangeStartMillis)

        val sessions = mutableListOf<ForegroundSession>()
        var activePackage: String? = null
        var activeSince = 0L
        val activeActivityIds = mutableSetOf<String>()
        var screenInteractive: Boolean? = null
        var keyguardShown: Boolean? = null
        var unmatchedPauses = 0
        var ignoredEvents = 0

        fun closeActive(timestampMillis: Long, reason: SessionEndReason) {
            val packageName = activePackage ?: return
            val end = timestampMillis.coerceIn(rangeStartMillis, rangeEndMillis)
            if (end > activeSince) {
                sessions += ForegroundSession(
                    packageName = packageName,
                    startMillis = activeSince,
                    endMillis = end,
                    endReason = reason
                )
            } else {
                ignoredEvents++
            }
            activePackage = null
            activeActivityIds.clear()
        }

        events.sortedWith(
            compareBy<UsageEventRecord>(UsageEventRecord::timestampMillis)
                .thenBy { eventPriority(it.kind) }
        ).forEach { event ->
            if (event.timestampMillis !in rangeStartMillis..rangeEndMillis) {
                ignoredEvents++
                return@forEach
            }

            when (event.kind) {
                UsageEventKind.Resumed -> {
                    val packageName = event.packageName
                    if (packageName.isNullOrBlank()) {
                        ignoredEvents++
                    } else if (screenInteractive == false || keyguardShown == true) {
                        ignoredEvents++
                    } else if (activePackage != packageName) {
                        closeActive(event.timestampMillis, SessionEndReason.AppTransition)
                        activePackage = packageName
                        activeSince = event.timestampMillis
                        event.activityId?.let(activeActivityIds::add)
                    } else {
                        event.activityId?.let(activeActivityIds::add)
                    }
                }

                UsageEventKind.Paused -> {
                    if (activePackage == event.packageName) {
                        val activityId = event.activityId
                        when {
                            activityId == null || activeActivityIds.isEmpty() -> {
                                closeActive(event.timestampMillis, SessionEndReason.Paused)
                            }
                            !activeActivityIds.remove(activityId) -> {
                                unmatchedPauses++
                            }
                            activeActivityIds.isEmpty() -> {
                                closeActive(event.timestampMillis, SessionEndReason.Paused)
                            }
                        }
                    } else {
                        unmatchedPauses++
                    }
                }

                UsageEventKind.ScreenNonInteractive -> {
                    screenInteractive = false
                    closeActive(event.timestampMillis, SessionEndReason.ScreenInactive)
                }

                UsageEventKind.ScreenInteractive -> screenInteractive = true

                UsageEventKind.KeyguardShown -> {
                    keyguardShown = true
                    closeActive(event.timestampMillis, SessionEndReason.KeyguardShown)
                }

                UsageEventKind.KeyguardHidden -> keyguardShown = false
            }
        }

        closeActive(rangeEndMillis, SessionEndReason.EndOfRange)

        return SessionReconstruction(
            sessions = sessions,
            unmatchedPauseCount = unmatchedPauses,
            ignoredEventCount = ignoredEvents
        )
    }

    private fun eventPriority(kind: UsageEventKind): Int = when (kind) {
        UsageEventKind.ScreenNonInteractive,
        UsageEventKind.KeyguardShown -> 0
        UsageEventKind.ScreenInteractive,
        UsageEventKind.KeyguardHidden -> 1
        UsageEventKind.Resumed -> 2
        UsageEventKind.Paused -> 3
    }
}
