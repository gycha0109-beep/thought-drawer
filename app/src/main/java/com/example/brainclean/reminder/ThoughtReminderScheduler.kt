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
import kotlin.math.max

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

        val expectedRemindAt = thought.remindAt ?: return ReminderSyncResult()
        val now = System.currentTimeMillis()
        val alarmAt = max(expectedRemindAt, now + OVERDUE_RETRY_DELAY_MS)

        val needsExactAlarmPermission = Build.VERSION.SDK_INT >= Build.VERSION_CODES.S &&
            !alarmManager.canScheduleExactAlarms()
        val needsNotificationPermission = Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.POST_NOTIFICATIONS
            ) != PackageManager.PERMISSION_GRANTED
        val pendingIntent = createStandardPendingIntent(
            thoughtId = thought.id,
            expectedRemindAt = expectedRemindAt
        )

        if (needsExactAlarmPermission) {
            alarmManager.setAndAllowWhileIdle(
                AlarmManager.RTC_WAKEUP,
                alarmAt,
                pendingIntent
            )
        } else {
            alarmManager.setExactAndAllowWhileIdle(
                AlarmManager.RTC_WAKEUP,
                alarmAt,
                pendingIntent
            )
        }

        return ReminderSyncResult(
            needsExactAlarmPermission = needsExactAlarmPermission,
            needsNotificationPermission = needsNotificationPermission
        )
    }

    fun cancelReminder(thoughtId: Long) {
        alarmManager.cancel(
            createStandardPendingIntent(
                thoughtId = thoughtId,
                expectedRemindAt = 0L
            )
        )
    }

    fun cancelLegacyStaleInboxReminder(thoughtId: Long) {
        alarmManager.cancel(
            createLegacyStalePendingIntent(
                thoughtId = thoughtId,
                triggerAt = 0L,
                inboxEnteredAt = 0L
            )
        )
    }

    private fun createStandardPendingIntent(
        thoughtId: Long,
        expectedRemindAt: Long
    ): PendingIntent {
        val intent = Intent(context, ThoughtReminderReceiver::class.java)
            .setAction(ThoughtReminderReceiver.ACTION_SHOW_REMINDER)
            .putExtra(ThoughtReminderReceiver.EXTRA_THOUGHT_ID, thoughtId)
            .putExtra(ThoughtReminderReceiver.EXTRA_REMIND_AT, expectedRemindAt)

        return PendingIntent.getBroadcast(
            context,
            standardRequestCodeFor(thoughtId),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }

    private fun createLegacyStalePendingIntent(
        thoughtId: Long,
        triggerAt: Long,
        inboxEnteredAt: Long
    ): PendingIntent {
        val intent = Intent(context, ThoughtReminderReceiver::class.java)
            .setAction(ThoughtReminderReceiver.ACTION_SHOW_STALE_INBOX_REMINDER)
            .putExtra(ThoughtReminderReceiver.EXTRA_THOUGHT_ID, thoughtId)
            .putExtra(ThoughtReminderReceiver.EXTRA_REMIND_AT, triggerAt)
            .putExtra(ThoughtReminderReceiver.EXTRA_INBOX_ENTERED_AT, inboxEnteredAt)

        return PendingIntent.getBroadcast(
            context,
            legacyStaleRequestCodeFor(thoughtId),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }

    private fun standardRequestCodeFor(thoughtId: Long): Int {
        return thoughtId.toInt() * REQUEST_CODE_MULTIPLIER
    }

    private fun legacyStaleRequestCodeFor(thoughtId: Long): Int {
        return (thoughtId.toInt() * REQUEST_CODE_MULTIPLIER) + 1
    }

    companion object {
        private const val REQUEST_CODE_MULTIPLIER = 31
        private const val OVERDUE_RETRY_DELAY_MS = 1_000L
    }
}
