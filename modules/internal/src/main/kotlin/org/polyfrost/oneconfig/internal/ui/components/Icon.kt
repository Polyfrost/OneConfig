package org.polyfrost.oneconfig.internal.ui.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.ColorMatrix
import androidx.compose.ui.graphics.FilterQuality
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.painter.BitmapPainter
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.graphics.toComposeImageBitmap
import androidx.compose.ui.res.loadImageBitmap
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.polyfrost.oneconfig.internal.ui.themes.Accent
import org.polyfrost.oneconfig.internal.ui.themes.LocalTheme
import java.io.ByteArrayInputStream
import java.io.File
import java.util.concurrent.ConcurrentHashMap
import javax.imageio.ImageIO
import kotlin.math.sqrt

private object IconResourceMarker
private const val DefaultIconSize = 18f
private val DefaultRasterIconShape = RoundedCornerShape(8.dp)

/**
 * A decoded raster icon plus the filter quality it should be scaled with
 *
 * Pixel art is upscaled with nearest neighbour so it stays crisp
 */
private class RasterIcon(val bitmap: ImageBitmap, val filterQuality: FilterQuality)

private object IconBitmapCache {
    private class Entry(val lastModified: Long, val icon: RasterIcon)

    private val cache = ConcurrentHashMap<String, Entry>()

    fun peek(key: String, lastModified: Long): RasterIcon? =
        cache[key]?.takeIf { it.lastModified == lastModified }?.icon

    fun put(key: String, lastModified: Long, icon: RasterIcon) {
        cache[key] = Entry(lastModified, icon)
    }
}

/**
 * Returns the cached raster icon for [key], or null while it is being decoded on a background thread
 */
@Composable
private fun rememberAsyncRasterIcon(key: String, lastModified: Long, read: () -> ByteArray?): RasterIcon? {
    return produceState(IconBitmapCache.peek(key, lastModified), key, lastModified) {
        IconBitmapCache.peek(key, lastModified)?.let {
            value = it
            return@produceState
        }
        value = withContext(Dispatchers.IO) {
            runCatching { read()?.let(::decodeRasterIcon) }.getOrNull()
                ?.also { IconBitmapCache.put(key, lastModified, it) }
        }
    }.value
}

/**
 * Decodes raster icon [bytes] picking nearest neighbour filtering when the image looks like pixel art
 *
 * Falls back to Compose's own decoder if AWT cannot read the format
 */
private fun decodeRasterIcon(bytes: ByteArray): RasterIcon {
    val buffered = runCatching { ImageIO.read(ByteArrayInputStream(bytes)) }.getOrNull()
        ?: return RasterIcon(loadImageBitmap(ByteArrayInputStream(bytes)), FilterQuality.Medium)
    val quality = if (buffered.isPixelArt()) FilterQuality.None else FilterQuality.Medium
    return RasterIcon(buffered.toComposeImageBitmap(), quality)
}

private fun rasterPainter(icon: RasterIcon) = BitmapPainter(icon.bitmap, filterQuality = icon.filterQuality)

@Composable
fun Icon(
    iconName: String,
    color: Color = Color.Unspecified,
    modifier: Modifier = Modifier,
    fitAspectRatio: Boolean = false,
) {
    val theme = LocalTheme.current
    val resolvedColor = if (color == Color.Unspecified) theme.textColor else color

    // absolute paths load from disk rather than the classpath so distinct mods cannot collide on a
    // shared resource name
    val file = if (iconName.contains('/') || iconName.contains('.')) {
        File(iconName).takeIf { it.isAbsolute && it.isFile }
    } else null
    if (file != null) {
        val isSvg = file.extension.equals("svg", ignoreCase = true)
        val lastModified = file.lastModified()
        if (isSvg) {
            val over = LocalUiOversample.current
            val painter = remember(iconName, lastModified, over) {
                runCatching { OversampledSvgPainter(file.readBytes(), over) }.getOrNull()
            }
            if (painter != null) {
                val aspectRatio = remember(iconName, lastModified) {
                    if (fitAspectRatio) file.inputStream().buffered().use(::readSvgAspectRatio) else null
                }
                Image(
                    painter = painter,
                    contentDescription = null,
                    modifier = modifier.then(iconSizeModifier(aspectRatio)),
                    colorFilter = ColorFilter.tint(resolvedColor)
                )
                return
            }
        } else {
            val icon = rememberAsyncRasterIcon(iconName, lastModified) { file.readBytes() }
            val imageModifier = modifier.then(iconSizeModifier(null)).clip(DefaultRasterIconShape)
            if (icon != null) {
                Image(
                    painter = remember(icon) { rasterPainter(icon) },
                    contentDescription = null,
                    modifier = imageModifier
                )
            } else {
                // keeps the icon's space reserved while it decodes in the background
                Box(imageModifier)
            }
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
        rememberIconRasterPainter(path) ?: run {
            // keeps the icon's space reserved while it decodes in the background
            Box(clippedResourceModifier)
            return
        }
    }
    Image(
        painter = painter,
        contentDescription = null,
        modifier = clippedResourceModifier,
        colorFilter = if (isSvg) ColorFilter.tint(resolvedColor) else null
    )
}

@Composable
private fun rememberIconSvgPainter(path: String): Painter? {
    val over = LocalUiOversample.current
    return remember(path, over) {
        readIconResourceBytes(path)?.let { OversampledSvgPainter(it, over) }
    }
}

@Composable
fun rememberSvgResourcePainter(path: String): Painter? {
    val over = LocalUiOversample.current
    return remember(path, over) {
        readIconResourceBytes(path)?.let { OversampledSvgPainter(it, over) }
    }
}

private const val LumR = 0.2126f
private const val LumG = 0.7152f
private const val LumB = 0.0722f

private val brandFillPeakCache = ConcurrentHashMap<String, Float>()
private val hexColorRegex = Regex("#([0-9a-fA-F]{6})\\b")

private fun brandFillPeak(path: String): Float = brandFillPeakCache.getOrPut(path) {
    val svg = readIconResourceBytes(path)?.toString(Charsets.UTF_8) ?: return@getOrPut 0f
    hexColorRegex.findAll(svg).mapNotNull { match ->
        val rgb = match.groupValues[1].toInt(16)
        val r = (rgb shr 16 and 0xFF) / 255f
        val g = (rgb shr 8 and 0xFF) / 255f
        val b = (rgb and 0xFF) / 255f
        if (r == g && g == b) null else LumR * r + LumG * g + LumB * b
    }.maxOrNull() ?: 0f
}

@Composable
fun rememberBrandTint(path: String, tint: Color): ColorFilter? = remember(path, tint) {
    val peak = brandFillPeak(path)
    if (peak <= 0f) return@remember null
    val scale = 1f / peak
    ColorFilter.colorMatrix(
        ColorMatrix(
            floatArrayOf(
                tint.red * scale * LumR, tint.red * scale * LumG, tint.red * scale * LumB, 0f, 0f,
                tint.green * scale * LumR, tint.green * scale * LumG, tint.green * scale * LumB, 0f, 0f,
                tint.blue * scale * LumR, tint.blue * scale * LumG, tint.blue * scale * LumB, 0f, 0f,
                0f, 0f, 0f, 1f, 0f,
            )
        )
    )
}

@Composable
private fun rememberIconRasterPainter(path: String): Painter? {
    val icon = rememberAsyncRasterIcon(path, 0L) { readIconResourceBytes(path) }
    return icon?.let { remember(it) { rasterPainter(it) } }
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

private val iconResourceExistsCache = ConcurrentHashMap<String, Boolean>()

private fun iconResourceExists(path: String): Boolean = iconResourceExistsCache.getOrPut(path) {
    val normalized = path.removePrefix("/")
    Thread.currentThread().contextClassLoader?.getResource(normalized) != null ||
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
