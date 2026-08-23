package com.example.brainclean.reminder

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.example.brainclean.MainActivity
import com.example.brainclean.data.BrainCleanDatabase
import com.example.brainclean.data.ThoughtRepository
import com.example.brainclean.domain.ThoughtCommandService
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class ThoughtReminderReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != ACTION_SHOW_REMINDER && intent.action != ACTION_SHOW_STALE_INBOX_REMINDER) {
            return
        }

        val pendingResult = goAsync()
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val thoughtId = intent.getLongExtra(EXTRA_THOUGHT_ID, -1L)
                val remindAt = intent.getLongExtra(EXTRA_REMIND_AT, -1L)
                val inboxEnteredAt = intent.getLongExtra(EXTRA_INBOX_ENTERED_AT, -1L)
                if (thoughtId == -1L || remindAt == -1L) return@launch

                val repository = ThoughtRepository(
                    BrainCleanDatabase.getDatabase(context).thoughtDao()
                )
                val commandService = ThoughtCommandService(
                    context = context.applicationContext,
                    repository = repository
                )

                when (intent.action) {
                    ACTION_SHOW_REMINDER -> {
                        val deliveredThought = commandService.consumeExplicitReminder(
                            id = thoughtId,
                            expectedRemindAt = remindAt
                        ) ?: return@launch

                        showNotification(
                            context = context,
                            notificationId = thoughtId,
                            title = "Thought reminder",
                            thoughtContent = deliveredThought.content
                        )
                    }

                    ACTION_SHOW_STALE_INBOX_REMINDER -> {
                        if (inboxEnteredAt == -1L) return@launch

                        val deliveredThought = commandService.consumeStaleInboxReminder(
                            id = thoughtId,
                            expectedInboxEnteredAt = inboxEnteredAt
                        ) ?: return@launch

                        showNotification(
                            context = context,
                            notificationId = thoughtId + STALE_NOTIFICATION_ID_OFFSET,
                            title = "Inbox thought still waiting",
                            thoughtContent = deliveredThought.content
                        )
                    }
                }
            } finally {
                pendingResult.finish()
            }
        }
    }

    private fun showNotification(
        context: Context,
        notificationId: Long,
        title: String,
        thoughtContent: String
    ) {
        createNotificationChannel(context)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.POST_NOTIFICATIONS
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            return
        }

        val contentIntent = PendingIntent.getActivity(
            context,
            notificationId.toInt(),
            Intent(context, MainActivity::class.java),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_popup_reminder)
            .setContentTitle(title)
            .setContentText(thoughtContent)
            .setStyle(NotificationCompat.BigTextStyle().bigText(thoughtContent))
            .setContentIntent(contentIntent)
            .setAutoCancel(true)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .build()

        NotificationManagerCompat.from(context).notify(notificationId.toInt(), notification)
    }

    companion object {
        const val ACTION_SHOW_REMINDER = "com.example.brainclean.action.SHOW_REMINDER"
        const val ACTION_SHOW_STALE_INBOX_REMINDER = "com.example.brainclean.action.SHOW_STALE_INBOX_REMINDER"
        const val EXTRA_THOUGHT_ID = "extra_thought_id"
        const val EXTRA_REMIND_AT = "extra_remind_at"
        const val EXTRA_INBOX_ENTERED_AT = "extra_inbox_entered_at"

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
