@file:OptIn(ExperimentalOneConfigUiApi::class)

package org.polyfrost.oneconfig.api.ui.v1.components

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalWindowInfo
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntRect
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.PopupPositionProvider
import org.polyfrost.oneconfig.api.ui.v1.ExperimentalOneConfigUiApi

@ExperimentalOneConfigUiApi
public enum class Placement {
    BelowStart,
    BelowCenter,
    BelowEnd,
    AboveStart,
    AboveCenter,
    AboveEnd,

    Start,

    End,
}

@ExperimentalOneConfigUiApi
@Composable
public fun rememberAnchoredPosition(placement: Placement = Placement.BelowStart, gap: Dp = 6.dp, flip: Boolean = true): PopupPositionProvider {
    val gapPx = with(LocalDensity.current) { gap.roundToPx() }
    return remember(placement, gapPx, flip) { AnchoredPositionProvider(placement, gapPx, flip) }
}

@ExperimentalOneConfigUiApi
@Composable
public fun popupMaxHeight(margin: Dp = 16.dp): Dp {
    val density = LocalDensity.current
    val window = LocalWindowInfo.current.containerSize
    return with(density) { (window.height.toDp() - margin * 2).coerceAtLeast(48.dp) }
}

internal class AnchoredPositionProvider(
    private val placement: Placement,
    private val gap: Int,
    private val flip: Boolean,
) : PopupPositionProvider {
    override fun calculatePosition(anchorBounds: IntRect, windowSize: IntSize, layoutDirection: LayoutDirection, popupContentSize: IntSize): IntOffset {
        val w = popupContentSize.width
        val h = popupContentSize.height
        val rtl = layoutDirection == LayoutDirection.Rtl
        val (x, y) = when (placement) {
            Placement.Start, Placement.End -> {
                val endSide = (placement == Placement.End) != rtl
                val after = anchorBounds.right + gap
                val before = anchorBounds.left - gap - w
                val fitsAfter = after + w <= windowSize.width
                val fitsBefore = before >= 0
                val x = when {
                    endSide && (fitsAfter || !flip || !fitsBefore) -> after
                    endSide -> before
                    fitsBefore || !flip || !fitsAfter -> before
                    else -> after
                }
                x to anchorBounds.top
            }
            else -> {
                val above = placement == Placement.AboveStart || placement == Placement.AboveCenter || placement == Placement.AboveEnd
                val startX = if (rtl) anchorBounds.right - w else anchorBounds.left
                val endX = if (rtl) anchorBounds.left else anchorBounds.right - w
                val x = when (placement) {
                    Placement.BelowStart, Placement.AboveStart -> startX
                    Placement.BelowEnd, Placement.AboveEnd -> endX
                    else -> anchorBounds.left + (anchorBounds.width - w) / 2
                }
                val belowY = anchorBounds.bottom + gap
                val aboveY = anchorBounds.top - gap - h
                val fitsBelow = belowY + h <= windowSize.height
                val fitsAbove = aboveY >= 0
                val y = when {
                    !above && (fitsBelow || !flip || !fitsAbove) -> belowY
                    !above -> aboveY
                    fitsAbove || !flip || !fitsBelow -> aboveY
                    else -> belowY
                }
                x to y
            }
        }
        return IntOffset(
            x.coerceIn(0, (windowSize.width - w).coerceAtLeast(0)),
            y.coerceIn(0, (windowSize.height - h).coerceAtLeast(0)),
        )
    }
}
