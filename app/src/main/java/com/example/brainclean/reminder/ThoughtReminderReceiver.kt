package com.example.brainclean.reminder

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.example.brainclean.capture.CaptureNotificationManager
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
                if (thoughtId == -1L) return@launch

                val repository = ThoughtRepository(
                    BrainCleanDatabase.getDatabase(context).thoughtDao()
                )
                val commandService = ThoughtCommandService(
                    context = context.applicationContext,
                    repository = repository
                )
                val notifier = ThoughtReminderNotifier(context)

                when (intent.action) {
                    ACTION_SHOW_REMINDER -> {
                        if (remindAt == -1L) return@launch

                        val deliveredThought = commandService.consumeExplicitReminder(
                            id = thoughtId,
                            expectedRemindAt = remindAt
                        ) ?: return@launch

                        notifier.showExplicitReminder(
                            thoughtId = deliveredThought.id,
                            content = deliveredThought.content
                        )
                    }

                    ACTION_SHOW_STALE_INBOX_REMINDER -> {
                        if (inboxEnteredAt == -1L) return@launch

                        val deliveredThought = commandService.consumeStaleInboxReminder(
                            id = thoughtId,
                            expectedInboxEnteredAt = inboxEnteredAt
                        ) ?: return@launch

                        notifier.showStaleInboxReminder(
                            thoughtId = deliveredThought.id,
                            content = deliveredThought.content
                        )
                    }
                }
            } finally {
                pendingResult.finish()
            }
        }
    }

    companion object {
        const val ACTION_SHOW_REMINDER = "com.example.brainclean.action.SHOW_REMINDER"

        // Kept temporarily so stale AlarmManager intents created by V1 can drain safely
        // after an app upgrade. V2 does not schedule new stale alarms with AlarmManager.
        const val ACTION_SHOW_STALE_INBOX_REMINDER =
            "com.example.brainclean.action.SHOW_STALE_INBOX_REMINDER"

        const val EXTRA_THOUGHT_ID = "extra_thought_id"
        const val EXTRA_REMIND_AT = "extra_remind_at"
        const val EXTRA_INBOX_ENTERED_AT = "extra_inbox_entered_at"

        fun createNotificationChannel(context: Context) {
            ThoughtReminderNotifier.createNotificationChannel(context)
            CaptureNotificationManager.ensureVisible(context)
        }
    }
}
