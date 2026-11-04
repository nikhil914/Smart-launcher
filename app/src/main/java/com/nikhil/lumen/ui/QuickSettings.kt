@file:OptIn(ExperimentalTvMaterial3Api::class)

package com.nikhil.lumen.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.platform.LocalContext
import androidx.tv.material3.ExperimentalTvMaterial3Api
import com.nikhil.lumen.HomeUiState
import com.nikhil.lumen.HomeViewModel
import com.nikhil.lumen.system.SleepTimer
import com.nikhil.lumen.system.SystemIntents

/** One button away from the settings Google TV buries several menus deep. Opens with Menu or the header button. */
@Composable
fun QuickSettingsPanel(
    state: HomeUiState,
    vm: HomeViewModel,
    onDismiss: () -> Unit,
    onOpenLumenSettings: () -> Unit,
    onAddLink: () -> Unit,
) {
    val ctx = LocalContext.current
    var sleepMinutes by remember { mutableIntStateOf(SleepTimer.remainingMinutes()) }
    fun go(action: () -> Unit): () -> Unit = { onDismiss(); action() }

    SidePanel(title = "Quick settings", onDismiss = onDismiss) { focus ->
        PanelButton("Wi-Fi", modifier = Modifier.focusRequester(focus), onClick = go { SystemIntents.wifi(ctx) })
        PanelButton("Bluetooth and remotes", onClick = go { SystemIntents.bluetooth(ctx) })
        PanelButton("Picture and display", onClick = go { SystemIntents.display(ctx) })
        PanelButton("Sound and audio output", onClick = go { SystemIntents.sound(ctx) })
        PanelButton(
            title = "Sleep timer",
            value = if (sleepMinutes == 0) "Off" else "$sleepMinutes min",
            subtitle = if (state.setup.overrideEnabled) null else "Needs the Lumen Home button service",
        ) { sleepMinutes = SleepTimer.cycle(ctx) }
        state.inputs.forEach { input ->
            PanelButton("Switch to ${input.label}", subtitle = input.kind, onClick = go { vm.launch(input) })
        }
        PanelButton("Add a link tile", onClick = onAddLink)
        PanelButton("Apps", onClick = go { SystemIntents.apps(ctx) })
        PanelButton("All settings", onClick = go { SystemIntents.allSettings(ctx) })
        PanelButton("Lumen settings", onClick = onOpenLumenSettings)
    }
}
