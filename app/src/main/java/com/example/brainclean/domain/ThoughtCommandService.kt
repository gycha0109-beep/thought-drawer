package com.example.brainclean.domain

import android.content.Context
import com.example.brainclean.data.ThoughtRepository
import com.example.brainclean.model.CaptureSource
import com.example.brainclean.model.Thought
import com.example.brainclean.model.ThoughtStatus
import com.example.brainclean.reminder.ReminderSyncResult
import com.example.brainclean.reminder.ThoughtReminderScheduler
import com.example.brainclean.widget.BrainCleanHomeWidget

sealed interface CaptureResult {
    data object Blank : CaptureResult

    data class Success(
        val thought: Thought,
        val reminderSyncResult: ReminderSyncResult
    ) : CaptureResult
}

class ThoughtCommandService(
    private val context: Context,
    private val repository: ThoughtRepository,
    private val reminderScheduler: ThoughtReminderScheduler = ThoughtReminderScheduler(context)
) {
    suspend fun captureThought(
        content: String,
        source: CaptureSource,
        remindAt: Long? = null
    ): CaptureResult {
        val trimmedContent = content.trim()
        if (trimmedContent.isBlank()) return CaptureResult.Blank

        val now = System.currentTimeMillis()
        val thought = Thought(
            id = now,
            content = trimmedContent,
            status = ThoughtStatus.INBOX,
            remindAt = remindAt,
            createdAt = now,
            inboxEnteredAt = now,
            captureSource = source
        )

        repository.insert(thought)
        val reminderSyncResult = reminderScheduler.syncReminder(thought)
        BrainCleanHomeWidget.refreshAll(context)

        return CaptureResult.Success(
            thought = thought,
            reminderSyncResult = reminderSyncResult
        )
    }

    suspend fun updateThoughtStatus(
        id: Long,
        status: ThoughtStatus
    ): ReminderSyncResult {
        val existingThought = repository.getThought(id) ?: return ReminderSyncResult()
        val now = System.currentTimeMillis()
        val updatedThought = when {
            status == ThoughtStatus.DONE -> existingThought.copy(
                status = status,
                remindAt = null,
                completedAt = now
            )

            status == ThoughtStatus.INBOX && existingThought.status != ThoughtStatus.INBOX ->
                existingThought.copy(
                    status = status,
                    completedAt = null,
                    inboxEnteredAt = now,
                    staleInboxReminderSentAt = null
                )

            else -> existingThought.copy(
                status = status,
                completedAt = null
            )
        }

        repository.update(updatedThought)
        val result = reminderScheduler.syncReminder(updatedThought)
        BrainCleanHomeWidget.refreshAll(context)
        return result
    }

    suspend fun updateThoughtContent(
        id: Long,
        content: String
    ): ReminderSyncResult {
        val trimmedContent = content.trim()
        if (trimmedContent.isBlank()) return ReminderSyncResult()

        val existingThought = repository.getThought(id) ?: return ReminderSyncResult()
        val updatedThought = existingThought.copy(
            content = trimmedContent,
            inboxEnteredAt = if (existingThought.status == ThoughtStatus.INBOX) {
                System.currentTimeMillis()
            } else {
                existingThought.inboxEnteredAt
            },
            staleInboxReminderSentAt = if (existingThought.status == ThoughtStatus.INBOX) {
                null
            } else {
                existingThought.staleInboxReminderSentAt
            }
        )

        repository.update(updatedThought)
        val result = reminderScheduler.syncReminder(updatedThought)
        BrainCleanHomeWidget.refreshAll(context)
        return result
    }

    suspend fun updateThoughtReminder(
        id: Long,
        remindAt: Long?
    ): ReminderSyncResult {
        val existingThought = repository.getThought(id) ?: return ReminderSyncResult()
        val updatedThought = existingThought.copy(remindAt = remindAt)

        repository.update(updatedThought)
        val result = reminderScheduler.syncReminder(updatedThought)
        BrainCleanHomeWidget.refreshAll(context)
        return result
    }

    suspend fun deleteThought(id: Long) {
        val existingThought = repository.getThought(id) ?: return

        reminderScheduler.cancelReminder(existingThought.id)
        reminderScheduler.cancelStaleInboxReminder(existingThought.id)
        repository.delete(existingThought)
        BrainCleanHomeWidget.refreshAll(context)
    }

    suspend fun deleteThoughts(ids: Set<Long>) {
        if (ids.isEmpty()) return

        val thoughts = repository.getAllThoughts().filter { it.id in ids }
        if (thoughts.isEmpty()) return

        thoughts.forEach { thought ->
            reminderScheduler.cancelReminder(thought.id)
            reminderScheduler.cancelStaleInboxReminder(thought.id)
        }
        repository.deleteMany(thoughts)
        BrainCleanHomeWidget.refreshAll(context)
    }

    suspend fun restoreThought(thought: Thought): ReminderSyncResult {
        repository.insert(thought)
        val result = reminderScheduler.syncReminder(thought)
        BrainCleanHomeWidget.refreshAll(context)
        return result
    }

    suspend fun syncScheduledReminders(): ReminderSyncResult {
        return repository.getAllThoughts().fold(ReminderSyncResult()) { acc, thought ->
            acc + reminderScheduler.syncReminder(thought)
        }
    }
}
