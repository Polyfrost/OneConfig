package org.polyfrost.oneconfig.internal.ui.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.ParagraphStyle
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.BaselineShift
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextGeometricTransform
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.sp
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.ComponentLike
import net.kyori.adventure.text.TextComponent
import org.apache.logging.log4j.LogManager
import org.polyfrost.oneconfig.api.platform.v1.Platform
import org.polyfrost.oneconfig.internal.ui.api.TextComponent
import org.polyfrost.oneconfig.internal.ui.themes.LocalTheme

fun Any.asRenderText(): String {
    val comp: Any = Platform.compatibility().wrapPlatformComponent(this)

    return when (comp) {
        is String -> comp
        is Component -> Platform.compatibility().resolveComponent(comp)
        is ComponentLike -> comp.asComponent().asRenderText()
        else -> comp.toString()
    }
}

fun Any.isEmptyText() = this.asRenderText().isEmpty()

private val LOGGER = LogManager.getLogger("OneConfig/Text-Renderer")

private const val LEGACY_CHAR = '§'

private val LEGACY_COLORS = mapOf(
    '0' to 0x000000, '1' to 0x0000AA, '2' to 0x00AA00, '3' to 0x00AAAA,
    '4' to 0xAA0000, '5' to 0xAA00AA, '6' to 0xFFAA00, '7' to 0xAAAAAA,
    '8' to 0x555555, '9' to 0x5555FF, 'a' to 0x55FF55, 'b' to 0x55FFFF,
    'c' to 0xFF5555, 'd' to 0xFF55FF, 'e' to 0xFFFF55, 'f' to 0xFFFFFF,
)

private fun parseLegacyFormatting(text: String, baseColor: Color): AnnotatedString = buildAnnotatedString {
    var color = baseColor
    var bold = false
    var italic = false
    var strikethrough = false
    var underline = false

    fun currentStyle() = SpanStyle(
        color = color,
        fontWeight = if (bold) FontWeight.Bold else null,
        textGeometricTransform = if (italic) TextGeometricTransform(1f, 0.5f) else null,
        textDecoration = when {
            strikethrough && underline -> TextDecoration.combine(listOf(TextDecoration.LineThrough, TextDecoration.Underline))
            strikethrough -> TextDecoration.LineThrough
            underline -> TextDecoration.Underline
            else -> null
        },
    )

    val pending = StringBuilder()
    fun flush() {
        if (pending.isEmpty()) return
        withStyle(currentStyle()) { append(pending.toString()) }
        pending.setLength(0)
    }

    var i = 0
    while (i < text.length) {
        val c = text[i]
        if (c == LEGACY_CHAR && i + 1 < text.length) {
            val code = text[i + 1].lowercaseChar()
            val colorValue = LEGACY_COLORS[code]
            when {
                colorValue != null -> {
                    flush()
                    color = Color(0xFF000000.toInt() or colorValue)
                    bold = false; italic = false; strikethrough = false; underline = false
                }
                code == 'l' -> { flush(); bold = true }
                code == 'o' -> { flush(); italic = true }
                code == 'm' -> { flush(); strikethrough = true }
                code == 'n' -> { flush(); underline = true }
                code == 'r' -> {
                    flush()
                    color = baseColor
                    bold = false; italic = false; strikethrough = false; underline = false
                }
                code == 'k' -> { /* obfuscated: unsupported, swallow code */ flush() }
                else -> {
                    pending.append(c).append(text[i + 1])
                }
            }
            i += 2
        } else {
            pending.append(c)
            i++
        }
    }
    flush()
}

@Composable
fun Text(
    text: Any,
    modifier: Modifier = Modifier,
    color: Color,
    fontSize: TextUnit = 14.sp,
    lineHeight: TextUnit? = null,
    shift: BaselineShift = BaselineShift.None,
    fontWeight: FontWeight = FontWeight.Normal,
    textAlign: TextAlign = TextAlign.Start
) {
    when (val comp: Any = Platform.compatibility().wrapPlatformComponent(text)) {
        is Component -> TextComponent(comp, modifier, color, fontSize, lineHeight, shift, fontWeight, textAlign)
        is ComponentLike -> TextComponent(comp.asComponent(), modifier, color, fontSize, lineHeight, shift, fontWeight, textAlign)
        is String -> BasicText(
            if (comp.indexOf(LEGACY_CHAR) >= 0) parseLegacyFormatting(comp, color) else AnnotatedString(comp),
            style = TextStyle(
                fontSize = fontSize, fontWeight = fontWeight,
                fontFamily = LocalTheme.current.typography.family,
                color = color,
                lineHeight = lineHeight ?: TextStyle.Default.lineHeight,
                textAlign = textAlign,
            ),
            modifier = modifier
        )
        is Iterable<*> -> {
            Column {
                comp.forEach { Text(it ?: return@forEach, modifier, color, fontSize, lineHeight, shift, fontWeight, textAlign) }
            }
        }

        else -> {
            LOGGER.warn("Unsupported text type " + comp.javaClass.simpleName)
            BasicText(
                buildAnnotatedString { withStyle(SpanStyle(color = Color.Red)) { append("Error") } }, style = TextStyle(
                    fontSize = fontSize, fontWeight = fontWeight,
                    fontFamily = LocalTheme.current.typography.family,
                    color = color,
                    lineHeight = lineHeight ?: TextStyle.Default.lineHeight,
                    textAlign = textAlign,
                ),
                modifier = modifier
            )
        }
    }


}
