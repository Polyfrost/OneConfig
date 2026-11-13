@file:OptIn(ExperimentalOneConfigUiApi::class)

package org.polyfrost.oneconfig.api.ui.v1.components

import androidx.compose.foundation.hoverable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsHoveredAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntRect
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.PopupPositionProvider
import kotlinx.coroutines.delay
import org.polyfrost.oneconfig.api.ui.v1.ExperimentalOneConfigUiApi
import org.polyfrost.oneconfig.api.ui.v1.a11y.isKeyboardMode
import org.polyfrost.oneconfig.api.ui.v1.internal.componentsBridge
import org.polyfrost.oneconfig.api.ui.v1.theme.OneConfigTheme

@ExperimentalOneConfigUiApi
public enum class TooltipPlacement {
    AboveCenter,
    AboveStart,
    AboveEnd,
    BelowCenter,
    BelowStart,
    BelowEnd,
}

private val TOOLTIP_GAP = 8.dp
private val TOOLTIP_EDGE = 8.dp

private class TooltipPosition(private val placement: TooltipPlacement, private val gap: Int, private val edge: Int) : PopupPositionProvider {
    override fun calculatePosition(anchorBounds: IntRect, windowSize: IntSize, layoutDirection: LayoutDirection, popupContentSize: IntSize): IntOffset {
        val rtl = layoutDirection == LayoutDirection.Rtl
        val startX = if (rtl) anchorBounds.right - popupContentSize.width else anchorBounds.left
        val endX = if (rtl) anchorBounds.left else anchorBounds.right - popupContentSize.width
        val x = when (placement) {
            TooltipPlacement.AboveCenter, TooltipPlacement.BelowCenter -> anchorBounds.center.x - popupContentSize.width / 2
            TooltipPlacement.AboveStart, TooltipPlacement.BelowStart -> startX
            TooltipPlacement.AboveEnd, TooltipPlacement.BelowEnd -> endX
        }
        val above = anchorBounds.top - gap - popupContentSize.height
        val below = anchorBounds.bottom + gap
        val fitsAbove = above >= 0
        val fitsBelow = below + popupContentSize.height <= windowSize.height
        val preferAbove = placement == TooltipPlacement.AboveCenter || placement == TooltipPlacement.AboveStart || placement == TooltipPlacement.AboveEnd
        val y = when {
            preferAbove && fitsAbove -> above
            !preferAbove && fitsBelow -> below
            fitsAbove -> above
            fitsBelow -> below
            else -> (if (preferAbove) above else below).coerceIn(0, (windowSize.height - popupContentSize.height).coerceAtLeast(0))
        }
        val clampedX = if (popupContentSize.width + edge * 2 > windowSize.width) {
            ((windowSize.width - popupContentSize.width) / 2).coerceAtLeast(0)
        } else {
            x.coerceIn(edge, windowSize.width - popupContentSize.width - edge)
        }
        return IntOffset(clampedX, y)
    }
}

@ExperimentalOneConfigUiApi
public object TooltipDefaults {
    public val maxWidth: Dp = 240.dp

    public const val delayMillis: Long = 300L

    public val placement: TooltipPlacement = TooltipPlacement.AboveCenter
}

@ExperimentalOneConfigUiApi
@Composable
public fun TooltipPopup(text: String, placement: TooltipPlacement) {
    TooltipPopup(text, placement, TooltipDefaults.maxWidth)
}

@ExperimentalOneConfigUiApi
@Composable
public fun TooltipPopup(text: String, placement: TooltipPlacement, maxWidth: Dp) {
    TooltipSurface(placement, maxWidth) { TooltipText(text) }
}

@Composable
private fun TooltipText(text: String) {
    val theme = OneConfigTheme.current
    Text(text, Modifier, theme.textColor, theme.typography.label.copy(fontWeight = FontWeight.Normal), textAlign = TextAlign.Center)
}

@Composable
private fun TooltipSurface(placement: TooltipPlacement, maxWidth: Dp, content: @Composable () -> Unit) {
    val density = LocalDensity.current
    val provider = remember(density, placement) {
        TooltipPosition(placement, with(density) { TOOLTIP_GAP.roundToPx() }, with(density) { TOOLTIP_EDGE.roundToPx() })
    }
    componentsBridge().Popup(provider, null, false) {
        Box(
            modifier = componentsBridge().popupSurface(Modifier.widthIn(max = maxWidth), OneConfigTheme.current.popupShape)
                .padding(horizontal = 12.dp, vertical = 8.dp),
            contentAlignment = Alignment.Center,
        ) {
            content()
        }
    }
}

@ExperimentalOneConfigUiApi
@Composable
public fun TooltipBox(
    text: String?,
    modifier: Modifier = Modifier,
    placement: TooltipPlacement = TooltipDefaults.placement,
    maxWidth: Dp = TooltipDefaults.maxWidth,
    showOnFocus: Boolean = true,
    content: @Composable () -> Unit,
) {
    val tooltip: (@Composable () -> Unit)? = if (text == null) null else { { TooltipText(text) } }
    TooltipHost(tooltip, modifier, placement, maxWidth, showOnFocus, content)
}

@ExperimentalOneConfigUiApi
@Composable
public fun TooltipBox(
    tooltip: @Composable () -> Unit,
    modifier: Modifier = Modifier,
    placement: TooltipPlacement = TooltipDefaults.placement,
    maxWidth: Dp = TooltipDefaults.maxWidth,
    showOnFocus: Boolean = true,
    content: @Composable () -> Unit,
) {
    TooltipHost(tooltip, modifier, placement, maxWidth, showOnFocus, content)
}

@Composable
private fun TooltipHost(
    tooltip: (@Composable () -> Unit)?,
    modifier: Modifier,
    placement: TooltipPlacement,
    maxWidth: Dp,
    showOnFocus: Boolean,
    content: @Composable () -> Unit,
) {
    val source = remember { MutableInteractionSource() }
    val hovered by source.collectIsHoveredAsState()
    var focused by remember { mutableStateOf(false) }
    val keyboard = isKeyboardMode()
    val wanted = tooltip != null && (hovered || showOnFocus && focused && keyboard)
    var shown by remember { mutableStateOf(false) }
    LaunchedEffect(wanted) {
        if (wanted) {
            delay(TooltipDefaults.delayMillis)
            shown = true
        } else {
            shown = false
        }
    }
    Box(modifier.hoverable(source).onFocusChanged { focused = it.hasFocus }) {
        content()
        if (shown && tooltip != null) TooltipSurface(placement, maxWidth, tooltip)
    }
}

@Deprecated("Kept for binary compatibility", level = DeprecationLevel.HIDDEN)
@ExperimentalOneConfigUiApi
@Composable
public fun TooltipBox(text: String?, modifier: Modifier, content: @Composable () -> Unit) {
    TooltipBox(text, modifier, TooltipDefaults.placement, TooltipDefaults.maxWidth, true, content)
}

@Deprecated("Kept for binary compatibility", level = DeprecationLevel.HIDDEN)
@ExperimentalOneConfigUiApi
@Composable
public fun TooltipBox(text: String?, modifier: Modifier, placement: TooltipPlacement, maxWidth: Dp, content: @Composable () -> Unit) {
    TooltipBox(text, modifier, placement, maxWidth, true, content)
}
