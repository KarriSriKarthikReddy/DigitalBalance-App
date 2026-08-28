package com.digitalbalance.app.data.local

import com.digitalbalance.app.domain.goal.DigitalGoal
import com.digitalbalance.app.domain.goal.GoalType

fun DigitalGoal.toEntity(updatedAtMillis: Long): GoalEntity = GoalEntity(
    id = id,
    typeKey = type.storageKey,
    targetDurationMillis = targetDurationMillis,
    packageName = packageName,
    appName = appName,
    updatedAtMillis = updatedAtMillis
)

fun GoalEntity.toDomain(): DigitalGoal? {
    val type = GoalType.fromStorageKey(typeKey) ?: return null
    if (targetDurationMillis <= 0L) return null
    if (type == GoalType.AppDailyLimit && packageName.isNullOrBlank()) return null
    return DigitalGoal(
        id = id,
        type = type,
        targetDurationMillis = targetDurationMillis,
        packageName = packageName,
        appName = appName
    )
}
