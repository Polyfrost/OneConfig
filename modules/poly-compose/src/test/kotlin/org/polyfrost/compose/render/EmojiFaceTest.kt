package org.polyfrost.compose.render

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Assumptions.assumeTrue
import org.junit.jupiter.api.Test
import java.awt.image.BufferedImage
import java.io.ByteArrayOutputStream
import java.io.File
import java.nio.ByteBuffer
import java.nio.channels.FileChannel
import java.nio.file.StandardOpenOption
import java.util.Locale
import javax.imageio.ImageIO

class EmojiFaceTest {
    private fun bytes(size: Int, fill: ByteBuffer.() -> Unit): ByteArray = ByteBuffer.allocate(size).apply(fill).array()

    private fun png(argb: Int, size: Int): ByteArray {
        val image = BufferedImage(size, size, BufferedImage.TYPE_INT_ARGB)
        for (y in 0 until size) for (x in 0 until size) image.setRGB(x, y, argb)
        return ByteArrayOutputStream().also { ImageIO.write(image, "png", it) }.toByteArray()
    }

    private fun font(tables: Map<String, ByteArray>): ByteBuffer {
        val all = tables + mapOf(
            "head" to bytes(54) { putShort(18, 100) },
            "hhea" to bytes(36) { putShort(34, 1) },
            "maxp" to bytes(6) { putShort(4, 3) },
            "hmtx" to bytes(4) { putShort(0, 120) },
            "cmap" to bytes(60) {
                putShort(2, 1); putShort(6, 3); putShort(8, 10); putInt(8, 12)
                putShort(12, 12); putInt(24, 2)
                putInt(28, 0x1F1E6); putInt(32, 0x1F1FF); putInt(36, 10)
                putInt(40, 0x1F600); putInt(44, 0x1F600); putInt(48, 2)
            },
        )
        val head = 12 + all.size * 16
        val out = ByteBuffer.allocate(head + all.values.sumOf { it.size })
        out.putShort(4, all.size.toShort())
        var at = head
        all.entries.forEachIndexed { i, (tag, data) ->
            out.position(12 + i * 16).put(tag.toByteArray()).putInt(0).putInt(at).putInt(data.size)
            out.position(at).put(data)
            at += data.size
        }
        return out
    }

    private fun assertRed(r: PolyGlyphPage.Raster?) {
        assertNotNull(r)
        assertTrue(r!!.color)
        assertEquals(r.w * r.h * 4, r.coverage.size)
        assertEquals(-1, r.coverage[0].toInt())
        assertEquals(0, r.coverage[1].toInt())
        assertEquals(-1, r.coverage[3].toInt())
    }

    @Test
    fun readsSbix() {
        val image = png(0xFFFF0000.toInt(), 40)
        val face = EmojiFace(font(mapOf("sbix" to sbix(image))))
        assertEquals(2, face.glyph(0x1F600))
        assertEquals(0, face.glyph('a'.code))
        assertEquals(1.2f, face.advance(2))
        assertNull(face.raster(1, 20f))
        val r = face.raster(2, 20f)
        assertRed(r)
        assertEquals(20, r!!.w)
        assertEquals(-20, r.offY)
    }

    private fun sbix(image: ByteArray) = bytes(40 + image.size) {
        putInt(4, 1); putInt(8, 12)
        putShort(12, 40)
        putInt(16, 20); putInt(20, 20); putInt(24, 20); putInt(28, 28 + image.size)
        put(32 + 4, 'p'.code.toByte()); put(37, 'n'.code.toByte()); put(38, 'g'.code.toByte()); put(39, ' '.code.toByte())
        position(40); put(image)
    }

    @Test
    fun readsCbdt() {
        val image = png(0xFFFF0000.toInt(), 40)
        val cblc = bytes(72) {
            putInt(4, 1)
            putInt(8, 56); putInt(16, 1); put(8 + 45, 40)
            putShort(56, 2); putShort(58, 2); putInt(60, 8)
            putShort(64, 1); putShort(66, 17); putInt(68, 4)
        }
        val face = EmojiFace(font(mapOf("CBLC" to cblc + bytes(8) { putInt(4, 9 + image.size) }, "CBDT" to bytes(13 + image.size) {
            put(4 + 2, 0); put(4 + 3, 30); putInt(4 + 5, image.size)
            position(13); put(image)
        })))
        val r = face.raster(2, 20f)
        assertRed(r)
        assertEquals(-15, r!!.offY)
    }

    @Test
    fun compositesColrLayers() {
        val outlines = object : PolyGlyphPage.Face {
            override fun glyph(codepoint: Int) = 0
            override fun advance(glyph: Int) = 0f
            override fun raster(glyph: Int, pixelSize: Float) = PolyGlyphPage.Raster(0, -2, 2, 2, ByteArray(4) { -1 })
        }
        val face = EmojiFace(font(mapOf(
            "COLR" to bytes(30) {
                putShort(2, 1); putInt(4, 14); putInt(8, 20); putShort(12, 1)
                putShort(14, 2); putShort(16, 0); putShort(18, 1)
                putShort(20, 1); putShort(22, 0)
            },
            "CPAL" to bytes(18) {
                putShort(2, 1); putShort(4, 1); putShort(6, 1); putInt(8, 14)
                put(16, 0xFF.toByte()); put(17, 0xFF.toByte())
            },
        ))) { outlines }
        assertRed(face.raster(2, 20f))
        assertNull(face.raster(1, 20f))
    }

    private val usa = 0x1F1FA to 0x1F1F8

    @Test
    fun readsFlagsFromPostNames() {
        val name = "u1F1FA_u1F1F8"
        val post = bytes(40 + 1 + name.length) {
            putInt(0, 0x00020000); putShort(32, 3)
            putShort(34, 0); putShort(36, 0); putShort(38, 258)
            put(40, name.length.toByte())
            position(41); put(name.toByteArray())
        }
        val face = EmojiFace(font(mapOf("post" to post, "sbix" to sbix(png(-1, 40)))))
        assertEquals(2, face.pair(usa.first, usa.second))
        assertEquals(0, face.pair(usa.second, usa.first))
        assertEquals(0, face.pair(0x1F600, usa.second))
    }

    @Test
    fun readsFlagsFromGsubLigatures() {
        val gsub = bytes(40) {
            putShort(8, 10)
            putShort(10, 1); putShort(12, 4)
            putShort(14, 4); putShort(18, 1); putShort(20, 8)
            putShort(22, 1); putShort(24, 8); putShort(26, 1); putShort(28, 14)
            putShort(30, 1); putShort(32, 1); putShort(34, 30)
            putShort(36, 1); putShort(38, 4)
        } + bytes(6) { putShort(0, 2); putShort(2, 2); putShort(4, 28) }
        val face = EmojiFace(font(mapOf("GSUB" to gsub, "sbix" to sbix(png(-1, 40)))))
        assertEquals(2, face.pair(usa.first, usa.second))
        assertEquals(0, face.pair(usa.second, usa.first))
    }

    @Test
    fun ignoresJoiners() {
        assertTrue(PolyGlyphPage.isIgnorable(0x200D))
        assertTrue(PolyGlyphPage.isIgnorable(PolyGlyphPage.EMOJI_VARIATION))
        assertFalse(PolyGlyphPage.isIgnorable('a'.code))
    }

    @Test
    fun readsAppleColorEmoji() {
        val file = File("/System/Library/Fonts/Apple Color Emoji.ttc")
        assumeTrue(file.isFile)
        val font = FileChannel.open(file.toPath(), StandardOpenOption.READ).use { it.map(FileChannel.MapMode.READ_ONLY, 0, it.size()) }
        val face = EmojiFace(font)
        val glyph = face.glyph(0x1F600)
        assertTrue(glyph != 0)
        assertEquals(1f, face.advance(glyph))
        val r = face.raster(glyph, 28f)
        assertNotNull(r)
        assertEquals(28, r!!.w)
        assertEquals(-28, r.offY)
        for (iso in Locale.getISOCountries()) {
            val flag = face.pair(0x1F1E6 + iso[0].code - 'A'.code, 0x1F1E6 + iso[1].code - 'A'.code)
            assertTrue(flag != 0, iso)
            assertNotNull(face.raster(flag, 28f), iso)
        }
    }
}
