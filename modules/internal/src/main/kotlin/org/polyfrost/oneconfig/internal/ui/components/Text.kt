package org.polyfrost.oneconfig.internal.ui.components

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.ComponentLike
import org.polyfrost.compose.animation.AnimatedColor
import org.polyfrost.compose.ui.Modifier
import org.polyfrost.compose.ui.graphics.Color
import org.polyfrost.compose.ui.text.font.FontWeight
import org.polyfrost.compose.ui.text.style.BaselineShift
import org.polyfrost.compose.ui.text.style.TextAlign
import org.polyfrost.compose.ui.text.style.TextOverflow
import org.polyfrost.compose.ui.unit.TextUnit
import org.polyfrost.compose.ui.unit.sp
import org.polyfrost.oneconfig.api.platform.v1.Platform
import org.polyfrost.oneconfig.internal.ui.poly.UiText
import org.polyfrost.oneconfig.internal.ui.poly.richText
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
    fontWeight: FontWeight = FontWeight.Normal,
    textAlign: TextAlign = TextAlign.Start,
    maxLines: Int = Int.MAX_VALUE,
    overflow: TextOverflow = TextOverflow.Clip,
    softWrap: Boolean = false,
    baselineShift: BaselineShift = BaselineShift.None,
) = ThemedText(text, modifier, color, null, fontSize, lineHeight, fontWeight, textAlign, maxLines, overflow, softWrap, baselineShift)

@Composable
fun Text(
    text: Any,
    modifier: Modifier = Modifier,
    color: AnimatedColor,
    fontSize: TextUnit = 14.sp,
    lineHeight: TextUnit? = null,
    fontWeight: FontWeight = FontWeight.Normal,
    textAlign: TextAlign = TextAlign.Start,
    maxLines: Int = Int.MAX_VALUE,
    overflow: TextOverflow = TextOverflow.Clip,
    softWrap: Boolean = false,
    baselineShift: BaselineShift = BaselineShift.None,
) = ThemedText(text, modifier, color.target, color, fontSize, lineHeight, fontWeight, textAlign, maxLines, overflow, softWrap, baselineShift)

@Composable
private fun ThemedText(
    text: Any, modifier: Modifier, color: Color, animated: AnimatedColor?, fontSize: TextUnit, lineHeight: TextUnit?,
    fontWeight: FontWeight, textAlign: TextAlign, maxLines: Int, overflow: TextOverflow, softWrap: Boolean,
    baselineShift: BaselineShift,
) {
    val theme = LocalTheme.current
    val family = theme.typography.family
    val font = family.resolve(fontWeight)
    val bold = family.resolve(FontWeight.Bold)
    val italic = if (theme.previewImage.startsWith("minecraft")) font else "poppins-italic"
    val (s, spans) = remember(text, font, bold, italic) { richText(text, font, bold, italic) }
    val ellipsis = overflow == TextOverflow.Ellipsis
    val center = textAlign == TextAlign.Center
    val shift = baselineShift.multiplier
    if (animated != null) UiText(s, animated, fontSize.value, font, modifier, softWrap, spans, maxLines, ellipsis, lineHeight?.value, center, shift)
    else UiText(s, color, fontSize.value, font, modifier, softWrap, spans, maxLines, ellipsis, lineHeight?.value, center, shift)
}
