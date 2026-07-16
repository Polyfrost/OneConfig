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
import java.io.ByteArrayInputStream
import java.util.concurrent.CompletableFuture
import java.util.concurrent.Executor
import java.util.zip.ZipInputStream
import kotlin.jvm.optionals.getOrNull

object SkiaFontRenderer : PreparableReloadListener {
    private val LOGGER = LoggerFactory.getLogger("OneConfig/SkiaFontRenderer")

    private const val BASELINE = 7
    private const val DEFAULT_BITMAP_HEIGHT = 8
    private const val LINE_HEIGHT = 8f
    private const val MISSING_ADVANCE = 6f

    private const val DEFAULT_OFFSET = 1f

    private const val UNIHEX_HEIGHT = 16
    private const val UNIHEX_OVERSAMPLE = 2f
    private const val UNIHEX_OFFSET = 0.5f

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

    private class UnihexGlyphs(val cps: IntArray, val lines: IntArray, val bounds: ShortArray) {
        fun indexOf(cp: Int): Int = cps.binarySearch(cp)
        fun left(i: Int): Int = (bounds[i].toInt() shr 8).toByte().toInt()
        fun right(i: Int): Int = bounds[i].toInt().toByte().toInt()
        fun width(i: Int): Int = right(i) - left(i) + 1
        fun advance(i: Int): Float = (width(i) / 2 + 1).toFloat()
    }

    @Volatile private var glyphs: Map<Int, Glyph> = emptyMap()
    @Volatile private var spaceAdvances: Map<Int, Float> = mapOf(' '.code to 4f)
    @Volatile private var atlases: List<Image> = emptyList()
    @Volatile private var unihex: UnihexGlyphs? = null
    @Volatile private var unihexImages = HashMap<Int, Image>()

    @Volatile private var loaded = false
    @Volatile private var builtOptions = -1
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

    private fun advanceOf(cp: Int, bold: Boolean): Float {
        glyphs[cp]?.let { return it.advance + if (bold) DEFAULT_OFFSET else 0f }
        spaceAdvances[cp]?.let { return it + if (bold) DEFAULT_OFFSET else 0f }
        val hex = unihex
        if (hex != null) {
            val i = hex.indexOf(cp)
            if (i >= 0) return hex.advance(i) + if (bold) UNIHEX_OFFSET else 0f
        }
        val fallback = unifontAdvance(cp) ?: MISSING_ADVANCE
        return fallback + if (bold) DEFAULT_OFFSET else 0f
    }

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
            w += advanceOf(cp, isBold)
            i += Character.charCount(cp)
        }
        return w
    }

    fun draw(canvas: Canvas, text: String, x: Float, y: Float, color: Int, shadow: Boolean, scale: Float) {
        ensureLoaded()
        if (glyphs.isEmpty() && unihex == null) return
        text.lines().forEachIndexed { index, line ->
            val lineY = y + index * LINE_HEIGHT * scale
            if (shadow) drawGlyphs(canvas, line, x, lineY, color, scale, true)
            drawGlyphs(canvas, line, x, lineY, color, scale, false)
        }
    }

    private fun ensureLoaded() {
        if (loaded && builtOptions == fontOptionsMask()) return
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

            val drawColor = if (isShadow) shadowColor(curColor) else curColor

            val glyph = glyphs[cp]
            if (glyph != null) {
                val shift = if (isShadow) DEFAULT_OFFSET * scale else 0f
                if (glyph.srcW > 0f) {
                    val src = Rect.makeXYWH(glyph.srcX, glyph.srcY, glyph.srcW, glyph.srcH)
                    val dstW = glyph.srcW * glyph.scale * scale
                    val dstH = glyph.srcH * glyph.scale * scale
                    val dstY = y + glyph.top * scale + shift
                    blit(canvas, glyph.image, src, curX + shift, dstY, dstW, dstH, drawColor, isBold, isItalic, DEFAULT_OFFSET * scale)
                }
                curX += (glyph.advance + if (isBold) DEFAULT_OFFSET else 0f) * scale
                continue
            }

            if (spaceAdvances[cp] == null) {
                val hex = unihex
                val hexIndex = hex?.indexOf(cp) ?: -1
                if (hex != null && hexIndex >= 0) {
                    val shift = if (isShadow) UNIHEX_OFFSET * scale else 0f
                    val image = unihexImage(hex, hexIndex, cp)
                    if (image != null) {
                        val w = hex.width(hexIndex)
                        val src = Rect.makeXYWH(0f, 0f, w.toFloat(), UNIHEX_HEIGHT.toFloat())
                        val dstW = w / UNIHEX_OVERSAMPLE * scale
                        val dstH = UNIHEX_HEIGHT / UNIHEX_OVERSAMPLE * scale
                        blit(canvas, image, src, curX + shift, y + shift, dstW, dstH, drawColor, isBold, isItalic, UNIHEX_OFFSET * scale)
                    }
                    curX += (hex.advance(hexIndex) + if (isBold) UNIHEX_OFFSET else 0f) * scale
                    continue
                }
                if (unifontAdvance(cp) != null) {
                    val shift = if (isShadow) DEFAULT_OFFSET * scale else 0f
                    val font = FontManager.getFont(UNIFONT_SIZE * scale, UNIFONT_KEY)
                    textPaint.color = drawColor
                    val str = String(Character.toChars(cp))
                    val baseline = y + BASELINE * scale + shift
                    canvas.save()
                    if (isItalic) canvas.skew(-0.25f, 0f)
                    canvas.drawString(str, curX + shift, baseline, font, textPaint)
                    if (isBold) canvas.drawString(str, curX + shift + scale, baseline, font, textPaint)
                    canvas.restore()
                }
            }
            curX += advanceOf(cp, isBold) * scale
        }
    }

    private fun blit(
        canvas: Canvas, image: Image, src: Rect,
        x: Float, y: Float, w: Float, h: Float,
        color: Int, bold: Boolean, italic: Boolean, boldOffset: Float
    ) {
        paint.colorFilter = colorFilterCache.getOrPut(color) { ColorFilter.makeBlend(color, BlendMode.MODULATE) }
        canvas.save()
        if (italic) canvas.skew(-0.25f, 0f)
        canvas.drawImageRect(image, src, Rect.makeXYWH(x, y, w, h), SamplingMode.DEFAULT, paint, true)
        if (bold) {
            canvas.drawImageRect(image, src, Rect.makeXYWH(x + boldOffset, y, w, h), SamplingMode.DEFAULT, paint, true)
        }
        canvas.restore()
    }

    private fun unihexImage(hex: UnihexGlyphs, index: Int, cp: Int): Image? {
        val cache = unihexImages
        cache[cp]?.let { return it }
        val left = hex.left(index)
        val right = hex.right(index)
        val w = right - left + 1
        if (w <= 0) return null

        val pixels = ByteArray(w * UNIHEX_HEIGHT * 4)
        var p = 0
        for (row in 0 until UNIHEX_HEIGHT) {
            val bits = hex.lines[index * UNIHEX_HEIGHT + row]
            for (bit in left..right) {
                val on = bit in 0..31 && (bits ushr (31 - bit)) and 1 != 0
                val value = if (on) 0xFF.toByte() else 0
                pixels[p++] = value
                pixels[p++] = value
                pixels[p++] = value
                pixels[p++] = value
            }
        }
        val image = Image.makeRaster(ImageInfo.makeN32Premul(w, UNIHEX_HEIGHT), pixels, w * 4)
        cache[cp] = image
        return image
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

    private const val OPTION_UNIFORM = 1
    private const val OPTION_JP = 2

    private fun fontOptionsMask(): Int {
        val options = Minecraft.getInstance().options
        return (if (options.forceUnicodeFont().get()) OPTION_UNIFORM else 0) or
            (if (options.japaneseGlyphVariants().get()) OPTION_JP else 0)
    }

    private fun filterPasses(provider: JsonObject, options: Int): Boolean {
        val filter = provider.getAsJsonObject("filter") ?: return true
        filter.get("uniform")?.let { if (it.asBoolean != (options and OPTION_UNIFORM != 0)) return false }
        filter.get("jp")?.let { if (it.asBoolean != (options and OPTION_JP != 0)) return false }
        return true
    }

    private fun collectProviders(rm: ResourceManager, id: String, out: MutableList<JsonObject>, visited: MutableSet<String>, options: Int) {
        if (!visited.add(id)) return
        val (ns, path) = splitId(id)
        for (bytes in readStack(rm, Identifier.fromNamespaceAndPath(ns, "font/$path.json"))) {
            val root = runCatching { JsonParser.parseString(String(bytes, Charsets.UTF_8)).asJsonObject }.getOrNull() ?: continue
            val providers = root.getAsJsonArray("providers") ?: continue
            for (element in providers) {
                val provider = element.asJsonObject
                if (!filterPasses(provider, options)) continue
                when (provider.get("type")?.asString) {
                    "reference" -> provider.get("id")?.asString?.let { collectProviders(rm, it, out, visited, options) }
                    else -> out.add(provider)
                }
            }
        }
    }

    private fun rebuild(rm: ResourceManager): Boolean {
        val options = fontOptionsMask()
        val providers = ArrayList<JsonObject>()
        runCatching { collectProviders(rm, ROOT_FONT, providers, HashSet(), options) }
        if (providers.isEmpty()) return false

        val newGlyphs = HashMap<Int, Glyph>(4096)
        val newSpace = HashMap<Int, Float>(4)
        val newAtlases = ArrayList<Image>()
        val claimed = HashSet<Int>(4096)
        val hex = HexAccumulator()

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
                "unihex" -> runCatching { loadUnihex(rm, provider, hex, claimed) }
                    .onFailure { LOGGER.warn("Failed to load unihex font provider, skipping", it) }
                else -> {}
            }
        }

        if (newGlyphs.isEmpty() && hex.size == 0) return false

        val oldAtlases = atlases
        val oldImages = unihexImages
        glyphs = newGlyphs
        spaceAdvances = if (newSpace.isEmpty()) mapOf(' '.code to 4f) else newSpace
        atlases = newAtlases
        unihex = hex.build()
        unihexImages = HashMap()
        colorFilterCache.clear()
        builtOptions = options
        oldAtlases.forEach { runCatching { it.close() } }
        oldImages.values.forEach { runCatching { it.close() } }
        return true
    }

    private class HexAccumulator {
        var cps = IntArray(1 shl 12)
        var lines = IntArray((1 shl 12) * UNIHEX_HEIGHT)
        var bounds = ShortArray(1 shl 12)
        var size = 0

        fun add(cp: Int, rows: IntArray, bitWidth: Int) {
            if (size == cps.size) {
                cps = cps.copyOf(size * 2)
                lines = lines.copyOf(size * 2 * UNIHEX_HEIGHT)
                bounds = bounds.copyOf(size * 2)
            }
            cps[size] = cp
            System.arraycopy(rows, 0, lines, size * UNIHEX_HEIGHT, UNIHEX_HEIGHT)
            bounds[size] = packBounds(rows, bitWidth)
            size++
        }

        fun build(): UnihexGlyphs? {
            if (size == 0) return null
            val keys = LongArray(size) { (cps[it].toLong() shl 32) or it.toLong() }
            keys.sort()

            var distinct = 0
            for (i in 0 until size) if (i == size - 1 || keys[i] ushr 32 != keys[i + 1] ushr 32) distinct++

            val outCps = IntArray(distinct)
            val outLines = IntArray(distinct * UNIHEX_HEIGHT)
            val outBounds = ShortArray(distinct)
            var out = 0
            for (i in 0 until size) {
                if (i != size - 1 && keys[i] ushr 32 == keys[i + 1] ushr 32) continue
                val src = (keys[i] and 0xFFFFFFFFL).toInt()
                outCps[out] = (keys[i] ushr 32).toInt()
                outBounds[out] = bounds[src]
                System.arraycopy(lines, src * UNIHEX_HEIGHT, outLines, out * UNIHEX_HEIGHT, UNIHEX_HEIGHT)
                out++
            }
            return UnihexGlyphs(outCps, outLines, outBounds)
        }
    }

    private fun packBounds(rows: IntArray, bitWidth: Int): Short {
        var mask = 0
        for (row in rows) mask = mask or row
        val left: Int
        val right: Int
        if (mask == 0) {
            left = 0
            right = bitWidth
        } else {
            left = Integer.numberOfLeadingZeros(mask)
            right = 32 - Integer.numberOfTrailingZeros(mask) - 1
        }
        return (((left and 0xFF) shl 8) or (right and 0xFF)).toShort()
    }

    private fun loadUnihex(rm: ResourceManager, provider: JsonObject, hex: HexAccumulator, claimed: MutableSet<Int>) {
        val hexRef = provider.get("hex_file")?.asString ?: return
        val (ns, path) = splitId(hexRef)
        val zipBytes = read(rm, Identifier.fromNamespaceAndPath(ns, path)) ?: run {
            LOGGER.warn("unihex font provider references missing file {}", hexRef)
            return
        }

        val start = hex.size
        ZipInputStream(ByteArrayInputStream(zipBytes)).use { zip ->
            while (true) {
                val entry = zip.nextEntry ?: break
                if (!entry.name.endsWith(".hex")) continue
                readHex(zip.readBytes()) { cp, rows, bitWidth ->
                    if (cp !in claimed) hex.add(cp, rows, bitWidth)
                }
            }
        }

        applySizeOverrides(provider, hex, start)
        for (i in start until hex.size) claimed.add(hex.cps[i])
    }

    private fun applySizeOverrides(provider: JsonObject, hex: HexAccumulator, start: Int) {
        val overrides = provider.getAsJsonArray("size_overrides") ?: return
        val ranges = ArrayList<IntArray>(overrides.size())
        for (element in overrides) {
            val range = runCatching {
                val json = element.asJsonObject
                intArrayOf(
                    json.get("from").asString.codePointAt(0),
                    json.get("to").asString.codePointAt(0),
                    json.get("left").asInt,
                    json.get("right").asInt
                )
            }.getOrNull() ?: continue
            ranges.add(range)
        }
        if (ranges.isEmpty()) return

        for (i in start until hex.size) {
            val cp = hex.cps[i]
            for (range in ranges) {
                if (cp < range[0] || cp > range[1]) continue
                hex.bounds[i] = (((range[2] and 0xFF) shl 8) or (range[3] and 0xFF)).toShort()
                break // vanilla removes the codepoint on the first matching range
            }
        }
    }

    private inline fun readHex(bytes: ByteArray, out: (cp: Int, rows: IntArray, bitWidth: Int) -> Unit) {
        val rows = IntArray(UNIHEX_HEIGHT)
        var i = 0
        var skipped = 0
        while (i < bytes.size) {
            var end = i
            while (end < bytes.size && bytes[end] != '\n'.code.toByte()) end++
            var lineEnd = end
            if (lineEnd > i && bytes[lineEnd - 1] == '\r'.code.toByte()) lineEnd--

            if (lineEnd > i && !readHexEntry(bytes, i, lineEnd, rows, out)) skipped++
            i = end + 1
        }
        if (skipped > 0) LOGGER.warn("Skipped {} malformed unihex entries", skipped)
    }

    private inline fun readHexEntry(
        bytes: ByteArray, start: Int, end: Int, rows: IntArray,
        out: (cp: Int, rows: IntArray, bitWidth: Int) -> Unit
    ): Boolean {
        var colon = start
        while (colon < end && bytes[colon] != ':'.code.toByte()) colon++
        if (colon >= end) return false

        val cpDigits = colon - start
        if (cpDigits !in 4..6) return false
        var cp = 0
        for (i in start until colon) {
            val digit = Character.digit(bytes[i].toInt(), 16)
            if (digit < 0) return false
            cp = (cp shl 4) or digit
        }

        val digits = end - colon - 1
        val bitWidth = when (digits) {
            32 -> 8
            64 -> 16
            96 -> 24
            128 -> 32
            else -> return false
        }
        val perRow = digits / UNIHEX_HEIGHT
        var p = colon + 1
        for (row in 0 until UNIHEX_HEIGHT) {
            var value = 0
            for (q in 0 until perRow) {
                val digit = Character.digit(bytes[p++].toInt(), 16)
                if (digit < 0) return false
                value = (value shl 4) or digit
            }
            rows[row] = value shl (32 - bitWidth)
        }
        out(cp, rows, bitWidth)
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
