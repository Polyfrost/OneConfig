package org.polyfrost.compose.ui.text.style

enum class TextAlign(internal val factor: Float) { Start(0f), Center(0.5f), End(1f) }

enum class TextOverflow { Visible, Clip, Ellipsis }

data class BaselineShift(val multiplier: Float) {
    companion object {
        @JvmField val Superscript = BaselineShift(0.5f)
        @JvmField val Subscript = BaselineShift(-0.5f)
        @JvmField val None = BaselineShift(0f)
    }
}
