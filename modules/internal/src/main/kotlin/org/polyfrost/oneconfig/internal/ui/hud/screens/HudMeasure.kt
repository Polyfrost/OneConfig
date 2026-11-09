package org.polyfrost.oneconfig.internal.ui.hud.screens

import androidx.compose.runtime.Composable
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import kotlin.math.abs
import kotlin.math.roundToInt
import org.polyfrost.compose.backend.drawPoly
import org.polyfrost.compose.render.PolyFont
import org.polyfrost.oneconfig.internal.ui.themes.ThemeRegistry
import org.polyfrost.oneconfig.internal.ui.themes.cornerRadiusOrNull

internal val hudMeasureColor = Color(0xFFF24822)
internal val hudSpacingColor = Color(0xFFFF3EA5)
private val hudSideGapLineColor = Color.White.copy(alpha = 0.45f)
private val hudSideGapBadgeColor = Color(0xE6202020)
private val hudGridColor = Color.White.copy(alpha = 0.07f)
private val hudMeasureDashEffect = PathEffect.dashPathEffect(floatArrayOf(3f, 3f), 0f)

private const val MEASURE_EPSILON = 0.05f
private const val MEASURE_TICK_PX = 3f
private const val MEASURE_BADGE_HEIGHT = 14f
private const val MEASURE_BADGE_PADDING = 4f

internal class HudMeasureLine(val from: Offset, val to: Offset, val value: Float)

internal class HudMeasurement(
    val target: Rect,
    val lines: List<HudMeasureLine>,
    val extensions: List<Pair<Offset, Offset>>,
)

internal fun formatHudPx(value: Float): String {
    val tenths = (abs(value) * 10f).roundToInt()
    return if (tenths % 10 == 0) (tenths / 10).toString() else "${tenths / 10}.${tenths % 10}"
}

internal fun hudSideGaps(rect: Rect, obstacles: List<Rect>, screen: Rect): List<HudMeasureLine> {
    var left: Rect? = null
    var right: Rect? = null
    var top: Rect? = null
    var bottom: Rect? = null
    for (o in obstacles) {
        val rowOverlap = o.top < rect.bottom && rect.top < o.bottom
        val columnOverlap = o.left < rect.right && rect.left < o.right
        if (rowOverlap) {
            if (o.right <= rect.left + MEASURE_EPSILON && (left == null || o.right > left.right)) left = o
            if (o.left >= rect.right - MEASURE_EPSILON && (right == null || o.left < right.left)) right = o
        }
        if (columnOverlap) {
            if (o.bottom <= rect.top + MEASURE_EPSILON && (top == null || o.bottom > top.bottom)) top = o
            if (o.top >= rect.bottom - MEASURE_EPSILON && (bottom == null || o.top < bottom.top)) bottom = o
        }
    }
    fun rowY(o: Rect?) = if (o == null) rect.center.y else (maxOf(o.top, rect.top) + minOf(o.bottom, rect.bottom)) / 2f
    fun columnX(o: Rect?) = if (o == null) rect.center.x else (maxOf(o.left, rect.left) + minOf(o.right, rect.right)) / 2f
    val lines = ArrayList<HudMeasureLine>(4)
    fun add(from: Offset, to: Offset) {
        val value = (to - from).getDistance()
        if (value > MEASURE_EPSILON && (to.x >= from.x && to.y >= from.y)) lines.add(HudMeasureLine(from, to, value))
    }
    val ly = rowY(left)
    add(Offset(left?.right ?: screen.left, ly), Offset(rect.left, ly))
    val ry = rowY(right)
    add(Offset(rect.right, ry), Offset(right?.left ?: screen.right, ry))
    val tx = columnX(top)
    add(Offset(tx, top?.bottom ?: screen.top), Offset(tx, rect.top))
    val bx = columnX(bottom)
    add(Offset(bx, rect.bottom), Offset(bx, bottom?.top ?: screen.bottom))
    return lines
}

internal fun hudMeasurement(a: Rect, b: Rect): HudMeasurement {
    val lines = ArrayList<HudMeasureLine>(4)
    val extensions = ArrayList<Pair<Offset, Offset>>(2)
    fun add(from: Offset, to: Offset) {
        val value = (to - from).getDistance()
        if (value > MEASURE_EPSILON) lines.add(HudMeasureLine(from, to, value))
    }
    val xOverlap = a.left < b.right && b.left < a.right
    val yOverlap = a.top < b.bottom && b.top < a.bottom
    if (xOverlap && yOverlap) {
        val cy = a.center.y
        val cx = a.center.x
        add(Offset(minOf(a.left, b.left), cy), Offset(maxOf(a.left, b.left), cy))
        add(Offset(minOf(a.right, b.right), cy), Offset(maxOf(a.right, b.right), cy))
        add(Offset(cx, minOf(a.top, b.top)), Offset(cx, maxOf(a.top, b.top)))
        add(Offset(cx, minOf(a.bottom, b.bottom)), Offset(cx, maxOf(a.bottom, b.bottom)))
        return HudMeasurement(b, lines, extensions)
    }
    if (!xOverlap) {
        val y = if (yOverlap) (maxOf(a.top, b.top) + minOf(a.bottom, b.bottom)) / 2f else a.center.y
        val bEdge = if (a.right <= b.left) b.left else b.right
        val aEdge = if (a.right <= b.left) a.right else a.left
        add(Offset(minOf(aEdge, bEdge), y), Offset(maxOf(aEdge, bEdge), y))
        if (!yOverlap) {
            val toY = if (y < b.top) b.top else b.bottom
            extensions.add(Offset(bEdge, y) to Offset(bEdge, toY))
        }
    }
    if (!yOverlap) {
        val x = if (xOverlap) (maxOf(a.left, b.left) + minOf(a.right, b.right)) / 2f else a.center.x
        val bEdge = if (a.bottom <= b.top) b.top else b.bottom
        val aEdge = if (a.bottom <= b.top) a.bottom else a.top
        add(Offset(x, minOf(aEdge, bEdge)), Offset(x, maxOf(aEdge, bEdge)))
        if (!xOverlap) {
            val toX = if (x < b.left) b.left else b.right
            extensions.add(Offset(x, bEdge) to Offset(toX, bEdge))
        }
    }
    return HudMeasurement(b, lines, extensions)
}

@Composable
internal fun hudBadgeRadius(): Dp = minOf(3.dp, ThemeRegistry.activeTheme?.buttonShape?.cornerRadiusOrNull() ?: 3.dp)

private fun DrawScope.drawHudPxBadge(label: String, center: Offset, color: Color, badgeRadius: Dp) {
    val font = PolyFont.default(9f)
    val metrics = font.metrics
    val textWidth = font.measureTextWidth(label)
    val textHeight = metrics.descent - metrics.ascent
    val width = textWidth + MEASURE_BADGE_PADDING * 2f
    val left = (center.x - width / 2f).coerceIn(0f, (size.width - width).coerceAtLeast(0f))
    val top = (center.y - MEASURE_BADGE_HEIGHT / 2f).coerceIn(0f, (size.height - MEASURE_BADGE_HEIGHT).coerceAtLeast(0f))
    drawRoundRect(
        color = color,
        topLeft = Offset(left, top),
        size = Size(width, MEASURE_BADGE_HEIGHT),
        cornerRadius = CornerRadius(badgeRadius.toPx()),
    )
    val textX = left + MEASURE_BADGE_PADDING
    val textY = top + (MEASURE_BADGE_HEIGHT - textHeight) / 2f - metrics.ascent
    drawPoly { it.renderer.text(label, textX, textY, Color.White.toArgb(), font) }
}

private fun DrawScope.drawHudDimension(
    from: Offset,
    to: Offset,
    value: Float,
    lineColor: Color,
    badgeColor: Color,
    mcToScreen: Float,
    badgeRadius: Dp,
) {
    val start = from * mcToScreen
    val end = to * mcToScreen
    drawLine(lineColor, start, end, strokeWidth = 1f)
    val horizontal = abs(end.y - start.y) < abs(end.x - start.x)
    val tick = if (horizontal) Offset(0f, MEASURE_TICK_PX) else Offset(MEASURE_TICK_PX, 0f)
    drawLine(lineColor, start - tick, start + tick, strokeWidth = 1f)
    drawLine(lineColor, end - tick, end + tick, strokeWidth = 1f)
    drawHudPxBadge(formatHudPx(value), (start + end) / 2f, badgeColor, badgeRadius)
}

internal fun DrawScope.drawHudMeasurement(measurement: HudMeasurement, mcToScreen: Float, badgeRadius: Dp) {
    val t = measurement.target
    drawRect(
        color = hudMeasureColor,
        topLeft = Offset(t.left * mcToScreen, t.top * mcToScreen),
        size = Size(t.width * mcToScreen, t.height * mcToScreen),
        style = Stroke(width = 1f),
    )
    for ((from, to) in measurement.extensions) {
        drawLine(hudMeasureColor, from * mcToScreen, to * mcToScreen, strokeWidth = 1f, pathEffect = hudMeasureDashEffect)
    }
    for (line in measurement.lines) {
        drawHudDimension(line.from, line.to, line.value, hudMeasureColor, hudMeasureColor, mcToScreen, badgeRadius)
    }
}

internal fun DrawScope.drawHudDragFeedback(result: HudSnapResult, mcToScreen: Float, badgeRadius: Dp) {
    for (guide in result.guides) {
        val p = guide.position * mcToScreen
        val a = guide.start * mcToScreen
        val b = guide.end * mcToScreen
        if (guide.vertical) drawLine(hudMeasureColor, Offset(p, a), Offset(p, b), strokeWidth = 1f)
        else drawLine(hudMeasureColor, Offset(a, p), Offset(b, p), strokeWidth = 1f)
    }
    for (line in result.sideGaps) {
        val start = line.from * mcToScreen
        val end = line.to * mcToScreen
        drawLine(hudSideGapLineColor, start, end, strokeWidth = 1f, pathEffect = hudMeasureDashEffect)
        drawHudPxBadge(formatHudPx(line.value), (start + end) / 2f, hudSideGapBadgeColor, badgeRadius)
    }
    for (gap in result.gaps) {
        drawHudDimension(gap.from, gap.to, gap.size, hudSpacingColor, hudSpacingColor, mcToScreen, badgeRadius)
    }
}

internal fun DrawScope.drawHudGrid(step: Float, mcToScreen: Float) {
    val px = step * mcToScreen
    if (px < 2f) return
    var x = px
    while (x < size.width) {
        drawLine(hudGridColor, Offset(x, 0f), Offset(x, size.height), strokeWidth = 1f)
        x += px
    }
    var y = px
    while (y < size.height) {
        drawLine(hudGridColor, Offset(0f, y), Offset(size.width, y), strokeWidth = 1f)
        y += px
    }
}
