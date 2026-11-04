package com.nikhil.lumen.domain

import org.json.JSONArray
import org.json.JSONObject

/** JSON round trip for [LauncherState]. Used for persistence and for backup/restore files. */
object StateCodec {
    private const val VERSION = 1

    fun encode(state: LauncherState): String = JSONObject().apply {
        put("version", VERSION)
        put("sortMode", state.sortMode.name)
        put("clockFormat", state.clockFormat.name)
        put("wallpaper", state.wallpaper.name)
        put("accent", state.accent.name)
        put("showFavourites", state.showFavourites)
        put("showContinueWatching", state.showContinueWatching)
        put("showInputs", state.showInputs)
        put("coverStockHome", state.coverStockHome)
        put("customOrder", JSONArray(state.customOrder))
        put("hidden", JSONArray(state.hidden.sorted()))
        put("favourites", JSONArray().apply {
            state.favourites.forEach { fav ->
                put(
                    when (fav) {
                        is Favourite.App -> JSONObject().put("type", "app").put("key", fav.appKey)
                        is Favourite.Link -> JSONObject().put("type", "link").put("id", fav.id)
                            .put("title", fav.title).put("uri", fav.uri)
                    },
                )
            }
        })
        put("launchCounts", JSONObject().apply { state.launchCounts.forEach { (k, v) -> put(k, v) } })
    }.toString(2)

    /** Decodes leniently: unknown or missing fields fall back to defaults, so old backups still load. */
    fun decode(json: String): LauncherState {
        val o = JSONObject(json)
        val d = LauncherState()
        return LauncherState(
            sortMode = enumOr(o.optString("sortMode"), d.sortMode),
            clockFormat = enumOr(o.optString("clockFormat"), d.clockFormat),
            wallpaper = enumOr(o.optString("wallpaper"), d.wallpaper),
            accent = enumOr(o.optString("accent"), d.accent),
            showFavourites = o.optBoolean("showFavourites", d.showFavourites),
            showContinueWatching = o.optBoolean("showContinueWatching", d.showContinueWatching),
            showInputs = o.optBoolean("showInputs", d.showInputs),
            coverStockHome = o.optBoolean("coverStockHome", d.coverStockHome),
            customOrder = o.optJSONArray("customOrder").strings(),
            hidden = o.optJSONArray("hidden").strings().toSet(),
            favourites = o.optJSONArray("favourites").objects().mapNotNull { f ->
                when (f.optString("type")) {
                    "app" -> f.optString("key").takeIf { it.isNotBlank() }?.let { Favourite.App(it) }
                    "link" -> {
                        val uri = f.optString("uri")
                        if (uri.isBlank()) null
                        else Favourite.Link(
                            id = f.optString("id").ifBlank { "link:${uri.hashCode()}" },
                            title = f.optString("title").ifBlank { uri },
                            uri = uri,
                        )
                    }
                    else -> null
                }
            },
            launchCounts = o.optJSONObject("launchCounts")?.let { c ->
                c.keys().asSequence().associateWith { c.optInt(it) }
            } ?: emptyMap(),
        )
    }

    private inline fun <reified E : Enum<E>> enumOr(name: String, fallback: E): E =
        enumValues<E>().firstOrNull { it.name == name } ?: fallback

    private fun JSONArray?.strings(): List<String> =
        if (this == null) emptyList() else (0 until length()).mapNotNull { optString(it).takeIf(String::isNotBlank) }

    private fun JSONArray?.objects(): List<JSONObject> =
        if (this == null) emptyList() else (0 until length()).mapNotNull { optJSONObject(it) }
}
