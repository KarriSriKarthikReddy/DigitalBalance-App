package com.digitalbalance.app.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "goals")
data class GoalEntity(
    @PrimaryKey val id: String,
    val typeKey: String,
    val targetDurationMillis: Long,
    val packageName: String?,
    val appName: String?,
    val updatedAtMillis: Long
)
