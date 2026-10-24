package org.polyfrost.compose.animation.core

fun interface Easing {
    fun transform(fraction: Float): Float
}

class CubicBezierEasing(private val a: Float, private val b: Float, private val c: Float, private val d: Float) : Easing {
    private fun bezier(p1: Float, p2: Float, t: Float): Float {
        val u = 1f - t
        return 3f * u * u * t * p1 + 3f * u * t * t * p2 + t * t * t
    }

    override fun transform(fraction: Float): Float {
        if (fraction <= 0f) return 0f
        if (fraction >= 1f) return 1f
        var lo = 0f
        var hi = 1f
        repeat(24) {
            val mid = (lo + hi) / 2f
            if (bezier(a, c, mid) < fraction) lo = mid else hi = mid
        }
        return bezier(b, d, (lo + hi) / 2f)
    }

    override fun equals(other: Any?) = other is CubicBezierEasing && a == other.a && b == other.b && c == other.c && d == other.d
    override fun hashCode() = ((a.hashCode() * 31 + b.hashCode()) * 31 + c.hashCode()) * 31 + d.hashCode()
}

val LinearEasing = Easing { it }
val FastOutSlowInEasing = CubicBezierEasing(0.4f, 0f, 0.2f, 1f)
val LinearOutSlowInEasing = CubicBezierEasing(0f, 0f, 0.2f, 1f)
val FastOutLinearInEasing = CubicBezierEasing(0.4f, 0f, 1f, 1f)

val Ease = CubicBezierEasing(0.25f, 0.1f, 0.25f, 1f)
val EaseIn = CubicBezierEasing(0.42f, 0f, 1f, 1f)
val EaseOut = CubicBezierEasing(0f, 0f, 0.58f, 1f)
val EaseInOut = CubicBezierEasing(0.42f, 0f, 0.58f, 1f)
val EaseInQuad = CubicBezierEasing(0.11f, 0f, 0.5f, 0f)
val EaseOutQuad = CubicBezierEasing(0.5f, 1f, 0.89f, 1f)
val EaseInOutQuad = CubicBezierEasing(0.45f, 0f, 0.55f, 1f)
val EaseInCubic = CubicBezierEasing(0.32f, 0f, 0.67f, 0f)
val EaseOutCubic = CubicBezierEasing(0.33f, 1f, 0.68f, 1f)
val EaseInOutCubic = CubicBezierEasing(0.65f, 0f, 0.35f, 1f)
val EaseInExpo = CubicBezierEasing(0.7f, 0f, 0.84f, 0f)
val EaseOutExpo = CubicBezierEasing(0.16f, 1f, 0.3f, 1f)
val EaseInOutExpo = CubicBezierEasing(0.87f, 0f, 0.13f, 1f)
val EaseInBack = CubicBezierEasing(0.36f, 0f, 0.66f, -0.56f)
val EaseOutBack = CubicBezierEasing(0.34f, 1.56f, 0.64f, 1f)
val EaseInOutBack = CubicBezierEasing(0.68f, -0.6f, 0.32f, 1.6f)
