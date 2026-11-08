@file:OptIn(ExperimentalOneConfigUiApi::class)

package org.polyfrost.oneconfig.api.ui.v1.components

import androidx.compose.foundation.hoverable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsHoveredAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.BaselineShift
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntRect
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.PopupPositionProvider
import org.polyfrost.oneconfig.api.ui.v1.ExperimentalOneConfigUiApi
import org.polyfrost.oneconfig.api.ui.v1.a11y.isKeyboardMode
import org.polyfrost.oneconfig.api.ui.v1.internal.componentsBridge
import org.polyfrost.oneconfig.api.ui.v1.theme.OneConfigTheme

@ExperimentalOneConfigUiApi
enum class TooltipPlacement {
    AboveCenter,
    AboveStart,
    AboveEnd,
    BelowCenter,
    BelowStart,
    BelowEnd,
}

private val TOOLTIP_GAP = 8.dp
private val TOOLTIP_EDGE = 8.dp
private val TOOLTIP_WRAP = 240.dp
private val TOOLTIP_SHAPE = RoundedCornerShape(8.dp)

private class TooltipPosition(private val placement: TooltipPlacement, private val gap: Int, private val edge: Int) : PopupPositionProvider {
    override fun calculatePosition(anchorBounds: IntRect, windowSize: IntSize, layoutDirection: LayoutDirection, popupContentSize: IntSize): IntOffset {
        val x = when (placement) {
            TooltipPlacement.AboveCenter, TooltipPlacement.BelowCenter -> anchorBounds.center.x - popupContentSize.width / 2
            TooltipPlacement.AboveStart, TooltipPlacement.BelowStart -> anchorBounds.left
            TooltipPlacement.AboveEnd, TooltipPlacement.BelowEnd -> anchorBounds.right - popupContentSize.width
        }
        val above = anchorBounds.top - gap - popupContentSize.height
        val below = anchorBounds.bottom + gap
        val fitsBelow = below + popupContentSize.height <= windowSize.height
        val y = when (placement) {
            TooltipPlacement.AboveCenter, TooltipPlacement.AboveStart, TooltipPlacement.AboveEnd -> if (above >= 0 || !fitsBelow) above.coerceAtLeast(0) else below
            TooltipPlacement.BelowCenter, TooltipPlacement.BelowStart, TooltipPlacement.BelowEnd -> if (fitsBelow || above < 0) below else above
        }
        val maxX = (windowSize.width - popupContentSize.width - edge).coerceAtLeast(edge)
        return IntOffset(x.coerceIn(edge, maxX), y)
    }
}

@ExperimentalOneConfigUiApi
@Composable
fun TooltipPopup(text: String, placement: TooltipPlacement) {
    TooltipPopup(text, placement, TOOLTIP_WRAP)
}

@ExperimentalOneConfigUiApi
@Composable
fun TooltipPopup(text: String, placement: TooltipPlacement, maxWidth: Dp) {
    val density = LocalDensity.current
    val provider = remember(density, placement) {
        TooltipPosition(placement, with(density) { TOOLTIP_GAP.roundToPx() }, with(density) { TOOLTIP_EDGE.roundToPx() })
    }
    componentsBridge().Popup(provider, null, false) {
        Box(
            modifier = componentsBridge().popupSurface(Modifier.widthIn(max = maxWidth), TOOLTIP_SHAPE)
                .padding(horizontal = 12.dp, vertical = 8.dp),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text,
                Modifier,
                OneConfigTheme.current.textColor,
                13.sp,
                null,
                BaselineShift.None,
                FontWeight.Normal,
                TextAlign.Center,
                Int.MAX_VALUE,
                TextOverflow.Clip,
            )
        }
    }
}

@ExperimentalOneConfigUiApi
@Composable
fun TooltipBox(text: String?, modifier: Modifier, content: @Composable () -> Unit) {
    TooltipBox(text, modifier, TooltipPlacement.AboveCenter, TOOLTIP_WRAP, content)
}

@ExperimentalOneConfigUiApi
@Composable
fun TooltipBox(text: String?, modifier: Modifier, placement: TooltipPlacement, maxWidth: Dp, content: @Composable () -> Unit) {
    val source = remember { MutableInteractionSource() }
    val hovered by source.collectIsHoveredAsState()
    var focused by remember { mutableStateOf(false) }
    val keyboard = isKeyboardMode()
    Box(modifier.hoverable(source).onFocusChanged { focused = it.hasFocus }) {
        content()
        if (text != null && (hovered || focused && keyboard)) TooltipPopup(text, placement, maxWidth)
    }
}
