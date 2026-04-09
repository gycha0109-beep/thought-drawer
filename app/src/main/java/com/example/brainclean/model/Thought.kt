package com.example.brainclean.model

data class Thought(
    val id: Long,
    val content: String,
    val status: ThoughtStatus,
    val remindAt: Long? = null,
    val createdAt: Long,
    val completedAt: Long? = null,
    val inboxEnteredAt: Long,
    val staleInboxReminderSentAt: Long? = null
)
