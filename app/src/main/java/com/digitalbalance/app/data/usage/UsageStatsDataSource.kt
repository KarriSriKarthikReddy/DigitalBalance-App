package com.digitalbalance.app.data.usage

import android.app.AppOpsManager
import android.app.usage.UsageEvents
import android.app.usage.UsageStatsManager
import android.content.Context
import android.content.pm.ApplicationInfo
import android.os.Build
import android.os.Process
import android.util.Log
import java.util.Calendar

class UsageStatsDataSource(context: Context) {
    private val appContext = context.applicationContext
    private val appOpsManager = appContext.getSystemService(AppOpsManager::class.java)
    private val usageStatsManager = appContext.getSystemService(UsageStatsManager::class.java)
    private val appClassifier = AppClassifier(appContext)
    private val reconstructor = UsageSessionReconstructor()
    private val debugLoggingEnabled =
        appContext.applicationInfo.flags and ApplicationInfo.FLAG_DEBUGGABLE != 0

    fun hasUsageAccess(): Boolean {
        val mode = appOpsManager.checkOpNoThrow(
            AppOpsManager.OPSTR_GET_USAGE_STATS,
            Process.myUid(),
            appContext.packageName
        )
        return mode == AppOpsManager.MODE_ALLOWED
    }

    fun loadTodayUsage(nowMillis: Long = System.currentTimeMillis()): TodayUsage {
        val startOfDay = localStartOfDay(nowMillis)
        val records = loadEventRecords(startOfDay, nowMillis)
        val reconstruction = reconstructor.reconstruct(records, startOfDay, nowMillis)
        val foregroundActivitiesByPackage = records
            .asSequence()
            .filter { it.kind == UsageEventKind.Resumed }
            .filter { !it.packageName.isNullOrBlank() && !it.activityId.isNullOrBlank() }
            .groupBy(
                keySelector = { requireNotNull(it.packageName) },
                valueTransform = { requireNotNull(it.activityId) }
            )
            .mapValues { (_, classNames) -> classNames.toSet() }
        val classifiedApps = reconstruction.sessions
            .asSequence()
            .map(ForegroundSession::packageName)
            .distinct()
            .associateWith { packageName ->
                appClassifier.classify(
                    packageName = packageName,
                    foregroundActivityClassNames = foregroundActivitiesByPackage[packageName].orEmpty()
                )
            }

        val includedSessions = reconstruction.sessions.filter { session ->
            classifiedApps.getValue(session.packageName).kind.includedInPrimaryUsage
        }
        val apps = includedSessions
            .groupBy(ForegroundSession::packageName)
            .map { (packageName, sessions) ->
                val app = classifiedApps.getValue(packageName)
                AppUsage(
                    packageName = packageName,
                    appName = app.label,
                    foregroundDurationMillis = sessions.sumOf(ForegroundSession::durationMillis),
                    openCount = sessions.size,
                    icon = appClassifier.loadIcon(packageName)
                )
            }
            .filter { it.foregroundDurationMillis > 0L }
            .sortedWith(
                compareByDescending<AppUsage> { it.foregroundDurationMillis }
                    .thenBy(String.CASE_INSENSITIVE_ORDER) { it.appName }
            )

        val totalDuration = includedSessions.sumOf(ForegroundSession::durationMillis)
        if (debugLoggingEnabled) {
            logGoogleSearchSummary(records, reconstruction, classifiedApps)
            logDebugComparison(
                startOfDay = startOfDay,
                nowMillis = nowMillis,
                reconstruction = reconstruction,
                classifiedApps = classifiedApps,
                includedSessions = includedSessions,
                eventApps = apps
            )
        }

        return TodayUsage(
            apps = apps,
            totalForegroundDurationMillis = totalDuration
        )
    }

    private fun loadEventRecords(startMillis: Long, endMillis: Long): List<UsageEventRecord> {
        val usageEvents = usageStatsManager.queryEvents(startMillis, endMillis)
        val event = UsageEvents.Event()
        return buildList {
            while (usageEvents.hasNextEvent()) {
                usageEvents.getNextEvent(event)
                val kind = eventKind(event.eventType)
                if (debugLoggingEnabled && event.packageName == GOOGLE_SEARCH_PACKAGE) {
                    Log.d(
                        DEBUG_TAG,
                        "GOOGLE_SEARCH_EVENT type=${event.eventType} mappedKind=$kind " +
                            "class=${event.className} timestampMs=${event.timeStamp}"
                    )
                }
                if (kind == null) continue
                add(
                    UsageEventRecord(
                        packageName = event.packageName,
                        timestampMillis = event.timeStamp,
                        kind = kind,
                        activityId = activityId(event)
                    )
                )
            }
        }
    }

    private fun logGoogleSearchSummary(
        records: List<UsageEventRecord>,
        reconstruction: SessionReconstruction,
        classifiedApps: Map<String, ClassifiedApp>
    ) {
        val googleRecords = records.filter { it.packageName == GOOGLE_SEARCH_PACKAGE }
        val googleSessions = reconstruction.sessions.filter {
            it.packageName == GOOGLE_SEARCH_PACKAGE
        }
        val classified = classifiedApps[GOOGLE_SEARCH_PACKAGE]
        Log.d(
            DEBUG_TAG,
            "GOOGLE_SEARCH_SUMMARY mappedEvents=${googleRecords.size} " +
                "sessions=${googleSessions.size} " +
                "durationMs=${googleSessions.sumOf(ForegroundSession::durationMillis)} " +
                "kind=${classified?.kind} included=${classified?.kind?.includedInPrimaryUsage}"
        )
    }

    private fun activityId(event: UsageEvents.Event): String? {
        return event.className
    }

    private fun eventKind(eventType: Int): UsageEventKind? {
        val resumedType = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            UsageEvents.Event.ACTIVITY_RESUMED
        } else {
            @Suppress("DEPRECATION")
            UsageEvents.Event.MOVE_TO_FOREGROUND
        }
        val pausedType = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            UsageEvents.Event.ACTIVITY_PAUSED
        } else {
            @Suppress("DEPRECATION")
            UsageEvents.Event.MOVE_TO_BACKGROUND
        }
        return when {
            eventType == resumedType -> UsageEventKind.Resumed
            eventType == pausedType -> UsageEventKind.Paused
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.P &&
                (eventType == UsageEvents.Event.SCREEN_NON_INTERACTIVE ||
                    eventType == UsageEvents.Event.KEYGUARD_SHOWN) -> UsageEventKind.StopAll
            else -> null
        }
    }

    private fun logDebugComparison(
        startOfDay: Long,
        nowMillis: Long,
        reconstruction: SessionReconstruction,
        classifiedApps: Map<String, ClassifiedApp>,
        includedSessions: List<ForegroundSession>,
        eventApps: List<AppUsage>
    ) {
        val aggregateStats = usageStatsManager.queryAndAggregateUsageStats(startOfDay, nowMillis)
        var aggregateTotal = 0L
        aggregateStats.values
            .filter { it.totalTimeInForeground > 0L }
            .sortedByDescending { it.totalTimeInForeground }
            .forEach { stats ->
                aggregateTotal += stats.totalTimeInForeground
                val app = appClassifier.classify(stats.packageName)
                Log.d(
                    DEBUG_TAG,
                    "AGGREGATED_APP package=${stats.packageName} " +
                        "durationMs=${stats.totalTimeInForeground} kind=${app.kind}"
                )
            }

        reconstruction.sessions.forEach { session ->
            val app = classifiedApps.getValue(session.packageName)
            Log.d(
                DEBUG_TAG,
                "SESSION package=${session.packageName} startMs=${session.startMillis} " +
                    "endMs=${session.endMillis} durationMs=${session.durationMillis} " +
                    "endReason=${session.endReason} kind=${app.kind}"
            )
        }
        eventApps.forEach { usage ->
            Log.d(
                DEBUG_TAG,
                "EVENT_APP package=${usage.packageName} durationMs=${usage.foregroundDurationMillis} " +
                    "opens=${usage.openCount}"
            )
        }
        Log.d(
            DEBUG_TAG,
            "AGGREGATED_OVERALL sumDurationMs=$aggregateTotal rangeStartMs=$startOfDay " +
                "rangeEndMs=$nowMillis"
        )
        Log.d(
            DEBUG_TAG,
            "EVENT_OVERALL includedDurationMs=${includedSessions.sumOf(ForegroundSession::durationMillis)} " +
                "allSessionDurationMs=${reconstruction.sessions.sumOf(ForegroundSession::durationMillis)} " +
                "sessions=${reconstruction.sessions.size} " +
                "unmatchedPauses=${reconstruction.unmatchedPauseCount} " +
                "ignoredEvents=${reconstruction.ignoredEventCount}"
        )
    }

    private fun localStartOfDay(nowMillis: Long): Long = Calendar.getInstance().run {
        timeInMillis = nowMillis
        set(Calendar.HOUR_OF_DAY, 0)
        set(Calendar.MINUTE, 0)
        set(Calendar.SECOND, 0)
        set(Calendar.MILLISECOND, 0)
        timeInMillis
    }

    private companion object {
        const val DEBUG_TAG = "DigitalBalanceUsage"
        const val GOOGLE_SEARCH_PACKAGE = "com.google.android.googlequicksearchbox"
    }
}
