package com.digitalbalance.app.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "category_overrides")
data class CategoryOverrideEntity(
    @PrimaryKey val packageName: String,
    val categoryKey: String,
    val updatedAtMillis: Long
)
