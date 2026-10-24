package org.polyfrost.compose.ui.graphics

import org.polyfrost.compose.render.PolyColor
import kotlin.math.cbrt
import kotlin.math.pow
import kotlin.math.roundToInt

typealias Color = PolyColor

fun Color(color: Long): PolyColor = PolyColor(color.toInt())

fun Color(red: Int, green: Int, blue: Int, alpha: Int = 0xFF): PolyColor = PolyColor.argb(alpha, red, green, blue)

fun Color(red: Float, green: Float, blue: Float, alpha: Float = 1f): PolyColor =
    PolyColor.argb((alpha * 255f).roundToInt(), (red * 255f).roundToInt(), (green * 255f).roundToInt(), (blue * 255f).roundToInt())

fun PolyColor.copy(alpha: Float = alphaF, red: Float = redF, green: Float = greenF, blue: Float = blueF): PolyColor =
    Color(red, green, blue, alpha).withChroma(chroma, chromaSpeed)

fun PolyColor.toArgb(): Int = argb

val PolyColor.isSpecified get() = this != PolyColor.Unspecified
val PolyColor.isUnspecified get() = this == PolyColor.Unspecified

fun PolyColor.takeOrElse(block: () -> PolyColor) = if (isSpecified) this else block()

private fun linear(c: Float) = if (c <= 0.04045f) c / 12.92f else ((c + 0.055f) / 1.055f).toDouble().pow(2.4).toFloat()

private fun gamma(c: Float) = if (c <= 0.0031308f) c * 12.92f else (1.055 * c.toDouble().pow(1 / 2.4) - 0.055).toFloat()

private fun toOklab(c: PolyColor): FloatArray {
    val r = linear(c.redF); val g = linear(c.greenF); val b = linear(c.blueF)
    val l = cbrt(0.4122214708f * r + 0.5363325363f * g + 0.0514459929f * b)
    val m = cbrt(0.2119034982f * r + 0.6806995451f * g + 0.1073969566f * b)
    val s = cbrt(0.0883024619f * r + 0.2817188376f * g + 0.6299787005f * b)
    return floatArrayOf(
        0.2104542553f * l + 0.7936177850f * m - 0.0040720468f * s,
        1.9779984951f * l - 2.4285922050f * m + 0.4505937099f * s,
        0.0259040371f * l + 0.7827717662f * m - 0.8086757660f * s,
    )
}

fun lerp(start: PolyColor, stop: PolyColor, fraction: Float): PolyColor {
    if (fraction <= 0f) return start
    if (fraction >= 1f) return stop
    val x = toOklab(start); val y = toOklab(stop)
    val t = fraction
    val L = x[0] + (y[0] - x[0]) * t; val A = x[1] + (y[1] - x[1]) * t; val B = x[2] + (y[2] - x[2]) * t
    val l = (L + 0.3963377774f * A + 0.2158037573f * B).let { it * it * it }
    val m = (L - 0.1055613458f * A - 0.0638541728f * B).let { it * it * it }
    val s = (L - 0.0894841775f * A - 1.2914855480f * B).let { it * it * it }
    fun ch(v: Float) = (gamma(v.coerceIn(0f, 1f)) * 255f).roundToInt().coerceIn(0, 255)
    val alpha = (start.alpha + (stop.alpha - start.alpha) * t).roundToInt()
    return PolyColor(
        (alpha shl 24) or (ch(4.0767416621f * l - 3.3077115913f * m + 0.2309699292f * s) shl 16) or
            (ch(-1.2684380046f * l + 2.6097574011f * m - 0.3413193965f * s) shl 8) or
            ch(-0.0041960863f * l - 0.7034186147f * m + 1.7076147010f * s)
    )
}
