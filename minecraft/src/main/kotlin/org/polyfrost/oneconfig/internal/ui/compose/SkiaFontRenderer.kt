package org.polyfrost.oneconfig.internal.ui.compose

import com.google.gson.JsonObject
import com.google.gson.JsonParser
import net.minecraft.client.Minecraft
import net.minecraft.resources.Identifier
import net.minecraft.server.packs.resources.PreparableReloadListener
import net.minecraft.server.packs.resources.ReloadableResourceManager
import net.minecraft.server.packs.resources.ResourceManager
//? < 1.21.4
//import net.minecraft.util.profiling.ProfilerFiller
import org.jetbrains.skia.*
import org.polyfrost.compose.mc.McFontQueue
import org.polyfrost.compose.render.FontManager
import org.slf4j.LoggerFactory
import java.util.concurrent.CompletableFuture
import java.util.concurrent.Executor
import kotlin.jvm.optionals.getOrNull

object SkiaFontRenderer : PreparableReloadListener {
    private val LOGGER = LoggerFactory.getLogger("OneConfig/SkiaFontRenderer")

    private const val BASELINE = 7
    private const val DEFAULT_BITMAP_HEIGHT = 8
    private const val LINE_HEIGHT = 8f
    private const val MISSING_ADVANCE = 6f

    const val UNIFONT_KEY = "unifont"
    private const val UNIFONT_SIZE = 8f
    private val unifontAdvances = HashMap<Int, Float>()

    private class Glyph(
        val image: Image,
        val srcX: Float,
        val srcY: Float,
        val srcW: Float,   // trimmed glyph width in source px (0 => nothing to draw)
        val srcH: Float,   // source cell height in px
        val scale: Float,  // provider height / cell height (source px -> target px)
        val top: Float,    // target-px offset from line top to glyph top (BASELINE - ascent)
        val advance: Float // target-px advance, trailing spacing included
    )

    @Volatile private var glyphs: Map<Int, Glyph> = emptyMap()
    @Volatile private var spaceAdvances: Map<Int, Float> = mapOf(' '.code to 4f)
    @Volatile private var atlases: List<Image> = emptyList()

    @Volatile private var loaded = false
    // Throttle lazy reload retries so a draw happening mid-reload (packs not yet settled)
    // doesn't re-read every frame; it heals once the resource manager settles.
    private var lastLoadAttempt = 0L
    private const val RETRY_INTERVAL_MS = 200L

    private const val ROOT_FONT = "minecraft:default"

    private val paint = Paint()
    private val textPaint = Paint().apply { isAntiAlias = false }
    private val colorFilterCache = HashMap<Int, ColorFilter>(64)

    fun init() {
        McFontQueue.measureWidth = { text, scale -> measureWidth(text) * scale }
        McFontQueue.measureHeight = { scale -> LINE_HEIGHT * scale }
        McFontQueue.renderer = ::draw
        val resourceManager = Minecraft.getInstance().resourceManager
        if (resourceManager is ReloadableResourceManager) {
            resourceManager.registerReloadListener(this)
        }
        ComposePreloader.preloadGpuWarmup()
    }

    private fun advanceOf(cp: Int): Float =
        glyphs[cp]?.advance ?: spaceAdvances[cp] ?: unifontAdvance(cp) ?: MISSING_ADVANCE

    private fun unifontAdvance(cp: Int): Float? {
        unifontAdvances[cp]?.let { return it }
        val font = FontManager.getFont(UNIFONT_SIZE, UNIFONT_KEY)
        if (font.getUTF32Glyph(cp).toInt() == 0) return null
        val advance = font.measureTextWidth(String(Character.toChars(cp)))
        unifontAdvances[cp] = advance
        return advance
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
            val cp = text.codePointAt(i)
            val advance = advanceOf(cp)
            w += if (isBold) advance + 1f else advance
            i += Character.charCount(cp)
        }
        return w
    }

    fun draw(canvas: Canvas, text: String, x: Float, y: Float, color: Int, shadow: Boolean, scale: Float) {
        ensureLoaded()
        if (glyphs.isEmpty()) return
        text.lines().forEachIndexed { index, line ->
            val lineY = y + index * LINE_HEIGHT * scale
            if (shadow) drawGlyphs(canvas, line, x + scale, lineY + scale, color, scale, true)
            drawGlyphs(canvas, line, x, lineY, color, scale, false)
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
        if (rebuild(Minecraft.getInstance().resourceManager)) loaded = true
    }

    private fun drawGlyphs(canvas: Canvas, text: String, x: Float, y: Float, color: Int, scale: Float, isShadow: Boolean) {
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

            val cp = text.codePointAt(i)
            i += Character.charCount(cp)

            val glyph = glyphs[cp]
            if (glyph == null) {
                if (spaceAdvances[cp] == null && unifontAdvance(cp) != null) {
                    val font = FontManager.getFont(UNIFONT_SIZE * scale, UNIFONT_KEY)
                    val drawColor = if (isShadow) shadowColor(curColor) else curColor
                    textPaint.color = drawColor
                    val str = String(Character.toChars(cp))
                    val baseline = y + BASELINE * scale
                    canvas.save()
                    if (isItalic) canvas.skew(-0.25f, 0f)
                    canvas.drawString(str, curX, baseline, font, textPaint)
                    if (isBold) canvas.drawString(str, curX + scale, baseline, font, textPaint)
                    canvas.restore()
                }
                curX += (if (isBold) advanceOf(cp) + 1f else advanceOf(cp)) * scale
                continue
            }

            if (glyph.srcW > 0f) {
                val drawColor = if (isShadow) shadowColor(curColor) else curColor
                paint.colorFilter = colorFilterCache.getOrPut(drawColor) {
                    ColorFilter.makeBlend(drawColor, BlendMode.MODULATE)
                }

                val src = Rect.makeXYWH(glyph.srcX, glyph.srcY, glyph.srcW, glyph.srcH)
                val dstW = glyph.srcW * glyph.scale * scale
                val dstH = glyph.srcH * glyph.scale * scale
                val dstY = y + glyph.top * scale

                canvas.save()
                if (isItalic) canvas.skew(-0.25f, 0f)
                canvas.drawImageRect(glyph.image, src, Rect.makeXYWH(curX, dstY, dstW, dstH), SamplingMode.DEFAULT, paint, true)
                if (isBold) {
                    canvas.drawImageRect(glyph.image, src, Rect.makeXYWH(curX + scale, dstY, dstW, dstH), SamplingMode.DEFAULT, paint, true)
                }
                canvas.restore()
            }

            curX += (if (isBold) glyph.advance + 1f else glyph.advance) * scale
        }
    }

    private fun shadowColor(color: Int): Int {
        val a = (color ushr 24) and 0xFF
        val r = ((color ushr 16) and 0xFF) / 4
        val g = ((color ushr 8) and 0xFF) / 4
        val b = (color and 0xFF) / 4
        return (a shl 24) or (r shl 16) or (g shl 8) or b
    }


    private fun read(rm: ResourceManager, id: Identifier): ByteArray? =
        runCatching { rm.getResource(id).getOrNull()?.open()?.use { it.readBytes() } }.getOrNull()

    private fun readStack(rm: ResourceManager, id: Identifier): List<ByteArray> =
        runCatching {
            rm.getResourceStack(id).asReversed().mapNotNull { res ->
                runCatching { res.open().use { it.readBytes() } }.getOrNull()
            }
        }.getOrNull() ?: emptyList()

    private fun splitId(id: String): Pair<String, String> =
        if (':' in id) id.substringBefore(':') to id.substringAfter(':') else "minecraft" to id

    private fun collectProviders(rm: ResourceManager, id: String, out: MutableList<JsonObject>, visited: MutableSet<String>) {
        if (!visited.add(id)) return
        val (ns, path) = splitId(id)
        for (bytes in readStack(rm, Identifier.fromNamespaceAndPath(ns, "font/$path.json"))) {
            val root = runCatching { JsonParser.parseString(String(bytes, Charsets.UTF_8)).asJsonObject }.getOrNull() ?: continue
            val providers = root.getAsJsonArray("providers") ?: continue
            for (element in providers) {
                val provider = element.asJsonObject
                when (provider.get("type")?.asString) {
                    "reference" -> provider.get("id")?.asString?.let { collectProviders(rm, it, out, visited) }
                    else -> out.add(provider)
                }
            }
        }
    }

    private fun rebuild(rm: ResourceManager): Boolean {
        val providers = ArrayList<JsonObject>()
        runCatching { collectProviders(rm, ROOT_FONT, providers, HashSet()) }
        if (providers.isEmpty()) return false

        val newGlyphs = HashMap<Int, Glyph>(4096)
        val newSpace = HashMap<Int, Float>(4)
        val newAtlases = ArrayList<Image>()
        val claimed = HashSet<Int>(4096)

        for (provider in providers) {
            when (provider.get("type")?.asString) {
                "space" -> {
                    val advances = provider.getAsJsonObject("advances") ?: continue
                    for ((key, value) in advances.entrySet()) {
                        if (key.isEmpty()) continue
                        val cp = key.codePointAt(0)
                        if (claimed.add(cp)) newSpace[cp] = value.asFloat
                    }
                }
                "bitmap" -> runCatching { loadBitmap(rm, provider, newGlyphs, newAtlases, claimed) }
                    .onFailure { LOGGER.warn("Failed to load bitmap font provider, skipping", it) }
                else -> {}
            }
        }

        if (newGlyphs.isEmpty()) return false

        val old = atlases
        glyphs = newGlyphs
        spaceAdvances = if (newSpace.isEmpty()) mapOf(' '.code to 4f) else newSpace
        atlases = newAtlases
        colorFilterCache.clear()
        old.forEach { runCatching { it.close() } }
        return true
    }

    private fun loadBitmap(rm: ResourceManager, provider: JsonObject, out: MutableMap<Int, Glyph>, atlasesOut: MutableList<Image>, claimed: MutableSet<Int>) {
        val fileRef = provider.get("file")?.asString ?: return
        val (ns, filePath) = splitId(fileRef)
        val bytes = read(rm, Identifier.fromNamespaceAndPath(ns, "textures/$filePath")) ?: return
        if (!isPng(bytes)) return
        val image = Image.makeFromEncoded(bytes)

        val charsArray = provider.getAsJsonArray("chars") ?: run { image.close(); return }
        val rows = charsArray.size()
        if (rows == 0) { image.close(); return }
        val gridRows = Array(rows) { charsArray[it].asString.codePoints().toArray() }
        val cols = gridRows[0].size
        if (cols == 0) { image.close(); return }

        val cellW = image.width / cols
        val cellH = image.height / rows
        if (cellW == 0 || cellH == 0) { image.close(); return }

        val ascent = provider.get("ascent")?.asInt ?: BASELINE
        val height = provider.get("height")?.asInt ?: DEFAULT_BITMAP_HEIGHT
        val scale = height.toFloat() / cellH
        val top = (BASELINE - ascent).toFloat()

        val bitmap = Bitmap()
        val pixmap = try {
            bitmap.allocPixels(ImageInfo.makeN32Premul(image.width, image.height))
            image.readPixels(bitmap)
            bitmap.peekPixels()
        } catch (t: Throwable) {
            bitmap.close(); image.close()
            throw t
        }

        var used = false
        for (r in 0 until rows) {
            val rowCps = gridRows[r]
            for (c in rowCps.indices) {
                val cp = rowCps[c]
                if (cp == 0) continue          //   = empty cell
                if (!claimed.add(cp)) continue

                val sx = c * cellW
                val sy = r * cellH
                var glyphW = 0
                if (pixmap != null) {
                    outer@ for (px in cellW - 1 downTo 0)
                        for (py in 0 until cellH)
                            if (pixmap.getAlphaF(sx + px, sy + py) > 0f) { glyphW = px + 1; break@outer }
                }
                val advance = Math.round(glyphW * scale) + 1f
                out[cp] = Glyph(image, sx.toFloat(), sy.toFloat(), glyphW.toFloat(), cellH.toFloat(), scale, top, advance)
                used = true
            }
        }
        bitmap.close()
        if (used) atlasesOut.add(image) else image.close()
    }

    private fun isPng(bytes: ByteArray?): Boolean =
        bytes != null && bytes.size >= 8 &&
            bytes[0] == 0x89.toByte() && bytes[1] == 0x50.toByte() &&
            bytes[2] == 0x4E.toByte() && bytes[3] == 0x47.toByte()

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

        return CompletableFuture.supplyAsync({ }, executor)
            .thenCompose {
                //? >= 1.21.10 {
                preparationBarrier.wait(Unit)
                //? } else
                //preparationBarrier!!.wait(Unit)
            }.thenAcceptAsync({
                if (rebuild(resourceManager)) {
                    loaded = true
                } else {
                    loaded = false
                    lastLoadAttempt = 0L
                }
            }, executor2)
    }
}
