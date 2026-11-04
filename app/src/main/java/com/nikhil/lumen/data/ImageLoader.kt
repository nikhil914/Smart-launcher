package com.nikhil.lumen.data

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.drawable.Drawable
import android.net.Uri
import android.util.LruCache
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.core.graphics.drawable.toBitmap
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.InputStream
import java.net.HttpURLConnection
import java.net.URL

/**
 * Small image cache for banners, icons and Watch Next posters. No third-party image library: the
 * launcher stays light, and the only network use is fetching posters that apps publish.
 */
class ImageLoader(private val context: Context) {
    private val cache = object : LruCache<String, ImageBitmap>(cacheBytes()) {
        override fun sizeOf(key: String, value: ImageBitmap): Int = value.width * value.height * 4
    }

    fun cached(key: String): ImageBitmap? = cache.get(key)

    suspend fun fromDrawable(key: String, width: Int, height: Int, load: () -> Drawable?): ImageBitmap? {
        cache.get(key)?.let { return it }
        return withContext(Dispatchers.IO) {
            val drawable = try { load() } catch (_: Exception) { null } ?: return@withContext null
            val w = if (drawable.intrinsicWidth > 0) minOf(drawable.intrinsicWidth, width) else width
            val h = if (drawable.intrinsicHeight > 0) minOf(drawable.intrinsicHeight, height) else height
            drawable.toBitmap(w, h, Bitmap.Config.ARGB_8888).asImageBitmap().also { cache.put(key, it) }
        }
    }

    suspend fun fromUri(uri: String, maxWidth: Int): ImageBitmap? {
        cache.get(uri)?.let { return it }
        return withContext(Dispatchers.IO) {
            try {
                val bytes = open(uri)?.use { it.readBytes() } ?: return@withContext null
                val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
                BitmapFactory.decodeByteArray(bytes, 0, bytes.size, bounds)
                var sample = 1
                while (bounds.outWidth / (sample * 2) >= maxWidth) sample *= 2
                val opts = BitmapFactory.Options().apply { inSampleSize = sample }
                BitmapFactory.decodeByteArray(bytes, 0, bytes.size, opts)?.asImageBitmap()?.also { cache.put(uri, it) }
            } catch (_: Exception) {
                null
            }
        }
    }

    private fun open(uri: String): InputStream? {
        val parsed = Uri.parse(uri)
        return when (parsed.scheme?.lowercase()) {
            "http", "https" -> (URL(uri).openConnection() as HttpURLConnection).run {
                connectTimeout = 5_000
                readTimeout = 8_000
                instanceFollowRedirects = true
                if (responseCode in 200..299) inputStream else null
            }
            else -> context.contentResolver.openInputStream(parsed)
        }
    }

    private companion object {
        fun cacheBytes(): Int = (Runtime.getRuntime().maxMemory() / 8).coerceAtMost(48L * 1024 * 1024).toInt()
    }
}
