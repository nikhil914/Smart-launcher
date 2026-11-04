@file:OptIn(ExperimentalTvMaterial3Api::class)

package com.nikhil.lumen.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.tv.material3.ExperimentalTvMaterial3Api
import androidx.tv.material3.Text
import com.nikhil.lumen.system.HomeSetup
import com.nikhil.lumen.system.SystemIntents

/**
 * Shown instead of the home screen after repeated crashes. Uses no app data, so it cannot crash
 * for the same reason the home screen did.
 */
@Composable
fun RecoveryScreen(onResetAndRetry: () -> Unit, onRetry: () -> Unit) {
    val ctx = LocalContext.current
    val first = remember { FocusRequester() }
    LaunchedEffect(Unit) { runCatching { first.requestFocus() } }
    Box(Modifier.fillMaxSize().background(Color(0xFF0E1116)), contentAlignment = Alignment.Center) {
        Column(Modifier.width(560.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text("Lumen stopped several times", fontSize = 26.sp, fontWeight = FontWeight.Light)
            Text(
                "To keep your TV usable, Lumen opened this safe screen. Try again, reset Lumen's layout, or go to the Google TV home.",
                color = LumenColors.TextDim,
                fontSize = 14.sp,
                modifier = Modifier.padding(bottom = 8.dp),
            )
            PanelButton("Try again", modifier = Modifier.focusRequester(first), onClick = onRetry)
            PanelButton("Reset layout and try again", onClick = onResetAndRetry)
            PanelButton("Open Google TV home") { HomeSetup.openStockHome(ctx) }
            PanelButton("Open Settings") { SystemIntents.allSettings(ctx) }
        }
    }
}
