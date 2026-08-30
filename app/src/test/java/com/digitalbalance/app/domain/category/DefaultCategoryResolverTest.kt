package com.digitalbalance.app.domain.category

import android.content.pm.ApplicationInfo
import org.junit.Assert.assertEquals
import org.junit.Test

class DefaultCategoryResolverTest {
    private val resolver = DefaultCategoryResolver()

    @Test
    fun `multipurpose apps remain mixed despite store metadata`() {
        listOf(
            "com.google.android.youtube",
            "com.android.chrome",
            "org.telegram.messenger",
            "com.reddit.frontpage"
        ).forEach { packageName ->
            assertEquals(
                AppCategory.MixedContextDependent,
                resolver.resolve(packageName, ApplicationInfo.CATEGORY_PRODUCTIVITY)
            )
        }
    }

    @Test
    fun `reliable application metadata supplies a default`() {
        assertEquals(
            AppCategory.Gaming,
            resolver.resolve("example.game", ApplicationInfo.CATEGORY_GAME)
        )
        assertEquals(
            AppCategory.Productivity,
            resolver.resolve("example.work", ApplicationInfo.CATEGORY_PRODUCTIVITY)
        )
    }

    @Test
    fun `unknown apps default to other`() {
        assertEquals(AppCategory.Other, resolver.resolve("example.unknown", null))
    }

    @Test
    fun `digital balance defaults to neutral utility`() {
        assertEquals(
            AppCategory.Utility,
            resolver.resolve("com.digitalbalance.app", ApplicationInfo.CATEGORY_PRODUCTIVITY)
        )
    }

    @Test
    fun `user override always takes precedence`() {
        assertEquals(
            AppCategory.Education,
            categoryWithOverride(AppCategory.Social, AppCategory.Education)
        )
        assertEquals(
            AppCategory.Social,
            categoryWithOverride(AppCategory.Social, null)
        )
    }
}
