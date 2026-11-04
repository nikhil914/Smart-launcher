@file:OptIn(ExperimentalTvMaterial3Api::class)

package com.nikhil.lumen.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.tv.material3.Border
import androidx.tv.material3.ClickableSurfaceDefaults
import androidx.tv.material3.ExperimentalTvMaterial3Api
import androidx.tv.material3.Surface
import androidx.tv.material3.Text

val TileShape = RoundedCornerShape(10.dp)

/** The focusable building block for every tile and button: scales up and gets an accent ring on focus. */
@Composable
fun FocusSurface(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    onLongClick: (() -> Unit)? = null,
    focusedScale: Float = 1.08f,
    container: Color = LumenColors.Tile,
    content: @Composable BoxScope.() -> Unit,
) {
    val accent = LocalAccent.current
    Surface(
        onClick = onClick,
        onLongClick = onLongClick,
        modifier = modifier,
        shape = ClickableSurfaceDefaults.shape(shape = TileShape),
        scale = ClickableSurfaceDefaults.scale(focusedScale = focusedScale),
        colors = ClickableSurfaceDefaults.colors(
            containerColor = container,
            contentColor = LumenColors.Text,
            focusedContainerColor = LumenColors.TileFocused,
            focusedContentColor = LumenColors.Text,
            pressedContainerColor = LumenColors.TileFocused,
        ),
        border = ClickableSurfaceDefaults.border(
            focusedBorder = Border(BorderStroke(3.dp, accent), shape = TileShape),
        ),
        content = content,
    )
}

@Composable
fun RowTitle(text: String, modifier: Modifier = Modifier) {
    Text(
        text = text,
        modifier = modifier.padding(bottom = 10.dp),
        color = LumenColors.TextDim,
        fontSize = 15.sp,
        fontWeight = FontWeight.Medium,
    )
}

/** A wide text button used in panels and settings. */
@Composable
fun PanelButton(
    title: String,
    modifier: Modifier = Modifier,
    value: String? = null,
    subtitle: String? = null,
    width: Dp? = null,
    onClick: () -> Unit,
) {
    FocusSurface(
        onClick = onClick,
        modifier = if (width != null) modifier.width(width) else modifier.fillMaxWidth(),
        focusedScale = 1.03f,
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 18.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Column(Modifier.weight(1f)) {
                Text(title, fontSize = 16.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                if (subtitle != null) {
                    Text(subtitle, fontSize = 12.sp, color = LumenColors.TextDim, maxLines = 3)
                }
            }
            if (value != null) {
                Text(value, fontSize = 15.sp, color = LocalAccent.current, maxLines = 1)
            }
        }
    }
}

@Composable
fun StatusDot(ok: Boolean, modifier: Modifier = Modifier) {
    Box(
        modifier
            .size(10.dp)
            .padding(0.dp),
    ) {
        Surface(
            modifier = Modifier.size(10.dp),
            shape = RoundedCornerShape(5.dp),
            colors = androidx.tv.material3.SurfaceDefaults.colors(
                containerColor = if (ok) LumenColors.Good else LumenColors.Warn,
            ),
        ) {}
    }
}
