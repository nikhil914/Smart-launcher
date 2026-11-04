package com.nikhil.lumen.system

import android.content.ContentValues
import android.content.Context
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import java.io.File

/**
 * Saves and restores the layout as JSON. On Android 10+ the file goes to Download/Lumen so it
 * survives a reinstall; older versions use the app's own external folder.
 */
object Backup {
    private const val FILE_NAME = "lumen-backup.json"
    private const val FOLDER = "Lumen"

    /** Returns a human-readable location, or throws. */
    fun export(ctx: Context, json: String): String {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            val resolver = ctx.contentResolver
            val existing = findDownload(ctx)
            val uri = existing ?: resolver.insert(
                MediaStore.Downloads.EXTERNAL_CONTENT_URI,
                ContentValues().apply {
                    put(MediaStore.MediaColumns.DISPLAY_NAME, FILE_NAME)
                    put(MediaStore.MediaColumns.MIME_TYPE, "application/json")
                    put(MediaStore.MediaColumns.RELATIVE_PATH, "${Environment.DIRECTORY_DOWNLOADS}/$FOLDER")
                },
            ) ?: error("Could not create the backup file")
            resolver.openOutputStream(uri, "wt")!!.use { it.write(json.toByteArray()) }
            return "Download/$FOLDER/$FILE_NAME"
        }
        val file = legacyFile(ctx)
        file.writeText(json)
        return file.absolutePath
    }

    /** Returns the saved JSON, or null when no backup exists. */
    fun read(ctx: Context): String? {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            findDownload(ctx)?.let { uri ->
                return ctx.contentResolver.openInputStream(uri)?.use { it.readBytes().decodeToString() }
            }
        }
        return legacyFile(ctx).takeIf { it.exists() }?.readText()
    }

    private fun legacyFile(ctx: Context) = File(ctx.getExternalFilesDir(null) ?: ctx.filesDir, FILE_NAME)

    private fun findDownload(ctx: Context): Uri? {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) return null
        val collection = MediaStore.Downloads.EXTERNAL_CONTENT_URI
        return ctx.contentResolver.query(
            collection,
            arrayOf(MediaStore.MediaColumns._ID),
            "${MediaStore.MediaColumns.DISPLAY_NAME}=? AND ${MediaStore.MediaColumns.RELATIVE_PATH}=?",
            arrayOf(FILE_NAME, "${Environment.DIRECTORY_DOWNLOADS}/$FOLDER/"),
            null,
        )?.use { c -> if (c.moveToFirst()) Uri.withAppendedPath(collection, c.getLong(0).toString()) else null }
    }
}
