package com.example.brainclean.reminder

import android.Manifest
import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.content.ContextCompat
import com.example.brainclean.model.Thought

data class ReminderSyncResult(
    val needsExactAlarmPermission: Boolean = false,
    val needsNotificationPermission: Boolean = false
) {
    operator fun plus(other: ReminderSyncResult): ReminderSyncResult {
        return ReminderSyncResult(
            needsExactAlarmPermission = needsExactAlarmPermission || other.needsExactAlarmPermission,
            needsNotificationPermission = needsNotificationPermission || other.needsNotificationPermission
        )
    }
}

class ThoughtReminderScheduler(
    private val context: Context
) {
    private val alarmManager = context.getSystemService(AlarmManager::class.java)

    fun syncReminder(thought: Thought): ReminderSyncResult {
        cancelReminder(thought.id)
        cancelStaleInboxReminder(thought.id)

        val scheduledTimes = buildList {
            thought.remindAt
                ?.takeIf { it > System.currentTimeMillis() }
                ?.let { add(ReminderSpec(AlarmType.STANDARD, it)) }

            if (
                thought.status.name == "INBOX" &&
                thought.staleInboxReminderSentAt == null
            ) {
                val staleTriggerAt = thought.inboxEnteredAt + STALE_INBOX_REMINDER_DELAY_MS
                if (staleTriggerAt > System.currentTimeMillis()) {
                    add(ReminderSpec(AlarmType.STALE_INBOX, staleTriggerAt))
                }
            }
        }

        if (scheduledTimes.isEmpty()) return ReminderSyncResult()

        val needsExactAlarmPermission = Build.VERSION.SDK_INT >= Build.VERSION_CODES.S &&
            !alarmManager.canScheduleExactAlarms()
        val needsNotificationPermission = Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.POST_NOTIFICATIONS
            ) != PackageManager.PERMISSION_GRANTED

        if (!needsExactAlarmPermission) {
            scheduledTimes.forEach { reminderSpec ->
                alarmManager.setExactAndAllowWhileIdle(
                    AlarmManager.RTC_WAKEUP,
                    reminderSpec.triggerAt,
                    createPendingIntent(
                        thoughtId = thought.id,
                        triggerAt = reminderSpec.triggerAt,
                        alarmType = reminderSpec.alarmType,
                        inboxEnteredAt = thought.inboxEnteredAt
                    )
                )
            }
        }

        return ReminderSyncResult(
            needsExactAlarmPermission = needsExactAlarmPermission,
            needsNotificationPermission = needsNotificationPermission
        )
    }

    fun cancelReminder(thoughtId: Long) {
        alarmManager.cancel(
            createPendingIntent(
                thoughtId = thoughtId,
                triggerAt = 0L,
                alarmType = AlarmType.STANDARD,
                inboxEnteredAt = 0L
            )
        )
    }

    fun cancelStaleInboxReminder(thoughtId: Long) {
        alarmManager.cancel(
            createPendingIntent(
                thoughtId = thoughtId,
                triggerAt = 0L,
                alarmType = AlarmType.STALE_INBOX,
                inboxEnteredAt = 0L
            )
        )
    }

    private fun createPendingIntent(
        thoughtId: Long,
        triggerAt: Long,
        alarmType: AlarmType,
        inboxEnteredAt: Long
    ): PendingIntent {
        val intent = Intent(context, ThoughtReminderReceiver::class.java)
            .setAction(alarmType.action)
            .putExtra(ThoughtReminderReceiver.EXTRA_THOUGHT_ID, thoughtId)
            .putExtra(ThoughtReminderReceiver.EXTRA_REMIND_AT, triggerAt)
            .putExtra(ThoughtReminderReceiver.EXTRA_INBOX_ENTERED_AT, inboxEnteredAt)

        return PendingIntent.getBroadcast(
            context,
            alarmType.requestCodeFor(thoughtId),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }

    private data class ReminderSpec(
        val alarmType: AlarmType,
        val triggerAt: Long
    )

    private enum class AlarmType(val action: String) {
        STANDARD(ThoughtReminderReceiver.ACTION_SHOW_REMINDER),
        STALE_INBOX(ThoughtReminderReceiver.ACTION_SHOW_STALE_INBOX_REMINDER);

        fun requestCodeFor(thoughtId: Long): Int {
            return (thoughtId.toInt() * 31) + ordinal
        }
    }

    companion object {
        private const val STALE_INBOX_REMINDER_DELAY_MS = 24 * 60 * 60 * 1000L
    }
}
