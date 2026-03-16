package org.polyfrost.oneconfig.internal.ui.hud.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.hoverable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsHoveredAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.*
import androidx.compose.runtime.snapshots.Snapshot
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.PointerEventType
import androidx.compose.ui.input.pointer.onPointerEvent
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import org.polyfrost.compose.layout.PolyAlign
import org.polyfrost.oneconfig.internal.ui.themes.Accent
import org.polyfrost.oneconfig.internal.ui.themes.LocalTheme

private val alignmentGrid: Map<PolyAlign, Pair<Int, Int>> = mapOf(
    PolyAlign.TopLeft     to Pair(0, 0),
    PolyAlign.Top         to Pair(1, 0),
    PolyAlign.TopRight    to Pair(2, 0),
    PolyAlign.Left        to Pair(0, 1),
    PolyAlign.Center      to Pair(1, 1),
    PolyAlign.Right       to Pair(2, 1),
    PolyAlign.BottomLeft  to Pair(0, 2),
    PolyAlign.Bottom      to Pair(1, 2),
    PolyAlign.BottomRight to Pair(2, 2),
)

private val gridAlignment: Map<Pair<Int, Int>, PolyAlign> =
    alignmentGrid.entries.associate { (k, v) -> v to k }

@Composable
fun AlignmentPicker(
    alignment: PolyAlign,
    onChange: (PolyAlign) -> Unit
) {
    val theme = LocalTheme.current
    val accentColor = Accent
    val activeColor = theme.textColor
    val inactiveColor = theme.textColorSecondary

    var hoveredCell by remember { mutableStateOf<Pair<Int, Int>?>(null) }

    val totalSize = 125.dp
    val cellSize = totalSize / 3f

    Box(
        modifier = Modifier
            .size(totalSize)
            .clip(RoundedCornerShape(8.dp))
            .background(Color(0xFF192126).copy(alpha = 0.7f), RoundedCornerShape(8.dp))
            .border(1.dp, theme.borderColor, RoundedCornerShape(8.dp))
    ) {
        for (row in 0..2) {
            for (col in 0..2) {
                val cellAlign = gridAlignment[Pair(col, row)] ?: continue
                val isSelected = alignment == cellAlign
                val isHovered = hoveredCell == Pair(col, row)

                AlignmentCell(
                    col = col,
                    row = row,
                    cellSize = cellSize,
                    isSelected = isSelected,
                    isHovered = isHovered,
                    accentColor = accentColor,
                    activeColor = activeColor,
                    inactiveColor = inactiveColor,
                    onHover = { hov -> hoveredCell = if (hov) Pair(col, row) else null },
                    onClick = {
                        Snapshot.withMutableSnapshot {
                            onChange(cellAlign)
                        }
                    }
                )
            }
        }
    }
}

@OptIn(ExperimentalComposeUiApi::class)
@Composable
private fun AlignmentCell(
    col: Int,
    row: Int,
    cellSize: Dp,
    isSelected: Boolean,
    isHovered: Boolean,
    accentColor: Color,
    activeColor: Color,
    inactiveColor: Color,
    onHover: (Boolean) -> Unit,
    onClick: () -> Unit
) {
    val targetColor = when {
        isSelected -> accentColor
        isHovered  -> activeColor
        else       -> inactiveColor
    }

    val animatedAlpha = remember { Animatable(0f) }
    val animatedScale = remember { Animatable(1f) }

    LaunchedEffect(isSelected) {
        if (isSelected) {
            animatedScale.animateTo(
                1.15f,
                spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessMedium)
            )
            animatedScale.animateTo(1f, tween(150))
        }
    }

    LaunchedEffect(isHovered) {
        animatedAlpha.animateTo(
            if (isHovered && !isSelected) 0.08f else 0f,
            tween(150)
        )
    }

    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier
            .absoluteOffset(x = cellSize * col, y = cellSize * row)
            .size(cellSize)
            .onPointerEvent(PointerEventType.Enter) { onHover(true) }
            .onPointerEvent(PointerEventType.Exit)  { onHover(false) }
            .onPointerEvent(PointerEventType.Press) { event ->
                event.changes.forEach { it.consume() }
                onClick()
            }
    ) {
        if (animatedAlpha.value > 0f) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(activeColor.copy(alpha = animatedAlpha.value))
            )
        }

        CellIndicator(
            col = col,
            row = row,
            isSelected = isSelected,
            isHovered = isHovered,
            scale = animatedScale.value,
            color = targetColor,
            cellSize = cellSize
        )
    }
}

@Composable
private fun CellIndicator(
    col: Int,
    row: Int,
    isSelected: Boolean,
    isHovered: Boolean,
    scale: Float,
    color: Color,
    cellSize: Dp
) {
    val isCenter = col == 1 && row == 1
    val isCorner = (col == 0 || col == 2) && (row == 0 || row == 2)

    Box(
        modifier = Modifier
            .size(cellSize)
            .drawBehind {
                val s = size
                val strokeWidth = (1.5f * scale).dp.toPx()
                val len = (8f * scale).dp.toPx()

                when {
                    isCenter -> drawCenterPlus(color, s, strokeWidth * 1.2f, len * 1.6f)
                    isCorner -> drawCornerBracket(col, row, color, s, strokeWidth, len)
                    else -> drawEdgeTick(col, row, color, s, strokeWidth, len)
                }
            }
    )
}

private fun DrawScope.drawCenterPlus(
    color: Color,
    s: Size,
    strokeWidth: Float,
    armLen: Float
) {
    val cx = s.width / 2f
    val cy = s.height / 2f
    val arm = armLen / 2f

    drawLine(
        color = color,
        start = Offset(cx - arm, cy),
        end = Offset(cx + arm, cy),
        strokeWidth = strokeWidth,
        cap = StrokeCap.Round
    )
    drawLine(
        color = color,
        start = Offset(cx, cy - arm),
        end = Offset(cx, cy + arm),
        strokeWidth = strokeWidth,
        cap = StrokeCap.Round
    )

    val boxSize = armLen * 1.55f
    drawRoundRect(
        color = color.copy(alpha = 0.13f),
        topLeft = Offset(cx - boxSize / 2f, cy - boxSize / 2f),
        size = Size(boxSize, boxSize),
        cornerRadius = CornerRadius(6f),
        style = Stroke(width = strokeWidth * 0.8f)
    )
}

private fun DrawScope.drawCornerBracket(
    col: Int,
    row: Int,
    color: Color,
    s: Size,
    strokeWidth: Float,
    len: Float
) {
    val pad = s.width * 0.22f
    val cx = if (col == 0) pad else s.width - pad
    val cy = if (row == 0) pad else s.height - pad
    val hDir = if (col == 0) 1f else -1f
    val vDir = if (row == 0) 1f else -1f

    drawLine(
        color = color,
        start = Offset(cx, cy),
        end = Offset(cx + hDir * len, cy),
        strokeWidth = strokeWidth,
        cap = StrokeCap.Round
    )
    drawLine(
        color = color,
        start = Offset(cx, cy),
        end = Offset(cx, cy + vDir * len),
        strokeWidth = strokeWidth,
        cap = StrokeCap.Round
    )
}

private fun DrawScope.drawEdgeTick(
    col: Int,
    row: Int,
    color: Color,
    s: Size,
    strokeWidth: Float,
    len: Float
) {
    val cx = s.width / 2f
    val cy = s.height / 2f
    val isHorizontalEdge = row == 1
    val isTopOrLeft = col == 0 || row == 0

    if (isHorizontalEdge) {
        val tipX = if (isTopOrLeft) cx + len * 0.5f else cx - len * 0.5f
        val baseX = if (isTopOrLeft) cx - len * 0.3f else cx + len * 0.3f

        drawLine(
            color = color,
            start = Offset(baseX, cy),
            end = Offset(tipX, cy),
            strokeWidth = strokeWidth,
            cap = StrokeCap.Round
        )
        drawLine(
            color = color,
            start = Offset(tipX, cy - len * 0.5f),
            end = Offset(tipX, cy + len * 0.5f),
            strokeWidth = strokeWidth,
            cap = StrokeCap.Round
        )
    } else {
        val tipY = if (isTopOrLeft) cy + len * 0.5f else cy - len * 0.5f
        val baseY = if (isTopOrLeft) cy - len * 0.3f else cy + len * 0.3f

        drawLine(
            color = color,
            start = Offset(cx, baseY),
            end = Offset(cx, tipY),
            strokeWidth = strokeWidth,
            cap = StrokeCap.Round
        )
        drawLine(
            color = color,
            start = Offset(cx - len * 0.5f, tipY),
            end = Offset(cx + len * 0.5f, tipY),
            strokeWidth = strokeWidth,
            cap = StrokeCap.Round
        )
    }
}