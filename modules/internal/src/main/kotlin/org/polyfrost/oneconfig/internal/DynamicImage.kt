package org.polyfrost.oneconfig.internal

import java.io.InputStream
import org.jetbrains.skia.Image

class DynamicImage(val path: String, private val stream: InputStream) {
    val image: Image by lazy { Image.makeFromEncoded(stream.readBytes()) }
}
