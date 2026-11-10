package org.polyfrost.compose.render

import kotlin.math.roundToInt

/**
 * A colour where named colours such as [WHITE] hand out a fresh instance on every access so they
 * can safely be used as config defaults without one option's change leaking into another
 */
public class PolyColor @JvmOverloads constructor(
    argb: Int = 0xFFFFFFFF.toInt(),
    public var chroma: Boolean = false,
    public var chromaSpeed: Float = 1f,
) {
    private var staticArgb: Int = argb

    public var argb: Int
        get() {
            if (!chroma) return staticArgb
            chromaReads++
            return chromaArgb(staticArgb, chromaSpeed)
        }
        set(value) {
            staticArgb = value
        }

    public val rawArgb: Int get() = staticArgb

    public var gradientEnabled: Boolean = false

    public var gradientArgb: Int = argb

    public val alpha: Int get() = (argb ushr 24) and 0xFF
    public val red: Int get() = (argb ushr 16) and 0xFF
    public val green: Int get() = (argb ushr 8) and 0xFF
    public val blue: Int get() = argb and 0xFF

    public val alphaF: Float get() = alpha / 255f
    public val redF: Float get() = red / 255f
    public val greenF: Float get() = green / 255f
    public val blueF: Float get() = blue / 255f

    public fun withAlpha(a: Int): PolyColor = PolyColor((argb and 0x00FFFFFF) or ((a.coerceIn(0, 255)) shl 24), chroma, chromaSpeed)
    public fun withAlpha(a: Float): PolyColor = withAlpha((a * 255f).roundToInt())

    public fun withRed(r: Int): PolyColor = PolyColor((argb and 0xFF00FFFF.toInt()) or ((r.coerceIn(0, 255)) shl 16), chroma, chromaSpeed)
    public fun withGreen(g: Int): PolyColor = PolyColor((argb and 0xFFFF00FF.toInt()) or ((g.coerceIn(0, 255)) shl 8), chroma, chromaSpeed)
    public fun withBlue(b: Int): PolyColor = PolyColor((argb and 0xFFFFFF00.toInt()) or (b.coerceIn(0, 255)), chroma, chromaSpeed)

    @JvmOverloads
    public fun withChroma(chroma: Boolean, speed: Float = chromaSpeed): PolyColor =
        PolyColor(staticArgb, chroma, speed).also {
            it.gradientArgb = gradientArgb
            it.gradientEnabled = gradientEnabled
        }

    @JvmOverloads
    public fun withGradient(enabled: Boolean, argb: Int = gradientArgb): PolyColor =
        PolyColor(staticArgb, chroma, chromaSpeed).also {
            it.gradientArgb = argb
            it.gradientEnabled = enabled
        }

    public fun multiplyAlpha(factor: Float): PolyColor = withAlpha((alpha * factor).roundToInt().coerceIn(0, 255))

    public fun lighten(amount: Float): PolyColor {
        val f = amount.coerceIn(0f, 1f)
        return rgb(
            (red + (255 - red) * f).roundToInt(),
            (green + (255 - green) * f).roundToInt(),
            (blue + (255 - blue) * f).roundToInt(),
        ).withAlpha(alpha)
    }

    public fun darken(amount: Float): PolyColor {
        val f = amount.coerceIn(0f, 1f)
        return rgb(
            (red * (1f - f)).roundToInt(),
            (green * (1f - f)).roundToInt(),
            (blue * (1f - f)).roundToInt(),
        ).withAlpha(alpha)
    }

    public fun lerp(other: PolyColor, t: Float): PolyColor {
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

    public fun invert(): PolyColor = rgb(255 - red, 255 - green, 255 - blue).withAlpha(alpha)

    public fun toSkiaColor(): Int = argb

    override fun toString(): String = "PolyColor(#%08X)".format(argb.toLong() and 0xFFFFFFFFL)

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is PolyColor) return false
        return staticArgb == other.staticArgb && chroma == other.chroma && chromaSpeed == other.chromaSpeed &&
            gradientEnabled == other.gradientEnabled && (!gradientEnabled || gradientArgb == other.gradientArgb)
    }

    override fun hashCode(): Int {
        var result = staticArgb
        result = 31 * result + chroma.hashCode()
        result = 31 * result + chromaSpeed.hashCode()
        result = 31 * result + gradientEnabled.hashCode()
        if (gradientEnabled) result = 31 * result + gradientArgb
        return result
    }

    public companion object {
        public const val CHROMA_CYCLE_SECONDS: Double = 10.0

        @JvmField
        public var chromaReads: Int = 0

        public val TRANSPARENT: PolyColor get() = PolyColor(0x00000000)
        public val WHITE: PolyColor get() = PolyColor(0xFFFFFFFF.toInt())
        public val BLACK: PolyColor get() = PolyColor(0xFF000000.toInt())
        public val RED: PolyColor get() = PolyColor(0xFFFF4444.toInt())
        public val GREEN: PolyColor get() = PolyColor(0xFF44FF44.toInt())
        public val BLUE: PolyColor get() = PolyColor(0xFF4488FF.toInt())
        public val YELLOW: PolyColor get() = PolyColor(0xFFFFFF44.toInt())
        public val ORANGE: PolyColor get() = PolyColor(0xFFFF8844.toInt())
        public val PURPLE: PolyColor get() = PolyColor(0xFFAA44FF.toInt())
        public val CYAN: PolyColor get() = PolyColor(0xFF44FFFF.toInt())
        public val PINK: PolyColor get() = PolyColor(0xFFFF44AA.toInt())
        public val GRAY: PolyColor get() = PolyColor(0xFF888888.toInt())
        public val DARK_GRAY: PolyColor get() = PolyColor(0xFF444444.toInt())
        public val DARKER_GRAY: PolyColor get() = PolyColor(0xFF222222.toInt())
        public val LIGHT_GRAY: PolyColor get() = PolyColor(0xFFCCCCCC.toInt())
        public val LIGHTER_GRAY: PolyColor get() = PolyColor(0xFFEEEEEE.toInt())

        public fun rgb(r: Int, g: Int, b: Int): PolyColor =
            PolyColor(0xFF000000.toInt() or ((r and 0xFF) shl 16) or ((g and 0xFF) shl 8) or (b and 0xFF))

        public fun argb(a: Int, r: Int, g: Int, b: Int): PolyColor =
            PolyColor(((a and 0xFF) shl 24) or ((r and 0xFF) shl 16) or ((g and 0xFF) shl 8) or (b and 0xFF))

        public fun rgba(r: Int, g: Int, b: Int, a: Int): PolyColor = argb(a, r, g, b)
        public fun hex(value: Long): PolyColor = PolyColor(value.toInt())
        public fun hex(value: Int): PolyColor = PolyColor(value)
        public fun hex(value: String): PolyColor {
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

        public fun hsv(h: Float, s: Float, v: Float, a: Float = 1f): PolyColor {
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

        public fun hsl(h: Float, s: Float, l: Float, a: Float = 1f): PolyColor {
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

        public fun lerp(from: PolyColor, to: PolyColor, t: Float): PolyColor = from.lerp(to, t)

        /** How far chroma at [speed] has turned every hue right now, from 0 to 1 of the colour wheel */
        public fun chromaTurn(speed: Float): Float =
            (System.nanoTime() / 1_000_000_000.0 * speed.coerceAtLeast(0f) / CHROMA_CYCLE_SECONDS % 1.0).toFloat()

        private fun chromaArgb(argb: Int, speed: Float): Int {
            val alpha = (argb ushr 24) and 0xFF
            val hsb = rgbToHsb((argb ushr 16) and 0xFF, (argb ushr 8) and 0xFF, argb and 0xFF)
            val hue = (hsb[0] + chromaTurn(speed)) % 1f
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
