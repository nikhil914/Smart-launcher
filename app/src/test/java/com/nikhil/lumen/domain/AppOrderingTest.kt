package com.nikhil.lumen.domain

import org.junit.Assert.assertEquals
import org.junit.Test

class AppOrderingTest {
    private val netflix = AppEntry("com.netflix", "Main", "Netflix")
    private val youtube = AppEntry("com.youtube", "Main", "YouTube")
    private val kodi = AppEntry("org.kodi", "Main", "kodi")
    private val apps = listOf(youtube, netflix, kodi)

    @Test
    fun alphabeticalIgnoresCase() {
        val result = AppOrdering.arrange(apps, LauncherState(sortMode = SortMode.ALPHABETICAL))
        assertEquals(listOf("kodi", "Netflix", "YouTube"), result.map { it.label })
    }

    @Test
    fun mostUsedSortsByLaunchCountThenLabel() {
        val state = LauncherState(
            sortMode = SortMode.MOST_USED,
            launchCounts = mapOf(youtube.key to 9, kodi.key to 2),
        )
        assertEquals(listOf("YouTube", "kodi", "Netflix"), AppOrdering.arrange(apps, state).map { it.label })
    }

    @Test
    fun customOrderKeepsPlacedAppsAndAppendsNewOnes() {
        val state = LauncherState(customOrder = listOf(youtube.key, "gone/app", netflix.key))
        assertEquals(listOf("YouTube", "Netflix", "kodi"), AppOrdering.arrange(apps, state).map { it.label })
    }

    @Test
    fun hiddenAppsAreRemovedUnlessRequested() {
        val state = LauncherState(sortMode = SortMode.ALPHABETICAL, hidden = setOf(netflix.key))
        assertEquals(listOf("kodi", "YouTube"), AppOrdering.arrange(apps, state).map { it.label })
        assertEquals(3, AppOrdering.arrange(apps, state, includeHidden = true).size)
    }

    @Test
    fun moveClampsAtEdges() {
        val order = listOf("a", "b", "c", "d")
        assertEquals(listOf("b", "a", "c", "d"), AppOrdering.move(order, "a", 1))
        assertEquals(listOf("a", "b", "c", "d"), AppOrdering.move(order, "a", -1))
        assertEquals(listOf("b", "c", "d", "a"), AppOrdering.move(order, "a", 10))
        assertEquals(order, AppOrdering.move(order, "missing", 1))
    }

    @Test
    fun moveByRowStride() {
        val order = (1..12).map { "app$it" }
        val moved = AppOrdering.move(order, "app2", 5)
        assertEquals("app2", moved[6])
    }

    @Test
    fun moveFavouriteKeepsObjects() {
        val favs = listOf(Favourite.App("x/y"), Favourite.Link("link:1", "News", "https://example.com"))
        val moved = AppOrdering.moveFavourite(favs, "link:1", -1)
        assertEquals(listOf("link:1", "app:x/y"), moved.map { it.id })
    }
}
