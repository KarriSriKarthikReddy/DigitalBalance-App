package com.digitalbalance.app.data.local

import com.digitalbalance.app.domain.goal.DigitalGoal
import com.digitalbalance.app.domain.goal.GoalType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class GoalMappingsTest {
    @Test
    fun `goal entity round trips stable fields`() {
        val goal = DigitalGoal(
            id = DigitalGoal.idFor(GoalType.AppDailyLimit, "example.app"),
            type = GoalType.AppDailyLimit,
            targetDurationMillis = 900_000L,
            packageName = "example.app",
            appName = "Example"
        )

        val entity = goal.toEntity(updatedAtMillis = 123L)

        assertEquals(goal, entity.toDomain())
        assertEquals(123L, entity.updatedAtMillis)
    }

    @Test
    fun `invalid persisted goals are ignored`() {
        assertNull(
            GoalEntity("bad", "unknown", 1L, null, null, 1L).toDomain()
        )
        assertNull(
            GoalEntity(
                "bad-app",
                GoalType.AppDailyLimit.storageKey,
                1L,
                null,
                null,
                1L
            ).toDomain()
        )
    }
}
