package org.polyfrost.compose.render

data class PolyFont(val name: String, val size: Float) {
    class Metrics @JvmOverloads constructor(
        val ascent: Float, val descent: Float, val leading: Float, val underlinePosition: Float, val underlineThickness: Float,
        val strikeoutPosition: Float = ascent / 3f, val strikeoutThickness: Float = underlineThickness,
    ) {
        val spacing: Float get() = descent - ascent + leading
    }

    interface Engine {
        fun measureTextWidth(font: PolyFont, text: String): Float
        fun metrics(font: PolyFont): Metrics
    }

    val metrics: Metrics by lazy(LazyThreadSafetyMode.NONE) { engine.metrics(this) }

    fun measureTextWidth(text: String): Float = engine.measureTextWidth(this, text)

    companion object {
        @JvmStatic
        fun default(size: Float) = PolyFont(FontManager.DEFAULT_KEY, size)

        @JvmStatic
        var engine: Engine = object : Engine {
            override fun measureTextWidth(font: PolyFont, text: String) = error("PolyFont.engine is not installed")
            override fun metrics(font: PolyFont) = error("PolyFont.engine is not installed")
        }
    }

}
