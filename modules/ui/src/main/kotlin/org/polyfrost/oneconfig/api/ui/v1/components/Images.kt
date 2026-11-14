@file:OptIn(ExperimentalOneConfigUiApi::class)

package org.polyfrost.oneconfig.api.ui.v1.components

import androidx.compose.runtime.Composable
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.ImageBitmap
import java.awt.RenderingHints
import java.awt.image.BufferedImage
import java.nio.file.Files
import java.nio.file.Path
import java.util.concurrent.ConcurrentHashMap
import javax.imageio.ImageIO
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.delay
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit
import kotlinx.coroutines.withContext
import org.polyfrost.compose.backend.asComposeImageBitmap
import org.polyfrost.compose.render.PolyBitmap
import org.polyfrost.oneconfig.api.ui.v1.ExperimentalOneConfigUiApi

@ExperimentalOneConfigUiApi
public fun PolyBitmap.Companion.fromFile(path: Path, maxBytes: Long = 16L shl 20): PolyBitmap? = runCatching {
    if (!Files.isRegularFile(path) || Files.size(path) > maxBytes) return null
    decode(Files.readAllBytes(path))
}.getOrNull()

@ExperimentalOneConfigUiApi
@Composable
public fun rememberImageBitmap(path: Path, maxWidth: Int): ImageBitmap? {
    val file = remember(path) { path.toAbsolutePath().normalize() }
    val width = maxWidth.coerceAtLeast(1)
    return produceState(KitImageCache.peek(file, width), file, width) {
        var loadedAt: Long? = null
        while (true) {
            val modified = withContext(Dispatchers.IO) { modifiedTime(file) }
            if (modified == null) {
                value = null
                loadedAt = null
            } else if (modified != loadedAt) {
                value = KitImageCache.load(KitImageCacheKey(file, width, modified))
                loadedAt = modified
            }
            delay(CHANGE_CHECK_MILLIS)
        }
    }.value
}

private const val CHANGE_CHECK_MILLIS = 3000L

private fun modifiedTime(path: Path): Long? =
    runCatching { if (Files.isRegularFile(path)) Files.getLastModifiedTime(path).toMillis() else null }.getOrNull()

private data class KitImageCacheKey(val path: Path, val maxWidth: Int, val modified: Long)

private class KitImageCacheEntry(val bitmap: ImageBitmap, val bytes: Long)

private object KitImageCache {
    private const val MAX_BYTES = 128L shl 20
    private const val MAX_FILE_BYTES = 256L shl 20

    private val decoders = Semaphore(2)
    private val entries = LinkedHashMap<KitImageCacheKey, KitImageCacheEntry>(64, 0.75f, true)
    private var bytes = 0L
    private val latest = ConcurrentHashMap<Pair<Path, Int>, KitImageCacheKey>()
    private val failed = ConcurrentHashMap.newKeySet<KitImageCacheKey>()
    private val inFlight = ConcurrentHashMap<KitImageCacheKey, CompletableDeferred<ImageBitmap?>>()

    fun peek(path: Path, maxWidth: Int): ImageBitmap? {
        val key = latest[path to maxWidth] ?: return null
        return synchronized(entries) { entries[key]?.bitmap }
    }

    suspend fun load(key: KitImageCacheKey): ImageBitmap? {
        while (true) {
            synchronized(entries) { entries[key] }?.let { return it.bitmap }
            if (key in failed) return null
            val mine = CompletableDeferred<ImageBitmap?>()
            val existing = inFlight.putIfAbsent(key, mine)
            if (existing != null) {
                try {
                    return existing.await()
                } catch (e: CancellationException) {
                    currentCoroutineContext().ensureActive()
                    continue
                }
            }
            try {
                val bitmap = decoders.withPermit { withContext(Dispatchers.IO) { decodeScaled(key.path, key.maxWidth) } }
                if (bitmap == null) {
                    failed += key
                    mine.complete(null)
                    return null
                }
                val image = bitmap.asComposeImageBitmap()
                put(key, KitImageCacheEntry(image, bitmap.width.toLong() * bitmap.height * 4))
                mine.complete(image)
                return image
            } catch (t: Throwable) {
                mine.completeExceptionally(t)
                throw t
            } finally {
                inFlight.remove(key, mine)
            }
        }
    }

    private fun put(key: KitImageCacheKey, entry: KitImageCacheEntry) = synchronized(entries) {
        entries.put(key, entry)?.let { bytes -= it.bytes }
        bytes += entry.bytes
        latest[key.path to key.maxWidth] = key
        val iterator = entries.entries.iterator()
        while (bytes > MAX_BYTES && entries.size > 1 && iterator.hasNext()) {
            val (eldestKey, eldest) = iterator.next()
            if (eldest === entry) continue
            bytes -= eldest.bytes
            iterator.remove()
            latest.remove(eldestKey.path to eldestKey.maxWidth, eldestKey)
        }
    }

    private fun decodeScaled(path: Path, maxWidth: Int): PolyBitmap? =
        decodeSubsampled(path, maxWidth) ?: PolyBitmap.fromFile(path, MAX_FILE_BYTES)?.let { scaleDown(it, maxWidth) }

    private fun decodeSubsampled(path: Path, maxWidth: Int): PolyBitmap? = runCatching {
        ImageIO.createImageInputStream(path.toFile())?.use { stream ->
            val reader = ImageIO.getImageReaders(stream).asSequence().firstOrNull() ?: return@use null
            try {
                reader.input = stream
                val param = reader.defaultReadParam
                val step = reader.getWidth(0) / maxWidth
                if (step > 1) param.setSourceSubsampling(step, step, 0, 0)
                PolyBitmap.fromAwt(scaleDown(reader.read(0, param), maxWidth))
            } finally {
                reader.dispose()
            }
        }
    }.getOrNull()

    private fun scaleDown(bitmap: PolyBitmap, maxWidth: Int): PolyBitmap {
        if (bitmap.width <= maxWidth) return bitmap
        val awt = BufferedImage(bitmap.width, bitmap.height, BufferedImage.TYPE_INT_ARGB)
        val px = bitmap.pixels
        val argb = IntArray(bitmap.width * bitmap.height) { i ->
            val o = i * 4
            (px[o + 3].toInt() and 0xFF shl 24) or (px[o].toInt() and 0xFF shl 16) or (px[o + 1].toInt() and 0xFF shl 8) or (px[o + 2].toInt() and 0xFF)
        }
        awt.setRGB(0, 0, bitmap.width, bitmap.height, argb, 0, bitmap.width)
        return PolyBitmap.fromAwt(scaleDown(awt, maxWidth))
    }

    private fun scaleDown(image: BufferedImage, maxWidth: Int): BufferedImage {
        if (image.width <= maxWidth) return image
        val height = (image.height * (maxWidth.toDouble() / image.width)).toInt().coerceAtLeast(1)
        val out = BufferedImage(maxWidth, height, BufferedImage.TYPE_INT_ARGB)
        val g = out.createGraphics()
        try {
            g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR)
            g.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY)
            g.drawImage(image, 0, 0, maxWidth, height, null)
        } finally {
            g.dispose()
        }
        return out
    }
}
