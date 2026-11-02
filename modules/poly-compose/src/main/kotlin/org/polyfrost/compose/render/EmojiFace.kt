package org.polyfrost.compose.render

import java.io.File
import java.nio.ByteBuffer
import java.nio.channels.FileChannel
import java.nio.file.StandardOpenOption
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt

class EmojiFace(private val font: ByteBuffer, outlines: ((ByteArray) -> PolyGlyphPage.Face)? = null) : PolyGlyphPage.Face {
    private val tables = HashMap<String, Int>()
    private val unitsPerEm: Int
    private val metricCount: Int
    private val glyphCount: Int
    private val cmap: Int
    private val layers: PolyGlyphPage.Face?

    init {
        val base = if (tag(0) == "ttcf") u32(12) else 0
        for (i in 0 until u16(base + 4)) {
            val r = base + 12 + i * 16
            tables[tag(r)] = u32(r + 8)
            tables[tag(r) + ".length"] = u32(r + 12)
        }
        unitsPerEm = u16(table("head") + 18)
        metricCount = u16(table("hhea") + 34)
        glyphCount = u16(table("maxp") + 4)
        val map = table("cmap")
        cmap = (0 until u16(map + 2)).map { map + u32(map + 8 + it * 8) }.firstOrNull { u16(it) == 12 } ?: error("no format 12 cmap")
        layers = if ("COLR" in tables && outlines != null) outlines(ByteArray(font.capacity()).also { font.get(0, it) }) else null
        require("sbix" in tables || "CBDT" in tables || layers != null) { "no supported colour table" }
    }

    override fun glyph(codepoint: Int): Int {
        var lo = 0
        var hi = u32(cmap + 12) - 1
        while (lo <= hi) {
            val mid = (lo + hi) ushr 1
            val group = cmap + 16 + mid * 12
            when {
                codepoint < u32(group) -> hi = mid - 1
                codepoint > u32(group + 4) -> lo = mid + 1
                else -> return u32(group + 8) + codepoint - u32(group)
            }
        }
        return 0
    }

    private val flags by lazy { runCatching { postFlags() + gsubFlags() }.getOrDefault(emptyMap()) }

    override fun pair(first: Int, second: Int): Int =
        if (first in REGIONAL && second in REGIONAL) flags[flagKey(first, second)] ?: 0 else 0

    private fun flagKey(first: Int, second: Int) = (first - REGIONAL.first) * 26 + second - REGIONAL.first

    private fun regionKey(region: String) = flagKey(REGIONAL.first + region[0].code - 'A'.code, REGIONAL.first + region[1].code - 'A'.code)

    private fun postFlags(): Map<Int, Int> {
        val post = tables["post"] ?: return emptyMap()
        if (u32(post) != 0x00020000) return emptyMap()
        val count = u16(post + 32)
        val names = ArrayList<String>()
        var at = post + 34 + count * 2
        val end = post + tables.getValue("post.length")
        while (at < end) {
            val length = u8(at)
            names += String(CharArray(length) { u8(at + 1 + it).toChar() })
            at += length + 1
        }
        val out = HashMap<Int, Int>()
        for (id in 0 until count) {
            val name = names.getOrNull(u16(post + 34 + id * 2) - STANDARD_MAC_NAMES) ?: continue
            val match = FLAG_NAME.matchEntire(name) ?: continue
            out[flagKey(match.groupValues[1].toInt(16), match.groupValues[2].toInt(16))] = id
        }
        for ((region, flag) in FLAG_ALIASES) out[regionKey(flag)]?.let { out[regionKey(region)] = it }
        return out
    }

    private fun gsubFlags(): Map<Int, Int> {
        val gsub = tables["GSUB"] ?: return emptyMap()
        val regional = REGIONAL.associateBy(::glyph).filterKeys { it != 0 }
        val out = HashMap<Int, Int>()
        val list = gsub + u16(gsub + 8)
        for (i in 0 until u16(list)) {
            val lookup = list + u16(list + 2 + i * 2)
            for (j in 0 until u16(lookup + 4)) {
                var sub = lookup + u16(lookup + 6 + j * 2)
                var type = u16(lookup)
                if (type == EXTENSION) {
                    type = u16(sub + 2)
                    sub += u32(sub + 4)
                }
                if (type != LIGATURE) continue
                coverage(sub + u16(sub + 2)).forEachIndexed { index, glyph ->
                    val first = regional[glyph] ?: return@forEachIndexed
                    val set = sub + u16(sub + 6 + index * 2)
                    for (k in 0 until u16(set)) {
                        val ligature = set + u16(set + 2 + k * 2)
                        val second = regional[u16(ligature + 4)]
                        if (u16(ligature + 2) == 2 && second != null) out[flagKey(first, second)] = u16(ligature)
                    }
                }
            }
        }
        return out
    }

    private fun coverage(at: Int): List<Int> =
        if (u16(at) == 1) List(u16(at + 2)) { u16(at + 4 + it * 2) }
        else (0 until u16(at + 2)).flatMap { (u16(at + 4 + it * 6)..u16(at + 6 + it * 6)).toList() }

    override fun advance(glyph: Int) = u16(table("hmtx") + min(glyph, metricCount - 1) * 4) / unitsPerEm.toFloat()

    override fun raster(glyph: Int, pixelSize: Float): PolyGlyphPage.Raster? = runCatching {
        when {
            "sbix" in tables -> sbix(glyph, pixelSize)
            "CBDT" in tables -> cbdt(glyph, pixelSize)
            else -> colr(glyph, pixelSize)
        }
    }.getOrNull()

    private fun sbix(glyph: Int, pixelSize: Float): PolyGlyphPage.Raster? {
        val sbix = table("sbix")
        val strikes = (0 until u32(sbix + 4)).map { sbix + u32(sbix + 8 + it * 4) }.sortedBy { u16(it) }
        val strike = strikes.firstOrNull { u16(it) >= pixelSize } ?: strikes.last()
        var id = glyph
        repeat(2) {
            val start = u32(strike + 4 + id * 4)
            val size = u32(strike + 8 + id * 4) - start - 8
            if (size <= 0 || id >= glyphCount) return null
            val at = strike + start
            when (tag(at + 4)) {
                "dupe" -> id = u16(at + 8)
                "png " -> return image(at + 8, size, pixelSize / u16(strike), s16(at), s16(at + 2), bottom = true)
                else -> return null
            }
        }
        return null
    }

    private fun cbdt(glyph: Int, pixelSize: Float): PolyGlyphPage.Raster? {
        val cblc = table("CBLC")
        val sizes = (0 until u32(cblc + 4)).map { cblc + 8 + it * 48 }.sortedBy { u8(it + 45) }
        val size = sizes.firstOrNull { u8(it + 45) >= pixelSize } ?: sizes.last()
        val array = cblc + u32(size)
        for (i in 0 until u32(size + 8)) {
            val entry = array + i * 8
            if (glyph !in u16(entry)..u16(entry + 2)) continue
            val sub = array + u32(entry + 4)
            if (u16(sub) != 1 || u16(sub + 2) != 17) return null
            val offsets = sub + 8 + (glyph - u16(entry)) * 4
            if (u32(offsets) == u32(offsets + 4)) return null
            val at = table("CBDT") + u32(sub + 4) + u32(offsets)
            return image(at + 9, u32(at + 5), pixelSize / u8(size + 45), font.get(at + 2).toInt(), font.get(at + 3).toInt(), bottom = false)
        }
        return null
    }

    private fun image(at: Int, size: Int, scale: Float, x: Int, y: Int, bottom: Boolean): PolyGlyphPage.Raster? {
        val bitmap = PolyBitmap.decode(ByteArray(size).also { font.get(at, it) }) ?: return null
        val w = max(1, (bitmap.width * scale).roundToInt())
        val h = max(1, (bitmap.height * scale).roundToInt())
        return PolyGlyphPage.Raster((x * scale).roundToInt(), -((y + if (bottom) bitmap.height else 0) * scale).roundToInt(), w, h, scaled(bitmap, w, h), true)
    }

    private fun colr(glyph: Int, pixelSize: Float): PolyGlyphPage.Raster? {
        val outlines = layers ?: return null
        val colr = table("COLR")
        val records = colr + u32(colr + 4)
        var lo = 0
        var hi = u16(colr + 2) - 1
        while (lo <= hi) {
            val mid = (lo + hi) ushr 1
            val id = u16(records + mid * 6)
            if (id < glyph) { lo = mid + 1; continue }
            if (id > glyph) { hi = mid - 1; continue }
            val first = colr + u32(colr + 8) + u16(records + mid * 6 + 2) * 4
            val parts = (0 until u16(records + mid * 6 + 4)).mapNotNull { i ->
                outlines.raster(u16(first + i * 4), pixelSize)?.let { it to u16(first + i * 4 + 2) }
            }
            return composite(parts)
        }
        return null
    }

    private fun composite(parts: List<Pair<PolyGlyphPage.Raster, Int>>): PolyGlyphPage.Raster? {
        if (parts.isEmpty()) return null
        val x0 = parts.minOf { it.first.offX }
        val y0 = parts.minOf { it.first.offY }
        val w = parts.maxOf { it.first.offX + it.first.w } - x0
        val h = parts.maxOf { it.first.offY + it.first.h } - y0
        val out = FloatArray(w * h * 4)
        val cpal = table("CPAL")
        val colors = cpal + u32(cpal + 8) + u16(cpal + 12) * 4
        for ((r, palette) in parts) {
            val c = colors + palette * 4
            val red = if (palette == FOREGROUND) 1f else u8(c + 2) / 255f
            val green = if (palette == FOREGROUND) 1f else u8(c + 1) / 255f
            val blue = if (palette == FOREGROUND) 1f else u8(c) / 255f
            val alpha = if (palette == FOREGROUND) 1f else u8(c + 3) / 255f
            for (y in 0 until r.h) for (x in 0 until r.w) {
                val a = (r.coverage[y * r.w + x].toInt() and 0xFF) / 255f * alpha
                val o = ((y + r.offY - y0) * w + x + r.offX - x0) * 4
                out[o] = red * a + out[o] * (1f - a)
                out[o + 1] = green * a + out[o + 1] * (1f - a)
                out[o + 2] = blue * a + out[o + 2] * (1f - a)
                out[o + 3] = a + out[o + 3] * (1f - a)
            }
        }
        return PolyGlyphPage.Raster(x0, y0, w, h, straight(out), true)
    }

    private fun table(name: String) = tables[name] ?: error("no $name table")
    private fun tag(o: Int) = String(CharArray(4) { (font.get(o + it).toInt() and 0xFF).toChar() })
    private fun u8(o: Int) = font.get(o).toInt() and 0xFF
    private fun u16(o: Int) = font.getShort(o).toInt() and 0xFFFF
    private fun s16(o: Int) = font.getShort(o).toInt()
    private fun u32(o: Int) = font.getInt(o)

    companion object {
        private const val FOREGROUND = 0xFFFF
        private const val LIGATURE = 4
        private const val EXTENSION = 7
        private const val STANDARD_MAC_NAMES = 258
        private val REGIONAL = 0x1F1E6..0x1F1FF
        private val FLAG_ALIASES = mapOf("BV" to "NO", "SJ" to "NO", "HM" to "AU", "MF" to "FR", "UM" to "US")
        private val FLAG_NAME = Regex("u(1F1[EF][0-9A-F])_u(1F1[EF][0-9A-F])")

        private fun scaled(b: PolyBitmap, w: Int, h: Int): ByteArray {
            val out = FloatArray(w * h * 4)
            for (y in 0 until h) for (x in 0 until w) {
                val sx0 = x * b.width / w
                val sy0 = y * b.height / h
                val sx1 = max((x + 1) * b.width / w, sx0 + 1)
                val sy1 = max((y + 1) * b.height / h, sy0 + 1)
                val o = (y * w + x) * 4
                for (sy in sy0 until sy1) for (sx in sx0 until sx1) {
                    val i = (sy * b.width + sx) * 4
                    val a = (b.pixels[i + 3].toInt() and 0xFF) / 255f
                    for (c in 0..2) out[o + c] += (b.pixels[i + c].toInt() and 0xFF) / 255f * a
                    out[o + 3] += a
                }
                val n = ((sx1 - sx0) * (sy1 - sy0)).toFloat()
                for (c in 0..3) out[o + c] /= n
            }
            return straight(out)
        }

        private fun straight(premultiplied: FloatArray) = ByteArray(premultiplied.size) { i ->
            val a = premultiplied[i or 3]
            val v = if (i and 3 == 3) a else if (a > 0f) premultiplied[i] / a else 0f
            (v.coerceIn(0f, 1f) * 255f).roundToInt().toByte()
        }

        private fun systemFont(): File? {
            val os = System.getProperty("os.name")
            return when {
                os.startsWith("Mac") -> File("/System/Library/Fonts/Apple Color Emoji.ttc")
                os.startsWith("Windows") -> File(System.getenv("WINDIR") ?: "C:\\Windows", "Fonts\\seguiemj.ttf")
                else -> File("/usr/share/fonts").walkTopDown().maxDepth(4).firstOrNull { it.name == "NotoColorEmoji.ttf" }
            }?.takeIf { it.isFile }
        }

        @JvmStatic
        fun system(outlines: (ByteArray) -> PolyGlyphPage.Face): EmojiFace? {
            val file = systemFont() ?: return null
            val mapped = FileChannel.open(file.toPath(), StandardOpenOption.READ).use { it.map(FileChannel.MapMode.READ_ONLY, 0, it.size()) }
            return EmojiFace(mapped, outlines)
        }
    }
}
