@file:OptIn(ExperimentalTvMaterial3Api::class)

package com.nikhil.lumen.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.tv.material3.ExperimentalTvMaterial3Api
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.darkColorScheme
import com.nikhil.lumen.domain.AccentPreset

object LumenColors {
    val Text = Color(0xFFF2F4F8)
    val TextDim = Color(0xFFA9B1C1)
    val Tile = Color(0x33FFFFFF)
    val TileFocused = Color(0x55FFFFFF)
    val Panel = Color(0xF0161B24)
    val Good = Color(0xFF6BD49A)
    val Warn = Color(0xFFFFB454)
}

val LocalAccent = staticCompositionLocalOf { Color(AccentPreset.BLUE.argb) }

@Composable
fun LumenTheme(accent: AccentPreset, content: @Composable () -> Unit) {
    val accentColor = Color(accent.argb)
    MaterialTheme(
        colorScheme = darkColorScheme(
            primary = accentColor,
            onPrimary = Color(0xFF0E1116),
            surface = Color(0xFF161B24),
            onSurface = LumenColors.Text,
            background = Color(0xFF0E1116),
            onBackground = LumenColors.Text,
        ),
    ) {
        CompositionLocalProvider(LocalAccent provides accentColor, content = content)
    }
}
