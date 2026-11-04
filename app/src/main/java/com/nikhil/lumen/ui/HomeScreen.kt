@file:OptIn(ExperimentalTvMaterial3Api::class, ExperimentalComposeUiApi::class)

package com.nikhil.lumen.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.focusRestorer
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.tv.material3.ExperimentalTvMaterial3Api
import androidx.tv.material3.Text
import com.nikhil.lumen.FavouriteTile
import com.nikhil.lumen.HomeUiState
import com.nikhil.lumen.HomeViewModel
import com.nikhil.lumen.domain.AppEntry
import com.nikhil.lumen.domain.ClockFormat
import com.nikhil.lumen.domain.WallpaperPreset
import com.nikhil.lumen.system.SystemIntents
import kotlinx.coroutines.delay
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

/** Apps per row in the apps grid. 5 × 160 dp tiles + gaps fit a 960 dp-wide TV screen with margins. */
const val APPS_PER_ROW = 5

private sealed interface Menu {
    data class ForApp(val app: AppEntry) : Menu
    data class ForFavourite(val tile: FavouriteTile) : Menu
}

@Composable
fun Wallpaper(preset: WallpaperPreset, content: @Composable () -> Unit) {
    Box(
        Modifier
            .fillMaxSize()
            .background(Brush.verticalGradient(listOf(Color(preset.top), Color(preset.bottom)))),
    ) { content() }
}

@Composable
fun HomeScreen(
    state: HomeUiState,
    vm: HomeViewModel,
    resetSignal: Int,
    onOpenSettings: () -> Unit,
    onOpenSetup: () -> Unit,
    onRequestWatchNext: () -> Unit,
) {
    val settings = state.settings
    var menu by remember { mutableStateOf<Menu?>(null) }
    /** Id of the tile being moved: "app:<key>" or a favourite id. */
    var moving by rememberSaveable { mutableStateOf<String?>(null) }
    var showQuick by remember { mutableStateOf(false) }
    var showAddLink by remember { mutableStateOf(false) }
    val listState = rememberLazyListState()
    val firstFocus = remember { FocusRequester() }

    // Home pressed while already home: scroll to top and focus the first tile.
    LaunchedEffect(resetSignal, state.loaded) {
        if (!state.loaded) return@LaunchedEffect
        moving = null
        listState.scrollToItem(0)
        delay(50)
        runCatching { firstFocus.requestFocus() }
    }

    Wallpaper(settings.wallpaper) {
        LazyColumn(
            state = listState,
            modifier = Modifier
                .fillMaxSize()
                .onPreviewKeyEvent { e ->
                    // The Menu key opens quick settings from anywhere on the home screen.
                    if (e.type == KeyEventType.KeyDown && e.key == Key.Menu && moving == null) {
                        showQuick = true
                        true
                    } else {
                        false
                    }
                },
            contentPadding = PaddingValues(horizontal = 48.dp, vertical = 28.dp),
            verticalArrangement = Arrangement.spacedBy(18.dp),
        ) {
            item(key = "header") {
                Header(
                    clockFormat = settings.clockFormat,
                    onQuick = { showQuick = true },
                    onSettings = onOpenSettings,
                )
            }

            if (!state.setup.homeButtonWorks && state.loaded) {
                item(key = "setup") {
                    PanelButton(
                        title = "Make the Home button open Lumen",
                        subtitle = "Google TV keeps its own home screen until you turn on Lumen's Home button service. Takes about a minute.",
                        value = "Set up",
                        onClick = onOpenSetup,
                    )
                }
            }

            var focusAssigned = false
            fun takeFirstFocus(): FocusRequester? = if (!focusAssigned) { focusAssigned = true; firstFocus } else null

            if (settings.showFavourites && state.favourites.isNotEmpty()) {
                val requester = takeFirstFocus()
                item(key = "favourites") {
                    Column {
                        RowTitle("Favourites")
                        LazyRow(
                            modifier = Modifier.focusRestorer(),
                            horizontalArrangement = Arrangement.spacedBy(16.dp),
                            contentPadding = PaddingValues(vertical = 8.dp),
                        ) {
                            itemsIndexed(state.favourites, key = { _, t -> t.id }) { index, tile ->
                                val isMoving = moving == tile.id
                                val mod = Modifier.moveMode(
                                    moving = isMoving,
                                    rowStride = 0,
                                    onMove = { vm.moveFavourite(tile.id, it) },
                                    onDone = { moving = null },
                                )
                                val fr = if (index == 0) requester else null
                                when (tile) {
                                    is FavouriteTile.AppTile -> AppTile(
                                        app = tile.app,
                                        onClick = { vm.launch(tile) },
                                        onLongClick = { menu = Menu.ForFavourite(tile) },
                                        modifier = mod,
                                        focusRequester = fr,
                                        moving = isMoving,
                                    )
                                    is FavouriteTile.LinkTile -> LinkTile(
                                        title = tile.link.title,
                                        onClick = { vm.launch(tile) },
                                        onLongClick = { menu = Menu.ForFavourite(tile) },
                                        modifier = mod,
                                        focusRequester = fr,
                                        moving = isMoving,
                                    )
                                }
                            }
                        }
                    }
                }
            }

            if (settings.showContinueWatching && state.watchNext.isNotEmpty()) {
                item(key = "watchnext") {
                    Column {
                        RowTitle("Continue watching")
                        LazyRow(
                            modifier = Modifier.focusRestorer(),
                            horizontalArrangement = Arrangement.spacedBy(16.dp),
                            contentPadding = PaddingValues(vertical = 8.dp),
                        ) {
                            items(state.watchNext, key = { it.id }) { item ->
                                WatchNextTile(item, onClick = { vm.launch(item) })
                            }
                        }
                    }
                }
            } else if (settings.showContinueWatching && !state.watchNextPermission && state.loaded) {
                item(key = "watchnext-permission") {
                    PanelButton(
                        title = "Show Continue watching",
                        subtitle = "Lets Lumen read the resume list that apps share with Android TV.",
                        value = "Allow",
                        onClick = onRequestWatchNext,
                    )
                }
            }

            val appRows = state.apps.chunked(APPS_PER_ROW)
            if (appRows.isNotEmpty()) {
                val requester = takeFirstFocus()
                appRows.forEachIndexed { rowIndex, row ->
                    item(key = "apps-$rowIndex") {
                        Column {
                            if (rowIndex == 0) RowTitle("Apps")
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(16.dp),
                                modifier = Modifier.padding(vertical = 4.dp),
                            ) {
                                row.forEachIndexed { col, app ->
                                    val id = "app:${app.key}"
                                    val isMoving = moving == id
                                    androidx.compose.runtime.key(app.key) {
                                        AppTile(
                                            app = app,
                                            onClick = { vm.launch(app) },
                                            onLongClick = { menu = Menu.ForApp(app) },
                                            modifier = Modifier.moveMode(
                                                moving = isMoving,
                                                rowStride = APPS_PER_ROW,
                                                onMove = { vm.moveApp(app.key, it) },
                                                onDone = { moving = null },
                                            ),
                                            focusRequester = if (rowIndex == 0 && col == 0) requester else null,
                                            moving = isMoving,
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            } else if (state.loaded) {
                item(key = "noapps") {
                    Text("No apps found.", color = LumenColors.TextDim)
                }
            }

            if (settings.showInputs && state.inputs.isNotEmpty()) {
                item(key = "inputs") {
                    Column {
                        RowTitle("Inputs")
                        LazyRow(
                            modifier = Modifier.focusRestorer(),
                            horizontalArrangement = Arrangement.spacedBy(16.dp),
                            contentPadding = PaddingValues(vertical = 8.dp),
                        ) {
                            items(state.inputs, key = { it.id }) { input ->
                                InputTile(input, onClick = { vm.launch(input) })
                            }
                        }
                    }
                }
            }

            item(key = "footer") { Spacer(Modifier.padding(24.dp)) }
        }
    }

    when (val m = menu) {
        is Menu.ForApp -> AppActionsDialog(
            app = m.app,
            isFavourite = vm.isFavourite(m.app),
            onDismiss = { menu = null },
            onOpen = { vm.launch(m.app) },
            onToggleFavourite = { vm.toggleFavourite(m.app) },
            onMove = { moving = "app:${m.app.key}" },
            onHide = { vm.hide(m.app) },
        )
        is Menu.ForFavourite -> FavouriteActionsDialog(
            tile = m.tile,
            onDismiss = { menu = null },
            onOpen = { vm.launch(m.tile) },
            onMove = { moving = m.tile.id },
            onRemove = { vm.removeFavourite(m.tile.id) },
        )
        null -> Unit
    }

    if (showQuick) {
        QuickSettingsPanel(
            state = state,
            vm = vm,
            onDismiss = { showQuick = false },
            onOpenLumenSettings = {
                showQuick = false
                onOpenSettings()
            },
            onAddLink = {
                showQuick = false
                showAddLink = true
            },
        )
    }
    if (showAddLink) {
        AddLinkDialog(onDismiss = { showAddLink = false }, onAdd = vm::addLink)
    }
}

@Composable
private fun Header(clockFormat: ClockFormat, onQuick: () -> Unit, onSettings: () -> Unit) {
    val now by produceState(LocalDateTime.now()) {
        while (true) {
            value = LocalDateTime.now()
            delay(1_000L - System.currentTimeMillis() % 1_000L)
        }
    }
    val timeFormat = remember(clockFormat) {
        DateTimeFormatter.ofPattern(if (clockFormat == ClockFormat.H24) "HH:mm" else "h:mm a")
    }
    val dateFormat = remember { DateTimeFormatter.ofPattern("EEEE, d MMMM") }
    Row(
        Modifier
            .fillMaxWidth()
            .padding(bottom = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(Modifier.weight(1f)) {
            Text(now.format(timeFormat), fontSize = 40.sp, fontWeight = FontWeight.Light)
            Text(now.format(dateFormat), fontSize = 15.sp, color = LumenColors.TextDim)
        }
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            PanelButton("Quick settings", width = 170.dp, onClick = onQuick)
            PanelButton("Settings", width = 120.dp, onClick = onSettings)
        }
    }
}

@Composable
private fun AppActionsDialog(
    app: AppEntry,
    isFavourite: Boolean,
    onDismiss: () -> Unit,
    onOpen: () -> Unit,
    onToggleFavourite: () -> Unit,
    onMove: () -> Unit,
    onHide: () -> Unit,
) {
    val ctx = LocalContext.current
    fun act(action: () -> Unit): () -> Unit = { onDismiss(); action() }
    LumenDialog(title = app.label, onDismiss = onDismiss) { focus ->
        PanelButton("Open", modifier = Modifier.focusRequester(focus), onClick = act(onOpen))
        PanelButton(if (isFavourite) "Remove from Favourites" else "Add to Favourites", onClick = act(onToggleFavourite))
        PanelButton("Move", subtitle = "Use the arrows, then press OK", onClick = act(onMove))
        PanelButton("Hide", subtitle = "Bring it back from Settings › Hidden apps", onClick = act(onHide))
        PanelButton("App info", onClick = act { SystemIntents.appInfo(ctx, app.packageName) })
        PanelButton("Uninstall", onClick = act { SystemIntents.uninstall(ctx, app.packageName) })
    }
}

@Composable
private fun FavouriteActionsDialog(
    tile: FavouriteTile,
    onDismiss: () -> Unit,
    onOpen: () -> Unit,
    onMove: () -> Unit,
    onRemove: () -> Unit,
) {
    val ctx = LocalContext.current
    fun act(action: () -> Unit): () -> Unit = { onDismiss(); action() }
    val title = when (tile) {
        is FavouriteTile.AppTile -> tile.app.label
        is FavouriteTile.LinkTile -> tile.link.title
    }
    LumenDialog(title = title, onDismiss = onDismiss) { focus ->
        PanelButton("Open", modifier = Modifier.focusRequester(focus), onClick = act(onOpen))
        PanelButton("Move", subtitle = "Use the arrows, then press OK", onClick = act(onMove))
        PanelButton("Remove from Favourites", onClick = act(onRemove))
        if (tile is FavouriteTile.AppTile) {
            PanelButton("App info", onClick = act { SystemIntents.appInfo(ctx, tile.app.packageName) })
        } else if (tile is FavouriteTile.LinkTile) {
            Text(tile.link.uri, fontSize = 12.sp, color = LumenColors.TextDim, modifier = Modifier.width(360.dp))
        }
    }
}
