package com.digitalbalance.app.domain.category

import android.content.pm.ApplicationInfo

class DefaultCategoryResolver {
    fun resolve(packageName: String, applicationCategory: Int?): AppCategory {
        exactDefaults[packageName]?.let { return it }
        if (packageName in mixedPurposePackages) return AppCategory.MixedContextDependent

        return when (applicationCategory) {
            ApplicationInfo.CATEGORY_GAME -> AppCategory.Gaming
            ApplicationInfo.CATEGORY_AUDIO,
            ApplicationInfo.CATEGORY_VIDEO -> AppCategory.Entertainment
            ApplicationInfo.CATEGORY_SOCIAL -> AppCategory.Social
            ApplicationInfo.CATEGORY_PRODUCTIVITY -> AppCategory.Productivity
            ApplicationInfo.CATEGORY_MAPS -> AppCategory.Utility
            else -> AppCategory.Other
        }
    }

    private companion object {
        val mixedPurposePackages = setOf(
            "com.google.android.youtube",
            "com.android.chrome",
            "com.chrome.beta",
            "com.chrome.dev",
            "org.mozilla.firefox",
            "com.microsoft.emmx",
            "org.telegram.messenger",
            "org.telegram.messenger.web",
            "com.reddit.frontpage"
        )

        val exactDefaults = mapOf(
            "com.digitalbalance.app" to AppCategory.Utility,
            "com.duolingo" to AppCategory.Education,
            "org.khanacademy.android" to AppCategory.Education,
            "com.google.android.apps.classroom" to AppCategory.Education,
            "com.google.android.gm" to AppCategory.Communication,
            "com.whatsapp" to AppCategory.Communication,
            "org.thoughtcrime.securesms" to AppCategory.Communication,
            "com.instagram.android" to AppCategory.Social,
            "com.facebook.katana" to AppCategory.Social,
            "com.zhiliaoapp.musically" to AppCategory.Social,
            "com.netflix.mediaclient" to AppCategory.Entertainment,
            "com.spotify.music" to AppCategory.Entertainment,
            "com.google.android.calendar" to AppCategory.Productivity,
            "com.google.android.apps.docs.editors.docs" to AppCategory.Productivity,
            "com.microsoft.office.officehubrow" to AppCategory.Productivity,
            "notion.id" to AppCategory.Productivity,
            "com.google.android.apps.maps" to AppCategory.Utility,
            "com.google.android.calculator" to AppCategory.Utility,
            "com.google.android.documentsui" to AppCategory.Utility,
            "com.android.settings" to AppCategory.Utility
        )
    }
}

fun categoryWithOverride(
    defaultCategory: AppCategory,
    userOverride: AppCategory?
): AppCategory = userOverride ?: defaultCategory
