package com.digitalbalance.app.data.usage

import org.junit.Assert.assertEquals
import org.junit.Test

class AppKindResolverTest {
    @Test
    fun defaultHomeIsExcludedEvenWhenItHasLauncherActivities() {
        assertEquals(
            AppKind.Launcher,
            resolveAppKind(
                isDigitalBalance = false,
                isDefaultHome = true,
                isLaunchable = true,
                isSystemApp = true,
                hasExportedForegroundActivity = true
            )
        )
    }

    @Test
    fun nonDefaultSystemAppWithLauncherEntryIsUserFacing() {
        assertEquals(
            AppKind.SystemUserFacing,
            resolveAppKind(
                isDigitalBalance = false,
                isDefaultHome = false,
                isLaunchable = true,
                isSystemApp = true,
                hasExportedForegroundActivity = false
            )
        )
    }

    @Test
    fun exportedForegroundActivityMakesNonLauncherSystemAppUserFacing() {
        assertEquals(
            AppKind.SystemUserFacing,
            resolveAppKind(
                isDigitalBalance = false,
                isDefaultHome = false,
                isLaunchable = false,
                isSystemApp = true,
                hasExportedForegroundActivity = true
            )
        )
    }

    @Test
    fun backgroundSystemComponentRemainsExcluded() {
        assertEquals(
            AppKind.BackgroundOrUnknown,
            resolveAppKind(
                isDigitalBalance = false,
                isDefaultHome = false,
                isLaunchable = false,
                isSystemApp = true,
                hasExportedForegroundActivity = false
            )
        )
    }

    @Test
    fun digitalBalanceRemainsExcluded() {
        assertEquals(
            AppKind.DigitalBalance,
            resolveAppKind(
                isDigitalBalance = true,
                isDefaultHome = false,
                isLaunchable = true,
                isSystemApp = false,
                hasExportedForegroundActivity = true
            )
        )
    }
}
