package org.polyfrost.oneconfig.internal.ui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.ScrollState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

fun Modifier.topBorder(
    color: Color,
    height: Float,
) = this.drawWithContent {
    drawContent()
    drawLine(
        color = color,
        start = Offset(0f, 0f),
        end = Offset(size.width, 0f),
        strokeWidth = height,
    )
}

fun Modifier.rightBorder(
    color: Color,
    width: Float,
) = this.drawWithContent {
    drawContent()
    drawLine(
        color = color,
        start = Offset(size.width, 0f),
        end = Offset(size.width, size.height),
        strokeWidth = width,
    )
}

fun Modifier.bottomBorder(
    color: Color,
    height: Float,
) = this.drawWithContent {
    drawContent()
    drawLine(
        color = color,
        start = Offset(0f, size.height),
        end = Offset(size.width, size.height),
        strokeWidth = height,
    )
}

fun Modifier.leftBorder(
    color: Color,
    width: Float,
) = this.drawWithContent {
    drawContent()
    drawLine(
        color = color,
        start = Offset(0f, 0f),
        end = Offset(0f, size.height),
        strokeWidth = width,
    )
}

/**
 * Draws a [color]-to-transparent scrim at the top and bottom edge of a scroll viewport to show more
 * content is available in that direction
 *
 * Each edge fades in only when the content can scroll that way
 *
 * Place on the same node that owns the [verticalScroll] modifier and after it
 */
@Composable
fun Modifier.fadingEdges(
    scrollState: ScrollState,
    color: Color,
    length: Dp = 32.dp,
): Modifier {
    val topAlpha by animateFloatAsState(if (scrollState.canScrollBackward) 1f else 0f)
    val bottomAlpha by animateFloatAsState(if (scrollState.canScrollForward) 1f else 0f)
    return this.drawWithCache {
        val lengthPx = length.toPx().coerceAtMost(size.height / 2f)
        val top = Brush.verticalGradient(
            colors = listOf(color, Color.Transparent),
            startY = 0f,
            endY = lengthPx,
        )
        val bottom = Brush.verticalGradient(
            colors = listOf(Color.Transparent, color),
            startY = size.height - lengthPx,
            endY = size.height,
        )
        val edge = Size(size.width, lengthPx)
        onDrawWithContent {
            drawContent()
            if (topAlpha > 0f) drawRect(brush = top, size = edge, alpha = topAlpha)
            if (bottomAlpha > 0f) drawRect(
                brush = bottom,
                topLeft = Offset(0f, size.height - lengthPx),
                size = edge,
                alpha = bottomAlpha,
            )
        }
    }
}