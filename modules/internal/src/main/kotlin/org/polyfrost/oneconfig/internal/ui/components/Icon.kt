package org.polyfrost.oneconfig.internal.ui.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.FilterQuality
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.painter.BitmapPainter
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.res.loadImageBitmap
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import org.polyfrost.oneconfig.internal.ui.themes.Accent
import org.polyfrost.oneconfig.internal.ui.themes.LocalTheme
import java.io.File
import java.util.concurrent.ConcurrentHashMap
import kotlin.math.sqrt

private object IconResourceMarker
private const val DefaultIconSize = 18f
private val DefaultRasterIconShape = RoundedCornerShape(8.dp)

private object IconBitmapCache {
    private class Entry(val lastModified: Long, val bitmap: ImageBitmap)

    private val cache = ConcurrentHashMap<String, Entry>()

    fun load(path: String, lastModified: Long, decode: () -> ImageBitmap): ImageBitmap {
        cache[path]?.let { if (it.lastModified == lastModified) return it.bitmap }
        val bitmap = decode()
        cache[path] = Entry(lastModified, bitmap)
        return bitmap
    }
}

@Composable
fun Icon(
    iconName: String,
    color: Color = Color.Unspecified,
    modifier: Modifier = Modifier,
    fitAspectRatio: Boolean = false,
) {
    val theme = LocalTheme.current
    val resolvedColor = if (color == Color.Unspecified) theme.textColor else color

    // Absolute filesystem path (e.g. a mod icon extracted from its own jar) — load from disk
    // rather than the classpath so distinct mods can't collide on a shared resource name.
    val file = if (iconName.contains('/') || iconName.contains('.')) {
        File(iconName).takeIf { it.isAbsolute && it.isFile }
    } else null
    if (file != null) {
        val isSvg = file.extension.equals("svg", ignoreCase = true)
        val over = LocalUiOversample.current
        val painter = remember(iconName, file.lastModified(), over) {
            runCatching {
                if (isSvg) OversampledSvgPainter(file.readBytes(), over)
                else BitmapPainter(
                    IconBitmapCache.load(iconName, file.lastModified()) {
                        file.inputStream().buffered().use(::loadImageBitmap)
                    }
                )
            }.getOrNull()
        }
        if (painter != null) {
            val aspectRatio = remember(iconName, file.lastModified()) {
                if (fitAspectRatio && isSvg) file.inputStream().buffered().use(::readSvgAspectRatio) else null
            }
            val imageModifier = modifier.then(iconSizeModifier(aspectRatio))
            val clippedModifier = if (!isSvg) {
                imageModifier.clip(DefaultRasterIconShape)
            } else {
                imageModifier
            }
            Image(
                painter = painter,
                contentDescription = null,
                modifier = clippedModifier,
                colorFilter = if (isSvg) ColorFilter.tint(resolvedColor) else null,
                // Raster mod icons ship larger than the 48dp card and get minified; the default
                // FilterQuality.Low has no mipmaps and softens/aliases them (SVGs stay crisp).
                filterQuality = if (isSvg) FilterQuality.Low else FilterQuality.Medium
            )
            return
        }
    }

    val defaultPath = iconName.toIconResourcePath()
    val overridePath = theme.iconOverrides[iconName]?.toIconResourcePath()
    val path = overridePath?.takeIf(::iconResourceExists) ?: defaultPath.takeIf(::iconResourceExists) ?: return
    val isSvg = path.endsWith(".svg", ignoreCase = true)
    val aspectRatio = remember(path, fitAspectRatio) { if (fitAspectRatio && isSvg) readSvgAspectRatio(path) else null }
    val resourceModifier = modifier.then(iconSizeModifier(aspectRatio))
    val clippedResourceModifier = if (!isSvg) {
        resourceModifier.clip(DefaultRasterIconShape)
    } else {
        resourceModifier
    }
    val painter = if (isSvg) {
        rememberIconSvgPainter(path) ?: return
    } else {
        painterResource(path)
    }
    Image(
        painter = painter,
        contentDescription = null,
        modifier = clippedResourceModifier,
        colorFilter = if (isSvg) ColorFilter.tint(resolvedColor) else null,
        filterQuality = if (isSvg) FilterQuality.Low else FilterQuality.Medium
    )
}

@Composable
private fun rememberIconSvgPainter(path: String): Painter? {
    val over = LocalUiOversample.current
    return remember(path, over) {
        readIconResourceBytes(path)?.let { OversampledSvgPainter(it, over) }
    }
}

private fun readIconResourceBytes(path: String): ByteArray? {
    val normalized = path.removePrefix("/")
    val loader = Thread.currentThread().contextClassLoader ?: IconResourceMarker::class.java.classLoader
    val stream = loader?.getResourceAsStream(normalized)
        ?: IconResourceMarker::class.java.getResourceAsStream(path)
    return stream?.use { it.readBytes() }
}

fun canRenderIcon(iconName: String): Boolean {
    if (iconName.contains('/') || iconName.contains('.')) {
        val file = File(iconName)
        if (file.isAbsolute) return file.isFile
    }
    return iconResourceExists(iconName.toIconResourcePath())
}

private fun String.toIconResourcePath(): String =
    if (contains('/') || contains('.')) this else "/assets/oneconfig/ico/$this.svg"

private fun iconResourceExists(path: String): Boolean {
    val normalized = path.removePrefix("/")
    return Thread.currentThread().contextClassLoader?.getResource(normalized) != null ||
        IconResourceMarker::class.java.classLoader?.getResource(normalized) != null
}

private fun iconSizeModifier(aspectRatio: Float?): Modifier {
    val ratio = aspectRatio?.takeIf { it.isFinite() && it > 0f } ?: 1f
    val scale = sqrt(ratio)
    return Modifier.size((DefaultIconSize * scale).dp, (DefaultIconSize / scale).dp)
}

private fun readSvgAspectRatio(path: String): Float? {
    val normalized = path.removePrefix("/")
    val loader = Thread.currentThread().contextClassLoader ?: IconResourceMarker::class.java.classLoader
    return loader.getResourceAsStream(normalized)?.buffered()?.use(::readSvgAspectRatio)
        ?: IconResourceMarker::class.java.classLoader?.getResourceAsStream(normalized)?.buffered()?.use(::readSvgAspectRatio)
}

private fun readSvgAspectRatio(stream: java.io.InputStream): Float? {
    val header = stream.reader().use { it.readText().take(512) }
    val viewBox = Regex("""viewBox\s*=\s*"([^"]+)"""").find(header)?.groupValues?.get(1) ?: return null
    val values = viewBox.trim().split(Regex("""[\s,]+""")).mapNotNull { it.toFloatOrNull() }
    if (values.size < 4) return null
    val width = values[2]
    val height = values[3]
    if (width <= 0f || height <= 0f) return null
    return width / height
}

@Composable
fun IconWithIndicator(
    iconName: String,
    color: Color = Color.Unspecified,
    modifier: Modifier = Modifier,
    showIndicator: Boolean = true,
) {
    val resolvedColor = if (color == Color.Unspecified) LocalTheme.current.textColor else color
    Box {
        val painter = rememberIconSvgPainter("/assets/oneconfig/ico/$iconName.svg")
        if (painter != null) Image(
            painter = painter,
            contentDescription = null,
            modifier = modifier.size(18.dp),
            colorFilter = ColorFilter.tint(resolvedColor)
        )
        if (showIndicator) Box(
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
