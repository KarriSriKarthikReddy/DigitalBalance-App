package com.digitalbalance.app.data.usage

import android.content.Context
import android.content.Intent
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import android.content.pm.ResolveInfo
import android.os.Build

enum class AppKind(val includedInPrimaryUsage: Boolean) {
    UserFacing(true),
    SystemUserFacing(true),
    Launcher(false),
    BackgroundOrUnknown(false),
    DigitalBalance(false)
}

data class ClassifiedApp(
    val packageName: String,
    val label: String,
    val kind: AppKind
)

class AppClassifier(context: Context) {
    private val appContext = context.applicationContext
    private val packageManager = appContext.packageManager
    private val launchablePackages by lazy {
        queryPackages(Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER))
    }
    private val launcherPackages by lazy {
        queryPackages(Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_HOME))
    }

    fun classify(packageName: String): ClassifiedApp {
        if (packageName == appContext.packageName) {
            return ClassifiedApp(packageName, resolveLabel(packageName), AppKind.DigitalBalance)
        }
        if (packageName in launcherPackages) {
            return ClassifiedApp(packageName, resolveLabel(packageName), AppKind.Launcher)
        }

        val applicationInfo = getApplicationInfo(packageName)
        val isLaunchable = packageName in launchablePackages
        val isSystemApp = applicationInfo?.flags?.let { flags ->
            flags and (ApplicationInfo.FLAG_SYSTEM or ApplicationInfo.FLAG_UPDATED_SYSTEM_APP) != 0
        } ?: false
        val kind = when {
            isLaunchable && isSystemApp -> AppKind.SystemUserFacing
            isLaunchable -> AppKind.UserFacing
            else -> AppKind.BackgroundOrUnknown
        }
        val label = applicationInfo?.let(packageManager::getApplicationLabel)
            ?.toString()
            ?.takeIf(String::isNotBlank)
            ?: packageName
        return ClassifiedApp(packageName, label, kind)
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
}
