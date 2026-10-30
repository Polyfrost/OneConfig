package org.polyfrost.compose.backend

import androidx.compose.ui.graphics.ImageBitmapConfig
import kotlin.math.ceil
import kotlin.math.floor
import kotlin.math.max
import kotlin.math.min
import org.polyfrost.compose.render.PolyBitmap
import org.polyfrost.compose.render.PolyGlyphPage
import org.polyfrost.compose.render.PolyRenderer
import org.polyfrost.compose.render.PolyTessellator

/**
 * Rasterizes the tessellator's output into a [PolyImageBitmap] on the CPU, backing `Canvas(ImageBitmap)`.
 * Compose uses it to cache vector images, which are only redrawn when they change.
 */
internal class BitmapRenderer(private val target: PolyImageBitmap) : PolyTessellator() {
    // Alpha8 images are drawn tinted, which the backend does by multiplying, so coverage is stored as white
    private val mask = target.config == ImageBitmapConfig.Alpha8
    private var depth = 0

    // The texture being sampled: an atlas page's packed pixels, or a bitmap's bytes when there is no page
    private var texPage: IntArray? = null
    private var texBytes = ByteArray(0)
    private var texW = 0
    private var texH = 0
    private var texNearest = false

    /** Whether drawing removes coverage instead of painting, as with `BlendMode.Clear`. */
    var erasing = false
        set(value) {
            flush()
            field = value
        }

    override fun bitmapTexture(bitmap: PolyBitmap): Any = bitmap
    override fun live(draw: (PolyRenderer) -> Unit) = draw(this)

    override fun save() {
        depth++
        super.save()
    }

    override fun restore() {
        depth--
        super.restore()
    }

    /** Rasterizes pending draws; called whenever the target's pixels are read. */
    fun sync() {
        if (drawBoxMark == 0) return
        flush()
        // Per-draw bookkeeping would otherwise grow with every redraw; the transform is kept for callers that never save
        if (depth == 0) state.let { resetTessellator(); state = it }
    }

    override fun submit(texture: Any, premultiplied: Boolean, nearest: Boolean, verts: FloatArray, colors: IntArray, bounds: FloatArray) {
        val px = target.pixelsForDrawing()
        if (texture is PolyGlyphPage.Page) {
            texPage = texture.pixels
            texW = PolyGlyphPage.SIZE
            texH = PolyGlyphPage.SIZE
        } else {
            val b = texture as PolyBitmap
            texPage = null
            texBytes = b.pixels
            texW = b.width
            texH = b.height
        }
        texNearest = nearest
        for (q in colors.indices step 4) {
            triangle(px, verts, colors, q, q + 1, q + 2, premultiplied)
            triangle(px, verts, colors, q, q + 2, q + 3, premultiplied)
        }
    }

    private fun triangle(px: ByteArray, v: FloatArray, c: IntArray, a: Int, b0: Int, d0: Int, premultiplied: Boolean) {
        val flip = edge(v, a, b0, v[d0 * 4], v[d0 * 4 + 1]) < 0f
        val b = if (flip) d0 else b0
        val d = if (flip) b0 else d0
        val area = edge(v, a, b, v[d * 4], v[d * 4 + 1])
        if (!(area > 0f)) return
        val ax = v[a * 4]; val ay = v[a * 4 + 1]
        val bx = v[b * 4]; val by = v[b * 4 + 1]
        val dx = v[d * 4]; val dy = v[d * 4 + 1]
        val au = v[a * 4 + 2]; val av = v[a * 4 + 3]
        val bu = v[b * 4 + 2]; val bv = v[b * 4 + 3]
        val du = v[d * 4 + 2]; val dv = v[d * 4 + 3]
        // Solid fills all sample the white texel, so it is only looked up once
        val flat = au == bu && au == du && av == bv && av == dv
        val flatTexel = if (flat) sample(au, av) else 0
        val w = target.width
        val x0 = max(0, ceil(min(ax, min(bx, dx)) - 0.5f).toInt())
        val x1 = min(w - 1, floor(max(ax, max(bx, dx)) - 0.5f).toInt())
        val y0 = max(0, ceil(min(ay, min(by, dy)) - 0.5f).toInt())
        val y1 = min(target.height - 1, floor(max(ay, max(by, dy)) - 0.5f).toInt())
        val tlA = topLeft(dx - bx, dy - by)
        val tlB = topLeft(ax - dx, ay - dy)
        val tlD = topLeft(bx - ax, by - ay)
        for (y in y0..y1) for (x in x0..x1) {
            val sx = x + 0.5f
            val sy = y + 0.5f
            val wa = edge(v, b, d, sx, sy)
            if (wa < 0f || wa == 0f && !tlA) continue
            val wb = edge(v, d, a, sx, sy)
            if (wb < 0f || wb == 0f && !tlB) continue
            val wd = edge(v, a, b, sx, sy)
            if (wd < 0f || wd == 0f && !tlD) continue
            val t = if (flat) flatTexel else sample((wa * au + wb * bu + wd * du) / area, (wa * av + wb * bv + wd * dv) / area)
            val sa = mix(c[a], c[b], c[d], wa, wb, wd, area, 24) / 255f * (t ushr 24) / 255f
            if (sa <= 0f) continue
            val o = (y * w + x) * 4
            val da = (px[o + 3].toInt() and 0xFF) / 255f
            if (erasing) {
                px[o + 3] = (da * (1f - sa) * 255f + 0.5f).toInt().toByte()
                continue
            }
            val outA = sa + da * (1f - sa)
            for (ch in 0..2) {
                val s = if (mask) sa
                else mix(c[a], c[b], c[d], wa, wb, wd, area, 16 - ch * 8) / 255f * (t ushr ch * 8 and 0xFF) / 255f * (if (premultiplied) 1f else sa)
                val dc = (px[o + ch].toInt() and 0xFF) / 255f * da
                px[o + ch] = ((s + dc * (1f - sa)) / outA * 255f + 0.5f).toInt().coerceIn(0, 255).toByte()
            }
            px[o + 3] = (outA * 255f + 0.5f).toInt().toByte()
        }
    }

    // Bilinear unless nearest, clamped to the edge like the GPU's samplers; packed like atlas pages, with red in the low byte
    private fun sample(u: Float, v: Float): Int {
        if (texNearest) return texel(floor(u * texW).toInt(), floor(v * texH).toInt())
        val fx = u * texW - 0.5f
        val fy = v * texH - 0.5f
        val x0 = floor(fx).toInt()
        val y0 = floor(fy).toInt()
        val ax = fx - x0
        val ay = fy - y0
        val c00 = texel(x0, y0); val c10 = texel(x0 + 1, y0)
        val c01 = texel(x0, y0 + 1); val c11 = texel(x0 + 1, y0 + 1)
        var out = 0
        var shift = 0
        while (shift <= 24) {
            val top = lerp(c00, c10, shift, ax)
            val bottom = lerp(c01, c11, shift, ax)
            out = out or ((top + (bottom - top) * ay + 0.5f).toInt() shl shift)
            shift += 8
        }
        return out
    }

    private fun lerp(a: Int, b: Int, shift: Int, t: Float): Float {
        val ca = a ushr shift and 0xFF
        return ca + ((b ushr shift and 0xFF) - ca) * t
    }

    private fun texel(x: Int, y: Int): Int {
        val i = y.coerceIn(0, texH - 1) * texW + x.coerceIn(0, texW - 1)
        texPage?.let { return it[i] }
        val p = texBytes
        return ((p[i * 4 + 3].toInt() and 0xFF) shl 24) or ((p[i * 4 + 2].toInt() and 0xFF) shl 16) or
            ((p[i * 4 + 1].toInt() and 0xFF) shl 8) or (p[i * 4].toInt() and 0xFF)
    }

    private fun mix(ca: Int, cb: Int, cd: Int, wa: Float, wb: Float, wd: Float, area: Float, shift: Int) =
        (wa * (ca ushr shift and 0xFF) + wb * (cb ushr shift and 0xFF) + wd * (cd ushr shift and 0xFF)) / area

    // Pixels exactly on an edge shared by two triangles belong to the one where it is a top or left edge, like on the GPU
    private fun topLeft(dx: Float, dy: Float) = dy < 0f || dy == 0f && dx > 0f

    // Evaluated in a canonical endpoint order so both triangles sharing an edge agree exactly on which side a pixel is
    private fun edge(v: FloatArray, i: Int, j: Int, x: Float, y: Float): Float {
        val ax = v[i * 4]; val ay = v[i * 4 + 1]
        val bx = v[j * 4]; val by = v[j * 4 + 1]
        return if (ax < bx || ax == bx && ay < by) (bx - ax) * (y - ay) - (by - ay) * (x - ax)
        else -((ax - bx) * (y - by) - (ay - by) * (x - bx))
    }
}
