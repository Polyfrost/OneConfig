package org.polyfrost.compose.render

import java.util.WeakHashMap
import java.util.concurrent.ConcurrentHashMap
import kotlin.math.abs
import kotlin.math.ceil
import kotlin.math.max
import kotlin.math.roundToInt
import kotlin.math.sqrt

object PolyGlyphPage {
    interface Rasterizer {
        fun face(name: String): Face?
    }

    interface Face {
        fun glyph(codepoint: Int): Int

        fun advance(glyph: Int): Float

        fun raster(glyph: Int, pixelSize: Float): Raster?

        fun raster(glyph: Int, pixelSize: Float, shiftX: Float): Raster? = raster(glyph, pixelSize)
    }

    class Raster(val offX: Int, val offY: Int, val w: Int, val h: Int, val coverage: ByteArray)

    class Glyph(val page: Page, val offX: Int, val offY: Int, val w: Int, val h: Int, val u0: Float, val v0: Float, val u1: Float, val v1: Float) {
        fun u(t: Float) = u0 + (u1 - u0) * t
        fun v(t: Float) = v0 + (v1 - v0) * t
    }

    class Page internal constructor() {
        @JvmField
        val pixels = IntArray(SIZE * SIZE).also { px ->
            for (y in 0 until WHITE) for (x in 0 until WHITE) px[(PAD + y) * SIZE + PAD + x] = -1
        }

        var version = 1
            private set

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
            return (x shl 16) or y
        }

        internal fun place(r: Raster): Glyph? {
            val at = alloc(r.w, r.h)
            if (at < 0) return null
            val x = at ushr 16
            val y = at and 0xFFFF
            for (gy in 0 until r.h) for (gx in 0 until r.w) {
                pixels[(y + gy) * SIZE + x + gx] = ((r.coverage[gy * r.w + gx].toInt() and 0xFF) shl 24) or 0xFFFFFF
            }
            return Glyph(this, r.offX, r.offY, r.w, r.h, x / SIZE.toFloat(), y / SIZE.toFloat(), (x + r.w) / SIZE.toFloat(), (y + r.h) / SIZE.toFloat())
        }

        internal fun place(b: PolyBitmap): Glyph? {
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
            return Glyph(this, 0, 0, w, h, x / SIZE.toFloat(), y / SIZE.toFloat(), (x + w) / SIZE.toFloat(), (y + h) / SIZE.toFloat())
        }
    }

    const val SIZE = 1024
    private const val PAD = 1
    private const val WHITE = 2

    const val WHITE_UV = (PAD + WHITE / 2f) / SIZE

    @JvmStatic
    var rasterizer: Rasterizer? = null

    private val _pages = arrayListOf(Page())

    @JvmStatic
    val pages: List<Page> get() = _pages

    private val atlases = HashMap<Pair<Face, Int>, HashMap<Int, Glyph?>>()
    private val outlineAtlases = HashMap<Triple<Face, Int, Int>, HashMap<Int, Glyph?>>()

    fun atlas(face: Face, bucket: Int): HashMap<Int, Glyph?> = atlases.getOrPut(face to bucket) { HashMap() }

    fun atlas(face: Face, bucket: Int, outlinePx: Float, round: Boolean): HashMap<Int, Glyph?> {
        if (outlinePx <= 0f) return atlas(face, bucket)
        val q = max(1, (outlinePx * 4f).roundToInt())
        return outlineAtlases.getOrPut(Triple(face, bucket, if (round) q else -q)) { HashMap() }
    }

    const val SUBPIXELS = 4

    private data class Prekey(val face: Face, val bucket: Int, val slot: Int)

    private val prerastered = ConcurrentHashMap<Prekey, Raster>()

    @JvmStatic
    var recorder: ((face: Face, bucket: Int, id: Int, phase: Int) -> Unit)? = null

    private fun raster(face: Face, bucket: Int, id: Int, phase: Int): Raster? =
        if (phase == 0) face.raster(id, bucket / 4f) else face.raster(id, bucket / 4f, phase / SUBPIXELS.toFloat())

    @Volatile
    private var prerasterDropped = false

    @JvmStatic
    fun dropPrerastered(): Int {
        prerasterDropped = true
        val n = prerastered.size
        prerastered.clear()
        return n
    }

    @JvmStatic
    fun prerasterize(face: Face, bucket: Int, id: Int, phase: Int) {
        if (prerasterDropped) return
        val key = Prekey(face, bucket, id * SUBPIXELS + phase)
        if (prerastered.containsKey(key)) return
        prerastered.putIfAbsent(key, raster(face, bucket, id, phase) ?: return)
        if (prerasterDropped) prerastered.remove(key)
    }

    fun glyph(atlas: HashMap<Int, Glyph?>, face: Face, bucket: Int, id: Int, outlinePx: Float = 0f, round: Boolean = false, phase: Int = 0): Glyph? = atlas.getOrPut(id * SUBPIXELS + phase) {
        if (outlinePx > 0f) return@getOrPut place(dilate(raster(face, bucket, id, phase) ?: return@getOrPut null, max(1, (outlinePx * 4f).roundToInt()) / 4f, round))
        recorder?.invoke(face, bucket, id, phase)
        place(prerastered.remove(Prekey(face, bucket, id * SUBPIXELS + phase)) ?: raster(face, bucket, id, phase) ?: return@getOrPut null)
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
    private var placedSince = 0L
    private var placed = 0

    @JvmStatic
    fun bitmap(b: PolyBitmap): Glyph? {
        bitmaps[b]?.let { return it }
        if (b.width > BITMAP_MAX || b.height > BITMAP_MAX) return null
        val now = System.nanoTime()
        if (now - placedSince > 1_000_000_000L) { placedSince = now; placed = 0 }
        if (++placed > 128) return null
        val g = _pages.last().place(b) ?: (if (_pages.size < BITMAP_PAGES) Page().also { _pages += it }.place(b) else null) ?: return null
        bitmaps[b] = g
        return g
    }

    private fun place(r: Raster): Glyph? {
        if (r.w <= 0 || r.h <= 0 || r.w + PAD * 2 > SIZE || r.h + PAD * 2 > SIZE) return null
        return _pages.last().place(r) ?: Page().also { _pages += it }.place(r)
    }
}
