package org.polyfrost.oneconfig.internal.ui.compose

import net.minecraft.client.Minecraft
import net.minecraft.resources.Identifier
import net.minecraft.server.packs.resources.PreparableReloadListener
import net.minecraft.server.packs.resources.ReloadableResourceManager
import net.minecraft.server.packs.resources.ResourceManager
import net.minecraft.util.profiling.ProfilerFiller
import org.jetbrains.skia.*
import org.polyfrost.compose.mc.McFontQueue
import org.slf4j.LoggerFactory
import java.util.concurrent.CompletableFuture
import java.util.concurrent.Executor
import kotlin.jvm.optionals.getOrNull

object SkiaFontRenderer : PreparableReloadListener {
    private val LOGGER = LoggerFactory.getLogger("OneConfig/SkiaFontRenderer")
    private const val COLS = 16
    private const val ROWS = 16
    private const val GLYPH_HEIGHT = 8f
    const val LINE_HEIGHT = GLYPH_HEIGHT

    private val charWidths = FloatArray(256) { 6f }
    private var cellW = 8
    private var cellH = 8

    private var atlas: Image? = null
    @Volatile private var loaded = false
    // Throttle lazy reload retries so a draw happening mid-reload (packs not yet settled)
    // doesn't decode every frame; it heals once the resource manager settles.
    private var lastLoadAttempt = 0L
    private const val RETRY_INTERVAL_MS = 200L

    private val ASCII = Identifier.withDefaultNamespace("textures/font/ascii.png")

    private val paint = Paint()
    private val colorFilterCache = HashMap<Int, ColorFilter>(8)

    fun init() {
        McFontQueue.measureWidth = { text, scale -> measureWidth(text) * scale }
        McFontQueue.measureHeight = { scale -> cellH.toFloat() * scale }
        McFontQueue.renderer = ::draw
        val resourceManager = Minecraft.getInstance().resourceManager
        if (resourceManager is ReloadableResourceManager) {
            resourceManager.registerReloadListener(this)
        }
        ComposePreloader.preloadGpuWarmup()
    }

    fun measureWidth(text: String): Float {
        ensureLoaded()
        var w = 0f
        var isBold = false
        var i = 0
        while (i < text.length) {
            val ch = text[i]
            if (ch == '§' && i + 1 < text.length) {
                val code = text[i + 1].lowercaseChar()
                if (code == 'l') isBold = true
                else if (code == 'r' || (code in '0'..'9') || (code in 'a'..'f')) isBold = false
                i += 2
                continue
            }
            val width = charWidths[ch.code and 0xFF]
            w += if (isBold) width + 1f else width
            i++
        }
        return w
    }

    fun draw(canvas: Canvas, text: String, x: Float, y: Float, color: Int, shadow: Boolean, scale: Float) {
        ensureLoaded()
        val img = atlas ?: return
        text.lines().forEachIndexed { index, line ->
            if (shadow) drawGlyphs(canvas, img, line, x + scale, y + scale + index * cellH * scale, color, scale, true)
            drawGlyphs(canvas, img, line, x, y + index * cellH * scale, color, scale, false)
        }
    }

    private fun ensureLoaded() {
        if (loaded) return
        // Lazy self-heal: read from the *live* resource manager, which reflects the final
        // pack stack only after a reload has fully settled. Throttled so we don't thrash
        // if a draw lands mid-reload (truncated read / packs being swapped).
        val now = System.currentTimeMillis()
        if (now - lastLoadAttempt < RETRY_INTERVAL_MS) return
        lastLoadAttempt = now

        val rm = Minecraft.getInstance().resourceManager
        val bytes = runCatching {
            rm.getResource(ASCII).getOrNull()?.open()?.use { it.readBytes() }
        }.getOrNull()
        if (buildAtlas(bytes)) loaded = true
    }

    /**
     * Decode [bytes] into the glyph atlas and swap it in. Returns false (without touching the
     * current atlas) if the bytes are missing, incomplete, or undecodable — e.g. the resource
     * pack zip was closed mid-read while reloads were settling. Never throws.
     */
    private fun buildAtlas(bytes: ByteArray?): Boolean {
        // Reject empty/truncated reads up front so skia isn't handed garbage. A complete PNG
        // starts with the 8-byte signature 89 50 4E 47 0D 0A 1A 0A.
        if (bytes == null || bytes.size < 8 ||
            bytes[0] != 0x89.toByte() || bytes[1] != 0x50.toByte() ||
            bytes[2] != 0x4E.toByte() || bytes[3] != 0x47.toByte()
        ) return false

        val img = runCatching { Image.makeFromEncoded(bytes) }.getOrNull() ?: return false

        val cw = img.width / COLS
        val ch = img.height / ROWS
        val widths = FloatArray(256) { 6f }
        val bitmap = Bitmap()
        try {
            bitmap.allocPixels(ImageInfo.makeN32Premul(img.width, img.height))
            img.readPixels(bitmap)
            val pixmap = bitmap.peekPixels()
            if (pixmap != null) {
                for (c in 0 until 256) {
                    if (c == ' '.code) { widths[c] = 4f; continue }
                    val startX = (c % COLS) * cw
                    val startY = (c / COLS) * ch
                    var glyphW = 0
                    outer@ for (px in cw - 1 downTo 0)
                        for (py in 0 until ch)
                            if (pixmap.getAlphaF(startX + px, startY + py) > 0f) { glyphW = px + 1; break@outer }
                    widths[c] = (glyphW + 1).toFloat()
                }
            }
        } catch (t: Throwable) {
            LOGGER.warn("Failed to build SkiaFontRenderer atlas, keeping previous", t)
            bitmap.close()
            img.close()
            return false
        }
        bitmap.close()

        // Only mutate shared state once decode fully succeeded.
        atlas?.close()
        colorFilterCache.clear()
        atlas = img
        cellW = cw
        cellH = ch
        widths.copyInto(charWidths)
        return true
    }

    private fun drawGlyphs(canvas: Canvas, img: Image, text: String, x: Float, y: Float, color: Int, scale: Float, isShadow: Boolean) {
        var curX = x
        var curColor = color
        var isBold = false
        var isItalic = false
        
        var i = 0
        while (i < text.length) {
            val ch = text[i]
            if (ch == '§' && i + 1 < text.length) {
                val code = text[i + 1].lowercaseChar()
                when (code) {
                    'l' -> isBold = true
                    'o' -> isItalic = true
                    'r' -> { isBold = false; isItalic = false; curColor = color }
                    in '0'..'9', in 'a'..'f' -> {
                        isBold = false
                        isItalic = false
                    }
                }
                i += 2
                continue
            }
            
            val idx = ch.code and 0xFF
            val advance = charWidths[idx]
            val glyphW = (advance - 1f).coerceAtLeast(0f)
            
            if (glyphW > 0f) {
                val drawColor = if (isShadow) shadowColor(curColor) else curColor
                paint.colorFilter = colorFilterCache.getOrPut(drawColor) { 
                    ColorFilter.makeBlend(drawColor, BlendMode.MODULATE) 
                }

                val srcX = (idx % COLS) * cellW
                val srcY = (idx / COLS) * cellH
                val src = Rect.makeXYWH(srcX.toFloat(), srcY.toFloat(), glyphW, cellH.toFloat())
                
                canvas.save()
                if (isItalic) {
                    canvas.skew(-0.25f, 0f)
                }
                
                val dst = Rect.makeXYWH(curX, y, glyphW * scale, cellH * scale)
                canvas.drawImageRect(img, src, dst, SamplingMode.DEFAULT, paint, true)
                if (isBold) {
                    val dstBold = Rect.makeXYWH(curX + scale, y, glyphW * scale, cellH * scale)
                    canvas.drawImageRect(img, src, dstBold, SamplingMode.DEFAULT, paint, true)
                }
                canvas.restore()
            }
            
            curX += (if (isBold) advance + 1f else advance) * scale
            i++
        }
    }

    private fun shadowColor(color: Int): Int {
        val a = (color ushr 24) and 0xFF
        val r = ((color ushr 16) and 0xFF) / 4
        val g = ((color ushr 8) and 0xFF) / 4
        val b = (color and 0xFF) / 4
        return (a shl 24) or (r shl 16) or (g shl 8) or b
    }


    //? < 1.21.10 {
    /*override fun reload(
        preparationBarrier: PreparableReloadListener.PreparationBarrier?,
        resourceManager: ResourceManager,
        //? < 1.21.4 {
        /*profilerFiller: ProfilerFiller?,
        profilerFiller2: ProfilerFiller?,
        *///? }
        executor: Executor?,
        executor2: Executor?
    ): CompletableFuture<Void?> {
    *///? } else {
    override fun reload(
       sharedState: PreparableReloadListener.SharedState,
       executor: Executor,
       preparationBarrier: PreparableReloadListener.PreparationBarrier,
       executor2: Executor
    ): CompletableFuture<Void> {
    //? }
        //? >= 1.21.10
        val resourceManager = sharedState.resourceManager()

        return CompletableFuture.supplyAsync({
            // Only read the encoded bytes here (cheap, no skia). Decoding/atlas build happens in
            // the apply stage. Never throw out of the reload future — a failure here must not abort
            // the whole resource reload (which would make MC drop the server's resource pack).
            runCatching {
                resourceManager.getResource(ASCII).getOrNull()?.open()?.use { it.readBytes() }
            }.getOrNull()
        }, executor).thenCompose { bytes ->
            //? >= 1.21.10 {
            @Suppress("UNCHECKED_CAST")
            preparationBarrier.wait(bytes as Any) as CompletableFuture<ByteArray?>
            //? } else
            //preparationBarrier!!.wait(bytes)
        }.thenAcceptAsync({ bytes ->
            if (buildAtlas(bytes)) {
                loaded = true
            } else {
                // Read/decode failed during reload (pack not settled yet). Defer to the lazy
                // self-heal in ensureLoaded(), which re-reads once the pack stack is stable.
                loaded = false
                lastLoadAttempt = 0L
            }
        }, executor2)
    }
}
