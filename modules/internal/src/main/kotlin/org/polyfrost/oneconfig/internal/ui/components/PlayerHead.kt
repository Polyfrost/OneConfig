package org.polyfrost.oneconfig.internal.ui.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.border
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.painter.BitmapPainter
import androidx.compose.ui.res.loadImageBitmap
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import org.polyfrost.oneconfig.internal.ui.shell.ShellState
import org.polyfrost.oneconfig.internal.ui.themes.LocalTheme
import java.io.ByteArrayInputStream

@Composable
fun PlayerHead(modifier: Modifier = Modifier) {
    val shape = LocalTheme.current.sideBarNavigationEntryShape
    val fallback = painterResource("/assets/oneconfig/images/head.png")
    val bytes = ShellState.playerHeadPng
    val painter = remember(bytes) {
        bytes?.let { BitmapPainter(loadImageBitmap(ByteArrayInputStream(it))) }
    }

    Image(
        painter = painter ?: fallback,
        contentDescription = null,
        modifier = modifier
            .clip(shape)
            .border(1.dp, LocalTheme.current.borderColor, shape),
    )
}
