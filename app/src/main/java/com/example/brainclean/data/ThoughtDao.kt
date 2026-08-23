package com.example.brainclean.data

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface ThoughtDao {
    @Query("SELECT * FROM thoughts ORDER BY createdAt DESC")
    suspend fun getAllThoughts(): List<ThoughtEntity>

    @Query("SELECT * FROM thoughts WHERE id = :id LIMIT 1")
    suspend fun getThoughtById(id: Long): ThoughtEntity?

    @Query("SELECT * FROM thoughts ORDER BY createdAt DESC")
    fun observeAllThoughts(): Flow<List<ThoughtEntity>>

    @Query("SELECT * FROM thoughts WHERE status = :status ORDER BY createdAt DESC LIMIT :limit")
    suspend fun getLatestThoughtsByStatus(status: String, limit: Int): List<ThoughtEntity>

    @Query("SELECT * FROM thoughts WHERE remindAt IS NOT NULL AND status != :doneStatus")
    suspend fun getThoughtsWithReminders(doneStatus: String): List<ThoughtEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertThought(thought: ThoughtEntity)

    @Update
    suspend fun updateThought(thought: ThoughtEntity)

    @Delete
    suspend fun deleteThought(thought: ThoughtEntity)
}
