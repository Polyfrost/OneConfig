package org.polyfrost.oneconfig.internal.ui.api

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorProducer
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.*
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.DpOffset
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.format.Style
import net.kyori.adventure.text.format.TextDecoration as KyoriDecoration
import org.polyfrost.oneconfig.api.platform.v1.Platform
import org.polyfrost.oneconfig.internal.ui.components.liquidGlass
import org.polyfrost.oneconfig.internal.ui.components.parseLegacyFormatting
import org.polyfrost.oneconfig.internal.ui.themes.LocalTheme

@Composable
@Suppress("FunctionName")
fun TextComponent(
    component: Component,
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
    BasicText(
        buildAnnotatedString { appendComponent(component, Style.empty()) },
        modifier,
        TextStyle(
            fontSize = fontSize, fontWeight = fontWeight,
            fontFamily = LocalTheme.current.typography.family,
            color = color,
            lineHeight = lineHeight ?: TextStyle.Default.lineHeight,
            textAlign = textAlign,
        ),
        maxLines = maxLines,
        overflow = overflow,
        color = colorProducer,
    )
}

// children inherit the style they don't set from their parent
private fun AnnotatedString.Builder.appendComponent(component: Component, inherited: Style) {
    val style = component.style().merge(inherited, Style.Merge.Strategy.IF_ABSENT_ON_TARGET)
    withStyle(spanStyle(style)) {
        append(parseLegacyFormatting(Platform.compatibility().resolveComponent(component.children(emptyList())), Color.Unspecified))
    }
    component.children().forEach { appendComponent(it, style) }
}

private fun spanStyle(style: Style) = SpanStyle(
    color = style.color()?.let { Color(it.value()).copy(alpha = 1f) } ?: Color.Unspecified,
    textGeometricTransform = if (style.hasDecoration(KyoriDecoration.ITALIC)) TextGeometricTransform(1f, 0.5f) else null,
    textDecoration = if (style.hasDecoration(KyoriDecoration.STRIKETHROUGH) || style.hasDecoration(KyoriDecoration.UNDERLINED)) {
        TextDecoration.combine(
            buildList {
                if (style.hasDecoration(KyoriDecoration.STRIKETHROUGH)) add(TextDecoration.LineThrough)
                if (style.hasDecoration(KyoriDecoration.UNDERLINED)) add(TextDecoration.Underline)
            })
    } else null,
    fontWeight = if (style.hasDecoration(KyoriDecoration.BOLD)) FontWeight.Bold else null,
)

@Composable
@OptIn(ExperimentalFoundationApi::class)
fun Tooltip(
    text: @Composable () -> Unit,
    modifier: Modifier,
    anchor: Alignment,
    alignment: Alignment = anchor,
    offset: DpOffset = DpOffset.Zero,
    content: @Composable () -> Unit
) {
    val placement = TooltipPlacement.ComponentRect(
        anchor = anchor, alignment = alignment, offset = offset
    )

    val theme = LocalTheme.current
    TooltipArea(
        content = content, delayMillis = 200, tooltip = @Composable {
            Box(
                modifier = modifier.liquidGlass(theme.popupShape, frosted = true)
                    .background(theme.popupBackground, shape = theme.popupShape)
                    .border(1.dp, theme.borderColor, shape = theme.popupShape)
                    .padding(horizontal = 12.dp, vertical = 8.dp)
            ) {
                text()
            }
        }, tooltipPlacement = placement
    )
}
