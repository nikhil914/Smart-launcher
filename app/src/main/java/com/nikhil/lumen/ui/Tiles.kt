@file:OptIn(ExperimentalTvMaterial3Api::class)

package com.nikhil.lumen.ui

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.tv.material3.ExperimentalTvMaterial3Api
import androidx.tv.material3.Text
import com.nikhil.lumen.LumenApp
import com.nikhil.lumen.data.TvInput
import com.nikhil.lumen.data.WatchNextItem
import com.nikhil.lumen.domain.AppEntry

val TileWidth = 160.dp
val TileHeight = 90.dp

private class AppArt(val image: ImageBitmap?, val isBanner: Boolean)

@Composable
private fun rememberAppArt(app: AppEntry): AppArt {
    val lumen = LumenApp.from(LocalContext.current)
    val art by produceState(
        initialValue = AppArt(lumen.images.cached("banner:${app.key}"), true),
        key1 = app.key,
    ) {
        val banner = lumen.images.fromDrawable("banner:${app.key}", 320, 180) { lumen.apps.banner(app) }
        value = if (banner != null) {
            AppArt(banner, true)
        } else {
            AppArt(lumen.images.fromDrawable("icon:${app.key}", 144, 144) { lumen.apps.icon(app) }, false)
        }
    }
    return art
}

/**
 * Handles "move mode": while [moving] is true, left/right (and up/down with [rowStride]) move the
 * tile instead of focus, and OK or Back drops it.
 */
fun Modifier.moveMode(
    moving: Boolean,
    rowStride: Int,
    onMove: (Int) -> Unit,
    onDone: () -> Unit,
): Modifier = if (!moving) this else onPreviewKeyEvent { event ->
    if (event.type != KeyEventType.KeyDown) return@onPreviewKeyEvent true
    when (event.key) {
        Key.DirectionLeft -> onMove(-1)
        Key.DirectionRight -> onMove(1)
        Key.DirectionUp -> if (rowStride > 0) onMove(-rowStride)
        Key.DirectionDown -> if (rowStride > 0) onMove(rowStride)
        Key.DirectionCenter, Key.Enter, Key.NumPadEnter, Key.Back, Key.Escape -> onDone()
        else -> return@onPreviewKeyEvent false
    }
    true
}

/** Labelled tile frame shared by every row. */
@Composable
private fun TileFrame(
    label: String,
    modifier: Modifier,
    focusRequester: FocusRequester?,
    moving: Boolean,
    onClick: () -> Unit,
    onLongClick: (() -> Unit)?,
    width: androidx.compose.ui.unit.Dp = TileWidth,
    height: androidx.compose.ui.unit.Dp = TileHeight,
    content: @Composable () -> Unit,
) {
    var focused by remember { mutableStateOf(false) }
    val requester = focusRequester ?: remember { FocusRequester() }
    // Keep focus on a tile while it is being moved: its position (and parent row) can change.
    LaunchedEffect(moving) { if (moving) runCatching { requester.requestFocus() } }
    Column(modifier.width(width)) {
        FocusSurface(
            onClick = onClick,
            onLongClick = onLongClick,
            modifier = Modifier
                .size(width, height)
                .focusRequester(requester)
                .onFocusChanged { focused = it.isFocused },
            focusedScale = if (moving) 1.14f else 1.08f,
        ) { content() }
        Text(
            text = if (moving) "◀  move  ▶" else label,
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 8.dp),
            color = if (focused || moving) LumenColors.Text else Color.Transparent,
            fontSize = 13.sp,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            textAlign = TextAlign.Center,
        )
    }
}

@Composable
fun AppTile(
    app: AppEntry,
    onClick: () -> Unit,
    onLongClick: () -> Unit,
    modifier: Modifier = Modifier,
    focusRequester: FocusRequester? = null,
    moving: Boolean = false,
) {
    val art = rememberAppArt(app)
    TileFrame(app.label, modifier, focusRequester, moving, onClick, onLongClick) {
        val image = art.image
        when {
            image != null && art.isBanner -> Image(
                bitmap = image,
                contentDescription = app.label,
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop,
            )
            image != null -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Image(bitmap = image, contentDescription = app.label, modifier = Modifier.size(52.dp))
            }
            else -> LetterTile(app.label)
        }
    }
}

@Composable
fun LinkTile(
    title: String,
    onClick: () -> Unit,
    onLongClick: () -> Unit,
    modifier: Modifier = Modifier,
    focusRequester: FocusRequester? = null,
    moving: Boolean = false,
) {
    TileFrame(title, modifier, focusRequester, moving, onClick, onLongClick) {
        Box(
            Modifier
                .fillMaxSize()
                .background(Brush.linearGradient(listOf(LocalAccent.current.copy(alpha = 0.55f), Color(0x22000000)))),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                title,
                modifier = Modifier.padding(10.dp),
                fontSize = 15.sp,
                fontWeight = FontWeight.SemiBold,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                textAlign = TextAlign.Center,
            )
        }
    }
}

@Composable
fun InputTile(input: TvInput, onClick: () -> Unit, modifier: Modifier = Modifier) {
    TileFrame(input.label, modifier, null, false, onClick, null, height = 64.dp) {
        Column(
            Modifier.fillMaxSize(),
            verticalArrangement = androidx.compose.foundation.layout.Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(input.kind, fontSize = 12.sp, color = LumenColors.TextDim)
            Text(input.label, fontSize = 15.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
    }
}

@Composable
fun WatchNextTile(item: WatchNextItem, onClick: () -> Unit, modifier: Modifier = Modifier) {
    val lumen = LumenApp.from(LocalContext.current)
    val poster by produceState(initialValue = item.posterUri?.let { lumen.images.cached(it) }, key1 = item.posterUri) {
        val uri = item.posterUri ?: return@produceState
        value = lumen.images.fromUri(uri, maxWidth = 400)
    }
    val label = listOfNotNull(item.title, item.subtitle).joinToString(" · ")
    TileFrame(label, modifier, null, false, onClick, null, width = 200.dp, height = 112.dp) {
        Box(Modifier.fillMaxSize()) {
            val image = poster
            if (image != null) {
                Image(image, label, Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
            } else {
                Box(Modifier.fillMaxSize().padding(12.dp), contentAlignment = Alignment.BottomStart) {
                    Text(item.title, fontSize = 15.sp, maxLines = 2, overflow = TextOverflow.Ellipsis)
                }
            }
            item.progress?.let { p ->
                Box(
                    Modifier
                        .align(Alignment.BottomStart)
                        .fillMaxWidth()
                        .height(4.dp)
                        .background(Color(0x66000000)),
                ) {
                    Box(
                        Modifier
                            .fillMaxHeight()
                            .fillMaxWidth(p)
                            .background(LocalAccent.current),
                    )
                }
            }
        }
    }
}

@Composable
private fun LetterTile(label: String) {
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Text(
            label.take(1).uppercase(),
            fontSize = 34.sp,
            fontWeight = FontWeight.Bold,
            color = LocalAccent.current,
        )
    }
}
