package com.example.brainclean.reminder

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.example.brainclean.data.BrainCleanDatabase
import com.example.brainclean.data.ThoughtRepository
import com.example.brainclean.domain.ThoughtCommandService

class StaleInboxWorker(
    appContext: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(appContext, workerParams) {

    override suspend fun doWork(): Result {
        val thoughtId = inputData.getLong(KEY_THOUGHT_ID, INVALID_ID)
        val expectedInboxEnteredAt = inputData.getLong(
            KEY_EXPECTED_INBOX_ENTERED_AT,
            INVALID_TIMESTAMP
        )
        if (thoughtId == INVALID_ID || expectedInboxEnteredAt == INVALID_TIMESTAMP) {
            return Result.success()
        }

        if (!ThoughtReminderNotifier.canPostNotifications(applicationContext)) {
            // Keep staleInboxReminderSentAt untouched. When notification capability returns,
            // the next command-service sync can enqueue a due work item immediately.
            return Result.success()
        }

        return try {
            val repository = ThoughtRepository(
                BrainCleanDatabase.getDatabase(applicationContext).thoughtDao()
            )
            val commandService = ThoughtCommandService(
                context = applicationContext,
                repository = repository
            )
            val deliveredThought = commandService.consumeStaleInboxReminder(
                id = thoughtId,
                expectedInboxEnteredAt = expectedInboxEnteredAt
            ) ?: return Result.success()

            ThoughtReminderNotifier(applicationContext).showStaleInboxReminder(
                thoughtId = deliveredThought.id,
                content = deliveredThought.content
            )
            Result.success()
        } catch (_: Exception) {
            Result.retry()
        }
    }

    companion object {
        const val KEY_THOUGHT_ID = "thought_id"
        const val KEY_EXPECTED_INBOX_ENTERED_AT = "expected_inbox_entered_at"

        private const val INVALID_ID = -1L
        private const val INVALID_TIMESTAMP = -1L
    }
}
