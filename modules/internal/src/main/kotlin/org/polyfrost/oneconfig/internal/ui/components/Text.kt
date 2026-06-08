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
            comp, style = TextStyle(
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
