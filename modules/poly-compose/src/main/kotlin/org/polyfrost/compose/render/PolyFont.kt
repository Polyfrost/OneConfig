package org.polyfrost.compose.render

public data class PolyFont(val name: String, val size: Float) {
    public class Metrics @JvmOverloads constructor(
        public val ascent: Float, public val descent: Float, public val leading: Float, public val underlinePosition: Float, public val underlineThickness: Float,
        public val strikeoutPosition: Float = ascent / 3f, public val strikeoutThickness: Float = underlineThickness,
    ) {
        public val spacing: Float get() = descent - ascent + leading
    }

    public interface Engine {
        public fun measureTextWidth(font: PolyFont, text: String): Float
        public fun metrics(font: PolyFont): Metrics
    }

    private var cachedMetrics: Metrics? = null
    private var metricsRevision = -1

    val metrics: Metrics
        get() {
            val revision = FontManager.revision
            cachedMetrics?.let { if (metricsRevision == revision) return it }
            return engine.metrics(this).also { cachedMetrics = it; metricsRevision = revision }
        }

    public fun measureTextWidth(text: String): Float = engine.measureTextWidth(this, text)

    public companion object {
        @JvmStatic
        public fun default(size: Float): PolyFont = PolyFont(FontManager.DEFAULT_KEY, size)

        @JvmStatic
        public var engine: Engine = object : Engine {
            override fun measureTextWidth(font: PolyFont, text: String) = error("PolyFont.engine is not installed")
            override fun metrics(font: PolyFont) = error("PolyFont.engine is not installed")
        }
    }

}
