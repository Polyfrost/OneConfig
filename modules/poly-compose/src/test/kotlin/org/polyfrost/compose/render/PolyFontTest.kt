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

    private fun sfnt(vararg tables: Pair<String, ByteArray>): ByteArray {
        val directory = 12 + tables.size * 16
        val out = ByteArray(directory + tables.sumOf { it.second.size })
        out[1] = 1
        out[5] = tables.size.toByte()
        var offset = directory
        tables.forEachIndexed { i, (tag, data) ->
            val record = 12 + i * 16
            tag.toByteArray(Charsets.ISO_8859_1).copyInto(out, record)
            for (b in 0 until 4) {
                out[record + 8 + b] = (offset shr (24 - b * 8)).toByte()
                out[record + 12 + b] = (data.size shr (24 - b * 8)).toByte()
            }
            data.copyInto(out, offset)
            offset += data.size
        }
        return out
    }

    private val minimalFont = arrayOf(
        "cmap" to ByteArray(4), "head" to ByteArray(54), "hhea" to ByteArray(36).also { it[35] = 1 }, "hmtx" to ByteArray(4),
        "maxp" to ByteArray(6), "loca" to ByteArray(2), "glyf" to ByteArray(0),
    )

    private fun replaced(tag: String, data: ByteArray) = minimalFont.map { if (it.first == tag) tag to data else it }.toTypedArray()

    @Test
    fun damagedFontsAreRejected() {
        FontManager.register("synthetic", sfnt(*minimalFont))
        assertThrows<IllegalArgumentException> { FontManager.register("synthetic", sfnt(*replaced("head", ByteArray(53)))) }
        assertThrows<IllegalArgumentException> { FontManager.register("synthetic", sfnt(*replaced("maxp", ByteArray(4)))) }
        val farSubtable = byteArrayOf(0, 0, 0, 1, 0, 3, 0, 1, 0x7f, -1, -1, -1)
        assertThrows<IllegalArgumentException> { FontManager.register("synthetic", sfnt(*replaced("cmap", farSubtable))) }
        assertThrows<IllegalArgumentException> { FontManager.register("synthetic", sfnt(*replaced("hhea", ByteArray(36)))) }
        assertThrows<IllegalArgumentException> { FontManager.register("synthetic", sfnt(*replaced("loca", byteArrayOf(0, 9)))) }
    }
}
