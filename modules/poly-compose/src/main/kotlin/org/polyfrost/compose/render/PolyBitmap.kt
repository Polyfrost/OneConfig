package org.polyfrost.compose.render

import java.awt.image.BufferedImage
import java.io.ByteArrayInputStream
import javax.imageio.ImageIO

class PolyBitmap(val width: Int, val height: Int, val pixels: ByteArray) {
    init {
        require(width > 0 && height > 0 && pixels.size == width * height * 4) { "bad bitmap ${width}x$height, ${pixels.size} bytes" }
    }

    fun interface Decoder {
        fun decode(bytes: ByteArray): PolyBitmap?
    }

    companion object {
        @JvmStatic
        var decoder: Decoder = Decoder { bytes -> runCatching { ImageIO.read(ByteArrayInputStream(bytes))?.let(::fromAwt) }.getOrNull() }

        @JvmStatic
        fun decode(bytes: ByteArray): PolyBitmap? = decoder.decode(bytes)

        @JvmStatic
        fun fromResource(path: String, loader: ClassLoader? = null): PolyBitmap? =
            ((loader ?: PolyBitmap::class.java.classLoader)?.getResourceAsStream(path) ?: PolyBitmap::class.java.getResourceAsStream(path))
                ?.use { decode(it.readBytes()) }

        @JvmStatic
        fun fromAwt(img: BufferedImage): PolyBitmap {
            val w = img.width
            val h = img.height
            val argb = img.getRGB(0, 0, w, h, null, 0, w)
            val px = ByteArray(w * h * 4)
            for (i in argb.indices) {
                val c = argb[i]
                px[i * 4] = (c shr 16).toByte(); px[i * 4 + 1] = (c shr 8).toByte(); px[i * 4 + 2] = c.toByte(); px[i * 4 + 3] = (c ushr 24).toByte()
            }
            return PolyBitmap(w, h, px)
        }
    }
}

enum class FilterQuality { None, Low, Medium, High }
