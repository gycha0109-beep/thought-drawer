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

        val remindAt = thought.remindAt ?: return ReminderSyncResult()
        if (remindAt <= System.currentTimeMillis()) return ReminderSyncResult()

        val needsExactAlarmPermission = Build.VERSION.SDK_INT >= Build.VERSION_CODES.S &&
            !alarmManager.canScheduleExactAlarms()
        val needsNotificationPermission = Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.POST_NOTIFICATIONS
            ) != PackageManager.PERMISSION_GRANTED

        if (!needsExactAlarmPermission) {
            alarmManager.setExactAndAllowWhileIdle(
                AlarmManager.RTC_WAKEUP,
                remindAt,
                createPendingIntent(thought.id, remindAt)
            )
        }

        return ReminderSyncResult(
            needsExactAlarmPermission = needsExactAlarmPermission,
            needsNotificationPermission = needsNotificationPermission
        )
    }

    fun cancelReminder(thoughtId: Long) {
        alarmManager.cancel(createPendingIntent(thoughtId, 0L))
    }

    private fun createPendingIntent(
        thoughtId: Long,
        remindAt: Long
    ): PendingIntent {
        val intent = Intent(context, ThoughtReminderReceiver::class.java)
            .setAction(ThoughtReminderReceiver.ACTION_SHOW_REMINDER)
            .putExtra(ThoughtReminderReceiver.EXTRA_THOUGHT_ID, thoughtId)
            .putExtra(ThoughtReminderReceiver.EXTRA_REMIND_AT, remindAt)

        return PendingIntent.getBroadcast(
            context,
            thoughtId.hashCode(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }
}
