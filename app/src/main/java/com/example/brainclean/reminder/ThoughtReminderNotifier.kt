package com.example.brainclean.reminder

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
import com.example.brainclean.MainActivity

class ThoughtReminderNotifier(
    context: Context
) {
    private val appContext = context.applicationContext

    fun showExplicitReminder(thoughtId: Long, content: String) {
        showNotification(
            notificationId = thoughtId,
            title = "Thought reminder",
            thoughtContent = content
        )
    }

    fun showStaleInboxReminder(thoughtId: Long, content: String) {
        showNotification(
            notificationId = thoughtId + STALE_NOTIFICATION_ID_OFFSET,
            title = "Inbox thought still waiting",
            thoughtContent = content
        )
    }

    private fun showNotification(
        notificationId: Long,
        title: String,
        thoughtContent: String
    ) {
        createNotificationChannel(appContext)

        if (
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            ContextCompat.checkSelfPermission(
                appContext,
                Manifest.permission.POST_NOTIFICATIONS
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            return
        }

        val contentIntent = PendingIntent.getActivity(
            appContext,
            notificationId.toInt(),
            Intent(appContext, MainActivity::class.java),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(appContext, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_popup_reminder)
            .setContentTitle(title)
            .setContentText(thoughtContent)
            .setStyle(NotificationCompat.BigTextStyle().bigText(thoughtContent))
            .setContentIntent(contentIntent)
            .setAutoCancel(true)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .build()

        NotificationManagerCompat.from(appContext)
            .notify(notificationId.toInt(), notification)
    }

    companion object {
        private const val CHANNEL_ID = "thought_reminders"
        private const val STALE_NOTIFICATION_ID_OFFSET = 1_000_000L

        fun createNotificationChannel(context: Context) {
            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return

            val manager = context.getSystemService(NotificationManager::class.java)
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Thought reminders",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Notifications for scheduled thought reminders"
            }

            manager.createNotificationChannel(channel)
        }
    }
}
