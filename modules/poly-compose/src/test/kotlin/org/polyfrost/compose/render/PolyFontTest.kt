package org.polyfrost.compose.render

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows

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

    @Test
    fun nonFontResourcesAreRejected() {
        assertFalse(FontManager.loadFromResource("org/polyfrost/compose/render/FontManager.class", "not-a-font"))
        assertThrows<IllegalArgumentException> { FontManager.register("not-a-font", ByteArray(64)) }
        assertNull(FontManager.fontData("not-a-font"))
        val truncated = ByteArray(12).also { it[1] = 1 }
        assertThrows<IllegalArgumentException> { FontManager.register("not-a-font", truncated) }
    }
}
