@file:OptIn(ExperimentalOneConfigUiApi::class)

package org.polyfrost.oneconfig.api.ui.v1.components

import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.Dp
import kotlin.math.max
import kotlin.math.roundToInt
import org.polyfrost.oneconfig.api.platform.v1.Platform
import org.polyfrost.oneconfig.api.ui.v1.ExperimentalOneConfigUiApi
import org.polyfrost.oneconfig.api.ui.v1.theme.OneConfigTheme
import org.polyfrost.oneconfig.api.ui.v1.theme.ThemeStyle

internal fun layoutString(key: String, fallback: String, vararg args: Any?): String =
    runCatching {
        val i18n = Platform.i18n()
        if (i18n.hasTranslation(key)) i18n.translateString(key, *args) else null
    }.getOrNull() ?: if (args.isEmpty()) fallback else fallback.format(*args)

internal fun Density.snappedStrokePx(width: Dp, pixel: Boolean): Float =
    if (pixel) max(1f, width.toPx().roundToInt().toFloat()) else width.toPx()

@Composable
internal fun snappedStroke(width: Dp): Dp {
    if (OneConfigTheme.current.style != ThemeStyle.Pixel) return width
    return with(LocalDensity.current) { snappedStrokePx(width, true).toDp() }
}

@Composable
internal fun LayoutText(
    text: String,
    style: TextStyle,
    color: Color,
    modifier: Modifier = Modifier,
    maxLines: Int = Int.MAX_VALUE,
    align: TextAlign = TextAlign.Unspecified,
    softWrap: Boolean = true,
) {
    BasicText(
        text = text,
        modifier = modifier,
        style = style.merge(color = color, textAlign = align),
        overflow = TextOverflow.Ellipsis,
        softWrap = softWrap,
        maxLines = maxLines,
    )
}

@Composable
internal fun LayoutText(
    text: AnnotatedString,
    style: TextStyle,
    color: Color,
    modifier: Modifier = Modifier,
    maxLines: Int = Int.MAX_VALUE,
    align: TextAlign = TextAlign.Unspecified,
    softWrap: Boolean = true,
) {
    BasicText(
        text = text,
        modifier = modifier,
        style = style.merge(color = color, textAlign = align),
        overflow = TextOverflow.Ellipsis,
        softWrap = softWrap,
        maxLines = maxLines,
    )
}
