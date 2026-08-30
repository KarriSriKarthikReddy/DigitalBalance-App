package com.digitalbalance.app.data.usage

import android.content.Context
import android.content.ComponentName
import android.content.Intent
import android.content.pm.ActivityInfo
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import android.content.pm.ResolveInfo
import android.graphics.Bitmap
import android.os.Build
import android.util.Log
import android.util.LruCache
import androidx.core.graphics.drawable.toBitmap

enum class AppKind(val includedInPrimaryUsage: Boolean) {
    UserFacing(true),
    SystemUserFacing(true),
    Launcher(false),
    BackgroundOrUnknown(false)
}

data class ClassifiedApp(
    val packageName: String,
    val label: String,
    val kind: AppKind
)

class AppClassifier(context: Context) {
    private val appContext = context.applicationContext
    private val packageManager = appContext.packageManager
    private val iconSizePixels = (48 * appContext.resources.displayMetrics.density)
        .toInt()
        .coerceIn(48, 96)
    private val iconCache = object : LruCache<String, Bitmap>(ICON_CACHE_KILOBYTES) {
        override fun sizeOf(key: String, value: Bitmap): Int = value.byteCount / 1024
    }
    private val launchablePackages by lazy {
        queryPackages(Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER))
    }
    private val homeCapablePackages by lazy {
        queryPackages(Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_HOME))
    }
    private val defaultHomePackage by lazy {
        resolveDefaultHomePackage()
    }
    private val debugLoggingEnabled =
        appContext.applicationInfo.flags and ApplicationInfo.FLAG_DEBUGGABLE != 0

    fun classify(
        packageName: String,
        foregroundActivityClassNames: Set<String> = emptySet(),
        hasForegroundSessionEvidence: Boolean = false
    ): ClassifiedApp {
        val applicationInfo = getApplicationInfo(packageName)
        val isLaunchable = packageName in launchablePackages
        val isDefaultHome = packageName == defaultHomePackage
        val hasExportedForegroundActivity = foregroundActivityClassNames.any { className ->
            getActivityInfo(packageName, className)?.let { it.exported && it.enabled } == true
        }
        val isSystemApp = applicationInfo?.flags?.let { flags ->
            flags and (ApplicationInfo.FLAG_SYSTEM or ApplicationInfo.FLAG_UPDATED_SYSTEM_APP) != 0
        } ?: false
        val kind = resolveAppKind(
            isDefaultHome = isDefaultHome,
            isLaunchable = isLaunchable,
            isSystemApp = isSystemApp,
            hasForegroundSessionEvidence = hasForegroundSessionEvidence
        )
        val label = applicationInfo?.let(packageManager::getApplicationLabel)
            ?.toString()
            ?.takeIf(String::isNotBlank)
            ?: packageName
        if (
            debugLoggingEnabled &&
            (packageName == GOOGLE_SEARCH_PACKAGE || (!isLaunchable && hasExportedForegroundActivity))
        ) {
            Log.d(
                DEBUG_TAG,
                "FOREGROUND_CLASSIFICATION package=$packageName launchable=$isLaunchable " +
                    "homeCapable=${packageName in homeCapablePackages} " +
                    "defaultHome=$isDefaultHome system=$isSystemApp " +
                    "foregroundSession=$hasForegroundSessionEvidence " +
                    "exportedForegroundActivity=$hasExportedForegroundActivity " +
                    "activityClasses=$foregroundActivityClassNames kind=$kind"
            )
        }
        return ClassifiedApp(packageName, label, kind)
    }

    fun loadIcon(packageName: String): Bitmap? {
        iconCache.get(packageName)?.let { return it }
        val icon = try {
            packageManager.getApplicationIcon(packageName).toBitmap(
                width = iconSizePixels,
                height = iconSizePixels,
                config = Bitmap.Config.ARGB_8888
            )
        } catch (_: PackageManager.NameNotFoundException) {
            null
        } catch (_: RuntimeException) {
            null
        }
        if (icon != null) iconCache.put(packageName, icon)
        return icon
    }

    fun applicationCategory(packageName: String): Int? {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return null
        return getApplicationInfo(packageName)?.category
            ?.takeUnless { it == ApplicationInfo.CATEGORY_UNDEFINED }
    }

    private fun resolveLabel(packageName: String): String {
        val info = getApplicationInfo(packageName) ?: return packageName
        return packageManager.getApplicationLabel(info).toString().ifBlank { packageName }
    }

    private fun getApplicationInfo(packageName: String): ApplicationInfo? = try {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            packageManager.getApplicationInfo(
                packageName,
                PackageManager.ApplicationInfoFlags.of(0)
            )
        } else {
            @Suppress("DEPRECATION")
            packageManager.getApplicationInfo(packageName, 0)
        }
    } catch (_: PackageManager.NameNotFoundException) {
        null
    } catch (_: RuntimeException) {
        null
    }

    private fun getActivityInfo(packageName: String, className: String): ActivityInfo? = try {
        val resolvedClassName = if (className.startsWith('.')) packageName + className else className
        val componentName = ComponentName(packageName, resolvedClassName)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            packageManager.getActivityInfo(
                componentName,
                PackageManager.ComponentInfoFlags.of(0)
            )
        } else {
            @Suppress("DEPRECATION")
            packageManager.getActivityInfo(componentName, 0)
        }
    } catch (_: PackageManager.NameNotFoundException) {
        null
    } catch (_: RuntimeException) {
        null
    }

    private fun queryPackages(intent: Intent): Set<String> = try {
        val activities: List<ResolveInfo> =
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                packageManager.queryIntentActivities(
                    intent,
                    PackageManager.ResolveInfoFlags.of(0)
                )
            } else {
                @Suppress("DEPRECATION")
                packageManager.queryIntentActivities(intent, 0)
            }
        activities.mapNotNullTo(mutableSetOf()) { it.activityInfo?.packageName }
    } catch (_: RuntimeException) {
        emptySet()
    }

    private fun resolveDefaultHomePackage(): String? = try {
        val intent = Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_HOME)
        val activity = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            packageManager.resolveActivity(
                intent,
                PackageManager.ResolveInfoFlags.of(PackageManager.MATCH_DEFAULT_ONLY.toLong())
            )
        } else {
            @Suppress("DEPRECATION")
            packageManager.resolveActivity(intent, PackageManager.MATCH_DEFAULT_ONLY)
        }
        activity?.activityInfo?.packageName
    } catch (_: RuntimeException) {
        null
    }

    private companion object {
        const val ICON_CACHE_KILOBYTES = 2 * 1024
        const val GOOGLE_SEARCH_PACKAGE = "com.google.android.googlequicksearchbox"
        const val DEBUG_TAG = "DigitalBalanceUsage"
    }
}

internal fun resolveAppKind(
    isDefaultHome: Boolean,
    isLaunchable: Boolean,
    isSystemApp: Boolean,
    hasForegroundSessionEvidence: Boolean
): AppKind = when {
    isDefaultHome -> AppKind.Launcher
    !isLaunchable && !hasForegroundSessionEvidence -> AppKind.BackgroundOrUnknown
    isSystemApp -> AppKind.SystemUserFacing
    else -> AppKind.UserFacing
}
