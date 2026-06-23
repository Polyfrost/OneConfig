package org.polyfrost.oneconfig.internal

import net.minecraft.network.chat.Component
import net.minecraft.network.chat.FormattedText
import net.minecraft.network.chat.MutableComponent
import net.minecraft.network.chat.Style
import net.minecraft.util.FormattedCharSink
import java.util.Optional

object ComponentUtil {

    @JvmStatic
    // Taken (with permission) from skyocean https://github.com/meowdding/SkyOcean/blob/998efb48f475f1233ddc495a4c88c8f9b27f0a28/src/main/kotlin/me/owdding/skyocean/features/text/TextReplacements.kt#L44
    fun toComponentCharSink(): Pair<FormattedCharSink, () -> MutableComponent> {
        val result = Component.empty()
        var currentStyle: Style? = null
        var current = Component.empty()
        var builder = StringBuilder()

        return FormattedCharSink { _, style, codepoint ->
            if (currentStyle == null) {
                currentStyle = style
                current.style = style
            }
            if (currentStyle != style) {
                currentStyle = style
                current.append(builder.toString())
                result.append(current)
                builder = StringBuilder()
                current = Component.empty()
                current.style = currentStyle
            }
            builder.appendCodePoint(codepoint)

            true
        } to {
            current.append(builder.toString())
            current.style = currentStyle ?: Style.EMPTY
            result.append(current)
        }
    }

    @JvmStatic
    fun toStringFormattedTextContentConsumer(): Pair<FormattedText.ContentConsumer<Any>, () -> String> {
        val result = StringBuilder()
        return FormattedText.ContentConsumer<Any> {
            result.append(it)
            Optional.empty()
        } to { result.toString() }
    }

}