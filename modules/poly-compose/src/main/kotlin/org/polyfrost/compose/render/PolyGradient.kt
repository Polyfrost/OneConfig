package org.polyfrost.compose.render

import org.jetbrains.skia.FilterTileMode
import org.jetbrains.skia.Color4f
import org.jetbrains.skia.Gradient
import org.jetbrains.skia.Shader
import kotlin.math.cos
import kotlin.math.sin

sealed class PolyGradient {
    abstract fun toShader(x: Float, y: Float, w: Float, h: Float): Shader

    data class Linear(
        val colors: List<PolyColor>,
        val stops: List<Float>? = null,
        val angle: Float = 0f,
        val tileMode: FilterTileMode = FilterTileMode.CLAMP,
    ) : PolyGradient() {
        override fun toShader(x: Float, y: Float, w: Float, h: Float): Shader {
            val rad = Math.toRadians(angle.toDouble())
            val cx = x + w / 2f
            val cy = y + h / 2f
            val hw = (w / 2f * cos(rad)).toFloat()
            val hh = (h / 2f * sin(rad)).toFloat()
            return Shader.makeLinearGradient(
                cx - hw, cy - hh, cx + hw, cy + hh,
                makeGradient(colors, stops, tileMode),
            )
        }
    }

    data class Radial(
        val colors: List<PolyColor>,
        val stops: List<Float>? = null,
        val centerX: Float = 0.5f,
        val centerY: Float = 0.5f,
        val radius: Float = 0.5f,
        val tileMode: FilterTileMode = FilterTileMode.CLAMP,
    ) : PolyGradient() {
        override fun toShader(x: Float, y: Float, w: Float, h: Float): Shader =
            Shader.makeRadialGradient(
                x + centerX * w, y + centerY * h,
                radius * minOf(w, h),
                makeGradient(colors, stops, tileMode),
            )
    }

    companion object {
        private fun makeGradient(
            colors: List<PolyColor>,
            stops: List<Float>?,
            tileMode: FilterTileMode,
        ) = Gradient(
            Gradient.Colors(
                colors.map { Color4f(it.argb) }.toTypedArray(),
                stops?.toFloatArray(),
                tileMode,
            ),
        )

        fun horizontal(vararg colors: PolyColor) = Linear(colors.toList(), angle = 0f)
        fun vertical(vararg colors: PolyColor) = Linear(colors.toList(), angle = 90f)
        fun diagonal(vararg colors: PolyColor) = Linear(colors.toList(), angle = 45f)
        fun radial(vararg colors: PolyColor) = Radial(colors.toList())

        fun horizontal(vararg colors: Int) = horizontal(*colors.map { PolyColor(it) }.toTypedArray())
        fun vertical(vararg colors: Int) = vertical(*colors.map { PolyColor(it) }.toTypedArray())
    }
}
