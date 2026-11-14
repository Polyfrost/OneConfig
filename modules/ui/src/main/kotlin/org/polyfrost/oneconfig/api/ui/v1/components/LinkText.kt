@file:OptIn(ExperimentalOneConfigUiApi::class)

package org.polyfrost.oneconfig.api.ui.v1.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsHoveredAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.net.URI
import org.apache.logging.log4j.LogManager
import org.polyfrost.oneconfig.api.platform.v1.DesktopHelper
import org.polyfrost.oneconfig.api.ui.v1.ExperimentalOneConfigUiApi
import org.polyfrost.oneconfig.api.ui.v1.a11y.A11yRole
import org.polyfrost.oneconfig.api.ui.v1.a11y.Motion
import org.polyfrost.oneconfig.api.ui.v1.theme.OneConfigTheme
import org.polyfrost.oneconfig.api.ui.v1.theme.legibleOn

private const val LINK_FADE_MILLIS = 120
private const val LINK_REST_ALPHA = 0.85f
private val LINK_ICON_SIZE = 12.dp

private val LOGGER = LogManager.getLogger("OneConfig/UI")

@ExperimentalOneConfigUiApi
@Composable
public fun LinkText(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    icon: String? = null,
    enabled: Boolean = true,
    interactionSource: MutableInteractionSource? = null,
) {
    val glyph: (@Composable (Color) -> Unit)? = if (icon == null) null else { { color -> Icon(icon, null, Modifier.size(LINK_ICON_SIZE), tint = color) } }
    LinkTextFrame(text, onClick, modifier, enabled, interactionSource, glyph)
}

@ExperimentalOneConfigUiApi
@Composable
public fun LinkText(
    text: String,
    onClick: () -> Unit,
    icon: Painter,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    interactionSource: MutableInteractionSource? = null,
) {
    LinkTextFrame(text, onClick, modifier, enabled, interactionSource) { color ->
        Image(icon, null, Modifier.size(LINK_ICON_SIZE), colorFilter = ColorFilter.tint(color))
    }
}

@ExperimentalOneConfigUiApi
@Composable
public fun LinkText(
    text: String,
    url: String,
    modifier: Modifier = Modifier,
    icon: String? = null,
    enabled: Boolean = true,
    interactionSource: MutableInteractionSource? = null,
) {
    LinkText(text, { openUrl(url) }, modifier, icon, enabled, interactionSource)
}

@ExperimentalOneConfigUiApi
@Composable
public fun LinkText(
    text: String,
    url: String,
    icon: Painter,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    interactionSource: MutableInteractionSource? = null,
) {
    LinkText(text, { openUrl(url) }, icon, modifier, enabled, interactionSource)
}

@Composable
private fun LinkTextFrame(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier,
    enabled: Boolean,
    interactionSource: MutableInteractionSource?,
    glyph: (@Composable (Color) -> Unit)?,
) {
    val theme = OneConfigTheme.current
    val source = interactionSource ?: remember { MutableInteractionSource() }
    val hovered by source.collectIsHoveredAsState()
    val active = hovered && enabled
    val base = OneConfigTheme.accent.legibleOn(theme.pageSurface)
    val color by animateColorAsState(if (active) base else base.copy(alpha = base.alpha * LINK_REST_ALPHA), Motion.spec(tween(LINK_FADE_MILLIS)))
    Row(
        modifier = modifier
            .graphicsLayer { alpha = if (enabled) 1f else theme.disabledAlpha }
            .oneConfigClickable(text, onClick, role = A11yRole.Link, enabled = enabled, shape = theme.shapes.small, indication = false, interactionSource = source),
        horizontalArrangement = Arrangement.spacedBy(4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text,
            Modifier.drawBehind {
                if (!active) return@drawBehind
                val y = size.height - 1.dp.toPx()
                drawLine(color, Offset(0f, y), Offset(size.width, y), 1.dp.toPx())
            },
            color,
            OneConfigTheme.typography.body.copy(fontSize = 13.sp, fontWeight = FontWeight.Normal),
            1,
            TextOverflow.Ellipsis,
        )
        glyph?.invoke(color)
    }
}

private fun openUrl(url: String) {
    val opened = runCatching { DesktopHelper.browse(URI(url)) }.getOrElse { false }
    if (!opened) LOGGER.warn("Could not open {} in a browser", url)
}
