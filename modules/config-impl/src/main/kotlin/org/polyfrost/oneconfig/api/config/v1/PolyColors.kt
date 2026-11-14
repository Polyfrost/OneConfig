@file:JvmName("PolyColors")

package org.polyfrost.oneconfig.api.config.v1

import com.electronwill.nightconfig.json.JsonFormat
import com.electronwill.nightconfig.json.JsonParser
import java.io.StringWriter
import org.jetbrains.annotations.ApiStatus
import org.polyfrost.compose.render.PolyColor
import org.polyfrost.oneconfig.api.config.v1.serialize.adapter.impl.PolyColorAdapter

@ApiStatus.Experimental
@JvmOverloads
public fun PolyColor.copy(
    argb: Int = rawArgb,
    chroma: Boolean = this.chroma,
    chromaSpeed: Float = this.chromaSpeed,
    gradientEnabled: Boolean = this.gradientEnabled,
    gradientArgb: Int = this.gradientArgb,
): PolyColor = PolyColor(argb, chroma, chromaSpeed).also {
    it.gradientEnabled = gradientEnabled
    it.gradientArgb = gradientArgb
}

@ApiStatus.Experimental
public object PolyColorJson {
    private val adapter = PolyColorAdapter()

    @JvmStatic
    public fun encode(color: PolyColor): String {
        val out = StringWriter()
        JsonFormat.minimalInstance().createWriter().writeValue(toJsonValue(adapter.serialize(color)), out)
        return out.toString()
    }

    @JvmStatic
    public fun decode(json: String): PolyColor {
        val text = json.trim()
        val value: Any? = try {
            when {
                text.startsWith('{') || text.startsWith('[') -> toPlain(JsonParser().parseDocument(text))
                text.length >= 2 && text.startsWith('"') && text.endsWith('"') -> text.substring(1, text.length - 1)
                else -> text.toLongOrNull() ?: text
            }
        } catch (e: Exception) {
            throw IllegalArgumentException("Not a PolyColor: $json", e)
        }
        return try {
            adapter.deserialize(value)
        } catch (e: IllegalArgumentException) {
            throw e
        } catch (e: Exception) {
            throw IllegalArgumentException("Not a PolyColor: $json", e)
        }
    }

    @JvmStatic
    public fun decodeOrNull(json: String): PolyColor? = runCatching { decode(json) }.getOrNull()
}
