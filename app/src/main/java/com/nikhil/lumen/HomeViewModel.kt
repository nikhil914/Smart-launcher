package com.nikhil.lumen

import android.app.Application
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.nikhil.lumen.data.TvInput
import com.nikhil.lumen.data.WatchNextItem
import com.nikhil.lumen.domain.AppEntry
import com.nikhil.lumen.domain.AppOrdering
import com.nikhil.lumen.domain.Favourite
import com.nikhil.lumen.domain.LauncherState
import com.nikhil.lumen.domain.SortMode
import com.nikhil.lumen.domain.StateCodec
import com.nikhil.lumen.system.Backup
import com.nikhil.lumen.system.HomeSetup
import com.nikhil.lumen.system.SystemIntents
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/** A resolved favourite tile: the app it points to is installed, or it is a link. */
sealed interface FavouriteTile {
    val id: String

    data class AppTile(val app: AppEntry) : FavouriteTile {
        override val id: String get() = "app:${app.key}"
    }

    data class LinkTile(val link: Favourite.Link) : FavouriteTile {
        override val id: String get() = link.id
    }
}

data class SetupStatus(
    val isDefaultHome: Boolean = false,
    val overrideEnabled: Boolean = false,
    val strongMode: Boolean = false,
) {
    val homeButtonWorks: Boolean get() = isDefaultHome || overrideEnabled
}

data class HomeUiState(
    val settings: LauncherState = LauncherState(),
    val apps: List<AppEntry> = emptyList(),
    val hiddenApps: List<AppEntry> = emptyList(),
    val favourites: List<FavouriteTile> = emptyList(),
    val inputs: List<TvInput> = emptyList(),
    val watchNext: List<WatchNextItem> = emptyList(),
    val watchNextPermission: Boolean = false,
    val setup: SetupStatus = SetupStatus(),
    val loaded: Boolean = false,
)

class HomeViewModel(application: Application) : AndroidViewModel(application) {
    private val app = LumenApp.from(application)
    private val store = app.settings

    private val allApps = MutableStateFlow<List<AppEntry>>(emptyList())
    private val inputs = MutableStateFlow<List<TvInput>>(emptyList())
    private val watchNext = MutableStateFlow<List<WatchNextItem>>(emptyList())
    private val system = MutableStateFlow(Pair(false, SetupStatus()))
    private val loaded = MutableStateFlow(false)

    val ui: StateFlow<HomeUiState> = combine(
        store.state, allApps, inputs, watchNext, combine(system, loaded) { s, l -> s to l },
    ) { settings, apps, inputs, watchNext, (sys, isLoaded) ->
        val byKey = apps.associateBy { it.key }
        HomeUiState(
            settings = settings,
            apps = AppOrdering.arrange(apps, settings),
            hiddenApps = apps.filter { it.key in settings.hidden }.sortedBy { it.label.lowercase() },
            favourites = settings.favourites.mapNotNull { fav ->
                when (fav) {
                    is Favourite.App -> byKey[fav.appKey]?.let { FavouriteTile.AppTile(it) }
                    is Favourite.Link -> FavouriteTile.LinkTile(fav)
                }
            },
            inputs = inputs,
            watchNext = watchNext,
            watchNextPermission = sys.first,
            setup = sys.second,
            loaded = isLoaded,
        )
    }.stateIn(viewModelScope, SharingStarted.Eagerly, HomeUiState())

    private val packageReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) = refreshApps()
    }

    init {
        ContextCompat.registerReceiver(
            application,
            packageReceiver,
            IntentFilter().apply {
                addAction(Intent.ACTION_PACKAGE_ADDED)
                addAction(Intent.ACTION_PACKAGE_REMOVED)
                addAction(Intent.ACTION_PACKAGE_CHANGED)
                addAction(Intent.ACTION_PACKAGE_REPLACED)
                addDataScheme("package")
            },
            ContextCompat.RECEIVER_EXPORTED,
        )
        refresh()
    }

    override fun onCleared() {
        runCatching { getApplication<Application>().unregisterReceiver(packageReceiver) }
    }

    /** Called on every resume: apps, inputs and Continue Watching can all change while we're away. */
    fun refresh() {
        refreshApps()
        viewModelScope.launch {
            val ctx = getApplication<Application>()
            val (ins, wn, perm, setup) = withContext(Dispatchers.IO) {
                Quad(
                    app.inputs.loadInputs(),
                    app.watchNext.load(),
                    app.watchNext.hasPermission(),
                    SetupStatus(
                        isDefaultHome = HomeSetup.isDefaultHome(ctx),
                        overrideEnabled = HomeSetup.isOverrideEnabled(ctx),
                        strongMode = HomeSetup.hasSecureSettings(ctx),
                    ),
                )
            }
            inputs.value = ins
            watchNext.value = wn
            system.value = perm to setup
        }
    }

    private fun refreshApps() {
        viewModelScope.launch {
            allApps.value = withContext(Dispatchers.IO) { app.apps.loadApps() }
            loaded.value = true
        }
    }

    // --- Launching ---

    fun launch(entry: AppEntry) {
        val ctx = getApplication<Application>()
        if (SystemIntents.open(ctx, app.apps.launchIntent(entry), fallbackToSettings = false)) {
            store.update { it.copy(launchCounts = it.launchCounts + (entry.key to (it.launchCounts[entry.key] ?: 0) + 1)) }
        }
    }

    fun launch(tile: FavouriteTile): Unit = when (tile) {
        is FavouriteTile.AppTile -> launch(tile.app)
        is FavouriteTile.LinkTile -> openLink(tile.link.uri)
    }

    fun launch(input: TvInput) {
        SystemIntents.open(getApplication(), input.intent(), fallbackToSettings = false)
    }

    fun launch(item: WatchNextItem) {
        val ctx = getApplication<Application>()
        val intent = item.intent()
            ?: item.packageName?.let { ctx.packageManager.getLeanbackLaunchIntentForPackage(it) ?: ctx.packageManager.getLaunchIntentForPackage(it) }
        if (intent == null) {
            toast("That app didn't say how to resume this")
            return
        }
        SystemIntents.open(ctx, intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK), fallbackToSettings = false)
    }

    private fun openLink(uri: String) {
        val intent = try {
            if (uri.startsWith("intent:")) Intent.parseUri(uri, Intent.URI_INTENT_SCHEME)
            else Intent(Intent.ACTION_VIEW, android.net.Uri.parse(uri))
        } catch (_: Exception) {
            toast("That link isn't valid")
            return
        }
        SystemIntents.open(getApplication(), intent, fallbackToSettings = false)
    }

    // --- Layout editing ---

    fun isFavourite(entry: AppEntry): Boolean = store.state.value.favourites.any { it.id == "app:${entry.key}" }

    fun toggleFavourite(entry: AppEntry) = store.update { s ->
        val id = "app:${entry.key}"
        if (s.favourites.any { it.id == id }) s.copy(favourites = s.favourites.filterNot { it.id == id })
        else s.copy(favourites = s.favourites + Favourite.App(entry.key))
    }

    fun removeFavourite(id: String) = store.update { s -> s.copy(favourites = s.favourites.filterNot { it.id == id }) }

    fun moveFavourite(id: String, delta: Int) = store.update { s ->
        s.copy(favourites = AppOrdering.moveFavourite(s.favourites, id, delta))
    }

    fun moveApp(key: String, delta: Int) {
        val displayed = ui.value.apps.map { it.key }
        store.update { s -> s.copy(sortMode = SortMode.CUSTOM, customOrder = AppOrdering.move(displayed, key, delta)) }
    }

    fun hide(entry: AppEntry) = store.update { s -> s.copy(hidden = s.hidden + entry.key) }

    fun unhide(key: String) = store.update { s -> s.copy(hidden = s.hidden - key) }

    fun addLink(title: String, uri: String) {
        val cleanUri = uri.trim()
        if (cleanUri.isEmpty()) return
        val link = Favourite.Link(
            id = "link:${System.currentTimeMillis()}",
            title = title.trim().ifEmpty { cleanUri },
            uri = cleanUri,
        )
        store.update { s -> s.copy(favourites = s.favourites + link) }
    }

    fun updateSettings(transform: (LauncherState) -> LauncherState) = store.update(transform)

    // --- Backup ---

    fun exportBackup() = viewModelScope.launch {
        val result = withContext(Dispatchers.IO) {
            runCatching { Backup.export(getApplication(), StateCodec.encode(store.state.value)) }
        }
        toast(result.fold({ "Saved to $it" }, { "Backup failed: ${it.message}" }))
    }

    fun importBackup() = viewModelScope.launch {
        val result = withContext(Dispatchers.IO) {
            runCatching { Backup.read(getApplication())?.let(StateCodec::decode) }
        }
        result.fold(
            { restored ->
                if (restored == null) toast("No backup found in Download/Lumen")
                else {
                    store.replace(restored)
                    toast("Layout restored")
                }
            },
            { toast("Restore failed: ${it.message}") },
        )
    }

    fun resetLayout() {
        store.replace(LauncherState())
        app.settings.clearCrashes()
        toast("Layout reset")
    }

    private fun toast(message: String) =
        Toast.makeText(getApplication(), message, Toast.LENGTH_LONG).show()

    private data class Quad<A, B, C, D>(val a: A, val b: B, val c: C, val d: D)
}
