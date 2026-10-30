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

    private fun sfnt(vararg tables: Pair<String, Int>): ByteArray {
        val directory = 12 + tables.size * 16
        val out = ByteArray(directory + tables.sumOf { it.second })
        out[1] = 1
        out[5] = tables.size.toByte()
        var offset = directory
        tables.forEachIndexed { i, (tag, length) ->
            val record = 12 + i * 16
            tag.toByteArray(Charsets.ISO_8859_1).copyInto(out, record)
            for (b in 0 until 4) {
                out[record + 8 + b] = (offset shr (24 - b * 8)).toByte()
                out[record + 12 + b] = (length shr (24 - b * 8)).toByte()
            }
            offset += length
        }
        return out
    }

    @Test
    fun fontsWithTruncatedTablesAreRejected() {
        val tables = arrayOf("cmap" to 4, "head" to 54, "hhea" to 36, "hmtx" to 0, "glyf" to 0, "loca" to 0)
        FontManager.register("synthetic", sfnt(*tables))
        assertThrows<IllegalArgumentException> { FontManager.register("synthetic", sfnt(*tables.map { if (it.first == "head") "head" to 53 else it }.toTypedArray())) }
        assertThrows<IllegalArgumentException> { FontManager.register("synthetic", sfnt(*tables, "maxp" to 4)) }
    }
}
