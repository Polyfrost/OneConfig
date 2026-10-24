package org.polyfrost.compose.ui.unit

import kotlin.math.roundToInt

open class Density(val density: Float, val fontScale: Float = 1f) {
    fun Dp.toPx() = value * density
    fun Dp.roundToPx() = toPx().roundToInt()
    fun TextUnit.toPx() = value * density * fontScale
    fun Int.toDp() = Dp(this / density)
    fun Float.toDp() = Dp(this / density)
    fun DpSize.toSize() = org.polyfrost.compose.ui.geometry.Size(width.toPx(), height.toPx())
}
