package com.nikhil.lumen.domain

/** A launchable app activity. [key] is stable across launches and is what we persist. */
data class AppEntry(
    val packageName: String,
    val activityName: String,
    val label: String,
) {
    val key: String get() = "$packageName/$activityName"
}

/** A tile in the Favourites row: either an installed app or a deep link (show, channel, playlist, URL). */
sealed interface Favourite {
    val id: String

    data class App(val appKey: String) : Favourite {
        override val id: String get() = "app:$appKey"
    }

    data class Link(override val id: String, val title: String, val uri: String) : Favourite
}

enum class SortMode { CUSTOM, ALPHABETICAL, MOST_USED }

enum class ClockFormat { H12, H24 }

/** Wallpaper presets. Colours are ARGB. Only gradients and solids for now: no storage permission needed. */
enum class WallpaperPreset(val label: String, val top: Long, val bottom: Long) {
    MIDNIGHT("Midnight", 0xFF0E1116, 0xFF1B2333),
    OCEAN("Ocean", 0xFF0B1E2D, 0xFF124559),
    EMBER("Ember", 0xFF1A0F0F, 0xFF4A1C1C),
    FOREST("Forest", 0xFF0D1A12, 0xFF1E3A2A),
    PLUM("Plum", 0xFF160F1F, 0xFF3A2150),
    GRAPHITE("Graphite", 0xFF121212, 0xFF2A2A2A),
    BLACK("Pure black", 0xFF000000, 0xFF000000),
}

enum class AccentPreset(val label: String, val argb: Long) {
    BLUE("Blue", 0xFF7AA7FF),
    TEAL("Teal", 0xFF4FD1C5),
    AMBER("Amber", 0xFFFFB454),
    ROSE("Rose", 0xFFFF7A9C),
    LIME("Lime", 0xFFB5E36B),
    WHITE("White", 0xFFF2F4F8),
}

/**
 * Everything the user can configure. Kept as one immutable value so backup/restore is a single
 * JSON round trip (see [StateCodec]).
 */
data class LauncherState(
    val sortMode: SortMode = SortMode.CUSTOM,
    val clockFormat: ClockFormat = ClockFormat.H24,
    val wallpaper: WallpaperPreset = WallpaperPreset.MIDNIGHT,
    val accent: AccentPreset = AccentPreset.BLUE,
    val showFavourites: Boolean = true,
    val showContinueWatching: Boolean = true,
    val showInputs: Boolean = true,
    /** When the stock Google TV home window appears anyway, bring Lumen back in front. */
    val coverStockHome: Boolean = true,
    val customOrder: List<String> = emptyList(),
    val hidden: Set<String> = emptySet(),
    val favourites: List<Favourite> = emptyList(),
    val launchCounts: Map<String, Int> = emptyMap(),
)
