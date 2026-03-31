package org.polyfrost.oneconfig.internal.ui.compose

import net.minecraft.client.Minecraft
import net.minecraft.resources.ResourceLocation
import net.minecraft.server.packs.resources.PreparableReloadListener
import net.minecraft.server.packs.resources.ReloadableResourceManager
import net.minecraft.server.packs.resources.ResourceManager
import net.minecraft.util.profiling.ProfilerFiller
import org.jetbrains.skia.*
import org.polyfrost.compose.mc.McFontQueue
import java.util.concurrent.CompletableFuture
import java.util.concurrent.Executor
import kotlin.jvm.optionals.getOrNull

object SkiaFontRenderer : PreparableReloadListener {
    private const val COLS = 16
    private const val ROWS = 16
    private const val GLYPH_HEIGHT = 8f
    const val LINE_HEIGHT = GLYPH_HEIGHT

    private val charWidths = FloatArray(256) { 6f }
    private var cellW = 8
    private var cellH = 8

    private var atlas: Image? = null
    private var loaded = false

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
        if (shadow) drawGlyphs(canvas, img, text, x + scale, y + scale, color, scale, true)
        drawGlyphs(canvas, img, text, x, y, color, scale, false)
    }

    private fun ensureLoaded() {
        if (loaded) return
        loaded = true
        runCatching { loadAtlas() }
    }

    private fun loadAtlas() {
        val rm = Minecraft.getInstance().resourceManager
        val bytes = rm.getResource(ResourceLocation.withDefaultNamespace("textures/font/ascii.png"))
            .getOrNull()?.open()?.readBytes() ?: return
        val img = Image.makeFromEncoded(bytes) ?: return
        atlas = img
        cellW = img.width / COLS
        cellH = img.height / ROWS
        computeWidths(img)
    }

    private fun computeWidths(image: Image) {
        val bitmap = Bitmap()
        bitmap.allocPixels(ImageInfo.makeN32Premul(image.width, image.height))
        image.readPixels(bitmap)
        val pixmap = bitmap.peekPixels() ?: return

        for (c in 0 until 256) {
            if (c == ' '.code) { charWidths[c] = 4f; continue }
            val startX = (c % COLS) * cellW
            val startY = (c / COLS) * cellH
            var glyphW = 0
            outer@ for (px in cellW - 1 downTo 0) {
                for (py in 0 until cellH) {
                    if (pixmap.getAlphaF(startX + px, startY + py) > 0f) {
                        glyphW = px + 1
                        break@outer
                    }
                }
            }
            charWidths[c] = (glyphW + 1).toFloat()
        }

        bitmap.close()
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

    //#if MC < 1.21.10
    override fun reload(
        preparationBarrier: PreparableReloadListener.PreparationBarrier?,
        resourceManager: ResourceManager?,
        //#if MC < 1.21.4
        profilerFiller: ProfilerFiller?,
        profilerFiller2: ProfilerFiller?,
        //#endif
        executor: Executor?,
        executor2: Executor?
    ): CompletableFuture<Void?> {
    //#else
    //$$ override fun reload(
    //$$    sharedState: PreparableReloadListener.SharedState,
    //$$    executor: Executor,
    //$$    preparationBarrier: PreparableReloadListener.PreparationBarrier,
    //$$    executor2: Executor
    //$$ ): CompletableFuture<Void> {
    //#endif
        //#if MC >= 1.21.10
        //$$ val resourceManager = sharedState.resourceManager()
        //#endif

        data class Prepared(val img: Image, val cw: Int, val ch: Int, val widths: FloatArray) {
            override fun equals(other: Any?): Boolean {
                if (this === other) return true
                if (javaClass != other?.javaClass) return false

                other as Prepared

                if (cw != other.cw) return false
                if (ch != other.ch) return false
                if (img != other.img) return false
                if (!widths.contentEquals(other.widths)) return false

                return true
            }

            override fun hashCode(): Int {
                var result = cw
                result = 31 * result + ch
                result = 31 * result + img.hashCode()
                result = 31 * result + widths.contentHashCode()
                return result
            }
        }
        return CompletableFuture.supplyAsync({
            val bytes = resourceManager
                ?.getResource(ResourceLocation.withDefaultNamespace("textures/font/ascii.png"))
                ?.getOrNull()?.open()?.readBytes() ?: return@supplyAsync null
            val img = Image.makeFromEncoded(bytes) ?: return@supplyAsync null
            val cw = img.width / COLS
            val ch = img.height / ROWS
            val widths = FloatArray(256) { 6f }
            val bitmap = Bitmap()
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
            bitmap.close()
            Prepared(img, cw, ch, widths)
        }, executor).thenCompose { prepared ->
            //#if MC >= 1.21.10
            //$$ @Suppress("UNCHECKED_CAST")
            //$$ preparationBarrier.wait(prepared as Any) as CompletableFuture<Prepared?>
            //#else
            preparationBarrier!!.wait(prepared)
            //#endif
        }.thenAcceptAsync({ prepared ->
            atlas?.close()
            colorFilterCache.clear()
            if (prepared != null) {
                atlas = prepared.img
                cellW = prepared.cw
                cellH = prepared.ch
                prepared.widths.copyInto(charWidths)
            }
            loaded = atlas != null
        }, executor2)
    }
}
