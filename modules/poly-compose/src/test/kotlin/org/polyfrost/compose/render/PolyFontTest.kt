package org.polyfrost.compose.render

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class PolyFontTest {
    @Test
    fun metricsRefreshWhenFontsChange() {
        var ascent = -8f
        PolyFont.engine = object : PolyFont.Engine {
            override fun measureTextWidth(font: PolyFont, text: String) = 0f
            override fun metrics(font: PolyFont) = PolyFont.Metrics(ascent, 2f, 0f, 1f, 1f)
        }
        val font = PolyFont("metrics", 10f)
        assertEquals(-8f, font.metrics.ascent)
        ascent = -9f
        assertEquals(-8f, font.metrics.ascent)
        FontManager.invalidateMetrics()
        assertEquals(-9f, font.metrics.ascent)
    }
}
