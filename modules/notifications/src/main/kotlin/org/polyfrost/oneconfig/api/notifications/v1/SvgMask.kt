package org.polyfrost.oneconfig.api.notifications.v1

import java.util.concurrent.ConcurrentHashMap
import org.jetbrains.annotations.ApiStatus
import org.polyfrost.compose.render.PolyBitmap

@ApiStatus.Internal
object SvgMask {
    private val cache = ConcurrentHashMap<String, Optional>()

    private class Optional(val bitmap: PolyBitmap?)

    fun get(resourcePath: String, pixelSize: Int, tint: Int? = null): PolyBitmap? {
        val size = pixelSize.coerceIn(1, 2048)
        return cache.getOrPut("$resourcePath@$size@$tint") {
            Optional(readResource(resourcePath)?.let { rasterize(String(it), size, tint) })
        }.bitmap
    }

    /** Renders [svg] so its longer side is [size] pixels; [tint] replaces every colour while keeping coverage */
    fun rasterize(svg: String, size: Int, tint: Int? = null): PolyBitmap? =
        runCatching { SvgCanvas.rasterize(svg, size, tint) }.getOrNull()

    fun parsePath(d: String): List<FloatArray> = SvgPath.parse(d).flatten(0.001f).map { it.pts }

    private fun readResource(path: String): ByteArray? {
        val loaders = listOf(SvgMask::class.java.classLoader, Thread.currentThread().contextClassLoader)
        for (loader in loaders) {
            val stream = loader?.getResourceAsStream(path.removePrefix("/")) ?: SvgMask::class.java.getResourceAsStream(path)
            if (stream != null) return stream.use { it.readBytes() }
        }
        return null
    }
}
