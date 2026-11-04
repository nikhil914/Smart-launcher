package com.nikhil.lumen.system

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.Settings
import android.widget.Toast

/** Opens system screens. Every call falls back to the main Settings app, since TV builds vary a lot. */
object SystemIntents {

    fun wifi(ctx: Context) = open(ctx, Intent(Settings.ACTION_WIFI_SETTINGS))
    fun bluetooth(ctx: Context) = open(ctx, Intent(Settings.ACTION_BLUETOOTH_SETTINGS))
    fun display(ctx: Context) = open(ctx, Intent(Settings.ACTION_DISPLAY_SETTINGS))
    fun sound(ctx: Context) = open(ctx, Intent(Settings.ACTION_SOUND_SETTINGS))
    fun apps(ctx: Context) = open(ctx, Intent(Settings.ACTION_APPLICATION_SETTINGS))
    fun accessibility(ctx: Context) = open(ctx, Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS))
    fun allSettings(ctx: Context) = open(ctx, Intent(Settings.ACTION_SETTINGS))
    fun homeChooser(ctx: Context) = open(ctx, Intent(Settings.ACTION_HOME_SETTINGS))

    fun appInfo(ctx: Context, packageName: String) = open(
        ctx,
        Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.fromParts("package", packageName, null)),
    )

    fun uninstall(ctx: Context, packageName: String) = open(
        ctx,
        Intent(Intent.ACTION_DELETE, Uri.fromParts("package", packageName, null)),
        fallbackToSettings = false,
    )

    /** Starts [intent]; returns false (and shows a short message) if nothing could handle it. */
    fun open(ctx: Context, intent: Intent, fallbackToSettings: Boolean = true): Boolean {
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        return try {
            ctx.startActivity(intent)
            true
        } catch (_: ActivityNotFoundException) {
            if (fallbackToSettings && intent.action != Settings.ACTION_SETTINGS) {
                open(ctx, Intent(Settings.ACTION_SETTINGS), fallbackToSettings = false)
            } else {
                Toast.makeText(ctx, "This TV can't open that screen", Toast.LENGTH_SHORT).show()
                false
            }
        } catch (_: SecurityException) {
            Toast.makeText(ctx, "This TV blocked that screen", Toast.LENGTH_SHORT).show()
            false
        }
    }
}
