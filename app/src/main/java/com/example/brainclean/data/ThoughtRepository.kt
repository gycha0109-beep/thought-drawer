package com.example.brainclean.data

import com.example.brainclean.model.Thought
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class ThoughtRepository(
    private val thoughtDao: ThoughtDao
) {
    fun observeThoughts(): Flow<List<Thought>> {
        return thoughtDao.observeAllThoughts().map { entities ->
            entities.map { it.toThought() }
        }
    }

    suspend fun getAllThoughts(): List<Thought> {
        return thoughtDao.getAllThoughts().map { it.toThought() }
    }

    suspend fun getThought(id: Long): Thought? {
        return thoughtDao.getThoughtById(id)?.toThought()
    }

    suspend fun insert(thought: Thought) {
        thoughtDao.insertThought(ThoughtEntity.fromThought(thought))
    }

    suspend fun update(thought: Thought) {
        thoughtDao.updateThought(ThoughtEntity.fromThought(thought))
    }

    suspend fun delete(thought: Thought) {
        thoughtDao.deleteThought(ThoughtEntity.fromThought(thought))
    }

    suspend fun deleteMany(thoughts: List<Thought>) {
        thoughts.forEach { thought ->
            thoughtDao.deleteThought(ThoughtEntity.fromThought(thought))
        }
    }
}
