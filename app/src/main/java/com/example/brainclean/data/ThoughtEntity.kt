package com.example.brainclean.data

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.example.brainclean.model.Thought
import com.example.brainclean.model.ThoughtStatus

@Entity(tableName = "thoughts")
data class ThoughtEntity(
    @PrimaryKey val id: Long,
    val content: String,
    val status: String,
    val remindAt: Long? = null,
    val createdAt: Long,
    val completedAt: Long? = null,
    val inboxEnteredAt: Long,
    val staleInboxReminderSentAt: Long? = null
) {
    fun toThought(): Thought {
        return Thought(
            id = id,
            content = content,
            status = ThoughtStatus.valueOf(status),
            remindAt = remindAt,
            createdAt = createdAt,
            completedAt = completedAt,
            inboxEnteredAt = inboxEnteredAt,
            staleInboxReminderSentAt = staleInboxReminderSentAt
        )
    }

    companion object {
        fun fromThought(thought: Thought): ThoughtEntity {
            return ThoughtEntity(
                id = thought.id,
                content = thought.content,
                status = thought.status.name,
                remindAt = thought.remindAt,
                createdAt = thought.createdAt,
                completedAt = thought.completedAt,
                inboxEnteredAt = thought.inboxEnteredAt,
                staleInboxReminderSentAt = thought.staleInboxReminderSentAt
            )
        }
    }
}
