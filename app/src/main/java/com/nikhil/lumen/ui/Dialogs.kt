@file:OptIn(ExperimentalTvMaterial3Api::class)

package com.nikhil.lumen.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.tv.material3.ExperimentalTvMaterial3Api
import androidx.tv.material3.Surface
import androidx.tv.material3.SurfaceDefaults
import androidx.tv.material3.Text

/** A centred dialog panel. The first child that takes [initialFocus] gets focus when it opens. */
@Composable
fun LumenDialog(
    title: String,
    onDismiss: () -> Unit,
    width: Dp = 420.dp,
    content: @Composable ColumnScope.(initialFocus: FocusRequester) -> Unit,
) {
    val initialFocus = remember { FocusRequester() }
    Dialog(onDismissRequest = onDismiss) {
        Surface(
            modifier = Modifier.width(width),
            shape = RoundedCornerShape(16.dp),
            colors = SurfaceDefaults.colors(containerColor = LumenColors.Panel),
        ) {
            Column(
                Modifier
                    .padding(24.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                Text(title, fontSize = 20.sp, fontWeight = FontWeight.SemiBold, modifier = Modifier.padding(bottom = 6.dp))
                content(initialFocus)
            }
        }
    }
    LaunchedEffect(Unit) { runCatching { initialFocus.requestFocus() } }
}

/** A panel that slides in from the right edge, used for quick settings. */
@Composable
fun SidePanel(
    title: String,
    onDismiss: () -> Unit,
    content: @Composable ColumnScope.(initialFocus: FocusRequester) -> Unit,
) {
    val initialFocus = remember { FocusRequester() }
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false),
    ) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.CenterEnd) {
            Surface(
                modifier = Modifier
                    .width(380.dp)
                    .fillMaxHeight(),
                colors = SurfaceDefaults.colors(containerColor = LumenColors.Panel),
            ) {
                Column(
                    Modifier
                        .padding(horizontal = 24.dp, vertical = 28.dp)
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    Text(title, fontSize = 22.sp, fontWeight = FontWeight.SemiBold, modifier = Modifier.padding(bottom = 8.dp))
                    content(initialFocus)
                }
            }
        }
    }
    LaunchedEffect(Unit) { runCatching { initialFocus.requestFocus() } }
}

@Composable
fun LumenTextField(
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String,
    modifier: Modifier = Modifier,
    keyboardType: KeyboardType = KeyboardType.Text,
) {
    var focused by remember { mutableStateOf(false) }
    val accent = LocalAccent.current
    BasicTextField(
        value = value,
        onValueChange = onValueChange,
        singleLine = true,
        textStyle = TextStyle(color = LumenColors.Text, fontSize = 16.sp),
        cursorBrush = SolidColor(accent),
        keyboardOptions = KeyboardOptions(keyboardType = keyboardType, imeAction = ImeAction.Next),
        modifier = modifier
            .fillMaxWidth()
            .onFocusChanged { focused = it.isFocused }
            .border(
                BorderStroke(if (focused) 2.dp else 1.dp, if (focused) accent else Color(0x44FFFFFF)),
                RoundedCornerShape(8.dp),
            )
            .padding(horizontal = 14.dp, vertical = 12.dp),
        decorationBox = { inner ->
            Box {
                if (value.isEmpty()) Text(placeholder, color = LumenColors.TextDim, fontSize = 16.sp)
                inner()
            }
        },
    )
}

@Composable
fun AddLinkDialog(onDismiss: () -> Unit, onAdd: (title: String, uri: String) -> Unit) {
    var title by remember { mutableStateOf("") }
    var uri by remember { mutableStateOf("") }
    LumenDialog(title = "Add a link tile", onDismiss = onDismiss, width = 520.dp) { focus ->
        Text(
            "Pin a show, channel, playlist or web page. Paste a link such as https://www.youtube.com/@channel " +
                "or an intent: URI. The app that owns the link opens it.",
            fontSize = 13.sp,
            color = LumenColors.TextDim,
        )
        LumenTextField(title, { title = it }, "Tile name", Modifier.focusRequester(focus))
        LumenTextField(uri, { uri = it }, "Link (https://… or intent:…)", keyboardType = KeyboardType.Uri)
        PanelButton("Add tile") {
            if (uri.isNotBlank()) {
                onAdd(title, uri)
                onDismiss()
            }
        }
        PanelButton("Cancel", onClick = onDismiss)
    }
}
