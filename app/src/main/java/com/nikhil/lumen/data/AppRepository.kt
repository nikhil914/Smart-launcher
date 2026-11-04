package com.nikhil.lumen.data

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.pm.ActivityInfo
import android.content.pm.PackageManager
import android.graphics.drawable.Drawable
import com.nikhil.lumen.domain.AppEntry

/** Lists launchable apps. Prefers each package's TV (leanback) activity, falls back to the phone one. */
class AppRepository(private val context: Context) {
    private val pm: PackageManager = context.packageManager

    fun loadApps(): List<AppEntry> {
        val tv = query(Intent.CATEGORY_LEANBACK_LAUNCHER)
        val phone = query(Intent.CATEGORY_LAUNCHER)
        val byPackage = LinkedHashMap<String, ActivityInfo>()
        (tv + phone).forEach { info -> byPackage.putIfAbsent(info.packageName, info) }
        byPackage.remove(context.packageName)
        return byPackage.values.map { info ->
            AppEntry(
                packageName = info.packageName,
                activityName = info.name,
                label = info.loadLabel(pm).toString().trim().ifEmpty { info.packageName },
            )
        }
    }

    private fun query(category: String): List<ActivityInfo> =
        pm.queryIntentActivities(Intent(Intent.ACTION_MAIN).addCategory(category), 0)
            .mapNotNull { it.activityInfo }
            .filter { it.enabled && it.exported }

    fun launchIntent(app: AppEntry): Intent =
        Intent(Intent.ACTION_MAIN)
            .setComponent(ComponentName(app.packageName, app.activityName))
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_RESET_TASK_IF_NEEDED)

    /** 16:9 TV banner, if the app ships one. */
    fun banner(app: AppEntry): Drawable? = try {
        val info = pm.getActivityInfo(ComponentName(app.packageName, app.activityName), 0)
        info.loadBanner(pm) ?: info.applicationInfo.loadBanner(pm)
    } catch (_: Exception) {
        null
    }

    fun icon(app: AppEntry): Drawable? = try {
        pm.getActivityIcon(ComponentName(app.packageName, app.activityName))
    } catch (_: Exception) {
        null
    }

    fun isInstalled(packageName: String): Boolean = try {
        pm.getPackageInfo(packageName, 0)
        true
    } catch (_: PackageManager.NameNotFoundException) {
        false
    }
}
