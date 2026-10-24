package org.polyfrost.compose.render

import org.polyfrost.compose.ui.graphics.ImageBitmap
import kotlin.math.max

object PolyGlyphPage {
    interface Rasterizer {
        fun face(name: String): Face?
    }

    interface Face {
        fun glyph(codepoint: Int): Int

        fun advance(glyph: Int): Float

        fun raster(glyph: Int, pixelSize: Float): Raster?
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

        internal fun place(b: ImageBitmap): Glyph? {
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

    fun atlas(face: Face, bucket: Int): HashMap<Int, Glyph?> = atlases.getOrPut(face to bucket) { HashMap() }

    fun glyph(atlas: HashMap<Int, Glyph?>, face: Face, bucket: Int, id: Int): Glyph? = atlas.getOrPut(id) {
        val r = face.raster(id, bucket / 4f) ?: return@getOrPut null
        place(r)
    }

    private const val BITMAP_MAX = 256
    private const val BITMAP_PAGES = 4

    private val bitmaps = java.util.WeakHashMap<ImageBitmap, Glyph>()
    private var placedSince = 0L
    private var placed = 0

    @JvmStatic
    fun bitmap(b: ImageBitmap): Glyph? {
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
