package org.polyfrost.oneconfig.internal.ui.components

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import org.polyfrost.compose.foundation.Image
import org.polyfrost.compose.foundation.layout.Box
import org.polyfrost.compose.foundation.layout.fillMaxSize
import org.polyfrost.compose.ui.Modifier
import org.polyfrost.compose.ui.draw.clip
import org.polyfrost.compose.ui.graphics.ImageBitmap
import org.polyfrost.oneconfig.internal.ui.shell.LocalShell
import org.polyfrost.oneconfig.internal.ui.shell.ShellController
import org.polyfrost.oneconfig.internal.ui.themes.LocalTheme

@Composable
fun PlayerHead(modifier: Modifier = Modifier) {
    val shape = LocalTheme.current.sideBarNavigationEntryShape
    val bytes = LocalShell.current.session.playerHeadPng
    val image = remember(bytes) {
        val png = bytes ?: ShellController::class.java.getResourceAsStream("/assets/oneconfig/images/head.png")?.use { it.readBytes() }
        png?.let { runCatching { ImageBitmap.decode(it) }.getOrNull() }
    }

    Box(
        modifier = modifier
            .clip(shape)
    ) {
        if (image != null) Image(image, null, Modifier.fillMaxSize())
    }
}
