package org.polyfrost.oneconfig.internal.ui.components

import androidx.compose.runtime.Composable
import org.polyfrost.compose.animation.AnimatedColor
import org.polyfrost.compose.layout.PolyLayoutStyle
import org.polyfrost.compose.layout.PolySize
import org.polyfrost.compose.ui.Modifier
import org.polyfrost.compose.ui.graphics.Color
import org.polyfrost.compose.ui.graphics.isUnspecified
import org.polyfrost.compose.ui.platform.LocalDensity
import org.polyfrost.oneconfig.internal.ui.themes.LocalTheme
import java.io.File
import java.util.concurrent.ConcurrentHashMap
import org.polyfrost.oneconfig.utils.v1.Multithreading
import androidx.compose.runtime.remember
import kotlin.math.roundToInt
import org.polyfrost.compose.foundation.Image
import org.polyfrost.compose.foundation.layout.Box
import org.polyfrost.compose.foundation.layout.size
import org.polyfrost.compose.ui.graphics.ImageBitmap
import org.polyfrost.compose.ui.unit.dp
import org.polyfrost.oneconfig.api.notifications.v1.SvgMask
import org.polyfrost.oneconfig.internal.ui.poly.SvgImage

private object IconResourceMarker

internal object IconResource {
    private class Entry(val bytes: ByteArray?)

    private val cache = ConcurrentHashMap<String, Entry>()

    fun bytes(path: String): ByteArray? = cache.getOrPut(path) { Entry(load(path)) }.bytes

    fun exists(path: String): Boolean = bytes(path) != null

    fun fileBytes(file: File, stamp: Long): ByteArray? =
        cache.getOrPut("file:${file.path}@$stamp") {
            Entry(runCatching { file.readBytes() }.getOrNull())
        }.bytes

    fun warm(names: Collection<String>) {
        val pending = names.asSequence().map { it.toIconResourcePath() }
            .filter { !cache.containsKey(it) && warming.add(it) }.toSet()
        if (pending.isEmpty()) return
        Multithreading.submit {
            for (name in pending) {
                runCatching {
                    val file = File(name).takeIf { it.isAbsolute && it.isFile }
                    if (file != null) fileBytes(file, file.lastModified()) else bytes(name)
                }
            }
        }
    }

    private val warming = ConcurrentHashMap.newKeySet<String>()

    private fun load(path: String): ByteArray? {
        val normalized = path.removePrefix("/")
        val loader = Thread.currentThread().contextClassLoader ?: IconResourceMarker::class.java.classLoader
        val stream = loader?.getResourceAsStream(normalized)
            ?: IconResourceMarker::class.java.getResourceAsStream(path)
        return stream?.use { it.readBytes() }
    }
}

fun warmIconCache(names: Collection<String>) = IconResource.warm(names)

fun canRenderIcon(iconName: String): Boolean {
    if (iconName.contains('/') || iconName.contains('.')) {
        val file = File(iconName)
        if (file.isAbsolute) return file.isFile
    }
    return IconResource.exists(iconName.toIconResourcePath())
}

internal fun String.toIconResourcePath(): String =
    if (contains('/') || contains('.')) this else "/assets/oneconfig/ico/$this.svg"

private const val DefaultIconSize = 18f

internal fun Modifier.sizeDp(default: Float = 0f): Float {
    val probe = PolyLayoutStyle()
    applyTo(probe)
    return (probe.width as? PolySize.Px)?.value ?: (probe.height as? PolySize.Px)?.value ?: default
}

@Suppress("UNUSED_PARAMETER")
@Composable
fun Icon(
    iconName: String,
    color: Color = Color.Unspecified,
    modifier: Modifier = Modifier,
    fitAspectRatio: Boolean = false,
    rotation: Float = 0f,
) {
    val theme = LocalTheme.current
    IconImage(iconName, if (color.isUnspecified) theme.textColor else color, null, modifier, rotation)
}

@Suppress("UNUSED_PARAMETER")
@Composable
fun Icon(
    iconName: String,
    color: AnimatedColor,
    modifier: Modifier = Modifier,
    fitAspectRatio: Boolean = false,
    rotation: Float = 0f,
) = IconImage(iconName, color.target, color, modifier, rotation)

private class RasterIcon(val bitmap: ImageBitmap?)

private val rasterIcons = ConcurrentHashMap<String, RasterIcon>()

@Composable
private fun IconImage(iconName: String, color: Color, animated: AnimatedColor?, modifier: Modifier, rotation: Float) {
    val size = modifier.sizeDp(DefaultIconSize)
    val pxPerDp = LocalDensity.current.density
    val path = if (iconName.contains('/') || iconName.contains('.')) iconName
        else LocalTheme.current.iconOverrides[iconName] ?: iconName.toIconResourcePath()
    if (path.endsWith(".svg", ignoreCase = true) && !File(path).isFile) {
        val deg = rotation.roundToInt()
        if (animated != null) SvgImage(path, size, size, pxPerDp, modifier, tint = -1, rotation = deg, animatedTint = animated)
        else SvgImage(path, size, size, pxPerDp, modifier, tint = color.argb, rotation = deg)
        return
    }
    val bitmap = remember(path) {
        rasterIcons.getOrPut(path) {
            val bytes = File(path).takeIf { it.isAbsolute && it.isFile }?.readBytes() ?: IconResource.bytes(path)
            RasterIcon(bytes?.let { b ->
                if (path.endsWith(".svg", ignoreCase = true)) SvgMask.rasterize(String(b), (size * pxPerDp).roundToInt(), color.argb)
                else runCatching { ImageBitmap.decode(b) }.getOrNull()
            })
        }.bitmap
    }
    Box(modifier.size(size.dp)) {
        if (bitmap != null) Image(bitmap, null, Modifier.size(size.dp))
    }
}
