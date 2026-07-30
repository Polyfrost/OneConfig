package org.polyfrost.compose.render

import kotlin.math.roundToInt

/**
 * A colour. Named colours such as [WHITE] hand out a fresh instance on every access, so they can
 * safely be used as config defaults without one option's change leaking into another.
 */
class PolyColor @JvmOverloads constructor(
    argb: Int = 0xFFFFFFFF.toInt(),
    var chroma: Boolean = false,
    var chromaSpeed: Float = 1f,
) {
    private var staticArgb: Int = argb

    var argb: Int
        get() = if (chroma) chromaArgb(staticArgb, chromaSpeed) else staticArgb
        set(value) {
            staticArgb = value
        }

    val rawArgb: Int get() = staticArgb

    val alpha: Int get() = (argb ushr 24) and 0xFF
    val red: Int get() = (argb ushr 16) and 0xFF
    val green: Int get() = (argb ushr 8) and 0xFF
    val blue: Int get() = argb and 0xFF

    val alphaF: Float get() = alpha / 255f
    val redF: Float get() = red / 255f
    val greenF: Float get() = green / 255f
    val blueF: Float get() = blue / 255f

    fun withAlpha(a: Int): PolyColor = PolyColor((argb and 0x00FFFFFF) or ((a.coerceIn(0, 255)) shl 24), chroma, chromaSpeed)
    fun withAlpha(a: Float): PolyColor = withAlpha((a * 255f).roundToInt())

    fun withRed(r: Int): PolyColor = PolyColor((argb and 0xFF00FFFF.toInt()) or ((r.coerceIn(0, 255)) shl 16), chroma, chromaSpeed)
    fun withGreen(g: Int): PolyColor = PolyColor((argb and 0xFFFF00FF.toInt()) or ((g.coerceIn(0, 255)) shl 8), chroma, chromaSpeed)
    fun withBlue(b: Int): PolyColor = PolyColor((argb and 0xFFFFFF00.toInt()) or (b.coerceIn(0, 255)), chroma, chromaSpeed)

    @JvmOverloads
    fun withChroma(chroma: Boolean, speed: Float = chromaSpeed): PolyColor = PolyColor(staticArgb, chroma, speed)

    fun multiplyAlpha(factor: Float): PolyColor = withAlpha((alpha * factor).roundToInt().coerceIn(0, 255))

    fun lighten(amount: Float): PolyColor {
        val f = amount.coerceIn(0f, 1f)
        return rgb(
            (red + (255 - red) * f).roundToInt(),
            (green + (255 - green) * f).roundToInt(),
            (blue + (255 - blue) * f).roundToInt(),
        ).withAlpha(alpha)
    }

    fun darken(amount: Float): PolyColor {
        val f = amount.coerceIn(0f, 1f)
        return rgb(
            (red * (1f - f)).roundToInt(),
            (green * (1f - f)).roundToInt(),
            (blue * (1f - f)).roundToInt(),
        ).withAlpha(alpha)
    }

    fun lerp(other: PolyColor, t: Float): PolyColor {
        val f = t.coerceIn(0f, 1f)
        fun mix(a: Int, b: Int) = (a + (b - a) * f).roundToInt()
        return PolyColor(
            argb(
                mix(alpha, other.alpha),
                mix(red, other.red),
                mix(green, other.green),
                mix(blue, other.blue)
            ).argb
        )
    }

    fun invert(): PolyColor = rgb(255 - red, 255 - green, 255 - blue).withAlpha(alpha)

    fun toSkiaColor(): Int = argb

    override fun toString(): String = "PolyColor(#%08X)".format(argb.toLong() and 0xFFFFFFFFL)

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is PolyColor) return false
        return staticArgb == other.staticArgb && chroma == other.chroma && chromaSpeed == other.chromaSpeed
    }

    override fun hashCode(): Int {
        var result = staticArgb
        result = 31 * result + chroma.hashCode()
        result = 31 * result + chromaSpeed.hashCode()
        return result
    }

    companion object {
        const val CHROMA_CYCLE_SECONDS = 10.0

        // These hand out a new instance on every access: they are commonly used as the default of
        // several options at once, and a shared instance would let a change to one leak into the rest.
        val TRANSPARENT get() = PolyColor(0x00000000)
        val WHITE get() = PolyColor(0xFFFFFFFF.toInt())
        val BLACK get() = PolyColor(0xFF000000.toInt())
        val RED get() = PolyColor(0xFFFF4444.toInt())
        val GREEN get() = PolyColor(0xFF44FF44.toInt())
        val BLUE get() = PolyColor(0xFF4488FF.toInt())
        val YELLOW get() = PolyColor(0xFFFFFF44.toInt())
        val ORANGE get() = PolyColor(0xFFFF8844.toInt())
        val PURPLE get() = PolyColor(0xFFAA44FF.toInt())
        val CYAN get() = PolyColor(0xFF44FFFF.toInt())
        val PINK get() = PolyColor(0xFFFF44AA.toInt())
        val GRAY get() = PolyColor(0xFF888888.toInt())
        val DARK_GRAY get() = PolyColor(0xFF444444.toInt())
        val DARKER_GRAY get() = PolyColor(0xFF222222.toInt())
        val LIGHT_GRAY get() = PolyColor(0xFFCCCCCC.toInt())
        val LIGHTER_GRAY get() = PolyColor(0xFFEEEEEE.toInt())

        fun rgb(r: Int, g: Int, b: Int) =
            PolyColor(0xFF000000.toInt() or ((r and 0xFF) shl 16) or ((g and 0xFF) shl 8) or (b and 0xFF))

        fun argb(a: Int, r: Int, g: Int, b: Int) =
            PolyColor(((a and 0xFF) shl 24) or ((r and 0xFF) shl 16) or ((g and 0xFF) shl 8) or (b and 0xFF))

        fun rgba(r: Int, g: Int, b: Int, a: Int) = argb(a, r, g, b)
        fun hex(value: Long) = PolyColor(value.toInt())
        fun hex(value: Int) = PolyColor(value)
        fun hex(value: String): PolyColor {
            val s = value.trimStart('#')
            return when (s.length) {
                6 -> PolyColor(0xFF000000.toInt() or s.toLong(16).toInt())
                8 -> PolyColor(s.toLong(16).toInt())
                3 -> {
                    val r = s[0].digitToInt(16)
                    val g = s[1].digitToInt(16)
                    val b = s[2].digitToInt(16)
                    rgb(r or (r shl 4), g or (g shl 4), b or (b shl 4))
                }

                else -> error("Invalid hex colour: $value")
            }
        }

        fun hsv(h: Float, s: Float, v: Float, a: Float = 1f): PolyColor {
            val hh = h % 360f
            val c = v * s
            val x = c * (1f - Math.abs(((hh / 60f) % 2f) - 1f))
            val m = v - c
            val (r1, g1, b1) = when {
                hh < 60f -> Triple(c, x, 0f)
                hh < 120f -> Triple(x, c, 0f)
                hh < 180f -> Triple(0f, c, x)
                hh < 240f -> Triple(0f, x, c)
                hh < 300f -> Triple(x, 0f, c)
                else -> Triple(c, 0f, x)
            }
            return argb(
                (a * 255).roundToInt(),
                ((r1 + m) * 255).roundToInt(),
                ((g1 + m) * 255).roundToInt(),
                ((b1 + m) * 255).roundToInt(),
            )
        }

        fun hsl(h: Float, s: Float, l: Float, a: Float = 1f): PolyColor {
            val c = (1f - Math.abs(2f * l - 1f)) * s
            val x = c * (1f - Math.abs(((h / 60f) % 2f) - 1f))
            val m = l - c / 2f
            val (r1, g1, b1) = when {
                h < 60f -> Triple(c, x, 0f)
                h < 120f -> Triple(x, c, 0f)
                h < 180f -> Triple(0f, c, x)
                h < 240f -> Triple(0f, x, c)
                h < 300f -> Triple(x, 0f, c)
                else -> Triple(c, 0f, x)
            }
            return argb(
                (a * 255).roundToInt(),
                ((r1 + m) * 255).roundToInt(),
                ((g1 + m) * 255).roundToInt(),
                ((b1 + m) * 255).roundToInt(),
            )
        }

        fun lerp(from: PolyColor, to: PolyColor, t: Float) = from.lerp(to, t)

        private fun chromaArgb(argb: Int, speed: Float): Int {
            val alpha = (argb ushr 24) and 0xFF
            val hsb = rgbToHsb((argb ushr 16) and 0xFF, (argb ushr 8) and 0xFF, argb and 0xFF)
            val elapsedSeconds = System.nanoTime() / 1_000_000_000.0
            val hue = ((hsb[0] + elapsedSeconds * speed.coerceAtLeast(0f) / CHROMA_CYCLE_SECONDS) % 1.0).toFloat()
            return hsv(hue * 360f, hsb[1], hsb[2], alpha / 255f).rawArgb
        }

        private fun rgbToHsb(red: Int, green: Int, blue: Int): FloatArray {
            val r = red / 255f
            val g = green / 255f
            val b = blue / 255f
            val max = maxOf(r, g, b)
            val min = minOf(r, g, b)
            val delta = max - min
            val hue = when {
                delta == 0f -> 0f
                max == r -> (((g - b) / delta).mod(6f)) / 6f
                max == g -> (((b - r) / delta) + 2f) / 6f
                else -> (((r - g) / delta) + 4f) / 6f
            }
            val saturation = if (max == 0f) 0f else delta / max
            return floatArrayOf(hue, saturation, max)
        }
    }
}
