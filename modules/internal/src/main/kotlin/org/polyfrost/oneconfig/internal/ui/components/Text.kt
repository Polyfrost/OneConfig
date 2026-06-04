package org.polyfrost.oneconfig.internal.ui.components

import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.ParagraphStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.BaselineShift
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.sp
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.ComponentLike
import net.kyori.adventure.text.TextComponent
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

@Composable
fun Text(
    text: Any,
    modifier: Modifier = Modifier,
    color: Color,
    fontSize: TextUnit = 14.sp,
    lineHeight: TextUnit? = null,
    shift: BaselineShift = BaselineShift.None,
    fontWeight: FontWeight = FontWeight.Normal
) {
    when (val comp: Any = Platform.compatibility().wrapPlatformComponent(text)) {
        is Component -> TextComponent(comp, modifier, color, fontSize, lineHeight, shift, fontWeight)
        is ComponentLike -> TextComponent(comp.asComponent(), modifier, color, fontSize, lineHeight, shift, fontWeight)
        is String -> BasicText(
            comp, style = TextStyle(
                fontSize = fontSize, fontWeight = fontWeight,
                fontFamily = LocalTheme.current.typography.family,
                color = color,
                lineHeight = lineHeight ?: TextStyle.Default.lineHeight,
            ),
            modifier = modifier
        )

        else -> TODO(comp.javaClass.simpleName)
    }


}