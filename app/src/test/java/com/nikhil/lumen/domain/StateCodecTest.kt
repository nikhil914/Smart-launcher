package com.nikhil.lumen.domain

import org.junit.Assert.assertEquals
import org.junit.Test

class StateCodecTest {
    @Test
    fun roundTripsEveryField() {
        val state = LauncherState(
            sortMode = SortMode.MOST_USED,
            clockFormat = ClockFormat.H12,
            wallpaper = WallpaperPreset.OCEAN,
            accent = AccentPreset.TEAL,
            showFavourites = false,
            showContinueWatching = false,
            showInputs = false,
            coverStockHome = false,
            customOrder = listOf("a/b", "c/d"),
            hidden = setOf("e/f"),
            favourites = listOf(Favourite.App("a/b"), Favourite.Link("link:7", "Lo-fi", "https://youtube.com/watch?v=x")),
            launchCounts = mapOf("a/b" to 3),
        )
        assertEquals(state, StateCodec.decode(StateCodec.encode(state)))
    }

    @Test
    fun unknownValuesFallBackToDefaults() {
        val decoded = StateCodec.decode("""{"sortMode":"SOMETHING_NEW","wallpaper":"NOPE","favourites":[{"type":"widget"}]}""")
        assertEquals(LauncherState(), decoded)
    }

    @Test
    fun emptyObjectGivesDefaults() {
        assertEquals(LauncherState(), StateCodec.decode("{}"))
    }
}
