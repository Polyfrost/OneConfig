package org.polyfrost.compose.render

import org.jetbrains.annotations.ApiStatus
import kotlin.math.roundToInt

class PolyColor(val argb: Int) {
    val alpha: Int get() = (argb ushr 24) and 0xFF
    val red: Int get() = (argb ushr 16) and 0xFF
    val green: Int get() = (argb ushr 8) and 0xFF
    val blue: Int get() = argb and 0xFF

    val alphaF: Float get() = alpha / 255f
    val redF: Float get() = red / 255f
    val greenF: Float get() = green / 255f
    val blueF: Float get() = blue / 255f

    fun withAlpha(a: Int): PolyColor = PolyColor((argb and 0x00FFFFFF) or ((a.coerceIn(0, 255)) shl 24))
    fun withAlpha(a: Float): PolyColor = withAlpha((a * 255f).roundToInt())

    fun withRed(r: Int): PolyColor = PolyColor((argb and 0xFF00FFFF.toInt()) or ((r.coerceIn(0, 255)) shl 16))
    fun withGreen(g: Int): PolyColor = PolyColor((argb and 0xFFFF00FF.toInt()) or ((g.coerceIn(0, 255)) shl 8))
    fun withBlue(b: Int): PolyColor = PolyColor((argb and 0xFFFFFF00.toInt()) or (b.coerceIn(0, 255)))

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

    companion object {
        val TRANSPARENT = PolyColor(0x00000000)
        val WHITE = PolyColor(0xFFFFFFFF.toInt())
        val BLACK = PolyColor(0xFF000000.toInt())
        val RED = PolyColor(0xFFFF4444.toInt())
        val GREEN = PolyColor(0xFF44FF44.toInt())
        val BLUE = PolyColor(0xFF4488FF.toInt())
        val YELLOW = PolyColor(0xFFFFFF44.toInt())
        val ORANGE = PolyColor(0xFFFF8844.toInt())
        val PURPLE = PolyColor(0xFFAA44FF.toInt())
        val CYAN = PolyColor(0xFF44FFFF.toInt())
        val PINK = PolyColor(0xFFFF44AA.toInt())
        val GRAY = PolyColor(0xFF888888.toInt())
        val DARK_GRAY = PolyColor(0xFF444444.toInt())
        val DARKER_GRAY = PolyColor(0xFF222222.toInt())
        val LIGHT_GRAY = PolyColor(0xFFCCCCCC.toInt())
        val LIGHTER_GRAY = PolyColor(0xFFEEEEEE.toInt())

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
    }
}