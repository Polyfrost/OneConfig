@file:OptIn(ExperimentalOneConfigUiApi::class)

package org.polyfrost.oneconfig.api.ui.v1.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntRect
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.window.PopupPositionProvider
import org.polyfrost.oneconfig.api.ui.v1.ExperimentalOneConfigUiApi
import org.polyfrost.oneconfig.api.ui.v1.a11y.notFocusable

@Composable
internal fun Modifier.swallowClicks(): Modifier =
    notFocusable().clickable(interactionSource = remember { MutableInteractionSource() }, indication = null) {}

internal class PointPosition(private val at: IntOffset, private val relative: Boolean) : PopupPositionProvider {
    override fun calculatePosition(anchorBounds: IntRect, windowSize: IntSize, layoutDirection: LayoutDirection, popupContentSize: IntSize): IntOffset {
        val point = if (relative) anchorBounds.topLeft + at else at
        val x = if (point.x + popupContentSize.width <= windowSize.width) point.x else point.x - popupContentSize.width
        val y = if (point.y + popupContentSize.height <= windowSize.height) point.y else point.y - popupContentSize.height
        return IntOffset(
            x.coerceIn(0, (windowSize.width - popupContentSize.width).coerceAtLeast(0)),
            y.coerceIn(0, (windowSize.height - popupContentSize.height).coerceAtLeast(0)),
        )
    }
}
