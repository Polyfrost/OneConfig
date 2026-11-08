@file:OptIn(ExperimentalOneConfigUiApi::class)

package org.polyfrost.oneconfig.api.ui.v1.components

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntRect
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.PopupPositionProvider
import org.polyfrost.oneconfig.api.ui.v1.ExperimentalOneConfigUiApi
import org.polyfrost.oneconfig.api.ui.v1.a11y.Announce
import org.polyfrost.oneconfig.api.ui.v1.internal.componentsBridge

private val POPUP_GAP = 6.dp
private val POPUP_BUTTON_SIZE = 32.dp

private class AnchoredPosition(private val alignment: Alignment.Horizontal, private val gap: Int) : PopupPositionProvider {
    override fun calculatePosition(anchorBounds: IntRect, windowSize: IntSize, layoutDirection: LayoutDirection, popupContentSize: IntSize): IntOffset {
        val x = when (alignment) {
            Alignment.Start -> anchorBounds.left
            Alignment.CenterHorizontally -> anchorBounds.center.x - popupContentSize.width / 2
            else -> anchorBounds.right - popupContentSize.width
        }
        val below = anchorBounds.bottom + gap
        val above = anchorBounds.top - gap - popupContentSize.height
        val y = if (below + popupContentSize.height <= windowSize.height || above < 0) below else above
        return IntOffset(
            x.coerceIn(0, (windowSize.width - popupContentSize.width).coerceAtLeast(0)),
            y.coerceIn(0, (windowSize.height - popupContentSize.height).coerceAtLeast(0)),
        )
    }
}

@ExperimentalOneConfigUiApi
@Composable
fun AnchoredPopupButton(icon: String, tooltip: String, content: @Composable (close: () -> Unit) -> Unit) {
    AnchoredPopupButton(icon, tooltip, Modifier, Alignment.End, POPUP_GAP, content)
}

@ExperimentalOneConfigUiApi
@Composable
fun AnchoredPopupButton(
    icon: String,
    tooltip: String,
    modifier: Modifier,
    alignment: Alignment.Horizontal,
    gap: Dp,
    content: @Composable (close: () -> Unit) -> Unit,
) {
    var open by remember { mutableStateOf(false) }
    val gapPx = with(LocalDensity.current) { gap.roundToPx() }
    val provider = remember(alignment, gapPx) { AnchoredPosition(alignment, gapPx) }
    Box(modifier) {
        TooltipBox(if (open) null else tooltip, Modifier, TooltipPlacement.BelowCenter, 240.dp) {
            IconButton(icon, Modifier.size(POPUP_BUTTON_SIZE)) { open = !open }
        }
        if (open) {
            EscapeCloses { open = false }
            componentsBridge().Popup(provider, { open = false }, true) {
                Announce(tooltip)
                content { open = false }
            }
        }
    }
}
