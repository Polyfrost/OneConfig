package org.polyfrost.compose.render

import kotlin.math.abs
import kotlin.math.pow
import kotlin.math.roundToInt

object PolyMaskGamma {
    private val mac = System.getProperty("os.name").startsWith("Mac")

    private val tables = Array(8) { table(scale255(it), if (mac) 0f else 0.5f) }

    fun bucket(argb: Int): Int {
        var r = canonical(argb ushr 16 and 0xFF)
        var g = canonical(argb ushr 8 and 0xFF)
        var b = canonical(argb and 0xFF)
        if (mac) {
            r = canonical(r / 2)
            g = canonical(g / 2)
            b = canonical(b / 2)
        }
        return ((r * 54 + g * 183 + b * 19) shr 8) ushr 5
    }

    fun apply(r: PolyGlyphPage.Raster, bucket: Int): PolyGlyphPage.Raster {
        val t = tables[bucket]
        return PolyGlyphPage.Raster(r.offX, r.offY, r.w, r.h, ByteArray(r.coverage.size) { t[r.coverage[it].toInt() and 0xFF] })
    }

    fun table(srcI: Int, contrast: Float): ByteArray {
        val src = srcI / 255f
        val linSrc = toLuma(src)
        val dst = 1f - src
        val linDst = toLuma(dst)
        val adjusted = contrast * linDst
        return ByteArray(256) { i ->
            val raw = i / 255f
            val srca = raw + (1f - raw) * adjusted * raw
            val v = if (abs(src - dst) < 1f / 256f) srca
            else (fromLuma(linSrc * srca + (1f - srca) * linDst) - dst) / (src - dst)
            (255f * v).roundToInt().coerceIn(0, 255).toByte()
        }
    }

    fun scale255(base: Int): Int {
        val v = base shl 5
        return v or (v ushr 3) or (v ushr 6)
    }

    private fun canonical(c: Int) = scale255(c ushr 5)

    private fun toLuma(v: Float) = if (v <= 0.04045f) v / 12.92f else ((v + 0.055f) / 1.055f).pow(2.4f)

    private fun fromLuma(v: Float) = if (v <= 0.0031308f) v * 12.92f else 1.055f * v.pow(1f / 2.4f) - 0.055f
}
