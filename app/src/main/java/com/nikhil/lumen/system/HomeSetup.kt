package com.nikhil.lumen.system

import android.Manifest
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.provider.Settings
import android.util.Log
import com.nikhil.lumen.MainActivity
import com.nikhil.lumen.service.HomeOverrideService

/**
 * Everything about making Lumen the screen the Home button opens.
 *
 * Three layers, from least to most setup:
 * 1. System default-home dialog (works on plain Android TV).
 * 2. The Home override accessibility service (needed on Google TV, which hides the dialog).
 * 3. "Strong mode" over ADB: set-home-activity, plus WRITE_SECURE_SETTINGS so Lumen can turn its
 *    override back on by itself after a force stop or update.
 *
 * Lumen never disables the stock Google TV launcher: on some builds that boot-loops the TV.
 */
object HomeSetup {

    /** Stock home packages Lumen covers when they appear. Kept enabled, never touched. */
    val STOCK_HOMES = setOf(
        "com.google.android.apps.tv.launcherx",
        "com.google.android.tvlauncher",
        "com.google.android.leanbacklauncher",
        "com.amazon.tv.launcher",
    )

    fun isDefaultHome(ctx: Context): Boolean {
        val resolved = ctx.packageManager.resolveActivity(
            Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_HOME),
            PackageManager.MATCH_DEFAULT_ONLY,
        )
        return resolved?.activityInfo?.packageName == ctx.packageName
    }

    private fun serviceComponent(ctx: Context) = ComponentName(ctx, HomeOverrideService::class.java)

    fun isOverrideEnabled(ctx: Context): Boolean {
        val enabled = Settings.Secure.getString(ctx.contentResolver, Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES)
            ?: return false
        val me = serviceComponent(ctx)
        return enabled.split(':').any { ComponentName.unflattenFromString(it) == me }
    }

    fun hasSecureSettings(ctx: Context): Boolean =
        ctx.checkSelfPermission(Manifest.permission.WRITE_SECURE_SETTINGS) == PackageManager.PERMISSION_GRANTED

    /** Turns the override on without the user visiting Settings. Needs the ADB grant. */
    fun enableOverride(ctx: Context): Boolean {
        if (!hasSecureSettings(ctx)) return false
        if (isOverrideEnabled(ctx)) return true
        return try {
            val resolver = ctx.contentResolver
            val current = Settings.Secure.getString(resolver, Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES)
                .orEmpty().split(':').filter { it.isNotBlank() }
            val updated = (current + serviceComponent(ctx).flattenToString()).distinct().joinToString(":")
            Settings.Secure.putString(resolver, Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES, updated)
            Settings.Secure.putInt(resolver, Settings.Secure.ACCESSIBILITY_ENABLED, 1)
            true
        } catch (e: Exception) {
            Log.w("Lumen", "Could not enable Home override", e)
            false
        }
    }

    /** Opens another installed home screen (the stock one when present). */
    fun openStockHome(ctx: Context): Boolean {
        val pm = ctx.packageManager
        val homes = pm.queryIntentActivities(Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_HOME), 0)
            .mapNotNull { it.activityInfo }
            .filter { it.packageName != ctx.packageName }
        val target = homes.firstOrNull { it.packageName in STOCK_HOMES } ?: homes.firstOrNull() ?: return false
        return try {
            ctx.startActivity(
                Intent(Intent.ACTION_MAIN)
                    .addCategory(Intent.CATEGORY_HOME)
                    .setComponent(ComponentName(target.packageName, target.name))
                    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
            )
            true
        } catch (_: Exception) {
            false
        }
    }

    fun openLumen(ctx: Context) {
        ctx.startActivity(
            Intent(ctx, MainActivity::class.java)
                .setAction(Intent.ACTION_MAIN)
                .addCategory(Intent.CATEGORY_HOME)
                .addFlags(
                    Intent.FLAG_ACTIVITY_NEW_TASK or
                        Intent.FLAG_ACTIVITY_CLEAR_TOP or
                        Intent.FLAG_ACTIVITY_SINGLE_TOP,
                ),
        )
    }

    fun adbCommands(ctx: Context): List<String> {
        val pkg = ctx.packageName
        return listOf(
            "adb shell cmd package set-home-activity $pkg/.MainActivity",
            "adb shell pm grant $pkg android.permission.WRITE_SECURE_SETTINGS",
            "adb shell appops set $pkg ACCESS_RESTRICTED_SETTINGS allow",
        )
    }
}
