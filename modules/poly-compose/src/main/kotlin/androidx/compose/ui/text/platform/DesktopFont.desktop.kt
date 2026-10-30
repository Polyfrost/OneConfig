@file:JvmName("DesktopFont_desktopKt")

package androidx.compose.ui.text.platform

import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import org.polyfrost.compose.backend.PolyFontSource
import java.io.File


fun Font(resource: String, weight: FontWeight = FontWeight.Normal, style: FontStyle = FontStyle.Normal): Font =
    PolyFontSource("res:$resource", {
        val cl = Thread.currentThread().contextClassLoader ?: PolyFontSource::class.java.classLoader
        (cl.getResourceAsStream(resource) ?: PolyFontSource::class.java.classLoader.getResourceAsStream(resource))
            ?.use { it.readBytes() } ?: error("font resource not found: $resource")
    }, weight, style)

fun Font(file: File, weight: FontWeight = FontWeight.Normal, style: FontStyle = FontStyle.Normal): Font =
    PolyFontSource("file:${file.absolutePath}", { file.readBytes() }, weight, style)
