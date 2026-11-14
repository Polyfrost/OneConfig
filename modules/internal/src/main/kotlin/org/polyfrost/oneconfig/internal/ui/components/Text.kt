package org.polyfrost.oneconfig.internal.ui.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorProducer
import androidx.compose.ui.graphics.isSpecified
import androidx.compose.ui.text.*
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.*
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.sp
import java.util.concurrent.ConcurrentHashMap
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.ComponentLike
import net.kyori.adventure.text.format.Style
import net.kyori.adventure.text.format.TextDecoration as KyoriDecoration
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

private val warnedTextTypes = ConcurrentHashMap.newKeySet<Class<*>>()

private const val LEGACY_CHAR = '§'

private val LEGACY_COLORS = mapOf(
    '0' to 0x000000, '1' to 0x0000AA, '2' to 0x00AA00, '3' to 0x00AAAA,
    '4' to 0xAA0000, '5' to 0xAA00AA, '6' to 0xFFAA00, '7' to 0xAAAAAA,
    '8' to 0x555555, '9' to 0x5555FF, 'a' to 0x55FF55, 'b' to 0x55FFFF,
    'c' to 0xFF5555, 'd' to 0xFF55FF, 'e' to 0xFFFF55, 'f' to 0xFFFFFF,
)

internal fun parseLegacyFormatting(text: String, baseColor: Color): AnnotatedString = buildAnnotatedString {
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
                code == 'k' -> { /* obfuscated is unsupported so swallow the code */ flush() }
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

@Deprecated("Kept for binary compatibility", level = DeprecationLevel.HIDDEN)
@Composable
fun Text(
    text: Any,
    modifier: Modifier = Modifier,
    color: Color,
    fontSize: TextUnit = 14.sp,
    lineHeight: TextUnit? = null,
    shift: BaselineShift = BaselineShift.None,
    fontWeight: FontWeight = FontWeight.Normal,
    textAlign: TextAlign = TextAlign.Start,
    maxLines: Int = Int.MAX_VALUE,
    overflow: TextOverflow = TextOverflow.Clip,
) = Text(text, modifier, color, fontSize, lineHeight, shift, fontWeight, textAlign, maxLines, overflow, null)

@Composable
fun Text(
    text: Any,
    modifier: Modifier = Modifier,
    color: Color,
    fontSize: TextUnit = 14.sp,
    lineHeight: TextUnit? = null,
    shift: BaselineShift = BaselineShift.None,
    fontWeight: FontWeight = FontWeight.Normal,
    textAlign: TextAlign = TextAlign.Start,
    maxLines: Int = Int.MAX_VALUE,
    overflow: TextOverflow = TextOverflow.Clip,
    colorProducer: ColorProducer? = null,
) {
    when (val comp: Any = Platform.compatibility().wrapPlatformComponent(text)) {
        is Component -> TextComponent(comp, modifier, color, fontSize, lineHeight, shift, fontWeight, textAlign, maxLines, overflow, colorProducer)
        is ComponentLike -> TextComponent(comp.asComponent(), modifier, color, fontSize, lineHeight, shift, fontWeight, textAlign, maxLines, overflow, colorProducer)
        is String -> BasicText(
            if (comp.indexOf(LEGACY_CHAR) >= 0) parseLegacyFormatting(comp, color) else AnnotatedString(comp),
            style = TextStyle(
                fontSize = fontSize, fontWeight = fontWeight,
                fontFamily = LocalTheme.current.typography.family,
                color = color,
                lineHeight = lineHeight ?: TextStyle.Default.lineHeight,
                textAlign = textAlign,
                baselineShift = shift,
            ),
            maxLines = maxLines,
            overflow = overflow,
            modifier = modifier,
            color = colorProducer,
        )
        is Iterable<*> -> {
            Column(modifier) {
                comp.forEach { Text(it ?: return@forEach, Modifier, color, fontSize, lineHeight, shift, fontWeight, textAlign, maxLines, overflow, colorProducer) }
            }
        }

        else -> {
            if (warnedTextTypes.add(comp.javaClass)) LOGGER.warn("Unsupported text type {}; drawing its toString() instead", comp.javaClass.name)
            Text(comp.toString(), modifier, color, fontSize, lineHeight, shift, fontWeight, textAlign, maxLines, overflow, colorProducer)
        }
    }


}

@Composable
fun StyledText(
    text: AnnotatedString,
    modifier: Modifier,
    color: Color,
    style: TextStyle,
    maxLines: Int,
    overflow: TextOverflow,
    softWrap: Boolean,
    textAlign: TextAlign?,
    colorProducer: ColorProducer?,
    onTextLayout: ((TextLayoutResult) -> Unit)?,
) {
    val theme = LocalTheme.current
    val resolved = when {
        color.isSpecified -> color
        style.color.isSpecified -> style.color
        else -> theme.textColor
    }
    BasicText(
        text = text,
        modifier = modifier,
        style = style.merge(
            TextStyle(
                color = resolved,
                fontFamily = style.fontFamily ?: theme.typography.family,
                textAlign = textAlign ?: style.textAlign,
            ),
        ),
        onTextLayout = onTextLayout,
        overflow = overflow,
        softWrap = softWrap,
        maxLines = maxLines,
        color = colorProducer,
    )
}

fun legacyText(text: String): AnnotatedString =
    if (text.indexOf(LEGACY_CHAR) >= 0) parseLegacyFormatting(text, Color.Unspecified) else AnnotatedString(text)

fun componentText(component: Component): AnnotatedString = buildAnnotatedString { appendComponent(component, Style.empty()) }

private fun AnnotatedString.Builder.appendComponent(component: Component, inherited: Style) {
    val style = component.style().merge(inherited, Style.Merge.Strategy.IF_ABSENT_ON_TARGET)
    withStyle(componentSpanStyle(style)) {
        append(parseLegacyFormatting(Platform.compatibility().resolveComponent(component.children(emptyList())), Color.Unspecified))
    }
    component.children().forEach { appendComponent(it, style) }
}

private fun componentSpanStyle(style: Style): SpanStyle {
    val strikethrough = style.hasDecoration(KyoriDecoration.STRIKETHROUGH)
    val underlined = style.hasDecoration(KyoriDecoration.UNDERLINED)
    return SpanStyle(
        color = style.color()?.let { Color(it.value()).copy(alpha = 1f) } ?: Color.Unspecified,
        fontWeight = if (style.hasDecoration(KyoriDecoration.BOLD)) FontWeight.Bold else null,
        textGeometricTransform = if (style.hasDecoration(KyoriDecoration.ITALIC)) TextGeometricTransform(1f, 0.5f) else null,
        textDecoration = when {
            strikethrough && underlined -> TextDecoration.combine(listOf(TextDecoration.LineThrough, TextDecoration.Underline))
            strikethrough -> TextDecoration.LineThrough
            underlined -> TextDecoration.Underline
            else -> null
        },
    )
}
