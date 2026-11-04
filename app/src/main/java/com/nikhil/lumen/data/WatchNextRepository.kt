package com.nikhil.lumen.data

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.database.Cursor
import android.media.tv.TvContract
import android.net.Uri
import android.util.Log
import androidx.core.content.ContextCompat

data class WatchNextItem(
    val id: Long,
    val title: String,
    val subtitle: String?,
    val posterUri: String?,
    val intentUri: String?,
    val packageName: String?,
    val progress: Float?,
) {
    fun intent(): Intent? {
        val raw = intentUri ?: return null
        val parsed = try {
            Intent.parseUri(raw, Intent.URI_INTENT_SCHEME)
        } catch (_: Exception) {
            Intent(Intent.ACTION_VIEW, Uri.parse(raw))
        }
        return parsed.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    }
}

/**
 * Reads the system Watch Next list (the "Continue watching" row). Google ends support for this API
 * in the second half of 2027, so the row is best-effort: it shows whatever apps still publish.
 */
class WatchNextRepository(private val context: Context) {

    fun hasPermission(): Boolean = PERMISSIONS.all {
        ContextCompat.checkSelfPermission(context, it) == PackageManager.PERMISSION_GRANTED
    }

    fun load(limit: Int = 20): List<WatchNextItem> {
        if (!hasPermission()) return emptyList()
        // Column names spelled out: the framework constants live on super-interfaces that Kotlin
        // cannot always reach through WatchNextPrograms.
        val projection = arrayOf(
            "_id",
            "title",
            "episode_title",
            "poster_art_uri",
            "intent_uri",
            "package_name",
            "last_playback_position_millis",
            "duration_millis",
        )
        return try {
            context.contentResolver.query(
                TvContract.WatchNextPrograms.CONTENT_URI,
                projection,
                null,
                null,
                "last_engagement_time_utc_millis DESC",
            )?.use { c ->
                val items = ArrayList<WatchNextItem>()
                while (c.moveToNext() && items.size < limit) {
                    val position = c.longOrNull(6)
                    val duration = c.longOrNull(7)
                    items += WatchNextItem(
                        id = c.getLong(0),
                        title = c.getString(1).orEmpty().ifBlank { "Untitled" },
                        subtitle = c.getString(2)?.takeIf { it.isNotBlank() },
                        posterUri = c.getString(3),
                        intentUri = c.getString(4),
                        packageName = c.getString(5),
                        progress = if (position != null && duration != null && duration > 0) {
                            (position.toFloat() / duration).coerceIn(0f, 1f)
                        } else {
                            null
                        },
                    )
                }
                items
            } ?: emptyList()
        } catch (e: Exception) {
            // No TV provider (emulators, some sticks) or permission revoked.
            Log.i("Lumen", "Watch Next unavailable: ${e.message}")
            emptyList()
        }
    }

    private fun Cursor.longOrNull(index: Int): Long? = if (isNull(index)) null else getLong(index)

    companion object {
        val PERMISSIONS = arrayOf(
            "android.permission.READ_TV_LISTINGS",
            "com.android.providers.tv.permission.READ_EPG_DATA",
        )
    }
}
