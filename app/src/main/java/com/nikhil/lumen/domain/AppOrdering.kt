package com.nikhil.lumen.domain

import java.util.Locale

object AppOrdering {

    /** Orders [apps] for display. Hidden apps are removed unless [includeHidden]. */
    fun arrange(
        apps: List<AppEntry>,
        state: LauncherState,
        includeHidden: Boolean = false,
    ): List<AppEntry> {
        val visible = if (includeHidden) apps else apps.filterNot { it.key in state.hidden }
        val byLabel = compareBy<AppEntry> { it.label.lowercase(Locale.ROOT) }.thenBy { it.key }
        return when (state.sortMode) {
            SortMode.ALPHABETICAL -> visible.sortedWith(byLabel)
            SortMode.MOST_USED -> visible.sortedWith(
                compareByDescending<AppEntry> { state.launchCounts[it.key] ?: 0 }.then(byLabel),
            )
            SortMode.CUSTOM -> {
                val position = state.customOrder.withIndex().associate { it.value to it.index }
                // Apps the user has placed keep their spot; new installs go to the end, alphabetically.
                val placed = visible.filter { it.key in position }.sortedBy { position.getValue(it.key) }
                val unplaced = visible.filterNot { it.key in position }.sortedWith(byLabel)
                placed + unplaced
            }
        }
    }

    /**
     * Moves [key] by [delta] places within the currently displayed order and returns the new
     * custom order. Moving always switches the user to [SortMode.CUSTOM] (callers do that), so the
     * returned list captures the full displayed order, not just the moved app.
     */
    fun move(displayed: List<String>, key: String, delta: Int): List<String> {
        val from = displayed.indexOf(key)
        if (from < 0) return displayed
        val to = (from + delta).coerceIn(0, displayed.lastIndex)
        if (to == from) return displayed
        val result = displayed.toMutableList()
        result.removeAt(from)
        result.add(to, key)
        return result
    }

    /** Moves a favourite by [delta] places. */
    fun moveFavourite(favourites: List<Favourite>, id: String, delta: Int): List<Favourite> {
        val ids = move(favourites.map { it.id }, id, delta)
        val byId = favourites.associateBy { it.id }
        return ids.map { byId.getValue(it) }
    }
}
