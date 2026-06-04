package org.polyfrost.oneconfig.internal.ui.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.painter.BitmapPainter
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.loadImageBitmap
import androidx.compose.ui.res.loadSvgPainter
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import org.polyfrost.oneconfig.internal.ui.themes.Accent
import org.polyfrost.oneconfig.internal.ui.themes.LocalTheme
import java.io.File

@Composable
fun Icon(iconName: String, color: Color = Color.Unspecified, modifier: Modifier = Modifier) {
    val resolvedColor = if (color == Color.Unspecified) LocalTheme.current.textColor else color

    // Absolute filesystem path (e.g. a mod icon extracted from its own jar) — load from disk
    // rather than the classpath so distinct mods can't collide on a shared resource name.
    val file = if (iconName.contains('/') || iconName.contains('.')) {
        File(iconName).takeIf { it.isAbsolute && it.isFile }
    } else null
    if (file != null) {
        val isSvg = file.extension.equals("svg", ignoreCase = true)
        val density = LocalDensity.current
        val painter = remember(iconName) {
            file.inputStream().buffered().use { stream ->
                if (isSvg) loadSvgPainter(stream, density) else BitmapPainter(loadImageBitmap(stream))
            }
        }
        Image(
            painter = painter,
            contentDescription = null,
            modifier = modifier.size(18.dp),
            colorFilter = if (isSvg) ColorFilter.tint(resolvedColor) else null
        )
        return
    }

    val path = if (iconName.contains('/') || iconName.contains('.')) iconName
               else "/assets/oneconfig/ico/$iconName.svg"
    val isSvg = path.endsWith(".svg", ignoreCase = true)
    Image(
        painter = painterResource(path),
        contentDescription = null,
        modifier = modifier.size(18.dp),
        colorFilter = if (isSvg) ColorFilter.tint(resolvedColor) else null
    )
}

@Composable
fun IconWithIndicator(iconName: String, color: Color = Color.Unspecified, modifier: Modifier = Modifier) {
    val resolvedColor = if (color == Color.Unspecified) LocalTheme.current.textColor else color
    Box {
        Image(
            painter = painterResource("/assets/oneconfig/ico/$iconName.svg"),
            contentDescription = null,
            modifier = modifier.size(18.dp),
            colorFilter = ColorFilter.tint(resolvedColor)
        )
        Box(
            modifier = Modifier.align(Alignment.TopEnd)
                .offset(2.dp, (-2).dp),
            contentAlignment = Alignment.Center
        ) {
            Box(
                Modifier
                    .size(8.dp)
                    .background(
                        Accent,
                        shape = LocalTheme.current.circleShape
                    )
                    .blur(7.dp)
            )

            Box(
                Modifier
                    .size(6.dp)
                    .background(
                        Accent,
                        shape = LocalTheme.current.circleShape
                    )
            )
        }
    }
}
