package org.polyfrost.compose.render

import java.util.Base64
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class PolyBitmapTest {
    @Test
    fun decodesWebpAndIco() {
        PolyBitmap.registerImageReaders()
        val previous = PolyBitmap.decoder
        PolyBitmap.decoder = PolyBitmap.Decoder { null }
        try {
            assertDecodes("UklGRhwAAABXRUJQVlA4TA8AAAAvAYAAAAcQ/Y/+ByKi/wEA", 2, 3, 0xFFFF0000.toInt())
            assertDecodes(
                "UklGRoQAAABXRUJQVlA4WAoAAAACAAAAAgAAAQAAQU5JTQYAAAAAAAAAAABBTk1GKAAAAAAAAAAAAAIAAAEAAGQAAAJWUDhMDwAAAC8CQAAAB9D/iP4HIqL/AQBBTk1GKAAAAAAAAAAAAAIAAAEAAGQAAABWUDhMDwAAAC8CQAAABxDR//4HIqL/AQA=",
                3, 2, 0xFF00FF00.toInt(),
            )
            assertDecodes(
                "AAABAAEAAgIAAAAAIABOAAAAFgAAAIlQTkcNChoKAAAADUlIRFIAAAACAAAAAggGAAAAcrYNJAAAABVJREFUeJxjZGD4/5+BgYGBCUSAMAAdGQICsWeR9gAAAABJRU5ErkJggg==",
                2, 2, 0xFF0000FF.toInt(),
            )
        } finally {
            PolyBitmap.decoder = previous
        }
    }

    private fun assertDecodes(base64: String, width: Int, height: Int, argb: Int) {
        val bitmap = PolyBitmap.decode(Base64.getDecoder().decode(base64))!!
        assertEquals(width, bitmap.width)
        assertEquals(height, bitmap.height)
        val px = bitmap.pixels
        val first = ((px[3].toInt() and 0xFF) shl 24) or ((px[0].toInt() and 0xFF) shl 16) or ((px[1].toInt() and 0xFF) shl 8) or (px[2].toInt() and 0xFF)
        assertEquals(argb, first)
    }
}
