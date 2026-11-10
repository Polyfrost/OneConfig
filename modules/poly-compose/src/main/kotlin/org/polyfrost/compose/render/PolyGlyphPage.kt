package org.polyfrost.compose.render

import java.util.WeakHashMap
import java.util.concurrent.ConcurrentHashMap
import kotlin.math.abs
import kotlin.math.ceil
import kotlin.math.max
import kotlin.math.roundToInt
import kotlin.math.sqrt

public object PolyGlyphPage {
    public interface Rasterizer {
        public fun face(name: String): Face?

        public fun emoji(): Face? = null
    }

    public interface Face {
        public fun glyph(codepoint: Int): Int

        public fun pair(first: Int, second: Int): Int = 0

        public fun advance(glyph: Int): Float

        public fun raster(glyph: Int, pixelSize: Float): Raster?

        public fun raster(glyph: Int, pixelSize: Float, shiftX: Float): Raster? = raster(glyph, pixelSize)
    }

    public class Raster @JvmOverloads constructor(public val offX: Int, public val offY: Int, public val w: Int, public val h: Int, public val coverage: ByteArray, public val color: Boolean = false)

    public class Glyph @JvmOverloads constructor(public val page: Page, public val offX: Int, public val offY: Int, public val w: Int, public val h: Int, public val u0: Float, public val v0: Float, public val u1: Float, public val v1: Float, public val color: Boolean = false) {
        public fun u(t: Float): Float = u0 + (u1 - u0) * t
        public fun v(t: Float): Float = v0 + (v1 - v0) * t
    }

    public class Page internal constructor() {
        @JvmField
        public val pixels: IntArray = IntArray(SIZE * SIZE).also { px ->
            for (y in 0 until WHITE) for (x in 0 until WHITE) px[(PAD + y) * SIZE + PAD + x] = -1
        }

        public var version: Int = 1
            private set

        private val rowVersions = IntArray(SIZE).also { it.fill(1, PAD, PAD + WHITE) }

        init {
            revision++
        }

        /** The rows written to since [version] was [since], so only those need uploading again. Empty if there are none. */
        public fun dirtyRows(since: Int): IntRange {
            var first = -1
            var last = -1
            for (y in 0 until SIZE) if (rowVersions[y] > since) {
                if (first < 0) first = y
                last = y
            }
            return if (first < 0) IntRange.EMPTY else first..last
        }

        private var penX = PAD * 2 + WHITE
        private var penY = PAD
        private var rowH = WHITE

        private fun alloc(w: Int, h: Int): Int {
            var x = penX
            var y = penY
            var row = rowH
            if (x + w + PAD > SIZE) { x = PAD; y += row + PAD; row = 0 }
            if (y + h + PAD > SIZE) return -1
            penX = x + w + PAD
            penY = y
            rowH = max(row, h)
            version++
            revision++
            rowVersions.fill(version, y, y + h)
            return (x shl 16) or y
        }

        internal fun place(r: Raster): Glyph? {
            if (r.color) return place(PolyBitmap(r.w, r.h, r.coverage), r.offX, r.offY, true)
            val at = alloc(r.w, r.h)
            if (at < 0) return null
            val x = at ushr 16
            val y = at and 0xFFFF
            for (gy in 0 until r.h) for (gx in 0 until r.w) {
                pixels[(y + gy) * SIZE + x + gx] = ((r.coverage[gy * r.w + gx].toInt() and 0xFF) shl 24) or 0xFFFFFF
            }
            return Glyph(this, r.offX, r.offY, r.w, r.h, x / SIZE.toFloat(), y / SIZE.toFloat(), (x + r.w) / SIZE.toFloat(), (y + r.h) / SIZE.toFloat())
        }

        internal fun place(b: PolyBitmap, offX: Int = 0, offY: Int = 0, color: Boolean = false): Glyph? {
            val w = b.width
            val h = b.height
            val at = alloc(w + 2, h + 2)
            if (at < 0) return null
            val x = (at ushr 16) + 1
            val y = (at and 0xFFFF) + 1
            val px = b.pixels
            for (gy in -1..h) for (gx in -1..w) {
                val o = (gy.coerceIn(0, h - 1) * w + gx.coerceIn(0, w - 1)) * 4
                pixels[(y + gy) * SIZE + x + gx] = ((px[o + 3].toInt() and 0xFF) shl 24) or ((px[o + 2].toInt() and 0xFF) shl 16) or
                    ((px[o + 1].toInt() and 0xFF) shl 8) or (px[o].toInt() and 0xFF)
            }
            return Glyph(this, offX, offY, w, h, x / SIZE.toFloat(), y / SIZE.toFloat(), (x + w) / SIZE.toFloat(), (y + h) / SIZE.toFloat(), color)
        }
    }

    public const val SIZE: Int = 1024
    private const val PAD = 1
    private const val WHITE = 2

    public const val WHITE_UV: Float = (PAD + WHITE / 2f) / SIZE

    @JvmStatic
    public var rasterizer: Rasterizer? = null

    // shortcut: pages and atlas entries (including those of replaced font faces) are never evicted (4 MiB per page,
    // plus a renderer texture) since retained recordings, cached layers and kept meshes reuse their textures without
    // revalidating; bound them once those can re-record or pin pages
    private val _pages = arrayListOf(Page())

    @JvmStatic
    public val pages: List<Page> get() = _pages

    @JvmStatic
    public var revision: Long = 0L
        private set

    private val atlases = HashMap<Triple<Face, Int, Int>, HashMap<Int, Glyph?>>()
    private val outlineAtlases = HashMap<Triple<Face, Int, Int>, HashMap<Int, Glyph?>>()

    public fun atlas(face: Face, bucket: Int, lum: Int = -1): HashMap<Int, Glyph?> = atlases.getOrPut(Triple(face, bucket, lum)) { HashMap() }

    public fun atlas(face: Face, bucket: Int, outlinePx: Float, round: Boolean, lum: Int = -1): HashMap<Int, Glyph?> {
        if (outlinePx <= 0f) return atlas(face, bucket, lum)
        val q = max(1, (outlinePx * 4f).roundToInt())
        return outlineAtlases.getOrPut(Triple(face, bucket, if (round) q else -q)) { HashMap() }
    }

    public const val SUBPIXELS: Int = 4

    public const val EMOJI_VARIATION: Int = 0xFE0F

    private val REGIONAL = 0x1F1E6..0x1F1FF

    private data class Prekey(val face: Face, val bucket: Int, val slot: Int)

    // Stands in for glyphs without a bitmap, such as spaces, so they're only rasterized once
    private val BLANK = Raster(0, 0, 0, 0, ByteArray(0))

    private val prerastered = ConcurrentHashMap<Prekey, Raster>()
    private val rasters = HashMap<Prekey, Raster>()

    @JvmStatic
    public var recorder: ((face: Face, bucket: Int, id: Int, phase: Int) -> Unit)? = null

    @JvmStatic
    public fun isIgnorable(cp: Int): Boolean = cp == 0x200D || cp in 0xFE00..EMOJI_VARIATION

    @JvmStatic
    public fun fallback(cp: Int, next: Int): Face? {
        val r = rasterizer ?: return null
        if (Character.isEmojiPresentation(cp) || (next == EMOJI_VARIATION && Character.isEmoji(cp))) {
            r.emoji()?.let { if (it.glyph(cp) != 0) return it }
        }
        return r.face(PolyTessellator.FALLBACK_FONT)
    }

    @JvmStatic
    public fun isPair(cp: Int, next: Int): Boolean = cp in REGIONAL && next in REGIONAL && fallback(cp, next)?.pair(cp, next).let { it != null && it != 0 }

    private fun raster(face: Face, bucket: Int, id: Int, phase: Int): Raster? =
        if (phase == 0) face.raster(id, bucket / 4f) else face.raster(id, bucket / 4f, phase / SUBPIXELS.toFloat())

    @Volatile
    private var prerasterDropped = false

    @JvmStatic
    public fun dropPrerastered(): Int {
        prerasterDropped = true
        val n = prerastered.size
        prerastered.clear()
        return n
    }

    @JvmStatic
    public fun prerasterize(face: Face, bucket: Int, id: Int, phase: Int) {
        if (prerasterDropped) return
        val key = Prekey(face, bucket, id * SUBPIXELS + phase)
        if (prerastered.containsKey(key)) return
        prerastered.putIfAbsent(key, raster(face, bucket, id, phase) ?: BLANK)
        if (prerasterDropped) prerastered.remove(key)
    }

    public fun glyph(atlas: HashMap<Int, Glyph?>, face: Face, bucket: Int, id: Int, outlinePx: Float = 0f, round: Boolean = false, phase: Int = 0, lum: Int = -1): Glyph? {
        val slot = id * SUBPIXELS + phase
        atlas[slot]?.let { return it }
        if (atlas.containsKey(slot)) return null
        return newGlyph(face, bucket, id, outlinePx, round, phase, lum).also { atlas[slot] = it }
    }

    private fun newGlyph(face: Face, bucket: Int, id: Int, outlinePx: Float, round: Boolean, phase: Int, lum: Int): Glyph? {
        if (outlinePx > 0f) {
            val r = raster(face, bucket, id, phase) ?: return null
            return if (r.color) null else place(dilate(r, max(1, (outlinePx * 4f).roundToInt()) / 4f, round))
        }
        val key = Prekey(face, bucket, id * SUBPIXELS + phase)
        val r = rasters.getOrPut(key) {
            recorder?.invoke(face, bucket, id, phase)
            prerastered.remove(key) ?: raster(face, bucket, id, phase) ?: BLANK
        }
        if (r === BLANK) return null
        return place(if (lum < 0 || r.color) r else PolyMaskGamma.apply(r, lum))
    }

    private fun dilate(r: Raster, radius: Float, round: Boolean): Raster {
        val e = ceil(radius).toInt()
        val w = r.w + e * 2
        val h = r.h + e * 2
        val weights = FloatArray((e * 2 + 1) * (e * 2 + 1)) { k ->
            val dx = abs(k % (e * 2 + 1) - e).toFloat()
            val dy = abs(k / (e * 2 + 1) - e).toFloat()
            (radius + 1f - if (round) sqrt(dx * dx + dy * dy) else max(dx, dy)).coerceIn(0f, 1f)
        }
        val out = ByteArray(w * h)
        for (y in 0 until h) for (x in 0 until w) {
            var a = 0f
            for (ky in 0..e * 2) {
                val sy = y - ky
                if (sy !in 0 until r.h) continue
                for (kx in 0..e * 2) {
                    val sx = x - kx
                    if (sx !in 0 until r.w) continue
                    val v = (r.coverage[sy * r.w + sx].toInt() and 0xFF) * weights[ky * (e * 2 + 1) + kx]
                    if (v > a) a = v
                }
            }
            out[y * w + x] = a.roundToInt().toByte()
        }
        return Raster(r.offX - e, r.offY - e, w, h, out)
    }

    private const val BITMAP_MAX = 256
    private const val BITMAP_PAGES = 4

    private val bitmaps = WeakHashMap<PolyBitmap, Glyph>()
    private var bitmapPages = 0
    private var placedSince = 0L
    private var placed = 0

    // shortcut: placements are never reclaimed (see the note on _pages), so once the budget is used up later bitmaps
    // fall back to their own textures; transient images should be cached by their content rather than re-created
    @JvmStatic
    public fun bitmap(b: PolyBitmap): Glyph? {
        bitmaps[b]?.let { return it }
        if (b.width > BITMAP_MAX || b.height > BITMAP_MAX) return null
        val now = System.nanoTime()
        if (now - placedSince > 1_000_000_000L) { placedSince = now; placed = 0 }
        if (++placed > 128) return null
        val g = _pages.last().place(b) ?: (if (bitmapPages < BITMAP_PAGES) Page().also { _pages += it; bitmapPages++ }.place(b) else null) ?: return null
        bitmaps[b] = g
        return g
    }

    private fun place(r: Raster): Glyph? {
        val edge = if (r.color) PAD + 1 else PAD
        if (r.w <= 0 || r.h <= 0 || r.w + edge * 2 > SIZE || r.h + edge * 2 > SIZE) return null
        return _pages.last().place(r) ?: Page().also { _pages += it }.place(r)
    }
}
