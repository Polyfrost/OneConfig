package org.polyfrost.oneconfig.internal.ui.api

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.TooltipArea
import androidx.compose.foundation.TooltipPlacement
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalFlexBoxApi
import androidx.compose.foundation.layout.FlexBox
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicText
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.BaselineShift
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextGeometricTransform
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.DpOffset
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.ComponentIteratorFlag
import net.kyori.adventure.text.ComponentIteratorType
import net.kyori.adventure.text.event.HoverEvent
import net.kyori.adventure.text.format.TextDecoration as KyoriDecoration
import org.polyfrost.oneconfig.api.platform.v1.Platform
import org.polyfrost.oneconfig.internal.ui.components.layout.FlexibleLayout
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
    textAlign: TextAlign = TextAlign.Start
) {
     Row {
        component.iterable(ComponentIteratorType.DEPTH_FIRST).forEach {
            val text = @Composable {
                BasicText(
                    text(it), style = TextStyle(
                        fontSize = fontSize, fontWeight = fontWeight,
                        fontFamily = LocalTheme.current.typography.family,
                        color = color,
                        lineHeight = lineHeight ?: TextStyle.Default.lineHeight,
                        textAlign = textAlign,
                    ), modifier = modifier
                )
            }

            val event = it.hoverEvent()
            val hover = when (val temp = event?.action()) {
                HoverEvent.Action.SHOW_TEXT -> {
                    event.value() as Component
                }
                else -> null
            }
            if (hover != null) {
                Tooltip(
                    text = { TextComponent(hover, color = color) },
                    modifier,
                    anchor = Alignment.TopCenter
                ) {
                    text()
                }
            } else  {
                text()
            }
        }
    }
}

@Composable
fun text(text: Component) = buildAnnotatedString {
    withStyle(
        SpanStyle(color = text.color()?.let { Color(it.value()).copy(alpha = 1f) } ?: Color.Unspecified,
            textGeometricTransform = if (text.hasDecoration(KyoriDecoration.ITALIC)) TextGeometricTransform(
                1f, 0.5f
            ) else null,
            textDecoration = if (text.hasDecoration(KyoriDecoration.STRIKETHROUGH) || text.hasDecoration(
                    KyoriDecoration.UNDERLINED
                )
            ) {
                TextDecoration.combine(
                    buildList {
                        if (text.hasDecoration(KyoriDecoration.STRIKETHROUGH)) {
                            add(TextDecoration.LineThrough)
                        }
                        if (text.hasDecoration(KyoriDecoration.UNDERLINED)) {
                            add(TextDecoration.Underline)
                        }
                    })
            } else null,
            fontWeight = if (text.hasDecoration(KyoriDecoration.BOLD)) FontWeight.Bold else null)) {
        append(Platform.compatibility().resolveComponent(text.children(emptyList())))
    }
}

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

    TooltipArea(
        content = content, delayMillis = 200, tooltip = @Composable {
            Box(
                modifier = modifier.background(Color(0xff2C2C2C), shape = RoundedCornerShape(8.dp))
                    .border(1.dp, Color.White.copy(alpha = 0.1f), shape = RoundedCornerShape(8.dp))
                    .padding(horizontal = 12.dp, vertical = 8.dp)
            ) {
                text()
            }
        }, tooltipPlacement = placement
    )
}
