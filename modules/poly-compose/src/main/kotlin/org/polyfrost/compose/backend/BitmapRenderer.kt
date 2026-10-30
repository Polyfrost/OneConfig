package org.polyfrost.compose.backend

import androidx.compose.ui.graphics.ImageBitmapConfig
import kotlin.math.ceil
import kotlin.math.floor
import kotlin.math.max
import kotlin.math.min
import org.polyfrost.compose.render.PolyBitmap
import org.polyfrost.compose.render.PolyRenderer
import org.polyfrost.compose.render.PolyTessellator

/**
 * Rasterizes the tessellator's output into a [PolyImageBitmap] on the CPU, backing `Canvas(ImageBitmap)`.
 * Compose uses it to cache vector images, which are only redrawn when they change.
 */
internal class BitmapRenderer(private val target: PolyImageBitmap) : PolyTessellator() {
    // Text, gradients and images need GPU textures, so only plain geometry is drawn
    override val cpuOnly get() = true

    // Alpha8 images are drawn tinted, which the backend does by multiplying, so coverage is stored as white
    private val mask = target.config == ImageBitmapConfig.Alpha8
    private var depth = 0

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
            val sa = mix(c[a], c[b], c[d], wa, wb, wd, area, 24) / 255f
            if (sa <= 0f) continue
            val o = (y * w + x) * 4
            val da = (px[o + 3].toInt() and 0xFF) / 255f
            if (erasing) {
                px[o + 3] = (da * (1f - sa) * 255f + 0.5f).toInt().toByte()
                continue
            }
            val outA = sa + da * (1f - sa)
            for (ch in 0..2) {
                val s = if (mask) sa else mix(c[a], c[b], c[d], wa, wb, wd, area, 16 - ch * 8) / 255f * (if (premultiplied) 1f else sa)
                val dc = (px[o + ch].toInt() and 0xFF) / 255f * da
                px[o + ch] = ((s + dc * (1f - sa)) / outA * 255f + 0.5f).toInt().coerceIn(0, 255).toByte()
            }
            px[o + 3] = (outA * 255f + 0.5f).toInt().toByte()
        }
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
