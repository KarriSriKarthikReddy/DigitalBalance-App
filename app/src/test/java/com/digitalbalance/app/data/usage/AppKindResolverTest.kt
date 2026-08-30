package com.digitalbalance.app.data.usage

import org.junit.Assert.assertEquals
import org.junit.Test

class AppKindResolverTest {
    @Test
    fun defaultHomeIsExcludedEvenWhenItHasLauncherActivities() {
        assertEquals(
            AppKind.Launcher,
            resolveAppKind(
                isDefaultHome = true,
                isLaunchable = true,
                isSystemApp = true,
                hasForegroundSessionEvidence = true
            )
        )
    }

    @Test
    fun nonDefaultSystemAppWithLauncherEntryIsUserFacing() {
        assertEquals(
            AppKind.SystemUserFacing,
            resolveAppKind(
                isDefaultHome = false,
                isLaunchable = true,
                isSystemApp = true,
                hasForegroundSessionEvidence = true
            )
        )
    }

    @Test
    fun foregroundSessionMakesNonLauncherSystemAppUserFacing() {
        assertEquals(
            AppKind.SystemUserFacing,
            resolveAppKind(
                isDefaultHome = false,
                isLaunchable = false,
                isSystemApp = true,
                hasForegroundSessionEvidence = true
            )
        )
    }

    @Test
    fun backgroundSystemComponentRemainsExcluded() {
        assertEquals(
            AppKind.BackgroundOrUnknown,
            resolveAppKind(
                isDefaultHome = false,
                isLaunchable = false,
                isSystemApp = true,
                hasForegroundSessionEvidence = false
            )
        )
    }

    @Test
    fun launchableDigitalBalanceAppUsesNormalUserFacingClassification() {
        assertEquals(
            AppKind.UserFacing,
            resolveAppKind(
                isDefaultHome = false,
                isLaunchable = true,
                isSystemApp = false,
                hasForegroundSessionEvidence = true
            )
        )
    }
}
