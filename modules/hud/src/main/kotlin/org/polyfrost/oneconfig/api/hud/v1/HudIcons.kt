@file:OptIn(ExperimentalOneConfigUiApi::class)

package org.polyfrost.oneconfig.api.hud.v1

import androidx.compose.runtime.Composable
import java.awt.RenderingHints
import java.awt.image.BufferedImage
import java.io.ByteArrayInputStream
import java.util.concurrent.ConcurrentHashMap
import javax.imageio.ImageIO
import kotlin.math.max
import kotlin.math.roundToInt
import org.polyfrost.compose.render.PolyBitmap
import org.polyfrost.oneconfig.api.notifications.v1.SvgMask
import org.polyfrost.oneconfig.api.ui.v1.ExperimentalOneConfigUiApi
import org.polyfrost.oneconfig.api.ui.v1.theme.OneConfigTheme
import org.polyfrost.oneconfig.api.ui.v1.theme.ThemeStyle

@ExperimentalOneConfigUiApi
public object HudIcons {
    private const val ICON_ROOT = "/assets/oneconfig/ico/"
    private const val PIXEL_ROOT = "/assets/oneconfig/ico/minecraft/"
    private const val MAX_SIZE = 2048
    private const val WHITE = -1

    private class Entry(val bitmap: PolyBitmap?)

    private val cache = ConcurrentHashMap<String, Entry>()

    @Composable
    public fun bitmap(name: String, size: Int, pixelStyle: Boolean = OneConfigTheme.current.style == ThemeStyle.Pixel): PolyBitmap? {
        val px = size.coerceIn(1, MAX_SIZE)
        return cache.getOrPut("$name@$px@$pixelStyle") { Entry(load(resolve(name, pixelStyle), px)) }.bitmap
    }

    private fun resolve(name: String, pixelStyle: Boolean): String {
        if ('/' in name || '.' in name) return name
        if (pixelStyle) {
            val pixel = "$PIXEL_ROOT$name.svg"
            if (HudIcons::class.java.getResource(pixel) != null) return pixel
        }
        return "$ICON_ROOT$name.svg"
    }

    private fun load(path: String, size: Int): PolyBitmap? {
        if (path.endsWith(".svg", ignoreCase = true)) {
            return runCatching { SvgMask.get(path, size, WHITE) }.getOrNull()
        }
        val bytes = readResource(path) ?: return null
        return runCatching { ImageIO.read(ByteArrayInputStream(bytes))?.let { PolyBitmap.fromAwt(scale(it, size)) } }.getOrNull()
    }

    private fun readResource(path: String): ByteArray? {
        val relative = path.removePrefix("/")
        val loaders = listOf(HudIcons::class.java.classLoader, Thread.currentThread().contextClassLoader)
        for (loader in loaders) {
            val stream = loader?.getResourceAsStream(relative) ?: continue
            return stream.use { it.readBytes() }
        }
        return HudIcons::class.java.getResourceAsStream(path)?.use { it.readBytes() }
    }

    private fun scale(image: BufferedImage, size: Int): BufferedImage {
        val longest = max(image.width, image.height)
        if (longest == size) return image
        val ratio = size.toFloat() / longest
        val w = (image.width * ratio).roundToInt().coerceAtLeast(1)
        val h = (image.height * ratio).roundToInt().coerceAtLeast(1)
        val out = BufferedImage(w, h, BufferedImage.TYPE_INT_ARGB)
        val g = out.createGraphics()
        try {
            g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR)
            g.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY)
            g.drawImage(image, 0, 0, w, h, null)
        } finally {
            g.dispose()
        }
        return out
    }
}
