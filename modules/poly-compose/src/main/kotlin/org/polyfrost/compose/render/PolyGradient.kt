package org.polyfrost.compose.render

import org.jetbrains.skia.FilterTileMode
import org.jetbrains.skia.GradientStyle
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
                colors.map { it.argb }.toIntArray(),
                stops?.toFloatArray(),
                GradientStyle(tileMode, true, null),
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
                colors.map { it.argb }.toIntArray(),
                stops?.toFloatArray(),
                GradientStyle(tileMode, true, null),
            )
    }

    companion object {
        fun horizontal(vararg colors: PolyColor) = Linear(colors.toList(), angle = 0f)
        fun vertical(vararg colors: PolyColor) = Linear(colors.toList(), angle = 90f)
        fun diagonal(vararg colors: PolyColor) = Linear(colors.toList(), angle = 45f)
        fun radial(vararg colors: PolyColor) = Radial(colors.toList())

        fun horizontal(vararg colors: Int) = horizontal(*colors.map { PolyColor(it) }.toTypedArray())
        fun vertical(vararg colors: Int) = vertical(*colors.map { PolyColor(it) }.toTypedArray())
    }
}