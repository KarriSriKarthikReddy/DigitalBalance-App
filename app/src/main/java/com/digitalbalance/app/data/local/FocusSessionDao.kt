package com.digitalbalance.app.data.local

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import kotlinx.coroutines.flow.Flow

@Dao
interface FocusSessionDao {
    @Query("SELECT * FROM focus_sessions ORDER BY startedAtEpochMillis DESC")
    fun observeSessions(): Flow<List<FocusSessionEntity>>

    @Query(
        "SELECT * FROM focus_sessions " +
            "WHERE statusKey IN ('running', 'paused') " +
            "ORDER BY updatedAtEpochMillis DESC LIMIT 1"
    )
    suspend fun activeSession(): FocusSessionEntity?

    @Upsert
    suspend fun upsert(session: FocusSessionEntity)
}
