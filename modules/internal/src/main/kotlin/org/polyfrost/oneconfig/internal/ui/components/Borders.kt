package org.polyfrost.oneconfig.internal.ui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.ScrollState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.geometry.isSimple
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Outline
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.drawOutline
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import kotlin.math.ceil
import kotlin.math.min
import org.polyfrost.oneconfig.internal.ui.themes.AccentBrush

fun Modifier.animatedFrame(
    shape: Shape,
    fill: () -> Color = { Color.Transparent },
    accentAlpha: () -> Float = { 0f },
    borderWidth: Dp = 1.dp,
    border: () -> Color,
) = drawWithCache {
    val outline = shape.createOutline(size, layoutDirection, this)
    val strokeWidth = min(ceil(borderWidth.toPx()), ceil(size.minDimension / 2f))
    val half = strokeWidth / 2f
    val stroke = Stroke(strokeWidth)
    val inset = Size(size.width - strokeWidth, size.height - strokeWidth)
    val accent = AccentBrush
    onDrawWithContent {
        val f = fill()
        if (f.alpha > 0f) drawOutline(outline, f)
        val a = accentAlpha()
        if (a > 0f) drawOutline(outline, accent, a)
        drawContent()
        val b = border()
        if (b.alpha <= 0f) return@onDrawWithContent
        val rr = (outline as? Outline.Rounded)?.roundRect
        when {
            outline is Outline.Rectangle -> drawRect(b, Offset(half, half), inset, style = stroke)
            rr != null && rr.isSimple && rr.topLeftCornerRadius.x >= half ->
                drawRoundRect(b, Offset(half, half), inset, CornerRadius(rr.topLeftCornerRadius.x - half, rr.topLeftCornerRadius.y - half), stroke)
            else -> drawOutline(outline, b, style = stroke)
        }
    }
}

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