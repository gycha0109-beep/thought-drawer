package com.example.brainclean.model

data class Thought(
    val id: Long,
    val content: String,
    val status: ThoughtStatus,
    val remindAt: Long? = null
)
