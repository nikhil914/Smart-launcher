@file:OptIn(ExperimentalTvMaterial3Api::class)

package com.nikhil.lumen.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.tv.material3.ExperimentalTvMaterial3Api
import androidx.tv.material3.Text
import com.nikhil.lumen.HomeUiState
import com.nikhil.lumen.system.HomeSetup
import com.nikhil.lumen.system.SystemIntents

/** Walks the user through the three ways to make Home open Lumen, showing which are already done. */
@Composable
fun SetupScreen(state: HomeUiState) {
    val ctx = LocalContext.current
    val first = remember { FocusRequester() }
    LaunchedEffect(Unit) { runCatching { first.requestFocus() } }
    val setup = state.setup

    Wallpaper(state.settings.wallpaper) {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(horizontal = 64.dp, vertical = 36.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            item {
                Text("Home button setup", fontSize = 30.sp, fontWeight = FontWeight.Light)
                Text(
                    "Google TV keeps its own home screen in charge. Pick the first option that works on your TV. " +
                        "Lumen never disables the Google TV launcher, so uninstalling Lumen always gets you back.",
                    color = LumenColors.TextDim,
                    fontSize = 14.sp,
                    modifier = Modifier.padding(top = 6.dp, bottom = 8.dp),
                )
            }

            item { StatusLine("Lumen is the default home screen", setup.isDefaultHome) }
            item { StatusLine("Lumen Home button service is on", setup.overrideEnabled) }
            item { StatusLine("Strong mode granted (keeps the service on)", setup.strongMode) }

            item { Step("1", "Plain Android TV (Shield, Sony, Philips…)") }
            item {
                PanelButton(
                    "Choose default home screen",
                    subtitle = "Opens the system picker. Choose Lumen. Google TV usually hides this screen; if nothing happens, use step 2.",
                    modifier = Modifier.focusRequester(first),
                ) { SystemIntents.homeChooser(ctx) }
            }

            item { Step("2", "Google TV: turn on the Lumen Home button service") }
            item {
                PanelButton(
                    "Open accessibility settings",
                    subtitle = "Find \"Lumen Home button\" and turn it on. Short press Home opens Lumen; hold Home for Google TV.",
                ) { SystemIntents.accessibility(ctx) }
            }
            item {
                Text(
                    "If the switch is greyed out (Android 13+ sideloaded apps): open App info for Lumen, choose \"Allow restricted settings\" " +
                        "from the menu, then try again. Some TVs hide that menu; the third command below does the same thing.",
                    color = LumenColors.TextDim,
                    fontSize = 13.sp,
                )
            }
            item { PanelButton("Open Lumen app info") { SystemIntents.appInfo(ctx, ctx.packageName) } }

            item { Step("3", "Strong mode (optional, needs a computer or phone with ADB)") }
            item {
                Text(
                    "Turn on Developer options and network or USB debugging, connect, then run any of these. The first makes Lumen the " +
                        "system home. The second lets Lumen switch its service back on after updates. The third unlocks restricted settings.",
                    color = LumenColors.TextDim,
                    fontSize = 13.sp,
                )
            }
            item {
                Column(
                    Modifier
                        .fillMaxWidth()
                        .background(Color(0x66000000), RoundedCornerShape(10.dp))
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    HomeSetup.adbCommands(ctx).forEach { cmd ->
                        Text(cmd, fontFamily = FontFamily.Monospace, fontSize = 13.sp)
                    }
                }
            }
            item {
                Text(
                    "Lumen does not run \"pm disable\" on the Google TV launcher. On some TVs that causes boot loops or breaks setup screens.",
                    color = LumenColors.TextDim,
                    fontSize = 13.sp,
                )
            }
        }
    }
}

@Composable
private fun StatusLine(text: String, ok: Boolean) {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        StatusDot(ok)
        Text(text, fontSize = 15.sp)
        Text(if (ok) "Yes" else "No", fontSize = 15.sp, color = if (ok) LumenColors.Good else LumenColors.Warn)
    }
}

@Composable
private fun Step(number: String, title: String) {
    Text(
        "$number  ·  $title",
        color = LocalAccent.current,
        fontSize = 16.sp,
        fontWeight = FontWeight.SemiBold,
        modifier = Modifier.padding(top = 16.dp),
    )
}
