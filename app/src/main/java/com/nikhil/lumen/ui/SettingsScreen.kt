@file:OptIn(ExperimentalTvMaterial3Api::class)

package com.nikhil.lumen.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.tv.material3.ExperimentalTvMaterial3Api
import androidx.tv.material3.Text
import com.nikhil.lumen.BuildConfigInfo
import com.nikhil.lumen.HomeUiState
import com.nikhil.lumen.HomeViewModel
import com.nikhil.lumen.domain.AccentPreset
import com.nikhil.lumen.domain.ClockFormat
import com.nikhil.lumen.domain.SortMode
import com.nikhil.lumen.domain.WallpaperPreset

private fun <T> List<T>.after(current: T): T = this[(indexOf(current) + 1) % size]

private fun onOff(b: Boolean) = if (b) "On" else "Off"

@Composable
fun SettingsScreen(
    state: HomeUiState,
    vm: HomeViewModel,
    onOpenSetup: () -> Unit,
    onRequestWatchNext: () -> Unit,
) {
    val s = state.settings
    val ctx = LocalContext.current
    val first = remember { FocusRequester() }
    var showAddLink by remember { mutableStateOf(false) }
    var confirmReset by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { runCatching { first.requestFocus() } }

    Wallpaper(s.wallpaper) {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .widthIn(max = 760.dp),
            contentPadding = PaddingValues(horizontal = 64.dp, vertical = 36.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            item { Text("Settings", fontSize = 30.sp, fontWeight = FontWeight.Light, modifier = Modifier.padding(bottom = 8.dp)) }

            item { SectionTitle("Home button") }
            item {
                PanelButton(
                    title = "Home button setup",
                    subtitle = when {
                        state.setup.isDefaultHome -> "Lumen is the default home screen."
                        state.setup.overrideEnabled -> "Home button service is on."
                        else -> "Not set up yet: Home still opens Google TV."
                    },
                    value = if (state.setup.homeButtonWorks) "Done" else "Set up",
                    modifier = Modifier.focusRequester(first),
                    onClick = onOpenSetup,
                )
            }
            item {
                PanelButton(
                    title = "Cover Google TV home when it appears",
                    subtitle = "If the stock home shows up anyway, bring Lumen back. Hold Home to reach Google TV on purpose.",
                    value = onOff(s.coverStockHome),
                ) { vm.updateSettings { it.copy(coverStockHome = !it.coverStockHome) } }
            }

            item { SectionTitle("Look") }
            item {
                PanelButton("Wallpaper", value = s.wallpaper.label) {
                    vm.updateSettings { it.copy(wallpaper = WallpaperPreset.entries.after(it.wallpaper)) }
                }
            }
            item {
                PanelButton("Accent colour", value = s.accent.label) {
                    vm.updateSettings { it.copy(accent = AccentPreset.entries.after(it.accent)) }
                }
            }
            item {
                PanelButton("Clock", value = if (s.clockFormat == ClockFormat.H24) "24-hour" else "12-hour") {
                    vm.updateSettings { it.copy(clockFormat = ClockFormat.entries.after(it.clockFormat)) }
                }
            }

            item { SectionTitle("Rows") }
            item {
                PanelButton(
                    "Sort apps",
                    value = when (s.sortMode) {
                        SortMode.CUSTOM -> "My order"
                        SortMode.ALPHABETICAL -> "A to Z"
                        SortMode.MOST_USED -> "Most used"
                    },
                    subtitle = "Most used counts launches from Lumen only. Nothing leaves the TV.",
                ) { vm.updateSettings { it.copy(sortMode = SortMode.entries.after(it.sortMode)) } }
            }
            item {
                PanelButton("Favourites row", value = onOff(s.showFavourites)) {
                    vm.updateSettings { it.copy(showFavourites = !it.showFavourites) }
                }
            }
            item {
                PanelButton(
                    "Continue watching row",
                    value = onOff(s.showContinueWatching),
                    subtitle = if (state.watchNextPermission) null else "Permission not granted yet",
                ) { vm.updateSettings { it.copy(showContinueWatching = !it.showContinueWatching) } }
            }
            if (!state.watchNextPermission) {
                item { PanelButton("Allow Continue watching", value = "Allow", onClick = onRequestWatchNext) }
            }
            item {
                PanelButton("Inputs row", value = onOff(s.showInputs), subtitle = "HDMI and other physical inputs") {
                    vm.updateSettings { it.copy(showInputs = !it.showInputs) }
                }
            }
            item { PanelButton("Add a link tile", subtitle = "Pin a show, channel or playlist to Favourites") { showAddLink = true } }

            item { SectionTitle("Hidden apps") }
            if (state.hiddenApps.isEmpty()) {
                item { Text("No hidden apps. Hold OK on an app to hide it.", color = LumenColors.TextDim, fontSize = 14.sp) }
            } else {
                items(state.hiddenApps, key = { it.key }) { app ->
                    PanelButton(app.label, value = "Show") { vm.unhide(app.key) }
                }
            }

            item { SectionTitle("Backup") }
            item {
                PanelButton("Back up layout", subtitle = "Saves to Download/Lumen/lumen-backup.json", onClick = { vm.exportBackup() })
            }
            item { PanelButton("Restore layout", subtitle = "Loads Download/Lumen/lumen-backup.json", onClick = { vm.importBackup() }) }
            item {
                PanelButton(
                    "Reset layout",
                    subtitle = "Clears favourites, order, hidden apps and look",
                    value = if (confirmReset) "Press again" else null,
                ) {
                    if (confirmReset) {
                        vm.resetLayout()
                        confirmReset = false
                    } else {
                        confirmReset = true
                    }
                }
            }

            item { SectionTitle("About") }
            item {
                Text(
                    "Lumen ${BuildConfigInfo.versionName(ctx)} · MIT licence · no ads, no tracking, no account.",
                    color = LumenColors.TextDim,
                    fontSize = 14.sp,
                )
            }
        }
    }

    if (showAddLink) AddLinkDialog(onDismiss = { showAddLink = false }, onAdd = vm::addLink)
}

@Composable
private fun SectionTitle(text: String) {
    Text(
        text,
        color = LocalAccent.current,
        fontSize = 14.sp,
        fontWeight = FontWeight.SemiBold,
        modifier = Modifier.padding(top = 14.dp, bottom = 2.dp),
    )
}
