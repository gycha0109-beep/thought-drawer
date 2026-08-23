package com.example.brainclean.reminder

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.content.ContextCompat
import androidx.work.Data
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequest
import androidx.work.WorkManager
import com.example.brainclean.model.Thought
import com.example.brainclean.model.ThoughtStatus
import java.util.concurrent.TimeUnit
import kotlin.math.max

class StaleInboxScheduler(
    context: Context
) {
    private val appContext = context.applicationContext
    private val workManager = WorkManager.getInstance(appContext)

    fun sync(thought: Thought): ReminderSyncResult {
        if (
            thought.status != ThoughtStatus.INBOX ||
            thought.staleInboxReminderSentAt != null
        ) {
            cancel(thought.id)
            return ReminderSyncResult()
        }

        val triggerAt = thought.inboxEnteredAt + STALE_INBOX_DELAY_MS
        val delayMs = max(0L, triggerAt - System.currentTimeMillis())
        val inputData = Data.Builder()
            .putLong(StaleInboxWorker.KEY_THOUGHT_ID, thought.id)
            .putLong(StaleInboxWorker.KEY_EXPECTED_INBOX_ENTERED_AT, thought.inboxEnteredAt)
            .build()
        val request = OneTimeWorkRequest.Builder(StaleInboxWorker::class.java)
            .setInitialDelay(delayMs, TimeUnit.MILLISECONDS)
            .setInputData(inputData)
            .build()

        workManager.enqueueUniqueWork(
            workName(thought.id),
            ExistingWorkPolicy.REPLACE,
            request
        )

        return ReminderSyncResult(
            needsNotificationPermission = Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
                ContextCompat.checkSelfPermission(
                    appContext,
                    Manifest.permission.POST_NOTIFICATIONS
                ) != PackageManager.PERMISSION_GRANTED
        )
    }

    fun cancel(thoughtId: Long) {
        workManager.cancelUniqueWork(workName(thoughtId))
    }

    private fun workName(thoughtId: Long): String = "stale_inbox_$thoughtId"

    companion object {
        private const val STALE_INBOX_DELAY_MS = 24L * 60L * 60L * 1000L
    }
}
