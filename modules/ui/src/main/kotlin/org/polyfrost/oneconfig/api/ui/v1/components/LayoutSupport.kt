@file:OptIn(ExperimentalOneConfigUiApi::class)

package org.polyfrost.oneconfig.api.ui.v1.components

import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import org.polyfrost.oneconfig.api.platform.v1.Platform
import org.polyfrost.oneconfig.api.ui.v1.ExperimentalOneConfigUiApi

internal fun layoutString(key: String, fallback: String, vararg args: Any?): String =
    runCatching {
        val i18n = Platform.i18n()
        if (i18n.hasTranslation(key)) i18n.translateString(key, *args) else null
    }.getOrNull() ?: if (args.isEmpty()) fallback else fallback.format(*args)

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
