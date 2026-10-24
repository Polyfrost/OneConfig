package org.polyfrost.compose.ui.unit

@JvmInline
value class Dp(val value: Float) : Comparable<Dp> {
    operator fun plus(other: Dp) = Dp(value + other.value)
    operator fun minus(other: Dp) = Dp(value - other.value)
    operator fun unaryMinus() = Dp(-value)
    operator fun times(other: Float) = Dp(value * other)
    operator fun times(other: Int) = Dp(value * other)
    operator fun div(other: Float) = Dp(value / other)
    operator fun div(other: Int) = Dp(value / other)
    operator fun div(other: Dp) = value / other.value
    override fun compareTo(other: Dp) = value.compareTo(other.value)
    override fun toString() = if (value.isNaN()) "Dp.Unspecified" else "$value.dp"

    companion object {
        val Hairline = Dp(0f)
        val Infinity = Dp(Float.POSITIVE_INFINITY)
        val Unspecified = Dp(Float.NaN)
    }
}

inline val Int.dp get() = Dp(toFloat())
inline val Float.dp get() = Dp(this)
inline val Double.dp get() = Dp(toFloat())

operator fun Float.times(other: Dp) = Dp(this * other.value)
operator fun Int.times(other: Dp) = Dp(this * other.value)

val Dp.isSpecified get() = !value.isNaN()

fun min(a: Dp, b: Dp) = if (a <= b) a else b
fun max(a: Dp, b: Dp) = if (a >= b) a else b
fun Dp.coerceAtLeast(min: Dp) = if (this < min) min else this
fun Dp.coerceAtMost(max: Dp) = if (this > max) max else this
fun Dp.coerceIn(min: Dp, max: Dp) = coerceAtLeast(min).coerceAtMost(max)

data class DpOffset(val x: Dp, val y: Dp) {
    companion object {
        val Zero = DpOffset(0.dp, 0.dp)
    }
}

data class DpSize(val width: Dp, val height: Dp) {
    companion object {
        val Zero = DpSize(0.dp, 0.dp)
    }
}

@JvmInline
value class TextUnit(val value: Float) {
    operator fun times(other: Float) = TextUnit(value * other)
    operator fun div(other: Float) = TextUnit(value / other)
    override fun toString() = if (value.isNaN()) "TextUnit.Unspecified" else "$value.sp"

    companion object {
        val Unspecified = TextUnit(Float.NaN)
    }
}

inline val Int.sp get() = TextUnit(toFloat())
inline val Float.sp get() = TextUnit(this)
inline val Double.sp get() = TextUnit(toFloat())

val TextUnit.isSpecified get() = !value.isNaN()
