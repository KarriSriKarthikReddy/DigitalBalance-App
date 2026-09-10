package com.digitalbalance.app.data.reminder

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.digitalbalance.app.MainActivity
import com.digitalbalance.app.R
import com.digitalbalance.app.domain.reminder.ReminderCandidate
import com.digitalbalance.app.domain.reminder.ReminderDestination

class ReminderNotificationManager(context: Context) {
    private val context = context.applicationContext

    fun canNotify(): Boolean =
        (
            Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU ||
                ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) ==
                PackageManager.PERMISSION_GRANTED
            ) && NotificationManagerCompat.from(context).areNotificationsEnabled()

    fun ensureChannel() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        val manager = context.getSystemService(NotificationManager::class.java)
        manager.createNotificationChannel(
            NotificationChannel(
                CHANNEL_ID,
                context.getString(R.string.notification_channel_name),
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply {
                description = context.getString(R.string.notification_channel_description)
            }
        )
    }

    fun notify(candidate: ReminderCandidate, expandedLines: List<String> = emptyList()): Boolean {
        if (!canNotify()) return false
        ensureChannel()
        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
            putExtra(EXTRA_DESTINATION, candidate.destination.name)
        }
        val pendingIntent = PendingIntent.getActivity(
            context,
            candidate.deliveryKey.storageKey.hashCode(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        val styleText = expandedLines.takeIf(List<String>::isNotEmpty)?.joinToString("\n")
        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_launcher_monochrome)
            .setContentTitle(candidate.title)
            .setContentText(candidate.message)
            .setStyle(styleText?.let { NotificationCompat.BigTextStyle().bigText(it) })
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .setOnlyAlertOnce(true)
            .setCategory(NotificationCompat.CATEGORY_REMINDER)
            .build()
        return try {
            NotificationManagerCompat.from(context).notify(
                candidate.deliveryKey.storageKey.hashCode() and Int.MAX_VALUE,
                notification
            )
            true
        } catch (_: SecurityException) {
            false
        }
    }

    companion object {
        const val EXTRA_DESTINATION = "com.digitalbalance.app.extra.REMINDER_DESTINATION"
        private const val CHANNEL_ID = "digital_balance_reminders"

        fun destinationFrom(intent: Intent?): ReminderDestination? = intent
            ?.getStringExtra(EXTRA_DESTINATION)
            ?.let { stored -> ReminderDestination.entries.firstOrNull { it.name == stored } }
    }
}
